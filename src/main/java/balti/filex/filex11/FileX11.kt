package balti.filex.filex11

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import balti.filex.FileX
import balti.filex.FileXInit.Companion.refreshFileOnCreation
import balti.filex.Tools.removeTrailingSlashOrColonAddFrontSlash
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.exceptions.ImproperFileXType
import balti.filex.filex11.Extend.operators.Info
import balti.filex.exceptions.RootNotInitializedException
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filex11.operators.Create
import balti.filex.filex11.operators.Delete
import balti.filex.filex11.operators.Filter
import balti.filex.filex11.operators.Modify
import balti.filex.filex11.utils.RootUri.getGlobalRootUri
import balti.filex.filex11.utils.Tools.buildTreeDocumentUriFromId
import java.io.File


internal class FileX11(path: String): FileX(false), LifecycleOwner {

    internal constructor(uri: Uri, currentRootUri: Uri) : this(
        currentRootUri.let {
            val docId = DocumentsContract.getDocumentId(uri)
            val rootId = DocumentsContract.getTreeDocumentId(it)
            if (!docId.startsWith(rootId)) throw IllegalArgumentException("Root uri not parent of given uri")
            else if (rootId == docId) ""
            else docId.substring(rootId.length)
        }
    ) { this.uri = uri; rootUri = currentRootUri; }

    fun setLocalRootUri(afterJob: ((resultCode: Int, data: Intent) -> Unit)? = null) {
        val JOB_CODE = 100
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ActivityFunctionDelegate(JOB_CODE,
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    flags = takeFlags
                }) { context, resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null) {
                data.data?.let {
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                    rootUri = it
                    uri = buildTreeDocumentUriFromId(rootDocumentId)
                    afterJob?.invoke(resultCode, data)
                }
            }
        }
    }

    var rootDocumentId: String = ""
    private set

    var rootUri: Uri? = null
    private set(value) {
        rootDocumentId = if (value != null) DocumentsContract.getTreeDocumentId(value) else ""
        field = value
    }

    var documentId: String? = null
    private set

    override var uri: Uri? = null
    private set(value) {
        if (value != null) {
            documentId =
                if (value != rootUri) DocumentsContract.getDocumentId(value)
                else DocumentsContract.getTreeDocumentId(value)
            field = value
        }
    }

    var mimeType: String = "*/*"
    internal set

    override var path: String = ""
    private set

    private val lifecycleRegistry: LifecycleRegistry

    private fun init(initPath: String? = null){
        if (rootUri == null) rootUri = getGlobalRootUri().apply {
            if (this == null) throw RootNotInitializedException("Global root uri not set")
        }
        if (initPath != null) {
            this.path = removeTrailingSlashOrColonAddFrontSlash(initPath)
            if (initPath == "") {
                uri = buildTreeDocumentUriFromId(rootDocumentId)
            }
        }
    }

    init {
        init(path)
        lifecycleRegistry = LifecycleRegistry(this)
        FileXServer.pathAndUri.observe(this) {
            if (it.first == rootUri && it.second == this.path && it.third != null) {
                uri = it.third
                if (it.fourth != null) this.path = removeTrailingSlashOrColonAddFrontSlash(it.fourth)
            }
        }
        if (refreshFileOnCreation) refreshFile()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    enum class FileXCodes {
        OK, OVERWRITE, SKIP, TERMINATE, MERGE, NEW_IF_EXISTS
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    private val Info = Info(this)

    override val file: File get() = File(Info.canonicalPath)

    override val canonicalPath: String = Info.canonicalPath
    override val absolutePath: String = Info.absolutePath
    override fun exists(): Boolean = Info.exists()
    override val isDirectory: Boolean = Info.isDirectory
    override val isFile: Boolean = Info.isFile
    override val name: String = Info.name
    override val parent: String? =  Info.parent
    override val parentFile: FileX? = Info.parentFile
    override val storagePath: String = Info.storagePath
    override val volumePath: String = Info.volumePath
    override val rootPath: String = Info.rootPath
    override val parentUri: Uri? = Info.parentUri
    override fun canExecute(): Boolean = false
    override val parentCanonical: String = Info.parentCanonical
    override fun length(): Long = Info.length()
    override fun lastModified(): Long = Info.lastModified()
    override fun canRead(): Boolean = Info.canRead()
    override fun canWrite(): Boolean = Info.canWrite()
    override val extension: String = Info.extension
    override val nameWithoutExtension: String = Info.nameWithoutExtension
    override val freeSpace: Long = Info.freeSpace
    override val usableSpace: Long = Info.usableSpace
    override val totalSpace: Long = Info.totalSpace
    override val isHidden: Boolean = Info.isHidden

    private val Delete = Delete(this)

    override fun delete(): Boolean = Delete.delete()
    override fun deleteRecursively(): Boolean = Delete.deleteRecursively()
    override fun deleteOnExit() {
        throw ImproperFileXType("Only applicable on traditional FileX")
    }

    private val Create = Create(this)

    override fun createNewFile(): Boolean = Create.createNewFile()
    override fun createNewFile(makeDirectories: Boolean, overwriteIfExists: Boolean, optionalMimeType: String): Boolean =
            Create.createNewFile(makeDirectories, overwriteIfExists, optionalMimeType)
    override fun mkdir(): Boolean = Create.mkdir()
    override fun mkdirs(): Boolean = Create.mkdirs()
    override fun createFileUsingPicker(optionalMimeType: String, afterJob: ((resultCode: Int, data: Intent?) -> Unit)?) =
            Create.createFileUsingPicker(optionalMimeType, afterJob)

    private val Modify = Modify(this)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun renameTo(dest: FileX): Boolean = Modify.renameTo(dest)
    override fun renameTo(newFileName: String): Boolean = Modify.renameTo(newFileName)

    private val Filter = Filter(this)

    override val isEmpty: Boolean = Filter.isEmpty
    override fun listFiles(): Array<FileX>? = Filter.listFiles()
    override fun listFiles(filter: FileXFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun listFiles(filter: FileXNameFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun list() = Filter.list()
    override fun list(filter: FileXFilter): Array<String>? = Filter.list(filter)
    override fun list(filter: FileXNameFilter): Array<String>? = Filter.list(filter)
}
