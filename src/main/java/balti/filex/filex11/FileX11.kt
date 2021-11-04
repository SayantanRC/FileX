package balti.filex.filex11

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.refreshFileOnCreation
import balti.filex.Quad
import balti.filex.Tools.removeTrailingSlashOrColonAddFrontSlash
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.exceptions.RootNotInitializedException
import balti.filex.filex11.operators.*
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
import balti.filex.filex11.utils.RootUri.getGlobalRootUri
import balti.filex.filex11.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.filex11.utils.Tools.checkUriExists
import balti.filex.filex11.utils.Tools.convertToDocumentUri
import java.io.File
import java.io.InputStream
import java.io.OutputStream


internal class FileX11(path: String, currentRootUri: Uri? = null): FileX(false), LifecycleOwner {

    internal constructor(uri: Uri, currentRootUri: Uri) : this(
        currentRootUri.let {
            val docId = DocumentsContract.getDocumentId(uri)
            val rootId = DocumentsContract.getTreeDocumentId(it)
            if (!docId.startsWith(rootId)) throw IllegalArgumentException("Root uri not parent of given uri")
            else if (rootId == docId) ""
            else docId.substring(rootId.length)
        }
    ) { this.uri = uri; rootUri = currentRootUri; }

    fun setLocalRootUri(afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
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
                } ?: afterJob?.invoke(resultCode, data)
            }
            else afterJob?.invoke(resultCode, data)
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

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private fun init(initPath: String? = null, currentRootUri: Uri? = null){
        currentRootUri?.let{ root ->
            convertToDocumentUri(root)?.let { conv ->
                val permissibleUris = fCResolver.persistedUriPermissions.map { it.uri }
                if (checkUriExists(conv) && permissibleUris.contains(root)) rootUri = root
            }
        }
        if (rootUri == null) rootUri = getGlobalRootUri().apply {
            if (this == null) throw RootNotInitializedException("Global root uri not set")
        }
        if (initPath != null) {
            this.path = removeTrailingSlashOrColonAddFrontSlash(initPath)
            if (initPath == "" || initPath == "/") {
                uri = buildTreeDocumentUriFromId(rootDocumentId)
            }
        }
    }

    init {
        init(path, currentRootUri)
        val runnable = Runnable {
            lifecycleRegistry = LifecycleRegistry(this)
            FileXServer.pathAndUri.observe(this) {
                if (it.first == rootUri && it.second == this.path && it.third != null) {
                    directlySetUriAndPath(it.third, it.fourth)
                }
            }
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
        if (refreshFileOnCreation) refreshFile()
        if (Looper.myLooper() == Looper.getMainLooper()) runnable.run()
        else Handler(Looper.getMainLooper()).post(runnable)
    }

    /*enum class FileXCodes {
        OK, OVERWRITE, SKIP, TERMINATE, MERGE, NEW_IF_EXISTS
    }*/

    internal fun directlySetUriAndPath(uri: Uri?, path: String?){
        uri?.let { this.uri = it }
        path?.let { this.path = removeTrailingSlashOrColonAddFrontSlash(it) }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    private val Info = Info(this)

    override val file: File get() = File(Info.canonicalPath)
    override fun refreshFile() = refreshFileX11()

    override val canonicalPath: String get() = Info.canonicalPath
    override val absolutePath: String get() = Info.absolutePath
    override fun exists(): Boolean = Info.exists()
    override val isDirectory: Boolean get() = Info.isDirectory
    override val isFile: Boolean get() = Info.isFile
    override val name: String get() = Info.name
    override val parent: String? get() = Info.parent
    override val parentFile: FileX? get() = Info.parentFile
    override val storagePath: String get() = Info.storagePath
    override val volumePath: String get() = Info.volumePath
    override val rootPath: String get() = Info.rootPath
    override val parentUri: Uri? get() = Info.parentUri
    override fun canExecute(): Boolean = false
    override val parentCanonical: String get() = Info.parentCanonical
    override fun length(): Long = Info.length()
    override fun lastModified(): Long = Info.lastModified()
    override fun canRead(): Boolean = Info.canRead()
    override fun canWrite(): Boolean = Info.canWrite()
    override val extension: String get() = Info.extension
    override val nameWithoutExtension: String get() = Info.nameWithoutExtension
    override val freeSpace: Long get() = Info.freeSpace
    override val usableSpace: Long get() = Info.usableSpace
    override val totalSpace: Long get() = Info.totalSpace
    override val isHidden: Boolean get() = Info.isHidden

    private val Delete = Delete(this)

    override fun delete(): Boolean = Delete.delete()
    override fun deleteRecursively(): Boolean = Delete.deleteRecursively()
    override fun deleteOnExit() = Delete.deleteOnExit()

    private val Create = Create(this)

    override fun createNewFile(): Boolean = Create.createNewFile()
    override fun createNewFile(makeDirectories: Boolean, overwriteIfExists: Boolean, optionalMimeType: String): Boolean =
            Create.createNewFile(makeDirectories, overwriteIfExists, optionalMimeType)
    override fun mkdir(): Boolean = Create.mkdir()
    override fun mkdirs(): Boolean = Create.mkdirs()
    override fun createFileUsingPicker(optionalMimeType: String, afterJob: ((resultCode: Int, data: Intent?) -> Unit)?) =
            Create.createFileUsingPicker(optionalMimeType, afterJob)

    private val Modify = Modify(this)

    override fun renameTo(dest: FileX): Boolean = Modify.renameTo(dest)
    override fun renameTo(newFileName: String): Boolean = Modify.renameTo(newFileName)

    private val Filter = Filter(this)

    override val isEmpty: Boolean get() = Filter.isEmpty
    override fun listFiles(): Array<FileX>? = Filter.listFiles()
    override fun listFiles(filter: FileXFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun listFiles(filter: FileXNameFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun list() = Filter.list()
    override fun list(filter: FileXFilter): Array<String>? = Filter.list(filter)
    override fun list(filter: FileXNameFilter): Array<String>? = Filter.list(filter)
    override fun listEverythingInQuad(): List<Quad<String, Boolean, Long, Long>>? = Filter.listEverythingInQuad()
    override fun listEverything(): Quad<List<String>, List<Boolean>, List<Long>, List<Long>>? = Filter.listEverything()

    private val Operations = Operations(this)

    override fun inputStream(): InputStream? = Operations.inputStream()
    override fun outputStream(mode: String): OutputStream? = Operations.outputStream(mode)
}
