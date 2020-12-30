package balti.filex

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import balti.filex.utils.Constants.PREF_GLOBAL_ROOT_URI
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.utils.Tools

class FileXInit(context: Context) {
    companion object{
        internal lateinit var fContext: Context
        private set
        internal val fCResolver by lazy { fContext.contentResolver }
        internal val storageVolumes = HashMap<String, String?>(0)

        internal val DEBUG_TAG = "FILEX_TAG"
        internal val PREF_NAME = "filex"

        internal val sharedPreferences by lazy { fContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

        internal fun tryIt(f: () -> Unit){
            try { f() } catch (e: Exception){
                try { Toast.makeText(fContext, e.message.toString(), Toast.LENGTH_SHORT).show() }
                catch (_: Exception){}
            }
        }

        // public methods and variables
        // *****************************************

        var refreshFileOnCreation = true

        fun setGlobalRootUri(afterJob: ((resultCode: Int, data: Intent) -> Unit)? = null){
            val uri = getGlobalRootUri()
            if (uri == null) resetGlobalRootUri(afterJob)
            else afterJob?.invoke(RESULT_OK, Intent().setData(uri))
        }

        fun resetGlobalRootUri(afterJob: ((resultCode: Int, data: Intent) -> Unit)? = null){
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ActivityFunctionDelegate(10,
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    flags = takeFlags
                }) { context, _, resultCode, data ->
                if (resultCode == RESULT_OK && data != null) {
                    data.data?.let {
                        context.contentResolver.takePersistableUriPermission(it, takeFlags)
                        sharedPreferences.edit().run {
                            putString(PREF_GLOBAL_ROOT_URI, it.toString())
                            commit()
                        }
                        afterJob?.invoke(resultCode, data)
                    }
                }
            }
        }

        fun getGlobalRootUri(): Uri? {
            val gr = sharedPreferences.getString(PREF_GLOBAL_ROOT_URI, "")
            if (gr == "") return null
            else {
                try {
                    val uri = Uri.parse(gr)
                    fContext.contentResolver.persistedUriPermissions.forEach {
                        if (it.uri == uri && it.isReadPermission && it.isWritePermission) return uri
                    }
                } catch (_: Exception){}
                return null
            }
        }

        fun refreshStorageVolumes(){
            storageVolumes.clear()
            Tools.getStorageVolumes().run {
                this.keys.forEach {
                    storageVolumes[it] = this[it]
                }
            }
        }
    }
    init {
        fContext = context.applicationContext
        refreshStorageVolumes()
    }
}