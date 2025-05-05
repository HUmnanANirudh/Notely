import { Hono } from 'hono'
import {cors} from 'hono/cors'
import { UserRoute } from './Routes/User'
import { NotesRoute } from './Routes/Notes'
const app = new Hono()


app.use('api/*',cors())
app.route('api/v1/users',UserRoute)
app.route('api/v1/notes',NotesRoute)

app.get('/', (c) => {
  return c.text('Hello Hono!')
})

export default app
