package balti.filex.operators

import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.fContext
import balti.filex.FileXServer
import balti.filex.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.utils.Tools.getChildrenUri

fun FileX.refreshFile(){
    val dirs = if (path.length > 1) path.substring(1).split("/") else ArrayList(0)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID)
    var childrenUri = getChildrenUri(rootUri!!)
    for (i in dirs.indices) {
        val dir = dirs[i]
        var nextDocId = ""
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    if (getString(0) == dir) {
                        nextDocId = getString(1)
                        break
                    }
                }
                close()
            }
        }
        catch (_: Exception){
            break
        }
        if (i < dirs.indices.last) childrenUri = getChildrenUri(nextDocId)
        else if (nextDocId != "") {
            FileXServer.setPathAndUri(rootUri!!, path, buildTreeDocumentUriFromId(nextDocId))
        }
    }
}

/*
private var copyError: Exception? = null

fun FileX.delete(): Boolean = documentFile?.delete()?: false
fun FileX.deleteRecursively(): Boolean {
    fun internalDelete(df: DocumentFile){
        if (df.isDirectory) df.listFiles().forEach { internalDelete(it) }
        df.delete()
    }
    return documentFile?.let { internalDelete(it); it.exists() }?: false
}

private fun FileX.copy(fileX: FileX, overWrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE,
                       optionalUpdateFunction: (copyingFileX: FileX, total: Long, done: Long) -> Boolean = {_, _, _ -> true },
                       onConflict: ((conflictSource: FileX, conflictDest: FileX) -> FileX.FileXCodes)? = null, fromRecursive: Boolean = false
): FileX.FileXCodes {
    if (fileX.exists()) {
        if (isDirectory && fileX.isEmpty && fromRecursive) return FileX.FileXCodes.OK
        else if (!overWrite) return FileX.FileXCodes.SKIP
        else if (onConflict != null) onConflict(this, fileX).let {
            if (it == FileX.FileXCodes.SKIP || it == FileX.FileXCodes.TERMINATE) return it
            else {
                // overwrite source is not a directory
                if ((isFile || isDirectory) && fileX.isFile) fileX.delete()
                if (isFile && fileX.isDirectory) {
                    if (fromRecursive) fileX.deleteRecursively()
                    else if (fileX.isEmpty) fileX.delete()
                    else return FileX.FileXCodes.SKIP
                }
                if (isDirectory && fileX.isDirectory) return if (it != FileX.FileXCodes.MERGE) FileX.FileXCodes.SKIP else FileX.FileXCodes.OK
            }
        }
        else if ((fileX.isDirectory && fileX.isEmpty) || fileX.isFile) fileX.delete()
        else return FileX.FileXCodes.SKIP
    }
    if (isFile) {
        fileX.createNewFile(mimeType).let { if (!it) return FileX.FileXCodes.SKIP }
        val dest = fContext.contentResolver.openOutputStream(fileX.uri)
        val source = fContext.contentResolver.openInputStream(uri)
        val buffer = ByteArray(bufferSize)
        val sLen = length
        var done = 0L
        return try {
            while (true) {
                val read = source?.read(buffer) ?: 0
                if (read == -1) break
                else dest?.write(buffer, 0, read)
                done += read
                if (!optionalUpdateFunction(fileX, sLen, done)) break
            }
            dest?.close()
            source?.close()
            if (fileX.exists() && fileX.length == length){ copyError = null; FileX.FileXCodes.OK }
            else FileX.FileXCodes.SKIP
        } catch (e: Exception) {
            copyError = e
            e.printStackTrace()
            FileX.FileXCodes.SKIP
        }
    }
    else {
        return if (!fileX.mkdirs()) FileX.FileXCodes.SKIP
        else if (isEmpty) FileX.FileXCodes.OK
        else FileX.FileXCodes.SKIP
    }
}

fun FileX.copyTo(fileX: FileX, overWrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE,
                 optionalUpdateFunction: (copyingFileX: FileX, total: Long, done: Long) -> Boolean = {_, _, _ -> true },
                 onConflict: ((conflictSource: FileX, conflictDest: FileX) -> FileX.FileXCodes)? = null
): Boolean = copy(fileX, overWrite, bufferSize, optionalUpdateFunction, onConflict) == FileX.FileXCodes.OK

fun FileX.copyRecursively(fileX: FileX, overWrite: Boolean = false,
                          onError: ((fileX: FileX, exception: Exception) -> OnErrorAction)? = null,
                          bufferSize: Int = DEFAULT_BUFFER_SIZE,
                          onConflict: ((conflictSource: FileX, conflictDest: FileX) -> FileX.FileXCodes)? = null,
                          optionalFileUpdateFunction: (copyingFileX: FileX, total: Long, done: Long) -> Boolean = {_, _, _ -> true },
                          optionalStatUpdateFunction: (total: Long, done: Long) -> Boolean = {_, _ -> true }): Boolean {

    fun internalCopy(toCopy: FileX, parentOrTarget: FileX): FileX.FileXCodes {
        if (!toCopy.exists()) return FileX.FileXCodes.SKIP
        if (toCopy.isFile) {
            toCopy.copy(parentOrTarget, overWrite, bufferSize, optionalFileUpdateFunction, onConflict, true).let {
                if (it == FileX.FileXCodes.SKIP) copyError?.let { onError?.invoke(parentOrTarget, it) }
                return it
            }
        }
        else {
            //if (!parentOrTarget.exists()) parentOrTarget.mkdirs().let { if (!it) return FileX.FileXCodes.SKIP }
            toCopy.copy(parentOrTarget, overWrite, bufferSize, onConflict = onConflict, fromRecursive = true).let {
                if (it == FileX.FileXCodes.SKIP || it == FileX.FileXCodes.TERMINATE) return it
            }
            val contents = toCopy.list()
            var r = FileX.FileXCodes.SKIP
            for (content in contents){
                val target = FileX(parentOrTarget, content.name)
                r = internalCopy(content, target)
                if (r == FileX.FileXCodes.SKIP) copyError?.let { onError?.invoke(target, it) }
                if (r == FileX.FileXCodes.TERMINATE) return FileX.FileXCodes.TERMINATE
            }
            return r
        }
    }

    return internalCopy(this, fileX) == FileX.FileXCodes.OK
}
*/
