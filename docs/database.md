# Database

**SupabaseDatabase** reads and writes the tables of the project. Row-level security
decides what the signed-in user may see and change, so set `Client` to the
Supabase component and write policies for every table.

Every call takes a `tag`. The result event carries it back, so one handler can tell
requests apart.

## Reading

`Select(tag, table, columns, filters, orderBy, ascending, limit, offset)` →
`GotRows(tag, rows)`

- `columns`: comma-separated names, or `*` for all. Related tables work with
  PostgREST's syntax, for example `id,title,author(name)`.
- `filters`: a list of filters made with the `Filter` block, all of which must
  match. An empty list returns every row the user may see.
- `orderBy`: a column, or empty to keep the database's order.
- `limit`: how many rows; `0` for no limit. `offset` skips rows, for pages.

`rows` is a list of dictionaries, one per row.

`Count(tag, table, filters)` → `GotCount(tag, count)`

## Filters

`Filter(column, operator, value)` makes one filter. The **FilterOperator**
dropdown has:

| Operator | Matches when the column… |
|---|---|
| `Equals`, `NotEquals` | equals / doesn't equal the value |
| `GreaterThan`, `GreaterOrEqual`, `LessThan`, `LessOrEqual` | compares to the value |
| `Like`, `LikeIgnoreCase` | matches a pattern, where `%` matches any text |
| `Is` | is `null`, `true` or `false` (give the text) |
| `In` | equals one of the values in a list |
| `Contains`, `ContainedBy`, `Overlaps` | (arrays and JSON) contains every value, is within, or shares a value with a list |
| `TextSearch` | matches a full-text search query |

## Writing

| Block | Result |
|---|---|
| `Insert(tag, table, rows)` | `GotRows(tag, rows)` with the added rows, including values the database filled in, such as `id` |
| `Upsert(tag, table, rows, onConflict)` | Adds rows, or updates the ones that exist. `onConflict` lists the unique columns that identify a row; empty for the primary key. |
| `Update(tag, table, values, filters)` | `GotRows` with the changed rows. `values` is a dictionary of columns and new values. |
| `Delete(tag, table, filters)` | `GotRows` with the deleted rows |

`rows` is one dictionary or a list of dictionaries. `Update` and `Delete` need at
least one filter, so a missing filter can't change every row.

## Database functions

`CallFunction(tag, name, arguments)` → `GotResult(tag, result)` calls a Postgres
function (RPC) with a dictionary of arguments. `result` is a list, a dictionary or
a single value. Functions are useful for work that needs several queries, or rules
row-level security can't express.

```sql
create function public.todo_count() returns bigint
language sql stable security invoker
as $$ select count(*) from public.todos $$;
```

## Values

Text, numbers, `true`/`false`, lists and dictionaries map to their JSON
equivalents. A `null` arrives as an empty text.

## Errors

`Error(tag, operation, code, message)`. Codes are PostgREST's (`PGRST205`: the
table doesn't exist) or Postgres's (`23505`: duplicate key, `42501`: blocked by a
row-level security policy). See [Errors](errors.md).
