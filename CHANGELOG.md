# Changelog

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
