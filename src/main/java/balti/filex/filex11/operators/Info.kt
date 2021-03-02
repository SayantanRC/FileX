package balti.filex.filex11.operators

import android.net.Uri
import android.provider.DocumentsContract
import android.system.Os
import balti.filex.FileXInit
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.storageVolumes
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.Tools.removeRearSlash
import balti.filex.filex11.FileX11
import balti.filex.filex11.utils.Tools.checkUriExists
import balti.filex.filex11.utils.Tools.convertToDocumentUri
import balti.filex.filex11.utils.Tools.getStringQuery

internal class Info(private val f: FileX11) {

    val canonicalPath: String get() = f.run { "${volumePath}${storagePath}" }
    val absolutePath: String get() = canonicalPath
    val rootPath: String get() = canonicalPath.let { it.substring(0, it.indexOf(f.path)) }

    val storagePath: String
        get() = f.run {
            rootDocumentId.split(":").let {
                if (it.size > 1 && it[1].isNotBlank()) "/${removeRearSlash(it[1])}$path" else path
            }
        }

    val volumePath: String
        get() = f.run {
            FileXInit.refreshStorageVolumes()

            rootDocumentId.split(":").let {
                //Log.d(FileXInit.DEBUG_TAG, "rootDocId: $rootDocumentId")
                if (it.isNotEmpty()) {
                    val uuid = it[0]
                    storageVolumes[uuid] ?: ""
                } else ""
            }
        }

    fun exists(): Boolean = f.run{
        refreshFile()
        return uri?.let { checkUriExists(it) } ?: false
    }

    val isDirectory: Boolean
        get() =
            exists() && try {
                f.getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) == DocumentsContract.Document.MIME_TYPE_DIR
            } catch (_: Exception) {
                false
            }

    val isFile: Boolean
        get() =
            exists() && try {
                f.getStringQuery(DocumentsContract.Document.COLUMN_MIME_TYPE) != DocumentsContract.Document.MIME_TYPE_DIR
            } catch (_: Exception) {
                false
            }

    val name: String
        get() = f.run {
            return when {
                path.isNotBlank() -> path.split("/").let { it[it.size - 1] }
                else -> try {
                    getStringQuery(DocumentsContract.Document.COLUMN_DISPLAY_NAME) ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
        }

    val parent: String?
        get() = f.run {
            return when {
                path == "/" -> null
                path.indexOf('/') != path.lastIndexOf('/') -> path.substring(0, path.lastIndexOf("/"))
                else -> "/"
            }
        }

    val parentFile: FileX11? get() = parent?.let { FileX11(it) }

    val parentUri: Uri? get() = if (parent != "/") parentFile?.uri else f.rootUri?.let { convertToDocumentUri(it) }

    val parentCanonical: String get() = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "/" }

    fun length(): Long = try {
        f.getStringQuery(DocumentsContract.Document.COLUMN_SIZE)!!.toLong()
    } catch (_: Exception) {
        0L
    }

    fun lastModified(): Long = try {
        f.getStringQuery(DocumentsContract.Document.COLUMN_LAST_MODIFIED)!!.toLong()
    } catch (_: Exception) {
        0L
    }

    fun canRead(): Boolean {
        fCResolver.persistedUriPermissions.forEach {
            if (f.uri.toString().startsWith(it.uri.toString())) return it.isReadPermission
        }
        return false
    }

    fun canWrite(): Boolean {
        fCResolver.persistedUriPermissions.forEach {
            if (f.uri.toString().startsWith(it.uri.toString())) return it.isWritePermission
        }
        return false
    }

    val freeSpace: Long get() = getSpace(Space.FREE)
    val usableSpace: Long get() = getSpace(Space.AVAILABLE)
    val totalSpace: Long get() = getSpace(Space.TOTAL)

    val isHidden: Boolean get() = name.startsWith(".")

    val extension: String get() = name.substringAfterLast('.', "")
    val nameWithoutExtension: String get() = name.substringBeforeLast(".")

//
//
// private methods
// *****************************************

    private enum class Space {
        FREE, AVAILABLE, TOTAL
    }

    private fun getSpace(spaceType: Space): Long = f.run {
        if (rootUri == null) return 0L
        return try {
            val pfd = fCResolver.openFileDescriptor(DocumentsContract.buildDocumentUriUsingTree(rootUri!!, rootDocumentId), "r")
            val stats = Os.fstatvfs(pfd?.fileDescriptor)
            (when (spaceType) {
                Space.FREE -> stats.f_bfree
                Space.AVAILABLE -> stats.f_bavail
                Space.TOTAL -> stats.f_blocks
            } * stats.f_bsize).apply { tryIt { pfd?.close() } }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}