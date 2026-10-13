# Block reference

Every block of the extension, generated from its code by `rush build`.

## Supabase

<p>Connects the app to a Supabase project and signs users in. Other Supabase components use it through their Client property.</p>

### Events

#### SignedUp(user)

An account was created by SignUp.

| Parameter | Type |
|---|---|
| user | dictionary |

#### SessionChanged(status, user)

The user signed in or out, or a saved session was restored when the app started.

| Parameter | Type |
|---|---|
| status | SessionStatus |
| user | dictionary |

#### Error(operation, code, message)

A Supabase call failed. Code is Supabase's error code, such as invalid_credentials, or network when Supabase couldn't be reached. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| operation | text |
| code | text |
| message | text |

### Methods

#### SignUp(email, password, metadata)

Creates an account with an email and password. Metadata is a dictionary saved with the user. SignedUp fires with the new user; when the project doesn't require email confirmation, the user is also signed in and SessionChanged fires.

| Parameter | Type |
|---|---|
| email | text |
| password | text |
| metadata | dictionary |

#### SignIn(email, password)

Signs in with an email and password. SessionChanged fires when it succeeds.

| Parameter | Type |
|---|---|
| email | text |
| password | text |

#### SignInAnonymously()

Signs in as a new anonymous user. Anonymous sign-ins must be enabled in the project. SessionChanged fires when it succeeds.

#### SignOut()

Signs the user out and forgets the saved session. SessionChanged fires.

### Properties

#### ProjectUrl

The project URL, for example https://abcd.supabase.co.

`text`, read-write, designer default ``.

#### PublishableKey

The project's publishable (anon) key. Never use a secret or service_role key.

`text`, read-write, designer default ``.

#### IsSignedIn

Whether a user is signed in.

`boolean`, read-only.

#### CurrentUser

The signed-in user as a dictionary, or an empty dictionary.

`dictionary`, read-only.

#### AccessToken

The signed-in user's access token, for calling your own servers. Empty when signed out.

`text`, read-only.

### Dropdowns

#### SessionStatus

| Option | Value | Description |
|---|---|---|
| SignedIn | `SignedIn` | A user is signed in. |
| SignedOut | `SignedOut` | No user is signed in. |
| RefreshFailed | `RefreshFailed` | The saved session couldn't be refreshed, usually because the device is offline. |

## SupabaseDatabase

<p>Reads and writes the tables of a Supabase project, and calls its database functions. Row-level security decides what the signed-in user may see and change.</p>

### Events

#### GotRows(tag, rows)

Rows arrived from Select, Insert, Upsert, Update or Delete.

| Parameter | Type |
|---|---|
| tag | text |
| rows | list |

#### GotCount(tag, count)

A count arrived from Count.

| Parameter | Type |
|---|---|
| tag | text |
| count | number |

#### GotResult(tag, result)

A database function called with CallFunction returned.

| Parameter | Type |
|---|---|
| tag | text |
| result | any |

#### Error(tag, operation, code, message)

A database call failed. Code is the PostgREST or Postgres error code, or network. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| tag | text |
| operation | text |
| code | text |
| message | text |

### Methods

#### Filter(column, operator, value)

Makes a filter for Select, Count, Update and Delete. For In, Contains, ContainedBy and Overlaps, the value is a list.

| Parameter | Type |
|---|---|
| column | text |
| operator | FilterOperator |
| value | any |

Returns `list`.

#### Select(tag, table, columns, filters, orderBy, ascending, limit, offset)

Reads the rows of a table that match every filter. GotRows fires with the tag and the rows, as a list of dictionaries. Columns is a comma-separated list, or * for all. Leave orderBy empty to keep the database's order, and limit at 0 for no limit.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| columns | text |
| filters | list |
| orderBy | text |
| ascending | boolean |
| limit | number |
| offset | number |

#### Count(tag, table, filters)

Counts the rows of a table that match every filter. GotCount fires with the tag and the count.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| filters | list |

#### Insert(tag, table, rows)

Adds rows to a table. Rows is a dictionary, or a list of dictionaries. GotRows fires with the added rows, including the values the database filled in.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| rows | any |

#### Upsert(tag, table, rows, onConflict)

Adds rows, or updates the rows that already exist. OnConflict lists the unique columns that identify a row, comma-separated; leave it empty to use the primary key. GotRows fires with the rows.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| rows | any |
| onConflict | text |

#### Update(tag, table, values, filters)

Changes columns of every row that matches the filters. Values is a dictionary of column names and new values. GotRows fires with the changed rows.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| values | dictionary |
| filters | list |

#### Delete(tag, table, filters)

Deletes every row that matches the filters. GotRows fires with the deleted rows.

| Parameter | Type |
|---|---|
| tag | text |
| table | text |
| filters | list |

#### CallFunction(tag, name, arguments)

