# Changelog

## Unreleased

- **SupabaseChannel:** the `Private` property makes a channel private: only users allowed by policies on `realtime.messages` can join it, send broadcasts and track presence. A refused join fires `Error` with `channel_refused`.
- **SupabaseChannel:** `StatusChanged` no longer misses `Subscribing` when subscribing again while connected.
- The Live Notes sample uses a private channel; its `schema.sql` adds the policies.

## 1.1.0 (2026-10-14)

- **Supabase:** `UpdateUser` changes the email, password or metadata, and turns an anonymous user into a full account (`UserUpdated`). `ResendConfirmation` and `Reauthenticate` send their emails (`MessageSent`).
- **SupabaseDatabase:** `Query` and `QueryPage` take the query as a dictionary with keys from the `QueryField` dropdown, checked before it's sent, with errors that say where the mistake is. `QueryPage` also returns the total count (`GotPage`). `OrderBy` takes several columns, each with `asc` or `desc`. `SetQueryField`, `InsertOrSkip`, and the `Schema` property for tables outside `public`. `Select` is deprecated: hidden from the drawer, still working in existing projects.
- **SupabaseStorage:** `SearchFiles` with a search dictionary (`SearchField`: search, sort, limit, offset), `SetSearchField`, `FileInfo`, `SignedUploadUrl` and `UploadWithToken`.
- **SupabaseChannel:** `PresenceState`, everyone on the channel right now.
- **SupabaseFunctions:** the `Region` property runs functions in a chosen region.
- The Live Notes sample reads notes with `Query`.
- [Blocks](docs/blocks.md) shows every block with its image.

## 1.0.1 (2026-10-14)

- The extension is now licensed under MPL-2.0 instead of GPL-3.0: apps that use it can keep their code private, while changes to the extension itself stay open.
- `LiveNotes.aia` contains only the project's own files.
- No changes to blocks or behavior.

## 1.0.0 (2026-10-13)

First release, with five components:

- **Supabase:** connection, sign-up, sign-in with email and password or anonymously, sign-out, and a session kept between app starts.
- **SupabaseDatabase:** select with filters, ordering and paging, count, insert, upsert, update, delete, and database functions.
- **SupabaseStorage:** upload, download, list, move, copy and delete files, and signed and public links.
- **SupabaseChannel:** database changes, broadcast and presence.
- **SupabaseFunctions:** Edge Function calls.

Includes the Live Notes sample project.
