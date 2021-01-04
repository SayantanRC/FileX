package balti.filex

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.*
import balti.filex.FileXInit.Companion.DEBUG_TAG
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.exceptions.RootNotInitializedException
import balti.filex.operators.refreshFile
import balti.filex.utils.Constants
import balti.filex.utils.Tools.buildTreeDocumentUriFromId
import balti.filex.utils.Tools.checkUriExists
import balti.filex.utils.Tools.removeLeadingTrailingSlashOrColon
import balti.filex.utils.Tools.removeRearSlash
import java.io.File


class FileX(path: String): LifecycleOwner {

    constructor(parent: String, child: String): this("$parent/$child")
    constructor(uri: Uri, currentRootUri: Uri) : this(
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

    fun setLocalRootUri(afterJob: ((resultCode: Int, data: Intent) -> Unit)? = null) {
        val JOB_CODE = 100
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ActivityFunctionDelegate(JOB_CODE,
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = takeFlags
            }) { context, _, resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null) {
                data.data?.let {
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                    rootUri = it
                    init()
                    afterJob?.invoke(resultCode, data)
                }
            }
        }
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

    var path: String = ""
    private set

    private val lifecycleRegistry: LifecycleRegistry

    private fun init(initPath: String? = null){
        if (rootUri == null) rootUri = FileXInit.getGlobalRootUri().apply {
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
        if (FileXInit.refreshFileOnCreation) refreshFile()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    enum class FileXCodes {
        OK, OVERWRITE, SKIP, TERMINATE, MERGE, NEW_IF_EXISTS
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
