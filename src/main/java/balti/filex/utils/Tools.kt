package balti.filex.utils

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.database.getFloatOrNull
import androidx.core.database.getStringOrNull
import androidx.documentfile.provider.DocumentFile
import balti.filex.FileX
import balti.filex.FileXInit
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

    @TargetApi(Build.VERSION_CODES.R)
    internal fun getVolumePathForAndroid11AndAbove(volumeId: String): String? {
        return try {
            getStorageVolumes()[volumeId]
        } catch (ex: Exception) {
            null
        }
    }

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
            }
        }
        else {
            allVolumes[PRIMARY_VOLUME_NAME] = fContext.getExternalFilesDir(null)?.absolutePath
        }
        return allVolumes
    }


    internal fun FileX.buildTreeDocumentUriFromId(documentId: String): Uri{
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(rootUri!!.authority)
            .appendPath("tree").appendPath(rootDocumentId)
            .appendPath("document").appendPath(documentId)
            .build()
    }

    internal fun FileX.getStringQuery(field: String, documentUri: Uri = this.uri): String? {
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


    internal fun removeLeadingTrailingSlashOrColon(path: String): String {
        if (path.isBlank()) return ""
        val noFrontSlashOrColon = if (path.startsWith(":") or path.startsWith("/")) {
            if (path.length > 1) path.substring(1)
            else ""
        }
        else path
        return removeRearSlash(noFrontSlashOrColon)
    }

    internal fun removeRearSlash(path: String): String {
        if (path.isBlank()) return ""
        return if (path.last() == '/') {
            if (path.length > 1) path.substring(0, path.length -1)
            else ""
        } else path
    }

    internal fun checkUriExists(uri: Uri): Boolean{
        var result = false
        try {
            val c = FileXInit.fCResolver.query(uri, null, null, null, null, null)
            if (c != null && c.count > 0) result = true
            c?.close()
        }
        catch (_: Exception){}
        return result
    }
}