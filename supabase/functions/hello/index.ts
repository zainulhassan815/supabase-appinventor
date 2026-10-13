import "@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "@supabase/server";

export default {
  fetch: withSupabase({ auth: ["publishable"] }, async (req) => {
    const { name } = await req.json();
    return Response.json({ message: `Hello ${name}!` });
  }),
};
