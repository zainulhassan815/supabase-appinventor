package dev.dreamerslab.supabase

import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.util.YailList
import io.github.jan.supabase.storage.FileObject
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
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

    private fun fileDictionary(file: FileObject) =
        YailJson.toYail(Json.encodeToJsonElement(FileObject.serializer(), file))
}
