# Roadmap

## v1.2

What's left of the smaller features, and closing a security gap in realtime. 1.1.0
shipped the rest; see the [changelog](CHANGELOG.md).

| Area | Feature | Notes |
|---|---|---|
| Realtime | Listening to every table in a schema | Waiting on supabase-kt, which drops the table name from the changes it reports. |
| Storage | Image transforms | Resized and cropped images through their links, for thumbnails. Needs a paid plan on hosted Supabase. |
| Testing | Apps with several screens; offline and refresh-failed sessions | |
| Testing | Kodular and other App Inventor based builders | |

## v1.3

Larger features.

| Area | Feature | Notes |
|---|---|---|
| Authentication | Browser sign-in: Google, GitHub and other OAuth providers, SSO, magic links | Opens the browser and returns to the app through a link, which needs an activity in the extension's manifest. |
| Authentication | Two-factor authentication (TOTP) | Enroll, challenge, verify, list and remove factors. Enrolling gives a QR code to show in an Image. |
| Storage | Upload progress and resumable uploads for large files | |
| Edge Functions | Streaming responses (server-sent events) | For AI chat and other answers that arrive in parts. |
| Database | GraphQL queries | |

## Not planned

| Feature | Why |
|---|---|
| Supabase's Compose sign-in UI | App Inventor apps can't use Jetpack Compose. |
| Native Google and Apple sign-in | Needs newer AndroidX libraries than App Inventor ships. |
| Admin APIs | They need the secret key, which must never be in an app. Use an Edge Function. |
