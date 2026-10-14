package dev.dreamerslab.supabase

import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.supabase.helpers.SearchField
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.storage.FileObject
import io.github.jan.supabase.storage.FileObjectV2
import io.github.jan.supabase.storage.SortOrder
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.defaultForFilePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@Extension(
    description = "Uploads and downloads files in a Supabase Storage bucket. Storage policies decide what the " +
        "signed-in user may read and write.",
    icon = "icon.png",
)
class SupabaseStorage(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener {

    private val requests = Requests(form, this, ::reportError)
    private val files = AppFiles(form)
    private var supabase: Supabase? = null
    private var bucket = ""

    init {
        form.registerForOnDestroy(this)
    }

    @SimpleProperty(description = "The Supabase component to use.")
    fun Client(): Supabase? = supabase

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT + ":dev.dreamerslab.supabase.Supabase")
    @SimpleProperty
    fun Client(client: Supabase) {
        supabase = client
    }

    @SimpleProperty(description = "The name of the bucket.")
    fun Bucket() = bucket

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    fun Bucket(name: String) {
        bucket = name.trim()
    }

    @SimpleFunction(description = "Uploads a file to a path in the bucket, such as photos/cat.jpg. File is a " +
        "media file of the app (cat.jpg), a picker or camera result, an absolute path, or a path in the app's " +
        "files. Leave contentType empty to guess it from the name. Uploaded fires when it's done.")
    fun Upload(tag: String, path: String, file: String, contentType: String, overwrite: Boolean) =
        requests.launch("Upload", tag) {
            val bytes = withContext(Dispatchers.IO) { files.read(file) }
            bucketApi().upload(path, bytes) {
                upsert = overwrite
                this.contentType = contentType(contentType, path)
            }
            Uploaded(tag, path)
        }

    @SimpleFunction(description = "Downloads a file from the bucket and saves it. ToFile is where to save it: a " +
        "path in the app's files, or an absolute path. Downloaded fires with the saved file's full path.")
    fun Download(tag: String, path: String, toFile: String) = requests.launch("Download", tag) {
        val bytes = bucketApi().downloadAuthenticated(path)
        val saved = withContext(Dispatchers.IO) { files.write(toFile.ifBlank { files.name(path) }, bytes) }
        Downloaded(tag, path, saved)
    }

    @SimpleFunction(description = "Lists the files and folders in a folder of the bucket. Leave folder empty for " +
        "the top level. GotFiles fires with a list of dictionaries, one per file.")
    fun ListFiles(tag: String, folder: String) = requests.launch("ListFiles", tag) {
        val found = bucketApi().list(folder)
        GotFiles(tag, YailList.makeList(found.map(::fileDictionary)))
    }

    @SimpleFunction(description = "Lists the files of a folder that match the search. Search is a dictionary " +
        "with SearchField keys, all optional. GotFiles fires.")
    fun SearchFiles(tag: String, folder: String, search: YailDictionary) = requests.launch("SearchFiles", tag) {
        val options = fileSearch(search)
        val found = bucketApi().list(folder) {
            options.text?.let { this.search = it }
            options.sort?.let { (column, order) -> sortBy(column, order) }
            options.limit?.let { this.limit = it }
            options.offset?.let { this.offset = it }
        }
        GotFiles(tag, YailList.makeList(found.map(::fileDictionary)))
    }

    @SimpleFunction(description = "Sets one setting of a search for SearchFiles, and checks its value.")
    fun SetSearchField(search: YailDictionary, @Options(SearchField::class) field: String, value: Any) =
        requests.run("SetSearchField") {
            fileSearch(YailDictionary(search).apply { put(field, value) })
            search[field] = value
        }

    @SimpleFunction(description = "Gets a file's details, such as its size, content type and when it changed. " +
        "GotFileInfo fires.")
    fun FileInfo(tag: String, path: String) = requests.launch("FileInfo", tag) {
        val info = Json.encodeToJsonElement(FileObjectV2.serializer(), bucketApi().info(path))
        GotFileInfo(tag, path, YailJson.toYail(info) as YailDictionary)
    }

    @SimpleFunction(description = "Makes a link that lets anyone upload one file to the path for the next two " +
        "hours, even without access to the bucket. GotSignedUploadUrl fires with the link and its token; " +
        "UploadWithToken uploads with the token.")
    fun SignedUploadUrl(tag: String, path: String, overwrite: Boolean) = requests.launch("SignedUploadUrl", tag) {
        val signed = bucketApi().createSignedUploadUrl(path, overwrite)
        GotSignedUploadUrl(tag, path, signed.url, signed.token)
    }

    @SimpleFunction(description = "Uploads a file with a token from SignedUploadUrl, usually made by an Edge " +
        "Function for a path the user can't write to. File and contentType work like in Upload. Uploaded fires.")
    @OptIn(SupabaseInternal::class)
    fun UploadWithToken(tag: String, path: String, token: String, file: String, contentType: String) =
        requests.launch("UploadWithToken", tag) {
            val bytes = withContext(Dispatchers.IO) { files.read(file) }
            val objectPath = (listOf(bucket) + path.split('/')).joinToString("/") { it.encodeURLPath() }
            bucketApi().supabaseClient.storage.api.request("object/upload/sign/$objectPath?token=$token") {
                method = HttpMethod.Put
                contentType(contentType(contentType, path))
                setBody(bytes)
            }
            Uploaded(tag, path)
        }

    @SimpleFunction(description = "Deletes files from the bucket. Paths is a list of paths. Removed fires when " +
        "they're gone.")
    fun Remove(tag: String, paths: YailList) = requests.launch("Remove", tag) {
        bucketApi().delete(paths.toArray().map(Any?::toString))
        Removed(tag, paths)
    }

    @SimpleFunction(description = "Moves or renames a file in the bucket. Moved fires when it's done.")
    fun Move(tag: String, from: String, to: String) = requests.launch("Move", tag) {
        bucketApi().move(from, to)
        Moved(tag, from, to)
    }

    @SimpleFunction(description = "Copies a file within the bucket. Copied fires when it's done.")
    fun Copy(tag: String, from: String, to: String) = requests.launch("Copy", tag) {
        bucketApi().copy(from, to)
        Copied(tag, from, to)
    }

    @SimpleFunction(description = "Makes a link to a file that anyone can open for the given number of seconds, " +
        "even in a private bucket. GotSignedUrl fires with the link.")
    fun SignedUrl(tag: String, path: String, seconds: Int) = requests.launch("SignedUrl", tag) {
        GotSignedUrl(tag, path, bucketApi().createSignedUrl(path, seconds.seconds))
    }

    @SimpleFunction(description = "Returns the permanent link to a file in a public bucket. It works only if the " +
        "bucket is public.")
    fun PublicUrl(path: String): String = bucketApi().publicUrl(path)

    @SimpleEvent(description = "A file was uploaded.")
    fun Uploaded(tag: String, path: String) {
        EventDispatcher.dispatchEvent(this, "Uploaded", tag, path)
    }

    @SimpleEvent(description = "A file was downloaded and saved. File is the saved file's full path, ready for " +
        "an Image or Player.")
    fun Downloaded(tag: String, path: String, file: String) {
        EventDispatcher.dispatchEvent(this, "Downloaded", tag, path, file)
    }

    @SimpleEvent(description = "The files of a folder arrived from ListFiles.")
    fun GotFiles(tag: String, files: YailList) {
        EventDispatcher.dispatchEvent(this, "GotFiles", tag, files)
    }

    @SimpleEvent(description = "A file's details arrived from FileInfo, as a dictionary.")
    fun GotFileInfo(tag: String, path: String, info: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "GotFileInfo", tag, path, info)
    }

    @SimpleEvent(description = "An upload link arrived from SignedUploadUrl. Token is what UploadWithToken needs.")
    fun GotSignedUploadUrl(tag: String, path: String, url: String, token: String) {
        EventDispatcher.dispatchEvent(this, "GotSignedUploadUrl", tag, path, url, token)
    }

    @SimpleEvent(description = "Files were deleted.")
    fun Removed(tag: String, paths: YailList) {
        EventDispatcher.dispatchEvent(this, "Removed", tag, paths)
    }

    @SimpleEvent(description = "A file was moved.")
    fun Moved(tag: String, from: String, to: String) {
        EventDispatcher.dispatchEvent(this, "Moved", tag, from, to)
    }

    @SimpleEvent(description = "A file was copied.")
    fun Copied(tag: String, from: String, to: String) {
        EventDispatcher.dispatchEvent(this, "Copied", tag, from, to)
    }

    @SimpleEvent(description = "A signed link arrived from SignedUrl.")
    fun GotSignedUrl(tag: String, path: String, url: String) {
        EventDispatcher.dispatchEvent(this, "GotSignedUrl", tag, path, url)
    }

    @SimpleEvent(description = "A storage call failed. Code is Supabase's error code, or network. Without this " +
        "event, Screen.ErrorOccurred fires instead.")
    fun Error(tag: String, operation: String, code: String, message: String) {
        reportError(operation, tag, code, message)
    }

    override fun onDestroy() = requests.close()

    private fun reportError(operation: String, tag: String, code: String, message: String) =
        EventDispatcher.dispatchEvent(this, "Error", tag, operation, code, message)

    private fun bucketApi() = run {
        require(bucket.isNotBlank()) { "Set the Bucket property" }
        requireNotNull(supabase) { "Set the Client property to a Supabase component" }.client.storage.from(bucket)
    }

    private fun contentType(given: String, path: String) =
        given.ifBlank { null }?.let(ContentType::parse) ?: ContentType.defaultForFilePath(path)

    private fun fileSearch(search: YailDictionary): FileSearch {
        val fields = Fields(search, "search", SearchField.entries)
        return FileSearch(
            fields.text(SearchField.Search),
            fields.text(SearchField.SortBy, ::sortOrder),
            fields.count(SearchField.Limit),
            fields.count(SearchField.Offset),
        )
    }

    private fun sortOrder(sortBy: String): Pair<String, SortOrder> {
        val (column, ascending) = Sorting.parse(sortBy, true).singleOrNull()
            ?: throw IllegalArgumentException("Sort by one column")
        require(column in SORT_COLUMNS) { "Sort by ${SORT_COLUMNS.joinToString()}, not $column" }
        return column to if (ascending) SortOrder.ASC else SortOrder.DESC
    }

    private class FileSearch(val text: String?, val sort: Pair<String, SortOrder>?, val limit: Int?, val offset: Int?)

    private fun fileDictionary(file: FileObject) =
        YailJson.toYail(Json.encodeToJsonElement(FileObject.serializer(), file))
}

private val SORT_COLUMNS = listOf("name", "created_at", "updated_at", "last_accessed_at")
