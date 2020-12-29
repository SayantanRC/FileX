package balti.filex.operators

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit
import balti.filex.FileXInit.Companion.DEBUG_TAG
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.fContext
import balti.filex.exceptions.FileXNotFoundException
import balti.filex.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.utils.Tools.checkUriExists
import balti.filex.utils.Tools.getStringQuery
import balti.filex.utils.Tools.removeLeadingTrailingSlashOrColon
import balti.filex.utils.Tools.removeRearSlash
import java.io.IOException

// public methods
// *****************************************

val FileX.canonicalPath: String get() = "${volumePath}/${storagePath}"

val FileX.storagePath: String get () = uri.let {
    documentId.split(":").let {
        if (it.size > 1) it[1] else ""
    }
}

val FileX.volumePath: String get () = uri.let {
    documentId.split(":").let {
        if (it.isNotEmpty()) {
            FileXInit.storageVolumes[it[0]].let { it ?: "" }
        } else ""
    }
}

fun FileX.exists(): Boolean {
    return checkUriExists(uri)
}
val FileX.isDirectory: Boolean get() =
    try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) == DocumentsContract.Document.MIME_TYPE_DIR }
    catch (_: Exception) {false}

val FileX.isFile: Boolean get() =
    try { getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) != DocumentsContract.Document.MIME_TYPE_DIR }
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
    return parentDocId.substring(rootDocumentId.length).let { if (it.isNotBlank()) removeLeadingTrailingSlashOrColon(it) else null}
}

val FileX.parentUri: Uri? get() = parentDocId.let {
    if (it != rootDocumentId) buildTreeDocumentUriFromId(it)
    else null
}

val FileX.parentCanonical: String get() = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "" }

val FileX.parentFile: FileX? get() = rootUri?.let { r-> parentUri?.let { p-> if (p.toString() != r.toString()) FileX(p, r) else null }}

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

//
//
// private methods
// *****************************************

private val FileX.parentDocId: String get() =
    if (rootDocumentId == documentId) rootDocumentId
    else documentId.split(":").let {
        if (it.size == 1) ""
        else {
            val part1 = removeRearSlash(it[1])
            "${it[0]}:${part1.substring(0, part1.lastIndexOf('/'))}"
        }
    }