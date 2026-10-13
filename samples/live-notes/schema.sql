create table if not exists public.notes (
  id bigint generated always as identity primary key,
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  body text not null,
  created_at timestamptz not null default now()
);

alter table public.notes enable row level security;

drop policy if exists "Users read their notes" on public.notes;
drop policy if exists "Users add their notes" on public.notes;
drop policy if exists "Users edit their notes" on public.notes;
drop policy if exists "Users delete their notes" on public.notes;

create policy "Users read their notes" on public.notes
  for select to authenticated using (user_id = (select auth.uid()));
create policy "Users add their notes" on public.notes
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Users edit their notes" on public.notes
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Users delete their notes" on public.notes
  for delete to authenticated using (user_id = (select auth.uid()));

do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'notes'
  ) then
    alter publication supabase_realtime add table public.notes;
  end if;
end $$;

insert into storage.buckets (id, name, public)
values ('uploads', 'uploads', false)
on conflict (id) do nothing;

drop policy if exists "Users read their uploads" on storage.objects;
drop policy if exists "Users add their uploads" on storage.objects;
drop policy if exists "Users move their uploads" on storage.objects;
drop policy if exists "Users delete their uploads" on storage.objects;

create policy "Users read their uploads" on storage.objects
  for select to authenticated
  using (bucket_id = 'uploads' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users add their uploads" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'uploads' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users move their uploads" on storage.objects
  for update to authenticated
  using (bucket_id = 'uploads' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'uploads' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users delete their uploads" on storage.objects
  for delete to authenticated
  using (bucket_id = 'uploads' and (storage.foldername(name))[1] = (select auth.uid())::text);

create or replace function public.note_count() returns bigint
language sql stable security invoker
as $$ select count(*) from public.notes $$;