Calls a database function (RPC) with a dictionary of arguments. GotResult fires with its result: a list, a dictionary, or a single value.

| Parameter | Type |
|---|---|
| tag | text |
| name | text |
| arguments | dictionary |

### Properties

#### Client

The Supabase component to use.

`component`, read-write, designer default ``.

### Dropdowns

#### FilterOperator

| Option | Value | Description |
|---|---|---|
| Equals | `eq` | The column equals the value. |
| NotEquals | `neq` | The column doesn't equal the value. |
| GreaterThan | `gt` | The column is greater than the value. |
| GreaterOrEqual | `gte` | The column is greater than or equal to the value. |
| LessThan | `lt` | The column is less than the value. |
| LessOrEqual | `lte` | The column is less than or equal to the value. |
| Like | `like` | The column matches a pattern, where % matches any text. Case-sensitive. |
| LikeIgnoreCase | `ilike` | The column matches a pattern, where % matches any text. Ignores case. |
| Is | `is` | The column is null, true or false. Use the text null, true or false as the value. |
| In | `in` | The column equals one of the values in a list. |
| Contains | `cs` | The array or JSON column contains every value in a list. |
| ContainedBy | `cd` | Every value of the array or JSON column is in a list. |
| Overlaps | `ov` | The array column shares at least one value with a list. |
| TextSearch | `fts` | The text column matches a full-text search query. |

## SupabaseStorage

<p>Uploads and downloads files in a Supabase Storage bucket. Storage policies decide what the signed-in user may read and write.</p>

### Events

#### Uploaded(tag, path)

A file was uploaded.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |

#### Downloaded(tag, path, file)

A file was downloaded and saved. File is the saved file's full path, ready for an Image or Player.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |
| file | text |

#### GotFiles(tag, files)

The files of a folder arrived from ListFiles.

| Parameter | Type |
|---|---|
| tag | text |
| files | list |

#### Removed(tag, paths)

Files were deleted.

| Parameter | Type |
|---|---|
| tag | text |
| paths | list |

#### Moved(tag, from, to)

A file was moved.

| Parameter | Type |
|---|---|
| tag | text |
| from | text |
| to | text |

#### Copied(tag, from, to)

A file was copied.

| Parameter | Type |
|---|---|
| tag | text |
| from | text |
| to | text |

#### GotSignedUrl(tag, path, url)

A signed link arrived from SignedUrl.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |
| url | text |

#### Error(tag, operation, code, message)

A storage call failed. Code is Supabase's error code, or network. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| tag | text |
| operation | text |
| code | text |
| message | text |

### Methods

#### Upload(tag, path, file, contentType, overwrite)

Uploads a file to a path in the bucket, such as photos/cat.jpg. File is a media file of the app (cat.jpg), a picker or camera result, an absolute path, or a path in the app's files. Leave contentType empty to guess it from the name. Uploaded fires when it's done.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |
| file | text |
| contentType | text |
| overwrite | boolean |

#### Download(tag, path, toFile)

Downloads a file from the bucket and saves it. ToFile is where to save it: a path in the app's files, or an absolute path. Downloaded fires with the saved file's full path.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |
| toFile | text |

#### ListFiles(tag, folder)

Lists the files and folders in a folder of the bucket. Leave folder empty for the top level. GotFiles fires with a list of dictionaries, one per file.

| Parameter | Type |
|---|---|
| tag | text |
| folder | text |

#### Remove(tag, paths)

Deletes files from the bucket. Paths is a list of paths. Removed fires when they're gone.

| Parameter | Type |
|---|---|
| tag | text |
| paths | list |

#### Move(tag, from, to)

Moves or renames a file in the bucket. Moved fires when it's done.

| Parameter | Type |
|---|---|
| tag | text |
| from | text |
| to | text |

#### Copy(tag, from, to)

Copies a file within the bucket. Copied fires when it's done.

| Parameter | Type |
|---|---|
| tag | text |
| from | text |
| to | text |

#### SignedUrl(tag, path, seconds)

Makes a link to a file that anyone can open for the given number of seconds, even in a private bucket. GotSignedUrl fires with the link.

| Parameter | Type |
|---|---|
| tag | text |
| path | text |
| seconds | number |

#### PublicUrl(path)

Returns the permanent link to a file in a public bucket. It works only if the bucket is public.

| Parameter | Type |
|---|---|
| path | text |

Returns `text`.

### Properties

#### Client

The Supabase component to use.

`component`, read-write, designer default ``.

#### Bucket

The name of the bucket.

`text`, read-write, designer default ``.

## SupabaseChannel

<p>A realtime channel: receives database changes as they happen, and sends messages and online status between the users of the same channel.</p>

### Events

#### StatusChanged(status)

The channel connected or disconnected.

| Parameter | Type |
|---|---|
| status | ChannelStatus |

