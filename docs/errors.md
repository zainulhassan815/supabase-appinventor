# Errors

When a call fails, the component's `Error` event fires. Data components include the
request's `tag`:

- Supabase and SupabaseChannel: `Error(operation, code, message)`
- SupabaseDatabase, SupabaseStorage and SupabaseFunctions:
  `Error(tag, operation, code, message)`

`operation` is the block that failed, like `SignIn` or `Insert`. Without an `Error`
handler, `Screen.ErrorOccurred` fires instead, with error number 3300.

## Codes

| Code | Meaning |
|---|---|
| `network` | Supabase couldn't be reached. Check the connection and `ProjectUrl`. |
| `invalid_argument` | A block got something it can't use: an empty `ProjectUrl`, a secret key, a missing `Client`, a malformed filter, or `Update`/`Delete` without filters. The message says which. |
| Auth codes | From Supabase Auth, such as `invalid_credentials`, `user_already_exists`, `email_not_confirmed`, `weak_password`, `anonymous_provider_disabled`, `over_request_rate_limit`. |
| `PGRST…` | From PostgREST, such as `PGRST205` (no such table) or `PGRST116` (no rows). |
| Postgres codes | Such as `23505` (duplicate key), `23503` (missing referenced row), `42501` (blocked by row-level security). |
| `http_` + status | Storage and Edge Functions, such as `http_400`, `http_403` or `http_404`. |
| `channel_refused` | The realtime server refused to join a channel: for a private channel, no policy on `realtime.messages` lets the user in. |

Supabase's [error code list](https://supabase.com/docs/guides/api/rest/postgres-error-codes)
explains the database codes.
