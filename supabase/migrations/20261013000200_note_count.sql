create function public.note_count() returns bigint
language sql stable security invoker
as $$ select count(*) from public.notes $$;
