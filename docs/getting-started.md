# Getting started

This guide connects an App Inventor app to a new Supabase project and stores its
first row.

## 1. Create the project

1. Sign up at [supabase.com](https://supabase.com) and create a project. Pick a
   region near your users.
2. In **Project Settings → API Keys**, note the **publishable** key
   (`sb_publishable_…`). Your project URL is `https://<project-ref>.supabase.co`,
   shown on the project's home page.

## 2. Create a table

Open the **SQL Editor**, paste this and run it:

```sql
create table public.todos (
  id bigint generated always as identity primary key,
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  title text not null,
  done boolean not null default false
);

alter table public.todos enable row level security;

create policy "Users manage their todos" on public.todos
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
```

`user_id` is filled in with the signed-in user, and the policy lets each user see
and change only their own rows. Without a policy, row-level security hides every
row, which is the safe default.

To follow the sample app instead, run
[`samples/live-notes/schema.sql`](../samples/live-notes/schema.sql).

## 3. Allow sign-ins

In **Authentication → Sign In / Providers**:

- **Email:** while testing, turn off **Confirm email**, so a new account is
  signed in right away.
- **Allow anonymous sign-ins:** turn on to use `SignInAnonymously`, which signs
  users in without an email.

## 4. Set up the app

1. Import `dev.dreamerslab.supabase.aix` (Palette → Extension → Import extension).
2. Drag in a **Supabase** component and set:
   - `ProjectUrl`: `https://<project-ref>.supabase.co`
   - `PublishableKey`: the publishable key
3. Drag in a **SupabaseDatabase** and set its `Client` to `Supabase1`.
4. Add an `EmailBox` TextBox, a `PasswordBox` PasswordTextBox, a `SignInButton`, a
   `TitleBox` TextBox, an `AddButton` and a `ListView`.

## 5. Blocks

Sign in, then load the user's todos:

- **when SignInButton.Click:** call `Supabase1.SignIn` with `EmailBox.Text` and
  `PasswordBox.Text`. (Use `SignUp` once to create the account.)
- **when Supabase1.SessionChanged (status, user):** if `status` = `SignedIn`
  (from the SessionStatus dropdown), call `SupabaseDatabase1.Query` with tag
  `"load"`, table `"todos"`, and as query a **make a dictionary** block with one
  pair: key `QueryField.OrderBy` (from the QueryField dropdown), value `"id"`.
- **when SupabaseDatabase1.GotRows (tag, rows):** if `tag` = `"load"`, set
  `ListView1.Elements` to a list made by mapping each row to
  `look up "title" in row`.

Add a todo and reload:

- **when AddButton.Click:** call `SupabaseDatabase1.Insert` with tag `"add"`,
  table `"todos"`, and rows set to a dictionary with `title` = `TitleBox.Text`.
- In **GotRows**, when `tag` = `"add"`, call `Query` again.

Show problems:

- **when Supabase1.Error** and **when SupabaseDatabase1.Error:** show `message`
  in a Label or a Notifier.

Test it with the AI Companion or a built APK. Next, read
[Authentication](authentication.md) and [Database](database.md), or open
`LiveNotes.aia` from the release to see every component working together.
