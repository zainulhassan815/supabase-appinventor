# Supabase for App Inventor

A [Supabase](https://supabase.com) client extension for MIT App Inventor and App
Inventor based platforms. It gives apps a real backend: user accounts, a Postgres
database protected by row-level security, live updates, file storage and server
functions.

Built with [Rush](https://github.com/zainulhassan815/rush-cli) on
[supabase-kt](https://github.com/supabase-community/supabase-kt).

<img src="docs/images/live-notes.png" alt="The Live Notes sample: notes kept live, who is online and typing, and an uploaded photo" width="320">

## Getting started

1. Create a project on [supabase.com](https://supabase.com) (the free plan is enough).
2. Open its **SQL Editor** and run your schema. To try the sample, run
   [`samples/live-notes/schema.sql`](samples/live-notes/schema.sql).
3. Download `dev.dreamerslab.supabase.aix` from the
   [latest release](https://github.com/zainulhassan815/supabase-appinventor/releases/latest)
   and import it in App Inventor (Palette → Extension → Import extension).
4. Add a **Supabase** component and set its `ProjectUrl` and `PublishableKey`
   (Project Settings → API Keys in the dashboard).
5. Add the other components you need and set their `Client` property to
   `Supabase1`.

The [getting started guide](docs/getting-started.md) walks through it step by step,
and `LiveNotes.aia` in the release is a complete app to open and learn from.

## Components

| Component | What it does | Guide |
|---|---|---|
| **Supabase** | Connects to a project and signs users in and out. Keeps the session between app starts. | [Authentication](docs/authentication.md) |
| **SupabaseDatabase** | Selects, counts, inserts, upserts, updates and deletes rows, and calls database functions. | [Database](docs/database.md) |
| **SupabaseStorage** | Uploads, downloads, lists, moves, copies and deletes files in a bucket, and makes links to them. | [Storage](docs/storage.md) |
| **SupabaseChannel** | Receives database changes as they happen, and sends messages and online status between users. | [Realtime](docs/realtime.md) |
| **SupabaseFunctions** | Calls Edge Functions, server code for work an app can't do on its own. | [Edge Functions](docs/functions.md) |

Every block is listed in the [block reference](docs/reference.md).

## How results arrive

Calls to Supabase take time, so their results arrive in **events**, like `Web.GotText`
does, never as the value of the block that started them:

- `SignIn` and the other sign-in blocks answer through `Supabase.SessionChanged`.
- Data blocks take a **tag**, any text you choose, and their events carry it back,
  so you can tell your requests apart: `Select("todos", …)` answers with
  `GotRows("todos", rows)`.
- When a call fails, the component's `Error` event fires with a short code, such as
  `invalid_credentials`, and a message. Without an `Error` handler,
  `Screen.ErrorOccurred` fires instead. See [Errors](docs/errors.md).

Rows, users and messages are dictionaries, and lists of rows are lists of
dictionaries, so App Inventor's dictionary and list blocks work on them directly.

## Security

- Use the **publishable** key (`sb_publishable_…`, or the legacy `anon` key). The
  extension refuses secret and `service_role` keys: anything inside an app can be
  extracted, and those keys bypass every rule.
- Turn on **row-level security** for every table and write policies for what each
  user may read and change. The publishable key alone should reveal nothing. The
  sample's [`schema.sql`](samples/live-notes/schema.sql) shows owner-only policies
  for a table and a storage bucket.
- The session is saved in the app's private storage, like TinyDB. It is not
  encrypted.

## FAQ

**Does it work in the AI Companion?** Yes. Live testing in the Companion, compiled
apps and installed apps all work, and the session survives restarts.

**Which devices?** Android 7.0 and newer.

**Kodular and other builders?** It ships no AARs and adds nothing to the manifest,
so it should work on App Inventor based builders, but only MIT App Inventor has been
tested so far.

**I signed up, but nobody is signed in.** New projects require email
confirmation: `SignedUp` fires, and the user is signed in after clicking the link
in the email. Turn off **Confirm email** (Authentication → Sign In / Providers →
Email) while testing.

**`SessionChanged` fired with `SignedIn` more than once.** It fires whenever the
session changes, which includes refreshing it. Guard one-time setup, such as
subscribing to a channel, with a variable.

**`oldRecord` only has the id.** Postgres only sends the primary key of the old row
unless you run `alter table <name> replica identity full;`.

**Can I sign in with Google, GitHub or a magic link?** Not yet. Browser-based sign
in is planned; see the [roadmap](ROADMAP.md).

## Building

```
rush build
python3 samples/package.py
```

`rush build` writes the extension to `out/dev.dreamerslab.supabase.aix` with its block
reference, and `samples/package.py` packs the sample into `out/LiveNotes.aia`. GitHub
Actions builds both on every push; pushing a tag named after the version in
`rush.yml`, like `v1.0.0`, publishes a release.

The tests run real App Inventor apps on a device against a local Supabase. See
[tests](docs/testing.md).

## License

[GPL-3.0](LICENSE). Apps that include the extension are covered by the GPL too, so
they must be shared under compatible terms if you distribute them.
