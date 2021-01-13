package balti.filex.filex11

import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.*
import balti.filex.FileX
import balti.filex.FileXInit.Companion.refreshFileOnCreation
import balti.filex.Tools.removeLeadingTrailingSlashOrColon
import balti.filex.filex11.exceptions.RootNotInitializedException
import balti.filex.filex11.operators.refreshFile
import balti.filex.filex11.utils.RootUri.getGlobalRootUri
import balti.filex.filex11.utils.Tools.buildTreeDocumentUriFromId


class FileX11(path: String): FileX(false), LifecycleOwner {

    constructor(parent: String, child: String): this("$parent/$child")
    internal constructor(uri: Uri, currentRootUri: Uri) : this(
        currentRootUri.let {
            val docId = DocumentsContract.getDocumentId(uri)
            val rootId = DocumentsContract.getTreeDocumentId(it)
            if (!docId.startsWith(rootId)) throw IllegalArgumentException("Root uri not parent of given uri")
            else if (rootId == docId) ""
            else docId.substring(rootId.length)
        }
    ) { this.uri = uri; rootUri = currentRootUri; }

    var rootDocumentId: String = ""
    private set

    var rootUri: Uri? = null
    private set(value) {
        rootDocumentId = if (value != null) DocumentsContract.getTreeDocumentId(value) else ""
        field = value
    }

    var documentId: String? = null
    private set

    var uri: Uri? = null
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
            this.path = removeLeadingTrailingSlashOrColon(initPath)
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
                if (it.fourth != null) this.path = removeLeadingTrailingSlashOrColon(it.fourth)
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

}
