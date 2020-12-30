package balti.filex.utils

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.database.getFloatOrNull
import androidx.core.database.getStringOrNull
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit
import balti.filex.FileXInit.Companion.DEBUG_TAG
import balti.filex.FileXInit.Companion.fContext
import balti.filex.exceptions.RootNotInitializedException
import balti.filex.utils.Tools.getStringQuery

object Tools {
    fun traversePath(
        fileX: FileX,
        fileFunc: (uri: Uri, name: String) -> Boolean?,
        directoryFunc: ((uri: Uri, name: String) -> Unit)? = null,
        autoCreateSubDirectories: Boolean = true
    ): Boolean {

        if (fileX.rootUri == null) throw RootNotInitializedException("Root uri not initialised")

        val parts = fileX.path.split("/")
        var previousUri: Uri = fileX.rootUri!!
        for (i in parts.indices){
            if (i == parts.size-1) return fileFunc(previousUri, parts[i])?: false
            else {
                val df = DocumentFile.fromTreeUri(fContext, previousUri)
                val sub = df?.run { findFile(parts[i])?: if (autoCreateSubDirectories) createDirectory(parts[i]) else null }
                val subUri = sub?.uri
                if (subUri != null && sub.isDirectory) {
                    previousUri = subUri
                    directoryFunc?.invoke(previousUri, parts[i])
                } else { directoryFunc?.invoke(Uri.EMPTY, parts[i]); return false }
            }
        }
        return true
    }

    private val PRIMARY_VOLUME_NAME = "primary"
    private val DOWNLOADS_VOLUME_NAME = "downloads"
    private val ACTUAL_DOWNLOAD_DIRECTORY_NAME = "Download"

    internal fun getStorageVolumes(): HashMap<String, String?> {
        val allVolumes = HashMap<String, String?>(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val mStorageManager: StorageManager =
                    fContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val storageVolumes: List<StorageVolume> = mStorageManager.storageVolumes
                for (storageVolume in storageVolumes) {
                    // primary volume?
                    if (storageVolume.isPrimary) allVolumes[PRIMARY_VOLUME_NAME] =
                        storageVolume.directory?.path

                    // other volumes?
                    val uuid: String? = storageVolume.uuid
                    if (uuid != null) allVolumes[uuid] = storageVolume.directory?.path
                }
                // not found.
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        else {
            allVolumes[PRIMARY_VOLUME_NAME] = Environment.getExternalStorageDirectory()?.absolutePath
            allVolumes[DOWNLOADS_VOLUME_NAME] = Environment.getExternalStorageDirectory()?.let { it.absolutePath + "/" + ACTUAL_DOWNLOAD_DIRECTORY_NAME }?: ""
        }
        return allVolumes
    }


    internal fun FileX.buildTreeDocumentUriFromId(documentId: String): Uri{
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(rootUri!!.authority)
            .appendPath("tree").appendPath(rootDocumentId)
            .appendPath("document").appendPath(documentId)
            .build()
    }

    internal fun FileX.getStringQuery(field: String, documentUri: Uri = this.uri?: Uri.EMPTY): String? {
        if (uri == Uri.EMPTY) return null
        return documentUri.let { uri ->
            val cursor = FileXInit.fCResolver.query(uri, arrayOf(field), null, null, null)
            cursor?.moveToFirst()
            cursor?.getString(0).apply {
                cursor?.close()
            }
        }
    }

    internal fun FileX.getStringQuery(field: String, documentId: String): String? =
        getStringQuery(field, buildTreeDocumentUriFromId(documentId))


    @Suppress("NAME_SHADOWING")
    internal fun removeLeadingTrailingSlashOrColon(path: String): String {
        path.trim().let { path ->
            if (path.isBlank()) return ""
            val noFrontColon = if (path.startsWith(":")) {
                if (path.length > 1) path.substring(1)
                else ""
            } else path
            val withFrontSlash = noFrontColon.let { if (!it.startsWith("/")) "/$it" else it }
            return removeRearSlash(withFrontSlash)
        }
    }

    @Suppress("NAME_SHADOWING")
    internal fun removeRearSlash(path: String): String {
        path.trim().let { path ->
            if (path.isBlank() || path == "/") return "/"
            return if (path.last() == '/') {
                if (path.length > 1) path.substring(0, path.length - 1)
                else "/"
            } else path
        }
    }

    internal fun checkUriExists(uri: Uri): Boolean{
        var result = false
        try {
            val c = FileXInit.fCResolver.query(uri, null, null, null, null, null)
            if (c != null && c.count > 0 && c.moveToFirst()) result = c.getString(4) != null
            c?.close()
        }
        catch (_: Exception){}
        return result
    }

    internal fun FileX.getChildrenUri(docId: Uri): Uri {
        return DocumentsContract.buildChildDocumentsUriUsingTree(
            rootUri,
            if (docId == rootUri) DocumentsContract.getTreeDocumentId(rootUri)
            else DocumentsContract.getDocumentId(docId)
        )
    }
    internal fun FileX.getChildrenUri(docId: String): Uri {
        return DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, docId)
    }
}