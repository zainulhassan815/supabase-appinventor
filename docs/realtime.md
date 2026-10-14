# Realtime

A **SupabaseChannel** is one realtime channel. Use one component per channel. It
does three things:

- **Database changes:** rows added, changed or deleted, as they happen.
- **Broadcast:** messages sent to everyone on the channel, like chat or "is typing".
- **Presence:** who is on the channel, and the state each user shares.

## Setting up

Set `Client` and `Topic`. Users on the same topic share broadcasts and presence.
Database changes reach only users that row-level security allows to see the row.

Before `Subscribe`, say what to receive:

- `ListenToTable(schema, table, event, filter)`: changes to a table. `schema` is
  usually `public`. `event` is a **ChangeEvent** option: `All`, `Insert`, `Update`
  or `Delete`. `filter` is empty, or one filter from SupabaseDatabase's `Filter`
  block using `Equals`, `NotEquals`, `GreaterThan`, `GreaterOrEqual`, `LessThan`,
  `LessOrEqual` or `In`.
- `ListenToBroadcast(event)`: broadcasts with that event name.

Then call `Subscribe`. `StatusChanged` fires with `Subscribing`, then
`Subscribed`. What you listen to stays registered, so after `Unsubscribe` you can
`Subscribe` again. The channel reconnects by itself after a dropped connection.

## Private channels

Anyone with the project's publishable key can join a public channel, if they
know its topic, and read its broadcasts and presence. Set `Private` to true to
let in only the users your policies allow. Policies on `realtime.messages` decide
who may join (`select`) and who may send broadcasts and track presence
(`insert`), and `realtime.topic()` is the channel's topic:

```sql
create policy "Signed-in users join the notes channel" on realtime.messages
  for select to authenticated using ((select realtime.topic()) = 'notes');
create policy "Signed-in users send on the notes channel" on realtime.messages
  for insert to authenticated with check ((select realtime.topic()) = 'notes');
```

A topic per user or per group works too, such as `'room:' || auth.uid()`. Turn off
**Allow public access** in the project's Realtime settings to require private
channels everywhere.

When the server refuses to join, `StatusChanged` fires with `Unsubscribed` and
`Error` with the code `channel_refused`.

Database changes don't need `Private`: row-level security on the table already
decides which changes each user receives.

## Tables

A table sends changes only if it is in the `supabase_realtime` publication:

```sql
alter publication supabase_realtime add table public.todos;
```

## Database changes

`ChangeReceived(table, event, record, oldRecord)`: `record` is the row after the
change, `oldRecord` the row before it, for updates and deletes. Postgres only sends
the old row's primary key unless the table keeps full old rows:

```sql
alter table public.todos replica identity full;
```

A simple way to stay in sync is to reload with `Query` whenever a change arrives.

Changes made before the channel is subscribed don't arrive, and when the realtime
server has just started, changes made in the first seconds after `Subscribed` can
be missed too. Load the data with `Query` once `StatusChanged` reports
`Subscribed`, so nothing made in between is lost.

## Broadcast

`Broadcast(event, payload)` sends a dictionary to everyone on the channel, and
`BroadcastReceived(event, payload)` receives it. `ReceiveOwnBroadcasts` decides
whether the sender receives its own messages too.

## Presence

`Track(state)` shares a dictionary, such as a name or status, under the component's
`PresenceKey` (empty for a random key; the user's id is a good choice). `Untrack`
stops sharing it.

`PresenceChanged(joins, leaves)` fires on every app on the channel, including the
sender. `joins` and `leaves` are dictionaries from presence key to state.

`PresenceState` is everyone on the channel right now, as a dictionary from presence
key to state. Read it in `PresenceChanged` to show who is online; its length is
how many.

## Errors

`Error(operation, code, message)`. See [Errors](errors.md).
