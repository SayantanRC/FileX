package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.filex11.FileX11
import balti.filex.filex11.utils.Tools
import balti.filex.filex11.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.filex11.utils.Tools.getChildrenUri
import java.io.InputStream
import java.io.OutputStream

internal fun FileX11.refreshFileX11(){
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
            val toSetUri = buildTreeDocumentUriFromId(nextDocId)
            directlySetUriAndPath(toSetUri, path)
            balti.filex.filex11.FileXServer.setPathAndUri(rootUri!!, path, toSetUri)
        }
    }
}

internal class Operations(private val f: FileX11) {

    fun inputStream(): InputStream? = f.run {
        refreshFile()
        uri?.let { fCResolver.openInputStream(it) }
    }

    fun outputStream(mode: String): OutputStream? = f.run {
        refreshFile()
        uri?.let { fCResolver.openOutputStream(it, mode) }
    }
}