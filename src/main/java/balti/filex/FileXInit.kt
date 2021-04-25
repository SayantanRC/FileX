package balti.filex

import android.Manifest
import android.annotation.SuppressLint
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
import balti.filex.filexTraditional.FileXT
import balti.filex.filex11.FileX11

/**
 * This class is to be initialised before performing ANY [FileX] related operations.
 * It can be done at the beginning of MainActivity of the app.
 *
 * @param context Context of the activity / service / anywhere else from where it is being called from.
 * In any case, in `init` function, [getApplicationContext()][android.content.Context.getApplicationContext] is called on the supplied context.
 * @param isTraditional This is a global boolean flag for all [FileX] objects.
 * If `true`, by default, all new [FileX] objects are of traditional type (i.e [FileXT], which is a wrapper around [java.io.File] class),
 * else if `false` then Storage Access Framework (SAF) way is used (i.e. [FileX11], which uses
 * [DocumentContract][android.provider.DocumentsContract] and content uris to perform operations).
 * - This value can later be changed by the function [setTraditional].
 */
class FileXInit(context: Context, isTraditional: Boolean) {

    /**
     * Variables inside companion object is available throughout the library.
     */
    companion object{

        /**
         * The context object shared everywhere inside this library.
         * Initialised inside the `init` function.
         */
        @SuppressLint("StaticFieldLeak")
        internal lateinit var fContext: Context
        private set

        /**
         * The global boolean `isTraditional` flag to denote new [FileX] objects will be traditional type ([FileXT])
         * or Storage Access Framework type ([FileX11]).
         *
         * In [FileX.new], if no value is passed for the argument `isTraditional`, then it defaults to the value set by this parameter.
         */
        internal var globalIsTraditional: Boolean = false

        /**
         * An instance of [ContentResolver][android.content.ContentResolver] to be used throughout the library.
         */
        internal val fCResolver by lazy { fContext.contentResolver }

        /**
         * A tag used in [Log.d][android.util.Log.d] for debugging purposes.
         */
        internal val DEBUG_TAG = "FILEX_TAG"

        /**
         * File name for shared preference of the library. Used in [sharedPreferences].
         */
        internal val PREF_NAME = "filex"

        /**
         * A function to execute a block of code and ignore the errors (if any) that occurs in the block.
         * Also displays a [Toast][android.widget.Toast] message if an error occurs, if [showErrorToast] is set to true.
         *
         * @param f A function block to be executed.
         */
        internal fun tryIt(f: () -> Unit){
            try { f() } catch (e: Exception){
                if (showErrorToast) {
                    try {
                        Toast.makeText(fContext, e.message.toString(), Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) { }
                }
            }
        }

        /**
         * Set to false if no [Toast][android.widget.Toast] message is to be shown in [tryIt] block, if any error occurs.
         * Can be set as:
         * > `FileXInit.showErrorToast = false`
         */
        var showErrorToast: Boolean = true

        /**
         * Returns the global value of `isTraditional` flag. Cannot be changed.
         * To change please use [setTraditional] method.
         */
        val isTraditional: Boolean
        get() = globalIsTraditional

        /**
         * Function to set value of global `isTraditional` flag.
         * @param isTraditional If `true`, all future FileX objects will be by default traditional type [FileXT], else SAF type [FileX11].
         */
        fun setTraditional(isTraditional: Boolean){
            globalIsTraditional = isTraditional
        }

        /**
         * A [SharedPreference][android.content.SharedPreferences] instance used to store information
         * like the global root uri (selected by the user from system picker). Used when [isTraditional] = `false`,
         * i.e. SAF way is being used ([FileX11]).
         */
        internal val sharedPreferences by lazy { fContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

        /**
         * This value denotes if a new [FileX] object will be "refreshed" on creation (i.e. the [FileX.refreshFile] will be called.)
         * This only works on [FileX11] (SAF way).
         * This can be changed by:
         * > `FileXInit.refreshFileOnCreation = false`
         */
        var refreshFileOnCreation: Boolean = true

        /**
         * Map of all storage volumes and their actual canonical paths in the file system.
         * This includes SD cards and USB-OTG drives.
         *
         * This is only useful for Android M (6.0) and above. Please see [Tools.getStorageVolumes].
         * For Android L (5.x), this variable is not that helpful. Please see [Tools.deduceVolumePathForLollipop].
         */
        val storageVolumes = HashMap<String, String?>(0)

        /**
         * Checks if permission to read-write to storage is available.
         * - For traditional type ([isTraditional] = `true`), checks if the permissions
         * [Manifest.permission.READ_EXTERNAL_STORAGE] and [Manifest.permission.WRITE_EXTERNAL_STORAGE] are granted.
         * - For SAF type ([isTraditional] = `false`), checks if a global root uri is previously set (using [sharedPreferences]),
         * i.e if the user has previously selected a root directory. Also checks if that directory exists and is not deleted.
         */
        fun isUserPermissionGranted(): Boolean{
            return if (!globalIsTraditional) RootUri.getGlobalRootUri().let {
                it != null && Tools.checkUriExists(it, true)
                // The last condition checks of the root exists.
                // Say, the user selects a root directory as [USB-OTG]/PDFs,
                // but in a later point, the "PDFs" directory is deleted, or the USB-OTG drive is ejected.
                // In this case this method will return `false`.
            }
            else {
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(fContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Request for "permission" to read-write to a location.
         * - Please note: This method uses a transparent auxiliary activities to call appropriate android APIs,
         * which can be only called from an [Activity]. As such in your current activity's `onPause()` method may be called.
         * - For traditional way ([isTraditional] = `true`): Uses [requestTraditionalPermission] function.
         * - For SAF way ([isTraditional] = `false`): Uses the [RootUri.resetGlobalRootUri] to initiate the system picker UI
         * for the user to select a root location.
         *
         * @param reRequest Useful only for SAF way. If `true` then even if a global root is previously set,
         * again the system picker will be started to select a new root location; signifying "requesting the permission again".
         * @param onResult Optional callback function called once permission is granted or denied.
         * This function will always be called even if permission is denied.
         * - `resultCode`: If success, it is `Activity.RESULT_OK` else usually `Activity.RESULT_CANCELED`.
         * - `data`: Intent with some information.
         *     - For FileXT
         *         `data.getStringArrayExtra("permissions")` = Array is requested permissions. Equal to array consisting `Manifest.permission.READ_EXTERNAL_STORAGE`, `Manifest.permission.WRITE_EXTERNAL_STORAGE`
         *         `data.getStringArrayExtra("grantResults")` = Array is granted permissions. If granted, should be equal to array of `PackageManager.PERMISSION_GRANTED`, `PackageManager.PERMISSION_GRANTED`
         *     - For FileX11
         *         `data.data` = Uri of the selected root directory.
         */
        fun requestUserPermission(reRequest: Boolean = false, onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
            if (!globalIsTraditional) {
                val globalRoot = RootUri.getGlobalRootUri()
                if (globalRoot == null || !Tools.checkUriExists(globalRoot, true) || reRequest) {
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

        /**
         * Request for traditional File read-write access. Note that from Android 11+,
         * this only grants READ access i.e. [Manifest.permission.READ_EXTERNAL_STORAGE].
         * [Manifest.permission.WRITE_EXTERNAL_STORAGE] is only granted in Android 10 and below.
         *
         * This function need not be explicitly called. The [requestUserPermission] method automatically
         * takes care of the appropriate method to call based on the value of [isTraditional].
         */
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

        /**
         * Similar to [requestUserPermission], without the `reRequest` argument.
         * The value of `reRequest` = `false`.
         *
         * @param onResult Optional callback function called once permission is granted or denied.
         * This function will always be called even if permission is denied.
         * - `resultCode`: If success, it is `Activity.RESULT_OK` else usually `Activity.RESULT_CANCELED`.
         * - `data`: Intent with some information.
         *     - For FileXT
         *         `data.getStringArrayExtra("permissions")` = Array is requested permissions. Equal to array consisting `Manifest.permission.READ_EXTERNAL_STORAGE`, `Manifest.permission.WRITE_EXTERNAL_STORAGE`
         *         `data.getStringArrayExtra("grantResults")` = Array is granted permissions. If granted, should be equal to array of `PackageManager.PERMISSION_GRANTED`, `PackageManager.PERMISSION_GRANTED`
         *     - For FileX11
         *         `data.data` = Uri of the selected root directory.
         */
        fun requestUserPermission(onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null) =
            Companion.requestUserPermission(false, onResult)

        /**
         * Reads all paths to all the available storage volumes (including SD cards and USB-OTG).
         * Please see [storageVolumes] and [Tools.getStorageVolumes].
         */
        fun refreshStorageVolumes() {
            storageVolumes.clear()
            Tools.getStorageVolumes().run {
                this.keys.forEach {
                    storageVolumes[it] = this[it]
                }
            }
        }
    }

    /**
     * Init method.
     * Used to initialise the context and global `isTraditional` flag.
     * Also reads all the available storage volumes.
     */
    init {
        fContext = context.applicationContext
        globalIsTraditional = isTraditional
        refreshStorageVolumes()
    }
}