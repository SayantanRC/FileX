package balti.filex.operators

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.system.Os
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit
import balti.filex.FileXInit.Companion.DEBUG_TAG
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.fContext
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.exceptions.FileXNotFoundException
import balti.filex.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.utils.Tools.checkUriExists
import balti.filex.utils.Tools.getStringQuery
import balti.filex.utils.Tools.removeLeadingTrailingSlashOrColon
import balti.filex.utils.Tools.removeRearSlash
import java.io.IOException

// public methods
// *****************************************

val FileX.canonicalPath: String get() = "${volumePath}${storagePath}"
val FileX.absolutePath: String get() = canonicalPath

val FileX.storagePath: String get () =
    rootDocumentId.split(":").let {
        if (it.size > 1) "/${removeRearSlash(it[1])}$path" else path
    }

val FileX.volumePath: String get () = uri.let {
    rootDocumentId.split(":").let {
        if (it.isNotEmpty()) {
            FileXInit.storageVolumes[it[0]].let { it ?: "" }
        } else ""
    }
}

fun FileX.exists(): Boolean {
    return uri?.let { checkUriExists(it) }?: false
}
val FileX.isDirectory: Boolean get() =
    try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) == DocumentsContract.Document.MIME_TYPE_DIR }
    catch (_: Exception) {false}

val FileX.isFile: Boolean get() =
    exists() && try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) != DocumentsContract.Document.MIME_TYPE_DIR }
    catch (_: Exception) {false}

val FileX.name: String
    get() {
        return when {
            path.isNotBlank() -> path.split("/").let { it[it.size - 1] }
            else -> try {
                getStringQuery(DocumentsContract.Document.COLUMN_DISPLAY_NAME) ?: ""
            } catch (_: Exception) { "" }
        }
    }

val FileX.parent: String? get() {
    return when {
        path == "/" -> null
        path.indexOf('/') != path.lastIndexOf('/') -> path.substring(0, path.lastIndexOf("/"))
        else -> "/"
    }
}

val FileX.parentFile: FileX? get() = parent?.let { FileX(it) }

val FileX.parentUri: Uri? get() = parentFile?.uri

val FileX.parentCanonical: String get() = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "" }

fun FileX.length(): Long = try { getStringQuery(DocumentsContract.Document.COLUMN_SIZE)!!.toLong() } catch (_: Exception) {0L}
fun FileX.lastModified(): Long = try { getStringQuery(DocumentsContract.Document.COLUMN_LAST_MODIFIED)!!.toLong() } catch (_: Exception) {0L}

fun FileX.canRead(): Boolean {
    fCResolver.persistedUriPermissions.forEach {
        if (uri.toString().startsWith(it.uri.toString())) return it.isReadPermission
    }
    return false
}

fun FileX.canWrite(): Boolean {
    fCResolver.persistedUriPermissions.forEach {
        if (uri.toString().startsWith(it.uri.toString())) return it.isWritePermission
    }
    return false
}

val FileX.freeSpace: Long get() = getSpace(Space.FREE)
val FileX.usableSpace: Long get() = getSpace(Space.AVAILABLE)
val FileX.totalSpace: Long get() = getSpace(Space.TOTAL)

val FileX.isHidden: Boolean get() = name.startsWith(".")

//
//
// private methods
// *****************************************

private enum class Space {
    FREE, AVAILABLE, TOTAL
}
private fun FileX.getSpace(spaceType: Space): Long {
    if (rootUri == null) return 0L
    return try {
        val pfd = fCResolver.openFileDescriptor(DocumentsContract.buildDocumentUriUsingTree(rootUri!!, rootDocumentId), "r")
        val stats = Os.fstatvfs(pfd?.fileDescriptor)
        (when(spaceType) {
            Space.FREE -> stats.f_bfree
            Space.AVAILABLE -> stats.f_bavail
            Space.TOTAL -> stats.f_blocks
        } * stats.f_bsize).apply { tryIt { pfd?.close() } }
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }
}