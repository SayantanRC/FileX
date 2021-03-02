package balti.filex.filex11.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import balti.filex.FileXInit
import balti.filex.FileXInit.Companion.fContext
import balti.filex.filex11.FileX11
import balti.filex.filex11.utils.Constants.PROBABLE_MNT
import java.io.File

object Tools {
    /*internal fun traversePath(
        fileX: FileX11,
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
    }*/

    private val PRIMARY_VOLUME_NAME = "primary"
    private val DOWNLOADS_VOLUME_NAME = "downloads"
    private val ACTUAL_DOWNLOAD_DIRECTORY_NAME = "Download"

    internal fun getStorageVolumes(): HashMap<String, String?> {
        val allVolumes = HashMap<String, String?>(0)
        val mStorageManager: StorageManager = fContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        // Android 11 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val storageVolumes: List<StorageVolume> = mStorageManager.storageVolumes
                storageVolumes.forEach { storageVolume ->
                    storageVolume.directory?.let {
                        if (storageVolume.isPrimary) allVolumes[PRIMARY_VOLUME_NAME] = it.path
                        val uuid: String? = storageVolume.uuid
                        if (uuid != null) allVolumes[uuid] = it.path
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        // lower android versions
        else {

            // next two lines common for all lower android versions
            Environment.getExternalStorageDirectory()?.absolutePath?.let { allVolumes[PRIMARY_VOLUME_NAME] = it }
            Environment.getExternalStorageDirectory()?.let { allVolumes[DOWNLOADS_VOLUME_NAME] =  it.absolutePath + "/" + ACTUAL_DOWNLOAD_DIRECTORY_NAME }

            val STOARGE_RAW_PATH = "/storage"
            val SELF_NAME = "self"
            val EMULATED_NAME = "emulated"

            // for Android N and above, useful to get SD-CARD paths
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fun getIfExists(uuid: String): String {
                    val expectedParents = arrayListOf(
                            STOARGE_RAW_PATH,
                            // Fill here with other known accessible paths, if available
                    )
                    expectedParents.forEach {
                        if (File(it, uuid).exists()) return it
                    }
                    return ""
                }
                mStorageManager.storageVolumes.forEach { storageVolume ->
                    storageVolume.uuid?.let { uuid ->
                        getIfExists(uuid).let {
                            if (it.isNotBlank()) allVolumes[uuid] = "$it/$uuid"
                            else allVolumes[uuid] = "$PROBABLE_MNT/$uuid"
                        }
                    }
                }
            }
            // For Android M
            else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                val storageDir = File(STOARGE_RAW_PATH)
                storageDir.list()?.run {
                    forEach {
                        if (it != SELF_NAME && it != EMULATED_NAME)
                            allVolumes[it] = "$STOARGE_RAW_PATH/$it"
                    }
                }
            }
            // No known way to find for Android L
        }

        return allVolumes
    }


    internal fun FileX11.buildTreeDocumentUriFromId(documentId: String): Uri{
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(rootUri!!.authority)
            .appendPath("tree").appendPath(rootDocumentId)
            .appendPath("document").appendPath(documentId)
            .build()
    }

    internal fun FileX11.getStringQuery(field: String, documentUri: Uri = this.uri?: Uri.EMPTY): String? {
        if (uri == Uri.EMPTY) return null
        return documentUri.let { uri ->
            val cursor = FileXInit.fCResolver.query(uri, arrayOf(field), null, null, null)
            cursor?.moveToFirst()
            cursor?.getString(0).apply {
                cursor?.close()
            }
        }
    }

    internal fun FileX11.getStringQuery(field: String, documentId: String): String? =
        getStringQuery(field, buildTreeDocumentUriFromId(documentId))

    internal fun convertToDocumentUri(uri: Uri): Uri? {
        return if (DocumentsContract.isDocumentUri(fContext, uri)) uri
        else try {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        }
        catch (_: Exception) { null }
    }

    internal fun checkUriExists(uri: Uri): Boolean{
        var result = false
        val evalUri = convertToDocumentUri(uri) ?: return false
        try {
            val c = FileXInit.fCResolver.query(evalUri, null, null, null, null, null)
            if (c != null && c.count > 0 && c.moveToFirst()) result = c.getString(4) != null
            c?.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return result
    }

    internal fun FileX11.getChildrenUri(docId: Uri): Uri {
        return DocumentsContract.buildChildDocumentsUriUsingTree(
            rootUri,
            if (docId == rootUri) DocumentsContract.getTreeDocumentId(rootUri)
            else DocumentsContract.getDocumentId(docId)
        )
    }
    internal fun FileX11.getChildrenUri(docId: String): Uri {
        return DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, docId)
    }
}