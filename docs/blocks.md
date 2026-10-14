# Blocks

Every block of the Supabase extension. Each image contains its block, so you can drag it straight into your workspace.

- [Supabase](#supabase)
- [SupabaseDatabase](#supabasedatabase)
- [SupabaseStorage](#supabasestorage)
- [SupabaseChannel](#supabasechannel)
- [SupabaseFunctions](#supabasefunctions)
- [Dropdowns](#dropdowns)
- [Deprecated](#deprecated)

## Supabase

Connects the app to a Supabase project and signs users in. Other Supabase components use it through their Client property. Guide: [Authentication](authentication.md).

### Supabase: Functions

![SignUp](images/blocks/Supabase1-SignUp.png)

**SignUp:** Creates an account with an email and password. Metadata is a dictionary saved with the user. SignedUp fires with the new user; when the project doesn't require email confirmation, the user is also signed in and SessionChanged fires.

![SignIn](images/blocks/Supabase1-SignIn.png)

**SignIn:** Signs in with an email and password. SessionChanged fires when it succeeds.

![SignInAnonymously](images/blocks/Supabase1-SignInAnonymously.png)

**SignInAnonymously:** Signs in as a new anonymous user. Anonymous sign-ins must be enabled in the project. SessionChanged fires when it succeeds.

![SignOut](images/blocks/Supabase1-SignOut.png)

**SignOut:** Signs the user out and forgets the saved session. SessionChanged fires.

![UpdateUser](images/blocks/Supabase1-UpdateUser.png)

**UpdateUser:** Changes the signed-in user. Leave email or password empty, or metadata an empty dictionary, to keep them; metadata is merged into the saved metadata. A new email must be confirmed through the link sent to it before it is used. Code is the code sent by Reauthenticate, needed for a new password when the project turns on Secure password change; otherwise leave it empty. Giving an anonymous user an email and then a password makes it a full account. UserUpdated fires.

![ResendConfirmation](images/blocks/Supabase1-ResendConfirmation.png)

**ResendConfirmation:** Sends the sign-up confirmation email again. MessageSent fires.

![Reauthenticate](images/blocks/Supabase1-Reauthenticate.png)

**Reauthenticate:** Sends the signed-in user a code by email, or by text message for a phone account, to confirm a password change with UpdateUser. MessageSent fires.

### Supabase: Events

![SignedUp](images/blocks/Supabase1-SignedUp.png)

**SignedUp:** An account was created by SignUp.

![UserUpdated](images/blocks/Supabase1-UserUpdated.png)

**UserUpdated:** UpdateUser changed the user.

![MessageSent](images/blocks/Supabase1-MessageSent.png)

**MessageSent:** An email or text message was sent. Operation is the block that sent it, such as Reauthenticate.

![SessionChanged](images/blocks/Supabase1-SessionChanged.png)

**SessionChanged:** The user signed in or out, or a saved session was restored when the app started.

![Error](images/blocks/Supabase1-Error.png)

**Error:** A Supabase call failed. Code is Supabase's error code, such as invalid_credentials, or network when Supabase couldn't be reached. Without this event, Screen.ErrorOccurred fires instead.

### Supabase: Properties

![get ProjectUrl](images/blocks/Supabase1-getProjectUrl.png) ![set ProjectUrl](images/blocks/Supabase1-setProjectUrl.png)

**ProjectUrl** (read-write): The project URL, for example https://abcd.supabase.co.

![get PublishableKey](images/blocks/Supabase1-getPublishableKey.png) ![set PublishableKey](images/blocks/Supabase1-setPublishableKey.png)

**PublishableKey** (read-write): The project's publishable (anon) key. Never use a secret or service_role key.

![get IsSignedIn](images/blocks/Supabase1-getIsSignedIn.png)

**IsSignedIn** (read-only): Whether a user is signed in.

![get CurrentUser](images/blocks/Supabase1-getCurrentUser.png)

**CurrentUser** (read-only): The signed-in user as a dictionary, or an empty dictionary.

![get AccessToken](images/blocks/Supabase1-getAccessToken.png)

**AccessToken** (read-only): The signed-in user's access token, for calling your own servers. Empty when signed out.

## SupabaseDatabase

Reads and writes the tables of a Supabase project, and calls its database functions. Row-level security decides what the signed-in user may see and change. Guide: [Database](database.md).

### SupabaseDatabase: Functions

![Filter](images/blocks/SupabaseDatabase1-Filter.png)

**Filter:** Makes a filter for Query, Count, Update and Delete. For In, Contains, ContainedBy and Overlaps, the value is a list.

![Query](images/blocks/SupabaseDatabase1-Query.png)

**Query:** Reads the rows of a table that match the query. Query is a dictionary with QueryField keys, all optional: an empty dictionary reads every row. GotRows fires with the tag and the rows, as a list of dictionaries.

![QueryPage](images/blocks/SupabaseDatabase1-QueryPage.png)

**QueryPage:** Reads one page of rows like Query, and counts every row that matches the filters, for paging. GotPage fires with the rows and the total.

![SetQueryField](images/blocks/SupabaseDatabase1-SetQueryField.png)

**SetQueryField:** Sets one setting of a query for Query and QueryPage, and checks its value.

![Count](images/blocks/SupabaseDatabase1-Count.png)

**Count:** Counts the rows of a table that match every filter. GotCount fires with the tag and the count.

![Insert](images/blocks/SupabaseDatabase1-Insert.png)

**Insert:** Adds rows to a table. Rows is a dictionary, or a list of dictionaries. GotRows fires with the added rows, including the values the database filled in.

![InsertOrSkip](images/blocks/SupabaseDatabase1-InsertOrSkip.png)

**InsertOrSkip:** Adds rows, skipping the rows that already exist. OnConflict works like in Upsert. GotRows fires with the rows that were added.

![Upsert](images/blocks/SupabaseDatabase1-Upsert.png)

**Upsert:** Adds rows, or updates the rows that already exist. OnConflict lists the unique columns that identify a row, comma-separated; leave it empty to use the primary key. GotRows fires with the rows.

![Update](images/blocks/SupabaseDatabase1-Update.png)

**Update:** Changes columns of every row that matches the filters. Values is a dictionary of column names and new values. GotRows fires with the changed rows.

![Delete](images/blocks/SupabaseDatabase1-Delete.png)

**Delete:** Deletes every row that matches the filters. GotRows fires with the deleted rows.

![CallFunction](images/blocks/SupabaseDatabase1-CallFunction.png)

**CallFunction:** Calls a database function (RPC) with a dictionary of arguments. GotResult fires with its result: a list, a dictionary, or a single value.

### SupabaseDatabase: Events

![GotRows](images/blocks/SupabaseDatabase1-GotRows.png)

**GotRows:** Rows arrived from Query, Insert, Upsert, Update or Delete.

![GotPage](images/blocks/SupabaseDatabase1-GotPage.png)

**GotPage:** A page of rows arrived from QueryPage. Total is how many rows match the filters.

![GotCount](images/blocks/SupabaseDatabase1-GotCount.png)

**GotCount:** A count arrived from Count.

![GotResult](images/blocks/SupabaseDatabase1-GotResult.png)

**GotResult:** A database function called with CallFunction returned.

![Error](images/blocks/SupabaseDatabase1-Error.png)

**Error:** A database call failed. Code is the PostgREST or Postgres error code, or network. Without this event, Screen.ErrorOccurred fires instead.

### SupabaseDatabase: Properties

![get Client](images/blocks/SupabaseDatabase1-getClient.png) ![set Client](images/blocks/SupabaseDatabase1-setClient.png)

**Client** (read-write): The Supabase component to use.

![get Schema](images/blocks/SupabaseDatabase1-getSchema.png) ![set Schema](images/blocks/SupabaseDatabase1-setSchema.png)

**Schema** (read-write): The database schema of the tables and functions, usually public. Other schemas must be added to Exposed schemas in the project's API settings.

## SupabaseStorage

Uploads and downloads files in a Supabase Storage bucket. Storage policies decide what the signed-in user may read and write. Guide: [Storage](storage.md).

### SupabaseStorage: Functions

![Upload](images/blocks/SupabaseStorage1-Upload.png)

**Upload:** Uploads a file to a path in the bucket, such as photos/cat.jpg. File is a media file of the app (cat.jpg), a picker or camera result, an absolute path, or a path in the app's files. Leave contentType empty to guess it from the name. Uploaded fires when it's done.

![Download](images/blocks/SupabaseStorage1-Download.png)

**Download:** Downloads a file from the bucket and saves it. ToFile is where to save it: a path in the app's files, or an absolute path. Downloaded fires with the saved file's full path.

![ListFiles](images/blocks/SupabaseStorage1-ListFiles.png)

**ListFiles:** Lists the files and folders in a folder of the bucket. Leave folder empty for the top level. GotFiles fires with a list of dictionaries, one per file.

![SearchFiles](images/blocks/SupabaseStorage1-SearchFiles.png)

**SearchFiles:** Lists the files of a folder that match the search. Search is a dictionary with SearchField keys, all optional. GotFiles fires.

![SetSearchField](images/blocks/SupabaseStorage1-SetSearchField.png)

**SetSearchField:** Sets one setting of a search for SearchFiles, and checks its value.

![FileInfo](images/blocks/SupabaseStorage1-FileInfo.png)

**FileInfo:** Gets a file's details, such as its size, content type and when it changed. GotFileInfo fires.

![SignedUploadUrl](images/blocks/SupabaseStorage1-SignedUploadUrl.png)

**SignedUploadUrl:** Makes a link that lets anyone upload one file to the path for the next two hours, even without access to the bucket. GotSignedUploadUrl fires with the link and its token; UploadWithToken uploads with the token.

![UploadWithToken](images/blocks/SupabaseStorage1-UploadWithToken.png)

**UploadWithToken:** Uploads a file with a token from SignedUploadUrl, usually made by an Edge Function for a path the user can't write to. File and contentType work like in Upload. Uploaded fires.

![Remove](images/blocks/SupabaseStorage1-Remove.png)

**Remove:** Deletes files from the bucket. Paths is a list of paths. Removed fires when they're gone.

![Move](images/blocks/SupabaseStorage1-Move.png)

**Move:** Moves or renames a file in the bucket. Moved fires when it's done.

![Copy](images/blocks/SupabaseStorage1-Copy.png)

**Copy:** Copies a file within the bucket. Copied fires when it's done.

![SignedUrl](images/blocks/SupabaseStorage1-SignedUrl.png)

**SignedUrl:** Makes a link to a file that anyone can open for the given number of seconds, even in a private bucket. GotSignedUrl fires with the link.

![PublicUrl](images/blocks/SupabaseStorage1-PublicUrl.png)

**PublicUrl:** Returns the permanent link to a file in a public bucket. It works only if the bucket is public.

### SupabaseStorage: Events

![Uploaded](images/blocks/SupabaseStorage1-Uploaded.png)

**Uploaded:** A file was uploaded.

![Downloaded](images/blocks/SupabaseStorage1-Downloaded.png)

**Downloaded:** A file was downloaded and saved. File is the saved file's full path, ready for an Image or Player.

![GotFiles](images/blocks/SupabaseStorage1-GotFiles.png)

**GotFiles:** The files of a folder arrived from ListFiles.

![GotFileInfo](images/blocks/SupabaseStorage1-GotFileInfo.png)

**GotFileInfo:** A file's details arrived from FileInfo, as a dictionary.

![GotSignedUploadUrl](images/blocks/SupabaseStorage1-GotSignedUploadUrl.png)

**GotSignedUploadUrl:** An upload link arrived from SignedUploadUrl. Token is what UploadWithToken needs.

![Removed](images/blocks/SupabaseStorage1-Removed.png)

**Removed:** Files were deleted.

![Moved](images/blocks/SupabaseStorage1-Moved.png)

**Moved:** A file was moved.

![Copied](images/blocks/SupabaseStorage1-Copied.png)

**Copied:** A file was copied.

![GotSignedUrl](images/blocks/SupabaseStorage1-GotSignedUrl.png)

**GotSignedUrl:** A signed link arrived from SignedUrl.

![Error](images/blocks/SupabaseStorage1-Error.png)

**Error:** A storage call failed. Code is Supabase's error code, or network. Without this event, Screen.ErrorOccurred fires instead.

### SupabaseStorage: Properties

![get Client](images/blocks/SupabaseStorage1-getClient.png) ![set Client](images/blocks/SupabaseStorage1-setClient.png)

**Client** (read-write): The Supabase component to use.

![get Bucket](images/blocks/SupabaseStorage1-getBucket.png) ![set Bucket](images/blocks/SupabaseStorage1-setBucket.png)

**Bucket** (read-write): The name of the bucket.

## SupabaseChannel

A realtime channel: receives database changes as they happen, and sends messages and online status between the users of the same channel. Guide: [Realtime](realtime.md).

### SupabaseChannel: Functions

![ListenToTable](images/blocks/SupabaseChannel1-ListenToTable.png)

**ListenToTable:** Receives changes to a table through ChangeReceived. Call it before Subscribe. Filter is empty, or one filter from SupabaseDatabase's Filter block (Equals, NotEquals, GreaterThan, GreaterOrEqual, LessThan, LessOrEqual or In). Realtime must be enabled for the table, and row-level security decides which changes arrive.

![ListenToBroadcast](images/blocks/SupabaseChannel1-ListenToBroadcast.png)

**ListenToBroadcast:** Receives broadcasts with the given event name through BroadcastReceived. Call it before Subscribe.

![Subscribe](images/blocks/SupabaseChannel1-Subscribe.png)

**Subscribe:** Connects the channel. StatusChanged fires with Subscribed once it receives. Listeners added with ListenToTable and ListenToBroadcast stay, so Subscribe works again after Unsubscribe.

![Unsubscribe](images/blocks/SupabaseChannel1-Unsubscribe.png)

**Unsubscribe:** Disconnects the channel. StatusChanged fires with Unsubscribed.

![Broadcast](images/blocks/SupabaseChannel1-Broadcast.png)

**Broadcast:** Sends a message to everyone subscribed to the channel. Payload is a dictionary.

![Track](images/blocks/SupabaseChannel1-Track.png)

**Track:** Shares this app's state, such as a name or status, with everyone on the channel. PresenceChanged fires on every app, including this one.

![Untrack](images/blocks/SupabaseChannel1-Untrack.png)

**Untrack:** Stops sharing this app's state.

### SupabaseChannel: Events

![StatusChanged](images/blocks/SupabaseChannel1-StatusChanged.png)

**StatusChanged:** The channel connected or disconnected.

![ChangeReceived](images/blocks/SupabaseChannel1-ChangeReceived.png)

**ChangeReceived:** A row changed. Record is the row after the change, and oldRecord the row before it (for Update and Delete; it has only the primary key unless the table's replica identity is full).

![BroadcastReceived](images/blocks/SupabaseChannel1-BroadcastReceived.png)

**BroadcastReceived:** A broadcast arrived. Payload is usually a dictionary.

![PresenceChanged](images/blocks/SupabaseChannel1-PresenceChanged.png)

**PresenceChanged:** Users joined or left the channel, or changed their tracked state. Joins and leaves are dictionaries from presence key to state.

![Error](images/blocks/SupabaseChannel1-Error.png)

**Error:** A channel call failed. Without this event, Screen.ErrorOccurred fires instead.

### SupabaseChannel: Properties

![get Client](images/blocks/SupabaseChannel1-getClient.png) ![set Client](images/blocks/SupabaseChannel1-setClient.png)

**Client** (read-write): The Supabase component to use.

![get Topic](images/blocks/SupabaseChannel1-getTopic.png) ![set Topic](images/blocks/SupabaseChannel1-setTopic.png)

**Topic** (read-write): The channel's name. Users on the same topic share broadcasts and presence.

![get ReceiveOwnBroadcasts](images/blocks/SupabaseChannel1-getReceiveOwnBroadcasts.png) ![set ReceiveOwnBroadcasts](images/blocks/SupabaseChannel1-setReceiveOwnBroadcasts.png)

**ReceiveOwnBroadcasts** (read-write): Whether BroadcastReceived also fires for this app's own broadcasts.

![get PresenceKey](images/blocks/SupabaseChannel1-getPresenceKey.png) ![set PresenceKey](images/blocks/SupabaseChannel1-setPresenceKey.png)

**PresenceKey** (read-write): The name this app's presence is tracked under, such as the user's id. Leave empty for a random one.

![get Private](images/blocks/SupabaseChannel1-getPrivate.png) ![set Private](images/blocks/SupabaseChannel1-setPrivate.png)

**Private** (read-write): Whether only users allowed by the project's policies on realtime.messages may join the channel, send broadcasts and track presence. Set it before Subscribe.

![get IsSubscribed](images/blocks/SupabaseChannel1-getIsSubscribed.png)

**IsSubscribed** (read-only): Whether the channel is subscribed.

![get PresenceState](images/blocks/SupabaseChannel1-getPresenceState.png)

**PresenceState** (read-only): Everyone on the channel right now, as a dictionary from presence key to tracked state. Empty until the channel is subscribed.

## SupabaseFunctions

Calls the Edge Functions of a Supabase project: server code that runs with the signed-in user's identity, for work an app can't do on its own, like calling paid APIs with secret keys. Guide: [Edge Functions](functions.md).

### SupabaseFunctions: Functions

![Invoke](images/blocks/SupabaseFunctions1-Invoke.png)

**Invoke:** Calls an Edge Function. Body is a dictionary or list (sent as JSON), text, or empty for none. Headers is a dictionary of extra HTTP headers. GotResponse fires with the response; a JSON response arrives as a dictionary or list.

### SupabaseFunctions: Events

![GotResponse](images/blocks/SupabaseFunctions1-GotResponse.png)

**GotResponse:** An Edge Function answered. Status is the HTTP status code.

![Error](images/blocks/SupabaseFunctions1-Error.png)

**Error:** A function call failed, or the function answered with an error status. Code is http_ and the status, such as http_404, or network. Without this event, Screen.ErrorOccurred fires instead.

### SupabaseFunctions: Properties

![get Client](images/blocks/SupabaseFunctions1-getClient.png) ![set Client](images/blocks/SupabaseFunctions1-setClient.png)

**Client** (read-write): The Supabase component to use.

![get Region](images/blocks/SupabaseFunctions1-getRegion.png) ![set Region](images/blocks/SupabaseFunctions1-setRegion.png)

**Region** (read-write): Where functions run. Choose the region of your database when a function uses it a lot, so its queries are fast.

## Dropdowns

Choices for blocks, properties and the keys of query and search dictionaries.

![ChangeEvent](images/blocks/helper-ChangeEvent.png)

**ChangeEvent:** All, Insert, Update, Delete

![ChannelStatus](images/blocks/helper-ChannelStatus.png)

**ChannelStatus:** Subscribing, Subscribed, Unsubscribed

![FilterOperator](images/blocks/helper-FilterOperator.png)

**FilterOperator:** Equals, NotEquals, GreaterThan, GreaterOrEqual, LessThan, LessOrEqual, Like, LikeIgnoreCase, Is, In, Contains, ContainedBy, Overlaps, TextSearch

![QueryField](images/blocks/helper-QueryField.png)

**QueryField:** Columns, Filters, OrderBy, Limit, Offset

![Region](images/blocks/helper-Region.png)

**Region:** Any, ApNortheast1, ApNortheast2, ApSouth1, ApSoutheast1, ApSoutheast2, CaCentral1, EuCentral1, EuWest1, EuWest2, EuWest3, SaEast1, UsEast1, UsWest1, UsWest2

![RequestMethod](images/blocks/helper-RequestMethod.png)

**RequestMethod:** Post, Get, Put, Patch, Delete

![SearchField](images/blocks/helper-SearchField.png)

**SearchField:** Search, SortBy, Limit, Offset

![SessionStatus](images/blocks/helper-SessionStatus.png)

**SessionStatus:** SignedIn, SignedOut, RefreshFailed

## Deprecated

Hidden from the blocks drawer and kept working for existing projects. The guides name the blocks that replace them: `SupabaseDatabase.Select`.