#### ChangeReceived(table, event, record, oldRecord)

A row changed. Record is the row after the change, and oldRecord the row before it (for Update and Delete; it has only the primary key unless the table's replica identity is full).

| Parameter | Type |
|---|---|
| table | text |
| event | ChangeEvent |
| record | dictionary |
| oldRecord | dictionary |

#### BroadcastReceived(event, payload)

A broadcast arrived. Payload is usually a dictionary.

| Parameter | Type |
|---|---|
| event | text |
| payload | any |

#### PresenceChanged(joins, leaves)

Users joined or left the channel, or changed their tracked state. Joins and leaves are dictionaries from presence key to state.

| Parameter | Type |
|---|---|
| joins | dictionary |
| leaves | dictionary |

#### Error(operation, code, message)

A channel call failed. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| operation | text |
| code | text |
| message | text |

### Methods

#### ListenToTable(schema, table, event, filter)

Receives changes to a table through ChangeReceived. Call it before Subscribe. Filter is empty, or one filter from SupabaseDatabase's Filter block (Equals, NotEquals, GreaterThan, GreaterOrEqual, LessThan, LessOrEqual or In). Realtime must be enabled for the table, and row-level security decides which changes arrive.

| Parameter | Type |
|---|---|
| schema | text |
| table | text |
| event | ChangeEvent |
| filter | any |

#### ListenToBroadcast(event)

Receives broadcasts with the given event name through BroadcastReceived. Call it before Subscribe.

| Parameter | Type |
|---|---|
| event | text |

#### Subscribe()

Connects the channel. StatusChanged fires with Subscribed once it receives. Listeners added with ListenToTable and ListenToBroadcast stay, so Subscribe works again after Unsubscribe.

#### Unsubscribe()

Disconnects the channel. StatusChanged fires with Unsubscribed.

#### Broadcast(event, payload)

Sends a message to everyone subscribed to the channel. Payload is a dictionary.

| Parameter | Type |
|---|---|
| event | text |
| payload | dictionary |

#### Track(state)

Shares this app's state, such as a name or status, with everyone on the channel. PresenceChanged fires on every app, including this one.

| Parameter | Type |
|---|---|
| state | dictionary |

#### Untrack()

Stops sharing this app's state.

### Properties

#### Client

The Supabase component to use.

`component`, read-write, designer default ``.

#### Topic

The channel's name. Users on the same topic share broadcasts and presence.

`text`, read-write, designer default ``.

#### ReceiveOwnBroadcasts

Whether BroadcastReceived also fires for this app's own broadcasts.

`boolean`, read-write, designer default `True`.

#### PresenceKey

The name this app's presence is tracked under, such as the user's id. Leave empty for a random one.

`text`, read-write, designer default ``.

#### IsSubscribed

Whether the channel is subscribed.

`boolean`, read-only.

### Dropdowns

#### ChannelStatus

| Option | Value | Description |
|---|---|---|
| Subscribing | `Subscribing` | The channel is connecting. |
| Subscribed | `Subscribed` | The channel is connected and receiving. |
| Unsubscribed | `Unsubscribed` | The channel is disconnected. |

#### ChangeEvent

| Option | Value | Description |
|---|---|---|
| All | `*` | Any change. |
| Insert | `INSERT` | A row was added. |
| Update | `UPDATE` | A row was changed. |
| Delete | `DELETE` | A row was deleted. |

## SupabaseFunctions

<p>Calls the Edge Functions of a Supabase project: server code that runs with the signed-in user's identity, for work an app can't do on its own, like calling paid APIs with secret keys.</p>

### Events

#### GotResponse(tag, status, body)

An Edge Function answered. Status is the HTTP status code.

| Parameter | Type |
|---|---|
| tag | text |
| status | number |
| body | any |

#### Error(tag, operation, code, message)

A function call failed, or the function answered with an error status. Code is http_ and the status, such as http_404, or network. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| tag | text |
| operation | text |
| code | text |
| message | text |

### Methods

#### Invoke(tag, name, body, headers, method)

Calls an Edge Function. Body is a dictionary or list (sent as JSON), text, or empty for none. Headers is a dictionary of extra HTTP headers. GotResponse fires with the response; a JSON response arrives as a dictionary or list.

| Parameter | Type |
|---|---|
| tag | text |
| name | text |
| body | any |
| headers | dictionary |
| method | RequestMethod |

### Properties

#### Client

The Supabase component to use.

`component`, read-write, designer default ``.

### Dropdowns

#### RequestMethod

| Option | Value | Description |
|---|---|---|
| Post | `POST` | Sends the body to the function. |
| Get | `GET` | Asks the function for data, without a body. |
| Put | `PUT` | Replaces data, with a body. |
| Patch | `PATCH` | Changes part of the data, with a body. |
| Delete | `DELETE` | Deletes data. |
