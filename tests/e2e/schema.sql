create table if not exists public.e2e_tags (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  name text not null,
  primary key (user_id, name)
);

alter table public.e2e_tags enable row level security;

drop policy if exists "Users manage their tags" on public.e2e_tags;

create policy "Users manage their tags" on public.e2e_tags
  for all to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
