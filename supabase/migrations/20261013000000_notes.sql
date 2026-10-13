create table public.notes (
  id bigint generated always as identity primary key,
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  body text not null,
  created_at timestamptz not null default now()
);

alter table public.notes enable row level security;

create policy "Users read their notes" on public.notes
  for select to authenticated using (user_id = (select auth.uid()));

create policy "Users add their notes" on public.notes
  for insert to authenticated with check (user_id = (select auth.uid()));

create policy "Users edit their notes" on public.notes
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy "Users delete their notes" on public.notes
  for delete to authenticated using (user_id = (select auth.uid()));

alter publication supabase_realtime add table public.notes;
