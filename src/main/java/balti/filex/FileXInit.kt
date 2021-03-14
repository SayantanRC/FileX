package balti.filex

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import balti.filex.activity.ActivityFunctionDelegate
import balti.filex.activity.TraditionalFileRequest
import balti.filex.filex11.utils.RootUri
import balti.filex.filex11.utils.Tools

class FileXInit(context: Context, isTraditional: Boolean) {
    companion object{
        internal lateinit var fContext: Context
        private set

        internal var globalIsTraditional: Boolean = false

        internal val fCResolver by lazy { fContext.contentResolver }

        internal val DEBUG_TAG = "FILEX_TAG"
        internal val PREF_NAME = "filex"

        internal fun tryIt(f: () -> Unit){
            try { f() } catch (e: Exception){
                try { Toast.makeText(fContext, e.message.toString(), Toast.LENGTH_SHORT).show() }
                catch (_: Exception){}
            }
        }

        val isTraditional: Boolean
        get() = globalIsTraditional

        internal val sharedPreferences by lazy { fContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

        var refreshFileOnCreation: Boolean = true
        val storageVolumes = HashMap<String, String?>(0)

        fun isUserPermissionGranted(): Boolean{
            return if (!globalIsTraditional) RootUri.getGlobalRootUri().let { it != null && Tools.checkUriExists(it) }
            else {
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun requestUserPermission(reRequest: Boolean = false, onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
            if (!globalIsTraditional) {
                val globalRoot = RootUri.getGlobalRootUri()
                if (globalRoot == null || !Tools.checkUriExists(globalRoot) || reRequest) {
                    RootUri.resetGlobalRootUri() { resultCode, data ->
                        onResult?.invoke(resultCode, data)
                    }
                }
                else onResult?.invoke(Activity.RESULT_OK, null)
            }
            else {
                requestTraditionalPermission(onResult)
            }
        }

        fun requestTraditionalPermission(onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityFunctionDelegate({}, { _, resultCode, data ->
                    onResult?.invoke(resultCode, data)
                }, TraditionalFileRequest::class.java)
            }
            else {
                onResult?.invoke(Activity.RESULT_OK, null)
            }
        }

        fun requestUserPermission(onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null) =
            Companion.requestUserPermission(false, onResult)

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
        globalIsTraditional = isTraditional
        refreshStorageVolumes()
    }
}