package dev.dreamerslab.supabase

import android.net.Uri
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.util.FileUtil
import com.google.appinventor.components.runtime.util.ScopedFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

internal class AppFiles(private val form: Form) {

    fun read(path: String): ByteArray = input(path).use(InputStream::readBytes)

    fun write(path: String, bytes: ByteArray): String {
        output(path).use { it.write(bytes) }
        return location(path)
    }

    fun name(path: String) = path.substringAfterLast('/')

    private fun input(path: String): InputStream = when {
        path.startsWith("content:") -> checkNotNull(form.contentResolver.openInputStream(Uri.parse(path)))
        path.startsWith("//") -> form.openAsset(path.removePrefix("//"))
        isAbsolute(path) -> absolute(path).inputStream()
        '/' !in path -> asset(path) ?: FileUtil.openForReading(form, scoped(path))
        else -> FileUtil.openForReading(form, scoped(path))
    }

    private fun asset(name: String): InputStream? = try {
        form.openAsset(name)
    } catch (e: IOException) {
        null
    }

    private fun output(path: String): OutputStream = when {
        path.startsWith("content:") -> checkNotNull(form.contentResolver.openOutputStream(Uri.parse(path)))
        path.startsWith("//") -> throw IllegalArgumentException("Assets are read-only")
        isAbsolute(path) -> absolute(path).also { it.parentFile?.mkdirs() }.outputStream()
        else -> {
            File(URI(FileUtil.resolveFileName(form, scoped(path)))).parentFile?.mkdirs()
            FileUtil.openForWriting(form, scoped(path))
        }
    }

    private fun location(path: String) = when {
        path.startsWith("content:") -> path
        isAbsolute(path) -> absolute(path).absolutePath
        else -> URI(FileUtil.resolveFileName(form, scoped(path))).path
    }

    private fun scoped(path: String) = ScopedFile(form.DefaultFileScope(), path)

    private fun isAbsolute(path: String) =
        path.startsWith("file://") || ABSOLUTE_ROOTS.any { path.startsWith(it) }

    private fun absolute(path: String) = if (path.startsWith("file://")) File(URI(path)) else File(path)

    private companion object {
        val ABSOLUTE_ROOTS = listOf("/storage/", "/sdcard/", "/data/")
    }
}
