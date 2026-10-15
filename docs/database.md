# Database

**SupabaseDatabase** reads and writes the tables of the project. Row-level security
decides what the signed-in user may see and change, so set `Client` to the
Supabase component and write policies for every table.

Every call takes a `tag`. The result event carries it back, so one handler can tell
requests apart.

## Reading

`Query(tag, table, query)` → `GotRows(tag, rows)`

`query` is a dictionary that describes what to read. Make it with the
**make a dictionary** block, and take its keys from the **QueryField** dropdown, so
they can't be misspelled. Every key is optional: an empty dictionary reads every row
the user may see.

| QueryField | Value |
|---|---|
| `Columns` | Comma-separated names. Related tables work with PostgREST's syntax, for example `id,title,author(name)`. All columns when left out. |
| `Filters` | A list of filters made with the `Filter` block, all of which must match. |
| `OrderBy` | A column, or several separated by commas, each optionally followed by `asc` or `desc`: `done, created_at desc`. |
| `Limit` | How many rows at most. |
| `Offset` | How many rows to skip first, for pages. |

`rows` is a list of dictionaries, one per row.

The extension checks the whole query before sending it. A mistake fires `Error`
with the code `invalid_argument` and the place it was found, such as
`query.filters[1]: Each filter is a list of column, operator and value` or
`query: "limt" isn't a QueryField`. Text keys work too, so a query decoded from
JSON (`{"orderBy": "id", "limit": 20}`) can be passed as it is.

`SetQueryField(query, field, value)` sets one key of a query and checks its value
right away.

### Pages

`QueryPage(tag, table, query)` → `GotPage(tag, rows, total)` reads one page like
`Query`, and also counts every row that matches the filters, so the app can show
"page 2 of 7". Use `Limit` for the page size and `Offset` for where it starts.

`Count(tag, table, filters)` → `GotCount(tag, count)` only counts.

### Other schemas

Tables and functions live in the `public` schema unless the `Schema` property
says otherwise. Other schemas must be added to **Exposed schemas** in the
project's API settings.

### Select

Projects made with version 1.0 use `Select(tag, table, columns, filters, orderBy,
ascending, limit, offset)`. It keeps working, but it's hidden from the blocks
drawer: use `Query` in new projects.

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

### Any, all and not

Every filter in a list has to match. To match when one of several does, or when
one doesn't, group them:

- `AnyOf(filters)`: any of the filters in the list matches.
- `AllOf(filters)`: every filter in the list matches. Useful inside `AnyOf` or
  `Not`.
- `Not(filter)`: the filter doesn't match.

Each makes a filter, so they nest: `AnyOf([AllOf([done = true, pinned = true]),
priority = high])`. They work in `Query`, `QueryPage`, `Count`, `Update` and
`Delete`. A group is a dictionary such as `{"anyOf": [["status", "eq", "open"],
{"not": ["owner", "is", "null"]}]}`, so groups decoded from JSON work too.

## Writing

| Block | Result |
|---|---|
| `Insert(tag, table, rows)` | `GotRows(tag, rows)` with the added rows, including values the database filled in, such as `id` |
| `Upsert(tag, table, rows, onConflict)` | Adds rows, or updates the ones that exist. `onConflict` lists the unique columns that identify a row; empty for the primary key. |
| `InsertOrSkip(tag, table, rows, onConflict)` | Adds rows and skips the ones that already exist. `GotRows` has only the added rows. |
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
