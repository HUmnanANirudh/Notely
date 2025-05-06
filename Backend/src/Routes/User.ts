import { Hono } from "hono";
import { PrismaClient } from "../generated/prisma";
import { PrismaNeon } from "@prisma/adapter-neon";
import bcrypt from "bcryptjs";
import { sign } from "hono/jwt";
import { z } from "zod"

const SignUpSchema = z.object({
  Username:z.string(),
  email:z.string().email(),
  password:z.string()
})
const SignInSchema = z.object({
  email:z.string().email(),
  password:z.string()
})
export const UserRoute = new Hono<{
  Bindings: {
    DATABASE_URL: string;
    JWT_SECRET: string;
  };
}>();

function getPrismaClient(databaseUrl: string) {
  const adapter = new PrismaNeon({ connectionString: databaseUrl });
  return new PrismaClient({ adapter } as any);
}

UserRoute.get("/", async (c) => {
  const prisma = getPrismaClient(c.env.DATABASE_URL);
  const userCount = await prisma.user.count();
  return c.json({ userCount });
});

UserRoute.post("/SignUp", async (c) => {
  const prisma = getPrismaClient(c.env.DATABASE_URL);
  const body = await c.req.json();
  const {success} = SignUpSchema.safeParse(body)
  if(!success){
    c.status(403);
    return c.json({msg:"Please Enter a valid Email"})
  }
  const existingUser = await prisma.user.findFirst({
    where: {
      OR: [{ email: body.email }, { Username: body.Username }],
    },
  });

  if (existingUser) {
    c.status(403);
    return c.json({ msg: "User already exists" });
  }

  const hashedPassword = await bcrypt.hash(body.password, 10);

  try {
    const user = await prisma.user.create({
      data: {
        email: body.email,
        Username: body.Username,
        password: hashedPassword,
      },
    });
    const jwt = await sign({ id: user.id }, c.env.JWT_SECRET);
    return c.json({ jwt });
  } catch (e) {
    c.status(500);
    return c.json({ msg: "Something went wrong", error: (e as any).message });
  }
});

UserRoute.post("/SignIn", async (c) => {
  const prisma = getPrismaClient(c.env.DATABASE_URL);
  const body = await c.req.json();
  const {success} = SignInSchema.safeParse(body)
  if(!success){
    c.status(403);
    return c.json({msg:"Please Enter a valid Email id"})
  }
  const user = await prisma.user.findUnique({
    where: {
      email: body.email,
    },
  });

  if (!user) {
    c.status(403);
    return c.json({ msg: "No such user exists" });
  }

  const isValidPassword = await bcrypt.compare(body.password, user.password);
  if (!isValidPassword) {
    c.status(403);
    return c.json({ msg: "Invalid password" });
  }

  const jwt = await sign({ id: user.id }, c.env.JWT_SECRET);
  return c.json({ jwt });
});
