import { Hono } from 'hono'
import {cors} from 'hono/cors'
import { UserRoute } from './Routes/User'
const app = new Hono()


app.use('api/*',cors())
app.route('api/v1/users',UserRoute)

app.get('/', (c) => {
  return c.text('Hello Hono!')
})

export default app
