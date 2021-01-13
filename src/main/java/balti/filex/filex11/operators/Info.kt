package balti.filex.filex11.operators

import android.net.Uri
import android.provider.DocumentsContract
import android.system.Os
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.storageVolumes
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.filex11.utils.Tools.checkUriExists
import balti.filex.filex11.utils.Tools.getStringQuery
import balti.filex.filex11.utils.Tools.removeRearSlash

// public methods
// *****************************************

val FileX11.canonicalPath: String get() = "${volumePath}${storagePath}"
val FileX11.absolutePath: String get() = canonicalPath

val FileX11.storagePath: String get () =
    rootDocumentId.split(":").let {
        if (it.size > 1) "/${removeRearSlash(it[1])}$path" else path
    }

val FileX11.volumePath: String get () = uri.let {
    rootDocumentId.split(":").let {
        if (it.isNotEmpty()) {
            storageVolumes[it[0]].let { it ?: "" }
        } else ""
    }
}

fun FileX11.exists(): Boolean {
    refreshFile()
    return uri?.let { checkUriExists(it) }?: false
}
val FileX11.isDirectory: Boolean get() =
    exists() && try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) == DocumentsContract.Document.MIME_TYPE_DIR }
    catch (_: Exception) {false}

val FileX11.isFile: Boolean get() =
    exists() && try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) != DocumentsContract.Document.MIME_TYPE_DIR }
    catch (_: Exception) {false}

val FileX11.name: String
    get() {
        return when {
            path.isNotBlank() -> path.split("/").let { it[it.size - 1] }
            else -> try {
                getStringQuery(DocumentsContract.Document.COLUMN_DISPLAY_NAME) ?: ""
            } catch (_: Exception) { "" }
        }
    }

val FileX11.parent: String? get() {
    return when {
        path == "/" -> null
        path.indexOf('/') != path.lastIndexOf('/') -> path.substring(0, path.lastIndexOf("/"))
        else -> "/"
    }
}

val FileX11.parentFile: FileX11? get() = parent?.let { FileX11(it) }

val FileX11.parentUri: Uri? get() = parentFile?.uri

val FileX11.parentCanonical: String get() = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "" }

fun FileX11.length(): Long = try { getStringQuery(DocumentsContract.Document.COLUMN_SIZE)!!.toLong() } catch (_: Exception) {0L}
fun FileX11.lastModified(): Long = try { getStringQuery(DocumentsContract.Document.COLUMN_LAST_MODIFIED)!!.toLong() } catch (_: Exception) {0L}

fun FileX11.canRead(): Boolean {
    fCResolver.persistedUriPermissions.forEach {
        if (uri.toString().startsWith(it.uri.toString())) return it.isReadPermission
    }
    return false
}

fun FileX11.canWrite(): Boolean {
    fCResolver.persistedUriPermissions.forEach {
        if (uri.toString().startsWith(it.uri.toString())) return it.isWritePermission
    }
    return false
}

val FileX11.freeSpace: Long get() = getSpace(balti.filex.filex11.operators.Space.FREE)
val FileX11.usableSpace: Long get() = getSpace(balti.filex.filex11.operators.Space.AVAILABLE)
val FileX11.totalSpace: Long get() = getSpace(balti.filex.filex11.operators.Space.TOTAL)

val FileX11.isHidden: Boolean get() = name.startsWith(".")

val FileX11.extension: String get() = name.substringAfterLast('.', "")
val FileX11.nameWithoutExtension: String get() = name.substringBeforeLast(".")

//
//
// private methods
// *****************************************

private enum class Space {
    FREE, AVAILABLE, TOTAL
}
private fun FileX11.getSpace(spaceType: Space): Long {
    if (rootUri == null) return 0L
    return try {
        val pfd = fCResolver.openFileDescriptor(DocumentsContract.buildDocumentUriUsingTree(rootUri!!, rootDocumentId), "r")
        val stats = Os.fstatvfs(pfd?.fileDescriptor)
        (when(spaceType) {
            balti.filex.filex11.operators.Space.FREE -> stats.f_bfree
            balti.filex.filex11.operators.Space.AVAILABLE -> stats.f_bavail
            balti.filex.filex11.operators.Space.TOTAL -> stats.f_blocks
        } * stats.f_bsize).apply { tryIt { pfd?.close() } }
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }
}