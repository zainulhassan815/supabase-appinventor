# Storage

**SupabaseStorage** works with files in one bucket, set in its `Bucket` property.
Create buckets in the dashboard (Storage) or with SQL, and write policies on
`storage.objects` for what each user may read and write. The sample's
[`schema.sql`](../samples/live-notes/schema.sql) gives each user a private folder
named after their id.

## Blocks

| Block | Result |
|---|---|
| `Upload(tag, path, file, contentType, overwrite)` | `Uploaded(tag, path)` |
| `Download(tag, path, toFile)` | `Downloaded(tag, path, file)` |
| `ListFiles(tag, folder)` | `GotFiles(tag, files)` |
| `Remove(tag, paths)` | `Removed(tag, paths)` |
| `Move(tag, from, to)` | `Moved(tag, from, to)` |
| `Copy(tag, from, to)` | `Copied(tag, from, to)` |
| `SignedUrl(tag, path, seconds)` | `GotSignedUrl(tag, path, url)` |
| `PublicUrl(path)` | Returns the link right away. Only works for public buckets. |

`path` is a file's place in the bucket, like `photos/cat.jpg`.

## Files on the device

`file` in `Upload` can be:

- a media file of the app, by its name: `cat.jpg`;
- the result of an ImagePicker, Camera or other picker;
- an absolute path, like `/storage/emulated/0/Download/cat.jpg`;
- a path in the app's own files, like `photos/cat.jpg`.

Leave `contentType` empty to guess it from the name. With `overwrite` false,
uploading over an existing file fails.

`toFile` in `Download` is where to save it: a path in the app's files, or an
absolute path. Leave it empty to use the file's name. `Downloaded` reports the
full path of the saved file, ready for an Image's `Picture` or a Player.

## Showing private files

A private bucket's files need the user's session, so an Image can't load them by
path. `SignedUrl` makes a link that works for anyone for the given number of
seconds; set an Image's `Picture` to it.

## Errors

`Error(tag, operation, code, message)`. Storage errors carry the HTTP status as the
code, like `http_400` (often a policy that refused the request) or `http_404`.
