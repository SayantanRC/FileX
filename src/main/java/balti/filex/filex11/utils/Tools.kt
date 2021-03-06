package balti.filex.filex11.utils

import android.annotation.TargetApi
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
import balti.filex.filex11.utils.Constants.MNT_MEDIA_RW
import balti.filex.filex11.utils.Constants.STOARGE_RAW_PATH
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object Tools {

    private val PRIMARY_VOLUME_NAME = "primary"
    private val predefinedVolNames = HashMap<String, String>().apply {
        this["downloads"] = "Download"
        this["home"] = "Documents"
    }

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
            Environment.getExternalStorageDirectory()?.absolutePath?.let { storagePath ->
                predefinedVolNames.forEach { vol ->
                    allVolumes[vol.key] = "$storagePath/${vol.value}"
                }
            }

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
                            else allVolumes[uuid] = "$MNT_MEDIA_RW/$uuid"
                        }
                    }
                }
            }
            // For Android M
            else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                val storageDir = File(STOARGE_RAW_PATH)
                storageDir.list()?.run {
                    forEach {
                        if (it != SELF_NAME && it != EMULATED_NAME) {
                            // check for USB devices.
                            // Observation: USB OTG drives are mounted with executable flag off, but SDCARD is with executable on.
                            // They are available under /storage/..., they are neither readable nor writable.
                            // Oddly this location of the USB OTG drive (under /storage/...) is also not accessible with
                            // any root explorer or root based processes. It always displays empty.
                            // However USB OTG is also mounted at /mnt/media_rw, with same name.
                            // This location is also not readable/writable, but is accessible to any root based file explorer.
                            // Hence it is at-least somewhat usable than the location under /storage/...
                            //if (it.toUpperCase(Locale.ROOT) == it && !it.contains('-')): older logic found incorrect.
                            if (!File("$STOARGE_RAW_PATH/$it").canExecute())
                                allVolumes[it] = "$MNT_MEDIA_RW/$it"

                            // for SD-CARD
                            else allVolumes[it] = "$STOARGE_RAW_PATH/$it"
                        }
                    }
                }
            }
            // No reliable way to find volumes for Android L
            // please check deduceVolumePathForLollipop() below which is called from Info.volumePath

        }

        return allVolumes
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun FileX11.deduceVolumePathForLollipop(): String {
        val rawStorageDir = File(STOARGE_RAW_PATH)

        fun generateRandomFileName(): String {
            val currentTime = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ROOT).format(Calendar.getInstance().time)
            val randomNumber = (10000000..99999999).random()
            return "$currentTime$randomNumber"
        }

        val randomFileName = generateRandomFileName()

        // create a random file in the root uri
        val testFile = FileX11(path = "/.$randomFileName", currentRootUri = rootUri)
        testFile.createNewFile()
        val testFileStoragePath = testFile.storagePath
        val testFileLastModified = testFile.lastModified()

        val filteredRawStorages = rawStorageDir.listFiles()?.filter { it.canWrite() } ?: listOf()

        // check for the above created file in all possible locations and return when found
        for (f in filteredRawStorages) {
            File(f, testFileStoragePath).run {
                if (exists() && testFileLastModified == lastModified()) {
                    // found. delete and return.
                    delete()
                    return f.canonicalPath
                }
            }
        }

        // not found. delete and return empty string.
        testFile.delete()
        return ""
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

    internal fun checkUriExists(uri: Uri, checkIfDirectory: Boolean = false): Boolean{
        var result = false
        val evalUri = convertToDocumentUri(uri) ?: return false
        val projection = if (checkIfDirectory) arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE) else null
        try {
            val c = FileXInit.fCResolver.query(evalUri, projection, null, null, null, null)
            if (c != null && c.count > 0 && c.moveToFirst()) result = c.columnCount != 0
            if (result && checkIfDirectory) result = c?.getString(0) == DocumentsContract.Document.MIME_TYPE_DIR
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