package balti.filex

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import balti.filex.filex11.FileX11
import balti.filex.filex11.activity.ActivityFunctionDelegate
import balti.filex.filex11.activity.TraditionalFileRequest
import balti.filex.filex11.utils.RootUri
import balti.filex.filex11.utils.Tools

class FileXInit(context: Context, val isTraditional: Boolean) {
    companion object{
        internal lateinit var fContext: Context
        private set

        private var fisTraditional: Boolean = false

        internal val fCResolver by lazy { fContext.contentResolver }

        internal val DEBUG_TAG = "FILEX_TAG"
        internal val PREF_NAME = "filex"

        internal fun tryIt(f: () -> Unit){
            try { f() } catch (e: Exception){
                try { Toast.makeText(fContext, e.message.toString(), Toast.LENGTH_SHORT).show() }
                catch (_: Exception){}
            }
        }

        internal val sharedPreferences by lazy { fContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

        var refreshFileOnCreation: Boolean = true
        val storageVolumes = HashMap<String, String?>(0)

        fun isUserPermissionGranted(): Boolean{
            return if (!fisTraditional) RootUri.getGlobalRootUri() != null
            else {
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun requestUserPermission(onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
            if (!fisTraditional) {
                RootUri.resetGlobalRootUri(){resultCode, data ->
                    onResult?.invoke(resultCode, data)
                }
            }
            else {
                ActivityFunctionDelegate({}, {_, resultCode, data ->
                    onResult?.invoke(resultCode, data)
                }, TraditionalFileRequest::class.java)
            }
        }

        fun refreshStorageVolumes() {
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
        fisTraditional = isTraditional
    }
}