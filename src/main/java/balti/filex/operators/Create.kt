package balti.filex.operators

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit.Companion.DEBUG_TAG
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.fContext
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.FileXServer
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.exceptions.DirectoryHierarchyBroken
import balti.filex.exceptions.RootNotInitializedException
import balti.filex.utils.Tools
import balti.filex.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.utils.Tools.checkUriExists
import balti.filex.utils.Tools.getChildrenUri
import balti.filex.utils.Tools.getStringQuery
import java.io.IOException

// public methods
// *****************************************

fun FileX.createFileUsingPicker(optionalMimeType: String = "*/*", afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
    if (rootUri == null) throw RootNotInitializedException("root not initialised")
    val JOB_CODE = 200
    ActivityFunctionDelegate(JOB_CODE, Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = optionalMimeType
        mimeType = optionalMimeType
        putExtra(Intent.EXTRA_TITLE, this@createFileUsingPicker.name)
    }) { _, _, resultCode, data ->
        FileXServer.setPathAndUri(rootUri!!, path, data?.data)
        afterJob?.invoke(resultCode, data)
    }
}

fun FileX.createNewFile(optionalMimeType: String = "*/*", makeDirectories: Boolean = false, overwriteIfExists: Boolean = false) : Boolean {
    if (!makeDirectories && uri != null){
        return uri?.let {
            if (!exists()) createBlankDoc(parentUri ?: rootUri!!, name, optionalMimeType)
            else if (overwriteIfExists) {
                tryIt { DocumentsContract.deleteDocument(cResolver, it) }
                createBlankDoc(parentUri ?: rootUri!!, name, optionalMimeType)
            }
            else false
        }?: false
    }
    else return traverse({dir, nextDocId, childrenUri ->
        if (nextDocId == "") {
            if (makeDirectories) getChildrenUri(DocumentsContract.createDocument(cResolver, childrenUri, DocumentsContract.Document.MIME_TYPE_DIR, dir)!!)
            else throw DirectoryHierarchyBroken("No such file or directory")
        } else getChildrenUri(nextDocId)
    }, {fileName, nextDocId, childrenUri ->
        return@traverse if (nextDocId != ""){
            val existingUri = buildTreeDocumentUriFromId(nextDocId)
            if (overwriteIfExists) {
                DocumentsContract.deleteDocument(cResolver, existingUri)
                createBlankDoc(childrenUri, fileName)
            }
            else {
                FileXServer.setPathAndUri(rootUri!!, path, existingUri)
                false
            }
        }
        else {
            createBlankDoc(childrenUri, fileName)
        }
    })
}

fun FileX.mkdirs(): Boolean = traverse({dir, nextDocId, childrenUri ->
        if (nextDocId == "") {
            getChildrenUri(DocumentsContract.createDocument(cResolver, childrenUri, DocumentsContract.Document.MIME_TYPE_DIR, dir)!!)
        } else getChildrenUri(nextDocId)
    }, {fileName, nextDocId, childrenUri ->
        if (nextDocId == "") createBlankDoc(childrenUri, fileName, DocumentsContract.Document.MIME_TYPE_DIR)
        else false
    }
)

fun FileX.mkdir(): Boolean = traverse({_, nextDocId, _ ->
        if (nextDocId == "") null else getChildrenUri(nextDocId)
    }, {fileName, nextDocId, childrenUri ->
        if (nextDocId == "") createBlankDoc(childrenUri, fileName, DocumentsContract.Document.MIME_TYPE_DIR)
        else false
    }
)

//
//
// private methods
// *****************************************

private val cResolver = fCResolver

private fun FileX.createBlankDoc(parentUri: Uri, fileName: String, optionalMimeType: String = "*/*"): Boolean {
    if (!parentUri.toString().endsWith("/children") && !checkUriExists(parentUri)) throw DirectoryHierarchyBroken("Complete parent uri not present: $parentUri")
    return DocumentsContract.createDocument(cResolver, parentUri, optionalMimeType, fileName).let {
        if (it != null) {
            FileXServer.setPathAndUri(rootUri!!, path, it)
            true
        }
        else false
    }
}

private fun FileX.traverse(
    directoryFunc: (dirName: String, nextDocId: String, childrenUri: Uri) -> Uri?,
    fileFunction: (fileName: String, nextDocId: String, childrenUri: Uri) -> Boolean): Boolean {

        val dirs = if (path.length > 1) path.substring(1).split("/") else ArrayList(0)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        var childrenUri = getChildrenUri(rootUri!!)
        for (i in dirs.indices) {
            val dir = dirs[i]
            var nextDocId = ""
            cResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    if (getString(0) == dir) {
                        nextDocId = getString(1)
                        break
                    }
                }
                close()
            }
            if (i < dirs.indices.last) directoryFunc(dir, nextDocId, childrenUri).let { if (it != null) childrenUri = it else return false }
            else return fileFunction(dirs.last(), nextDocId, childrenUri)
        }
        return false
    }