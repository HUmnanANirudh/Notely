import { Hono } from "hono";
import { PrismaClient } from "../generated/prisma";
import { PrismaNeon } from "@prisma/adapter-neon";
import { verify } from "hono/jwt";

export const NotesRoute = new Hono<{
  Bindings: {
    DATABASE_URL: string;
    JWT_SECRET: string;
  },
  Variables:{
    UserId:string;
  }
}>();

function getPrismaClient(databaseUrl: string) {
  const adapter = new PrismaNeon({ connectionString: databaseUrl });
  return new PrismaClient({ adapter } as any);
}

NotesRoute.use('/*',async (c,next)=>{
    const Header = c.req.header('Authorization') || "";
    if (!Header.startsWith("Bearer ")) {
        c.status(403);
        return c.json({ msg: "Access Denied: Missing or Invalid Token" });
    }
    const token = Header.split(' ')[1];
   
    try{
        const user = await verify(token,c.env.JWT_SECRET)
        if(user){
        c.set("UserId",String(user.id))
        await next()
    }else{
        c.status(403)
        return c.json({msg:"Acces Denied"})
    }
    }catch(e){
        c.status(403)
        return c.json({error:"Invalid Token"})
    }
})
NotesRoute.get('/all',async (c)=>{
    const prisma = getPrismaClient(c.env.DATABASE_URL);
    const userid = c.get("UserId");
    try{
        const Notes = await prisma.notes.findMany({
            where:{id: userid},
            select:{
                id:true,
                title:true,
                content:true,
                createdAt:true,
                updatedAt:true
            },
            orderBy:{updatedAt:'desc'}
        })
        return c.json({Notes})
    }catch(e){
        c.status(403)
        return c.json({msg:'Something went wrong'})
    }
})
NotesRoute.get('/:id',async (c)=>{
    const prisma =  getPrismaClient(c.env.DATABASE_URL);
    const userId = c.get('UserId');
    const notesid = c.req.param("id");
    try{
        const note = await prisma.notes.findFirst({
            where:{
                id:notesid,
                userId
            }
        })
        if(!note){
            c.status(404)
            return c.json({msg:"Note not found"})
        }
        return c.json({note})
    }catch(e){
        c.status(403)
        return c.json({msg:"Something went wrong"})
    }
})
NotesRoute.post("/create",async (c)=>{
    const prisma = getPrismaClient(c.env.DATABASE_URL);
    const userId = c.get('UserId');
    const Body = await c.req.json();
    try{
        const note = await prisma.notes.create({
            data:{
                title:Body.title,
                content:Body.content,
                userId
            }
        })
        if(!note){
            c.status(404)
            return c.json({msg:"Note was not Created"})
        }
        return c.json({note})
    }catch(e){
        c.status(403)
        return c.json({msg:"Something went wrong"})
    }
})
NotesRoute.put('/:id',async (c)=>{
    const prisma = getPrismaClient(c.env.DATABASE_URL);
    const userId = c.get('UserId');
    const notesId = c.req.param("id");
    const Body = await c.req.json();
    const existingNote = await prisma.notes.findFirst({
        where:{
            id:notesId,
            userId
        }
    })
    if(!existingNote){
        c.status(403)
        return c.json({msg:"Note Doesn't exist"})
    }
    try{
        const UpdatedNote = await prisma.notes.update({
            where:{id:notesId},
            data:{
                title:Body.title,
                content:Body.content
            }
        })
        return c.json({UpdatedNote})
    }catch(e){
        c.status(403)
        return c.json({msg:"Something went wrong"})
    }
})
NotesRoute.delete('/:id',async (c)=>{
    const prisma = getPrismaClient(c.env.DATABASE_URL);
    const userId = c.get('UserId');
    const noteId = c.req.param('id');

    const existingNote = await prisma.notes.findFirst({
        where:{
            id:noteId,
            userId
        }
    })
    if(!existingNote){
        c.status(403)
        return c.json({msg:"Note Doesn't exist"})
    }
    try{
        await prisma.notes.delete({
            where:{id:noteId}
        })
        return c.json({msg:'Note deleted Successfully'})
    }catch(e){
        return c.json({msg:"Something went wrong"})
    }
})
NotesRoute.get('/', async(c)=>{
    const prisma = getPrismaClient(c.env.DATABASE_URL);
    const userId = c.get('UserId');
    const query = c.req.query("q");

    if(!query || query.trim()==""){
        c.status(403)
        return c.json({msg:"Search Query Empty"})
    }
    try{
        const result = await prisma.notes.findFirst({
            where:{
                userId,
                OR:[
                    {
                        title:{
                            contains:query,
                            mode:"insensitive"
                        },
                        content:{
                            contains:query,
                            mode:"insensitive"
                        }
                    }
                ]
            },
            orderBy:{
                updatedAt:"desc"
            }
        })
        return c.json({result})
    }catch(e){
        c.status(403)
        return c.json({msg:"Something went wrong"})
    }
})