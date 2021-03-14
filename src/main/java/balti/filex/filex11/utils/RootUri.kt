package balti.filex.filex11.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import balti.filex.FileXInit.Companion.fContext
import balti.filex.FileXInit.Companion.sharedPreferences
import balti.filex.activity.ActivityFunctionDelegate

internal object RootUri {

    fun setGlobalRootUri(afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
        val uri = getGlobalRootUri()
        if (uri == null) resetGlobalRootUri(afterJob)
        else afterJob?.invoke(Activity.RESULT_OK, Intent().setData(uri))
    }

    fun resetGlobalRootUri(afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ActivityFunctionDelegate(10,
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = takeFlags
            }) { context, resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null) {
                data.data?.let {
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                    sharedPreferences.edit().run {
                        putString(Constants.PREF_GLOBAL_ROOT_URI, it.toString())
                        commit()
                    }
                    afterJob?.invoke(resultCode, data)
                } ?: afterJob?.invoke(resultCode, data)
            }
            else afterJob?.invoke(resultCode, data)
        }
    }

    fun getGlobalRootUri(): Uri? {
        val gr = sharedPreferences.getString(Constants.PREF_GLOBAL_ROOT_URI, "")
        if (gr == "") return null
        else {
            try {
                val uri = Uri.parse(gr)
                fContext.contentResolver.persistedUriPermissions.forEach {
                    if (it.uri == uri && it.isReadPermission && it.isWritePermission) return uri
                }
            } catch (_: Exception) {
            }
            return null
        }
    }
}