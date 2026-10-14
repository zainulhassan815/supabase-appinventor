# Edge Functions

[Edge Functions](https://supabase.com/docs/guides/functions) run your code on
Supabase's servers. Use them for work an app can't do safely on its own, such as
calling a paid API with a secret key, sending email, or anything that needs the
`service_role` key.

**SupabaseFunctions** calls them as the signed-in user: the function receives the
user's access token, so it knows who called.

## Calling a function

`Invoke(tag, name, body, headers, method)` → `GotResponse(tag, status, body)`

- `body`: a dictionary or list (sent as JSON), text, or empty for none.
- `headers`: a dictionary of extra HTTP headers.
- `method`: a **RequestMethod** option: `Post`, `Get`, `Put`, `Patch` or `Delete`.

`GotResponse` carries the HTTP status and the body: a dictionary or list when the
function answers with JSON, text otherwise.

## Region

Functions run in the region closest to the user. Set the `Region` property, a
dropdown of Supabase's regions, to run them in one region instead, for example
next to the project's database. `Any` keeps the default.

## Example

The sample's [`note-stats`](../supabase/functions/note-stats/index.ts) function
counts notes and words:

```ts
export default {
  async fetch(req: Request) {
    const { notes }: { notes: string[] } = await req.json();
    const words = notes.reduce((total, note) => total + note.split(/\s+/).filter(Boolean).length, 0);
    return Response.json({ count: notes.length, words });
  },
};
```

Deploy it with the [Supabase CLI](https://supabase.com/docs/guides/cli):

```
supabase functions deploy note-stats --project-ref <project-ref>
```

## Errors

An answer with an error status fires `Error(tag, operation, code, message)` with
the code `http_` and the status, like `http_404` when the function doesn't exist.
See [Errors](errors.md).
