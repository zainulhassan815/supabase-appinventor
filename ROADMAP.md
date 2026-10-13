# Roadmap

## v1.1

Smaller features most apps ask for, and closing a security gap in realtime.

| Area | Feature | Notes |
|---|---|---|
| Realtime | **Private channels** (realtime authorization) | Priority. Today anyone who knows a topic's name can join its broadcast and presence. Database changes are already protected by row-level security. |
| Realtime | `PresenceState` block | Who is on the channel right now, without tracking joins and leaves. |
| Realtime | Listening to every table in a schema | |
| Authentication | One-time codes by email and SMS | Sign in without a password. |
| Authentication | Password reset | By email link or code. |
| Authentication | Updating the user: email, password, metadata | Also turns an anonymous user into a full account. |
| Authentication | Resending the confirmation email, reauthenticating | |
| Database | OR and NOT filters | Today every filter must match. |
| Database | Ordering by several columns, other schemas than `public`, ignoring duplicates on upsert | |
| Database | Rows and the total count in one call | For paging. |
| Storage | Image transforms | Resized and cropped images through their links, for thumbnails. |
| Storage | List options (limit, search, sort), file info, signed upload links | |
| Edge Functions | Choosing the function's region | |
| Testing | Apps with several screens; offline and refresh-failed sessions | |
| Testing | Kodular and other App Inventor based builders | |

## v1.2

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
