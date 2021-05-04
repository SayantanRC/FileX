package balti.filex

import android.content.Intent
import android.net.Uri
import balti.filex.exceptions.*
import balti.filex.filex11.FileX11
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
import balti.filex.filexTraditional.FileXT
import java.io.InputStream
import java.io.OutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.nio.charset.Charset
import kotlin.collections.ArrayList

/**
 * FileX is a modern approach to deal with files in the Android Storage Access Framework (SAF) world.
 * FileX allows you to use some access and write to files using file paths rather than having to deal with [Uri][android.net.Uri].
 * FileX also allows you to use traditional [Java File][java.io.File] for operations in the private data directory of the app.
 *
 * - Internally, FileX is categorised in two ways:
 * [FileXT] - for using traditional [Java File][java.io.File]; [FileX11] - for using the Storage Access Framework way.
 * However only this FileX class is available for use, the `FileX11` and `FileXT` classes are hidden, to keep the use simple.
 * Both the classes `FileXT` and `FileX11` are child classes this FileX class, and share common characteristics.
 * In many cases both the formats are compatible with each other. Like a file referred by `FileXT` can be copied to a location referred by `FileX11`.
 * The user of this library REALLY NEED NOT KNOW which class is being used internally in most cases.
 * Please refer to the README.md for further information.
 *
 * - Please note that the constructor is hidden. To create a FileX object, see [new] function.
 *
 * @param isTraditional If true, FileX internally uses [FileXT] class which acts as a wrapper to [Java File][java.io.File].
 * If this variable is false, FileX internally uses [FileX11] class, which is created for operating in the SAF way, using content Uri and content resolver for many of the tasks.
 */
abstract class FileX internal constructor(val isTraditional: Boolean) {

    /**
     * Refers to the path of the document / file.
     * For traditional cases ([isTraditional] = true), this variable should contain full canonical path of the File reference.
     * Else this variable will contain relative path from the user selected root location (from system file picker).
     * This is also formatted with leading slash (`/`) and no trailing slash. Duplicate slashes in the middle are also removed.
     *
     * - Examples:
     *
     * 1. In case of traditional way ([isTraditional] = true):
     * In case, we want to refer to a file say `aFile.txt` inside the internal storage directory path `/sdcard/Dir123/anotherDir`,
     * we simply need to set [path] = `/sdcard/Dir123/anotherDir/aFile.txt`, just like how we do with [Java File][java.io.File].
     *
     * 2. In case of SAF way ([isTraditional] = false):
     * Just like the previous case, to refer to a file say "aFile.txt" inside the internal storage directory path "/sdcard/Dir123/anotherDir";
     * Say user selects a root location "/sdcard/Dir123" via the [FileXInit.requestUserPermission] function.
     * In this case, we need to set [path] = "/anotherDir/aFile.txt". This is a relative path from the user selected root location.
     */
    abstract val path: String

    companion object {

        /**
         * Public method to create a new FileX object.
         * @param path Accepts the path to the document / file. Please see [FileX.path] to see how this is interpreted.
         * @param isTraditional Accepts a boolean to denote if this object is a traditional FileX object or not.
         * By default, it is the same value set by the global flag [FileXInit.globalIsTraditional].
         *
         * @return A FileX object.
         */
        fun new(path: String, isTraditional: Boolean = FileXInit.globalIsTraditional): FileX {
            return Tools.removeDuplicateSlashes(path).let {
                if (isTraditional) FileXT(it) else FileX11(it)
            }
        }

        /**
         * Public method to create a new FileX object.
         * @param parent Path to the parent directory of a document / file. Follows same convention as [FileX.path].
         * @param child Name of the file inside the parent directory.
         * @param isTraditional Accepts a boolean to denote if this object is a traditional FileX object or not.
         * By default, it is the same value set by the global flag [FileXInit.globalIsTraditional].
         *
         * @return A FileX object.
         */
        fun new(parent: String, child: String, isTraditional: Boolean = FileXInit.globalIsTraditional): FileX =
                FileX.new("$parent/$child", isTraditional)
    }

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Uri of the document. If used on [FileX11] (for SAF way), returns the tree uri.
     * If used on [FileXT] (for traditional way), returns result of [Uri.fromFile()][android.net.Uri.fromFile]
     */
    abstract val uri: Uri?

    /**
     * `FileXT exclusive` (traditional way)
     *
     * Returns raw [Java File][java.io.File] object for the FileX object
     * Useful only for [FileXT] (for traditional way).
     * But usually not of much use for [FileX11] (SAF way) as the returned [Java File][java.io.File] object cannot be read from or written to.
     */
    abstract val file: java.io.File

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * This is useful for `FileX11`. For `FileXT` (traditional way) it is not very useful.
     *
     * If the document was not present during declaration of the FileX object, and the document is later created by any other app,
     * or this app from a background thread, then call [refreshFile] on it to update the Uri pointing to the file.
     * Do note that if your app is itself creating the document on the main thread, you need not call [refreshFile()] again.
     * - Example:
     * ```
     * val fx1 = FileX.new("aFile")
     * val fx2 = FileX.new("/aFile")
     * fx2.createNewFile()
     * ```
     * In this case, provided all the operations are on the main thread, you need not call [refreshFile] on `fx1`.
     * However if any other app creates the document, or all the above operations are performed on background thread,
     * then you will not be able to refer to `fx1` unless it is refreshed. `fx2` can still be referred to without refreshing.
     *
     * Even in the case of the file being created in a background thread, the Uri of the file does get updated after about 200 ms.
     * But this is not very reliable, hence it is recommended to call [refreshFile].
     *
     * - For [FileX11] See [FileX11.refreshFile]
     * - For [FileXT] See [FileXT.refreshFile]
     */
    abstract fun refreshFile()

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Opens system file picker UI allow user select a new root for this FileX object. The method [FileXInit.requestUserPermission] is used to set a global root.
     * This method is similar, but it only changes the root of this object, not the global root of all other FileX objects.
     */
    fun resetRoot(onResult: ((resultCode: Int, data: Intent?) -> Unit)){
        if (this is FileX11) this.setLocalRootUri(onResult)
    }

    //
    // Info
    //

    /**
     * Canonical path of the object.
     * - For [FileX11] returns complete path for any physical storage location (including SD cards).
     * This path is calculated fairly reliably on Android 11+. However for lower Android versions,
     * especially below Android 7 (Nougat), the result may not be very reliable if this path points to USB-OTG or SD cards.
     * - For [FileXT] it simply uses the [java.io.File.getCanonicalPath]
     */
    abstract val canonicalPath: String

    /**
     * Absolute path of the object.
     * - For [FileX11] returns same value as [canonicalPath]
     * - For [FileXT] it simply uses the [java.io.File.getAbsolutePath]
     */
    abstract val absolutePath: String

    /**
     * Returns if the document exist.
     * For [FileX11] (SAF way), internally calls [refreshFile] before checking.
     *
     * @return `true` if the document exists, else `false`.
     */
    abstract fun exists(): Boolean

    /**
     * Returns if the document referred to by the FileX object is directory or not.
     * Returns false if document does not exist already. Internally calls [exists] before checking.
     */
    abstract val isDirectory: Boolean

    /**
     * Returns if the document is a file or not (like text, jpeg etc).
     * Returns false if document does not exist already. Internally calls [exists] before checking.
     */
    abstract val isFile: Boolean

    /**
     * Name of the document / directory referred by this FileX object.
     */
    abstract val name: String

    /**
     * String path of the parent directory of this FileX object.
     * - For [FileX11] (SAF way), the path returned is the relative path from the user selected root and not full [canonicalPath].
     * Returns null if called on the root itself (as no permission to see beyond root).
     * - For [FileXT] (traditional way), executes [java.io.File.getParent]
     */
    abstract val parent: String?

    /**
     * Returns a FileX object pointing to the parent directory. Null if no parent.
     * @see parent
     */
    abstract val parentFile: FileX?

    /**
     * [canonicalPath] of the parent directory.
     * This does not return null even if called on root. If called on root, it returns "/". Applicable for both [FileXT] and [FileX11].
     */
    abstract val parentCanonical: String

    /**
     * Length of the file in bytes.
     * - For [FileX11] (SAF way) - uses content resolver query to get the length.
     * - For [FileXT] (traditional way) - please see [Java File length()][java.io.File.length].
     *
     * @return Length of the file in bytes. Returns 0L if file does not exist.
     */
    abstract fun length(): Long

    /**
     * Value representing the time the file was last modified, measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970).
     * - For [FileX11] (SAF way) - uses content resolver query to get the last modified value.
     * - For [FileXT] (traditional way) - [Java File lastModified()][java.io.File.lastModified].
     *
     * @return Last modified value of the file. Returns 0L if file does not exist.
     */
    abstract fun lastModified(): Long

    /**
     * Returns if the document can be read from. Usually always true for `FileX11`.
     * - For [FileX11] (SAF way) - checks if the [Uri][android.net.Uri] of the file has the read permission
     * via [UriPermission isReadPermission()][android.content.UriPermission.isReadPermission].
     * See [Info.canRead()][balti.filex.filex11.operators.Info.canRead].
     * - For [FileXT] (traditional way) - See [Java File canRead()][java.io.File.canRead].
     *
     * @return `true` if file / document can be read, else `false`
     */
    abstract fun canRead(): Boolean

    /**
     * Returns if the document can be written to. Usually always true for `FileX11`.
     * - For [FileX11] (SAF way) - checks if the [Uri][android.net.Uri] of the file has the write permission
     * via [UriPermission isWritePermission()][android.content.UriPermission.isWritePermission].
     * See [Info.canWrite()][balti.filex.filex11.operators.Info.canWrite].
     * - For [FileXT] (traditional way) - See [Java File canWrite()][java.io.File.canWrite].
     *
     * @return `true` if file / document can be written to, else `false`
     */
    abstract fun canWrite(): Boolean

    /**
     * Number of bytes of free space available in the root storage location.
     * - For [FileX11] (SAF way) - uses [ParcelFileDescriptor][android.os.ParcelFileDescriptor] and [OS fstatvfs][android.system.Os.fstatvfs].
     * See [Info.getSpace()][balti.filex.filex11.operators.Info.getSpace].
     * - For [FileXT] (traditional way) - See [Java File getFreeSpace()][java.io.File.getFreeSpace].
     */
    abstract val freeSpace: Long

    /**
     * Number of bytes of usable space to write data in the root storage location. This usually takes care of permissions and other restrictions and more accurate than `freeSpace`.
     * - For [FileX11] (SAF way) - uses [ParcelFileDescriptor][android.os.ParcelFileDescriptor] and [OS fstatvfs][android.system.Os.fstatvfs].
     * See [Info.getSpace()][balti.filex.filex11.operators.Info.getSpace].
     * - For [FileXT] (traditional way) - See [Java File getUsableSpace()][java.io.File.getUsableSpace].
     */
    abstract val usableSpace: Long

    /**
     * Number of bytes representing total space of the root storage location.
     * - For [FileX11] (SAF way) - uses [ParcelFileDescriptor][android.os.ParcelFileDescriptor] and [OS fstatvfs][android.system.Os.fstatvfs].
     * See [Info.getSpace()][balti.filex.filex11.operators.Info.getSpace].
     * - For [FileXT] (traditional way) - See [Java File getTotalSpace()][java.io.File.getTotalSpace].
     */
    abstract val totalSpace: Long

    /**
     * Checks if the document is hidden.
     * - For [FileX11] (SAF way) - There is not much available way to check if a document is really hidden. So this checks if the name begins with a `.`
     * - For [FileXT] (traditional way) - See [Java File isHidden()][java.io.File.isHidden].
     */
    abstract val isHidden: Boolean

    /**
     * Extension of the document. Works identically to [kotlin.io.extension].
     */
    abstract val extension: String

    /**
     * The name of the document without the extension part. Works identically to [kotlin.io.nameWithoutExtension].
     */
    abstract val nameWithoutExtension: String

    //FileX11 exclusive

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Returns the path of the document from the root of the storage. Returns null for `FileXT`.
     * See [Info.storagePath][balti.filex.filex11.operators.Info.storagePath].
     *
     * - Example 1 : For a document with user selected root = `[Internal storage]/dir1/dir2` and
     * having a [path] = `my/test_doc.txt`: the value of `storagePath` = `/dir1/dir2/my/test_doc.txt`.
     * - Example 2 : For a document with user selected root = `[SD card]/all_documents` and
     * having a [path] = `/thesis/doc.pdf`: the value of `storagePath` = `/all_documents/thesis/doc.pdf`.
     * - Important note: This returns `null` for [FileXT] (traditional way).
     */
    abstract val storagePath: String?

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Returns the canonical path of the of the storage medium. Useful to find the mount point of SD cards and USB-OTG drives.
     * This path, in most cases, is not readable or writable unless the user selects it from the system file picker.
     * Returns null for `FileXT`.
     *
     * See [Info.volumePath][balti.filex.filex11.operators.Info.volumePath],
     * [getStorageVolumes()][balti.filex.filex11.utils.Tools.getStorageVolumes]
     * and for Android 5.x: [deduceVolumePathForLollipop()][balti.filex.filex11.utils.Tools.deduceVolumePathForLollipop].
     *
     * - Example 1 : For a document with user selected root = `[Internal storage]/dir1/dir2` and
     * having a [path] = `my/doc.txt`: the value of `volumePath` = `/storage/emulated/0`.
     * - Example 2 : For a document with user selected root = `[SD card]/all_documents` and
     * having a [path] = `/thesis/doc.pdf`: the value of `volumePath` = `/storage/B840-4A40`.
     * (the location name is based on the UUID of the storage medium)
     * - Important note: This returns `null` for [FileXT] (traditional way).
     */
    abstract val volumePath: String?

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Returns the canonical path upto the root selected by the user from the system file picker. Returns null for `FileXT`.
     * See [Info.rootPath][balti.filex.filex11.operators.Info.rootPath].
     *
     * - Example 1 : For a document with user selected root = `[Internal storage]/dir1/dir2` and
     * having a [path] = `my/test_doc.txt`: the value of `rootPath` = `/storage/emulated/0/dir1/dir2`.
     * - Example 2 : For a document with user selected root = `[SD card]/all_documents` and
     * having a [path] = `/thesis/doc.pdf`: the value of `rootPath` = `/storage/B840-4A40/all_documents`.
     * - Important note: This returns `null` for [FileXT] (traditional way).
     */
    abstract val rootPath: String?

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Returns the tree uri of the parent directory if present, else null.
     * Will return null if run on the user selected root as we have no permission to see beyond that location.
     * See [Info.parentUri][balti.filex.filex11.operators.Info.parentUri].
     * - Returns null for `FileXT`
     */
    abstract val parentUri: Uri?

    // FileXT exclusive

    /**
     * `FileXT exclusive` (traditional way)
     *
     * Returns if the Java File pointed by a FileX object is executable. Always false for `FileX11`.
     * - For [FileX11] (SAF way) - Simply returns `false`
     * - For [FileXT] (traditional way) - See [Java File canExecute()][java.io.File.canExecute].
     *
     * @return `true` if the file is executable, else `false`.
     */
    abstract fun canExecute(): Boolean

    //
    // Delete
    //

    /**
     * Deletes a single document or empty directory. Does not delete a non-empty directory. Returns true if successful, else false.
     * Deletion is only performed if the FileX object refers to a file or an empty directory.
     * - For [FileX11] (SAF way) - Uses [DocumentsContract.deleteDocument()][android.provider.DocumentsContract.deleteDocument].
     * See [Delete.delete()][balti.filex.filex11.operators.Delete.delete]
     * - For [FileXT] (traditional way) - See [Java File delete()][java.io.File.delete].
     *
     * @return `true` is the file / document could be deleted, else `false`.
     */
    abstract fun delete(): Boolean

    /**
     * Deletes a directory and all documents and other directories inside it, even if not empty. Returns true if successful.
     * - For [FileX11] (SAF way) - Uses [DocumentsContract.deleteDocument()][android.provider.DocumentsContract.deleteDocument].
     * See [Delete.deleteRecursively()][balti.filex.filex11.operators.Delete.deleteRecursively]
     * - For [FileXT] (traditional way) - See [Kotlin deleteRecursively()][kotlin.io.deleteRecursively].
     */
    abstract fun deleteRecursively(): Boolean

    /**
     * Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.
     *
     * All files on which this method is called will get deleted once `System.exit()` is called, similar to [java.io.File.deleteOnExit].
     *
     * - For [FileX11] (SAF way) - Use copied logic to add a shutdown hook to runtime.
     * See [FileX11DeleteOnExit][balti.filex.filex11.utils.FileX11DeleteOnExit] and [Delete.deleteOnExit()][balti.filex.filex11.operators.Delete.deleteOnExit]
     * - For [FileXT] (traditional way) - See [Java File deleteOnExit()][java.io.File.deleteOnExit].
     *
     * - RECOMMENDED: Although this works both on `FileX11` and `FileXT`, but implementation for `FileX11` is basically a patch work
     * from the implementation from `java.io.DeleteOnExitHook`. It is highly recommended to surround the code by `try-catch` block.
     */
    abstract fun deleteOnExit()

    //
    // Create
    //

    /**
     * Create a blank document.
     * - For [FileX11] (SAF way) - See [FileX11 Create.createNewFile()][balti.filex.filex11.operators.Create.createNewFile].
     * - For [FileXT] (traditional way) - See [FileXT.createNewFile()][FileXT.createNewFile].
     *
     * @param makeDirectories Creates the whole directory tree before the document if not present.
     * @param overwriteIfExists Deletes the document if already present and creates a blank document.
     * @param optionalMimeType For `FileX11` a mime type of the document (as string) can be specified. Ignored for `FileXT`.
     *
     * @return `true` if document creation is successful, else `false`.
     *
     * @throws DirectoryHierarchyBroken If `makeDirectories = false` and the full tree path is not present.
     * Also see [DocumentsContract.createDocument()][android.provider.DocumentsContract.createDocument].
     * For FileXT, also see [Java File createNewFile()][java.io.File.createNewFile] for additional exceptions that may be thrown.
     */
    abstract fun createNewFile(makeDirectories: Boolean = false, overwriteIfExists: Boolean = false, optionalMimeType: String = "*/*"): Boolean

    /**
     * Creates a blank document referred to by the FileX object.
     *
     * See [createNewFile(makeDirectories, overwriteIfExists, optionalMimeType][createNewFile].
     *
     * This function is basically the same with the options:
     * `makeDirectories = false` and `overwriteIfExists = false`.
     *
     * @return `true` if document creation is successful, else `false`.
     * @throws DirectoryHierarchyBroken If the full tree path is not present.
     * For FileXT, also see [Java File createNewFile()][java.io.File.createNewFile] for additional exceptions that may be thrown.
     */
    abstract fun createNewFile(): Boolean

    /**
     * Make all directories specified by the path of the FileX object
     * (including the last element of the path and other non-existing parent directories).
     *
     * - For [FileX11] (SAF way) - See [FileX11 Create.mkdirs()][balti.filex.filex11.operators.Create.mkdirs].
     * - For [FileXT] (traditional way) - See [Java File mkdirs()][java.io.File.mkdirs].
     *
     * @return `true` if all the directories in the path was created, else `false`.
     * @throws DirectoryHierarchyBroken For `FileX11`
     * if [DocumentsContract.createDocument()][android.provider.DocumentsContract.createDocument] fails while creating directory.
     * For `FileXT`, also see [Java File mkdirs()][java.io.File.mkdirs] for additional exceptions that may be thrown.
     */
    abstract fun mkdirs(): Boolean

    /**
     * Creates only the last element of the path as a directory. Parent directories must be already present.
     *
     * - For [FileX11] (SAF way) - See [FileX11 Create.mkdir()][balti.filex.filex11.operators.Create.mkdir].
     * - For [FileXT] (traditional way) - See [Java File mkdir()][java.io.File.mkdir].
     * For `FileXT`, also see [Java File mkdirs()][java.io.File.mkdirs] for exceptions that may be thrown.
     *
     * @return `true` if the directory was created, else `false`.
     */
    abstract fun mkdir(): Boolean

    /**
     * `FileX11 exclusive`
     *
     * Invoke the System file picker to create the file. Only applicable on `FileX11`.
     * After the system file picker opens, the user selects a location and the file is created in that loaction.
     *
     * @param optionalMimeType mime type can be spcified as a string.
     * @param afterJob callback function can be passed to execute after document is created.
     * - `resultCode` = `Activity.RESULT_OK` if document is successfully created.
     * - `data` = Intent data returned by System after document creation.
     */
    abstract fun createFileUsingPicker(optionalMimeType: String = "*/*", afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null)

    //
    // Modify
    //

    /**
     * Move the current document to the path mentioned by the FileX parameter `dest`
     * - For [FileX11] (SAF way) - this only works in the actual sense of "moving" for Android 7+ (API 24) due to Android limitations.
     * For lower Android versions, this copies the file / directory and then deletes from the old location.
     * See the functions in [FileX11 Modify][balti.filex.filex11.operators.Modify].
     * - For [FileXT] (traditional way) - this mainly uses [Java File renameTo()][java.io.File.renameTo] to additional modifications.
     * See the functions in [FileXT Modify][balti.filex.filexTraditional.operators.Modify].
     *
     * - For exceptions, see [DocumentsContract.moveDocument()][android.provider.DocumentsContract.moveDocument] for `FileX11`
     * and [Java File renameTo()][java.io.File.renameTo] `FileXT`.
     *
     * @param dest A FileX object pointing to the location where the current file / directory is to be moved.
     * This argument can be of both type, `FileXT` (traditional way, with [isTraditional] = true) or
     * `FileX11` (SAF way, with [isTraditional] = false). Similarly, this method can be called on any FileX object,
     * immaterial of it being traditional type or SAF type.
     *
     * @return `true` if move was successful, `false` otherwise.
     */
    abstract fun renameTo(dest: FileX): Boolean

    /**
     * Rename the document in place. This is used to only change the name and cannot move the document.
     * @param newFileName New string file name of the current document / file.
     * @return `true` if rename was successful, `false` otherwise.
     */
    abstract fun renameTo(newFileName: String): Boolean

    //
    // Copy
    //

    private val Copy = Copy(this)

    /**
     * Copies a file and returns the target. Does not copy a directory. Logic is completely copied from [kotlin.io.copyTo]
     * @param target FileX object referring to destination location, immaterial whether `FileX11` or `FileXT` type.
     * @param overwrite If `true` deletes the file / directory (if empty) and copies the file referred by the current FileX object.
     * @param bufferSize Int value specifying the stream buffer size while copying the file size.
     *
     * @return The target FileX object, basically same as the parameter [target].
     * This occurs only if the operation is successful, else one of the below exceptions sre thrown.
     *
     * @throws NoSuchFileXException If the source file doesn't exist.
     * @throws FileXAlreadyExistsException If the destination file already exists and [overwrite] argument is set to `false`.
     * @throws FileXSystemException If the source is a directory, this function only creates and empty directory at [target].
     * This exception is thrown if creating the empty directory fails.
     * @throws NullPointerException If the input stream (from source) or output stream (to target) is null.
     * @throws IOException If any errors occur while copying.
     */
    fun copyTo(target: FileX, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): FileX = Copy.copyTo(target, overwrite, bufferSize)

    /**
     * Directory copy recursively, return true if success else false. Logic is completely copied from kotlin.io copyRecursively().
     * For better documentation of the logic, please refer to [Copy.copyRecursively] or [kotlin.io.copyRecursively].
     *
     * Note that if this function fails, then partial copying may have taken place.
     *
     * @param target FileX object referring to destination location, immaterial whether `FileX11` or `FileXT` type.
     * @param overwrite If `true` deletes conflicting files / directories if exists inside the target.
     * @param onError Function to execute in case of errors. Expected to return one of [OnErrorAction].
     * @return `false` if the copying was terminated, `true` otherwise.
     */
    fun copyRecursively(
        target: FileX,
        overwrite: Boolean = false,
        onError: (FileX, Exception) -> OnErrorAction = { _, exception -> throw exception }
    ): Boolean = Copy.copyRecursively(target, overwrite, onError)

    //
    // Filter
    //

    /**
     * Applicable on directories. Returns `true` if the directory is empty. Will return `false` if run on a file / document.
     * - For [FileX11] (SAF way) - See [FileX11 Filter.isEmpty][balti.filex.filex11.operators.Filter.isEmpty]
     * - For [FileXT] (traditional way) - See [FileXT Filter.isEmpty][balti.filex.filexTraditional.operators.Filter.isEmpty]
     */
    abstract val isEmpty: Boolean

    /**
     * Returns array of all the files and directories under the current directory as FileX objects.
     * This is to be only run on a directory location and not a file.
     *
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listFiles()][balti.filex.filex11.operators.Filter.listFiles]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listFiles()][balti.filex.filexTraditional.operators.Filter.listFiles]
     *
     * @return Array of FileX objects pointing to all the contents of the directory.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun listFiles(): Array<FileX>?

    /**
     * Returns array of files and directories under the current directory as FileX objects, which pass a filtering logic provided with [FileXFilter].
     * This is to be only run on a directory location and not a file.
     * The condition to filter is specified in the `accept()` method.
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir = FileX.new("a_directory")
     * val allTextFiles: Array<FileX>? = dir.listFiles(object : FileXFilter {
     *     override fun accept(file: FileX): Boolean {
     *         // Filter all text files
     *         return file.extension == "txt"
     *     }
     * })
     * ```
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listFiles()][balti.filex.filex11.operators.Filter.listFiles]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listFiles()][balti.filex.filexTraditional.operators.Filter.listFiles]
     *
     * @return Array of FileX objects pointing to the contents of the directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun listFiles(filter: FileXFilter): Array<FileX>?

    /**
     * Returns array of files and directories under the current directory as FileX objects, which pass a filtering logic provided with [FileXNameFilter].
     * This is to be only run on a directory location and not a file.
     * A condition to filter is specified in the `accept()` method.
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir1 = FileX.new("a_directory")
     * val dir2 = FileX.new("pdf_document_directory")
     * val filter = object : FileXNameFilter{
     *     override fun accept(dir: FileX, name: String): Boolean {
     *         // Lets say we need to filter all PDF files stored in
     *         // directories having "document" in their name
     *         return dir.name.contains("document") && name.endsWith(".pdf")
     *     }
     * }
     * dir1.listFiles(filter)
     * // dir1's name does not contain "document", no file inside will qualify.
     * dir2.listFiles(filter)
     * // here files ending with ".pdf" will qualify.
     * ```
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listFiles()][balti.filex.filex11.operators.Filter.listFiles]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listFiles()][balti.filex.filexTraditional.operators.Filter.listFiles]
     *
     * @return Array of FileX objects pointing to the contents of the directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun listFiles(filter: FileXNameFilter): Array<FileX>?

    /**
     * This function is made for use as arrow function, without needing to explicitly define the filter object.
     * This is just an implementation of [listFiles(filter: FileXFilter)][FileX#listFiles(balti.filex.filex11.publicInterfaces.FileXFilter)].
     *
     * - Example:
     * ```
     * val dir = FileX.new("a_directory")
     * val allTextFiles = dir.listFiles { file ->
     *     // Filter all text files
     *     file.extension == "txt"
     * }
     * ```
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listFiles()][balti.filex.filex11.operators.Filter.listFiles]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listFiles()][balti.filex.filexTraditional.operators.Filter.listFiles]
     *
     * @return Array of FileX objects pointing to the contents of the directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    fun listFiles(filter: (file: FileX) -> Boolean): Array<FileX>? =
            listFiles(object : FileXFilter {
                override fun accept(file: FileX): Boolean = filter(file)
            })

    /**
     * This function is made for use as arrow function, without needing to explicitly define the filter object.
     * This is just an implementation of [listFiles(filter: FileXNameFilter)][FileX#listFiles(balti.filex.filex11.publicInterfaces.FileXNameFilter)].
     *
     * - Example:
     * ```
     * val directory = FileX.new("a_directory")
     * val allTextFiles = directory.list { dir, name ->
     *     // Filter all pdf files
     *     name.endsWith(".pdf")
     *     // In this example 'dir' is fairly useless.
     * }
     * ```
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listFiles()][balti.filex.filex11.operators.Filter.listFiles]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listFiles()][balti.filex.filexTraditional.operators.Filter.listFiles]
     *
     * @return Array of FileX objects pointing to the contents of the directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    fun listFiles(filter: (dir: FileX, name: String) -> Boolean): Array<FileX>? =
            listFiles(object : FileXNameFilter {
                override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
            })

    /**
     * Returns string array of names of all the files and directories under the current directory.
     * This is to be only run on a directory location and not a file.
     *
     * - Can be compared with [listFiles], but this returns only names of the files and not FileX objects.
     * - For [FileX11] (SAF way) - See [FileX11 Filter.list()][balti.filex.filex11.operators.Filter.list]
     * - For [FileXT] (traditional way) - See [FileXT Filter.list()][balti.filex.filexTraditional.operators.Filter.list]
     *
     * @return Array of names (strings) of all files and directories under the current directory.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun list(): Array<String>?

    /**
     * Returns array of names of files and directories under the current directory, which pass a filtering logic provided with [FileXNameFilter].
     * This is to be only run on a directory location and not a file.
     * A condition to filter is specified in the `accept()` method.
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir = FileX.new("a_directory")
     * val allTextFileNames: Array<String>? = dir.list(object : FileXFilter {
     *     override fun accept(file: FileX): Boolean {
     *         // Get names of all text files
     *         return file.extension == "txt"
     *     }
     * })
     * ```
     *
     * - Can be compared with [listFiles], but this returns only names of the files and not FileX objects.
     * - For [FileX11] (SAF way) - See [FileX11 Filter.list()][balti.filex.filex11.operators.Filter.list]
     * - For [FileXT] (traditional way) - See [FileXT Filter.list()][balti.filex.filexTraditional.operators.Filter.list]
     *
     * @return Array of names (strings) of files and directories under the current directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun list(filter: FileXFilter): Array<String>?

    /**
     * Returns array of names of files and directories under the current directory, which pass a filtering logic provided with [FileXNameFilter].
     * This is to be only run on a directory location and not a file.
     * A condition to filter is specified in the `accept()` method.
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir1 = FileX.new("a_directory")
     * val dir2 = FileX.new("pdf_document_directory")
     * val filter = object : FileXNameFilter{
     *     override fun accept(dir: FileX, name: String): Boolean {
     *         // Lets say we need to filter all PDF files stored in
     *         // directories having "document" in their name
     *         return dir.name.contains("document") && name.endsWith(".pdf")
     *     }
     * }
     * dir1.list(filter)
     * // dir1's name does not contain "document", no file inside will qualify.
     * val names: Array<String>? = dir2.list(filter)
     * // here files ending with ".pdf" will qualify.
     * // it will contain names of the pdf files, rather than FileX objects.
     * ```
     *
     * - Can be compared with [listFiles], but this returns only names of the files and not FileX objects.
     * - For [FileX11] (SAF way) - See [FileX11 Filter.list()][balti.filex.filex11.operators.Filter.list]
     * - For [FileXT] (traditional way) - See [FileXT Filter.list()][balti.filex.filexTraditional.operators.Filter.list]
     *
     * @return Array of names (strings) of files and directories under the current directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    abstract fun list(filter: FileXNameFilter): Array<String>?

    /**
     * This function is made for use as arrow function, without needing to explicitly define the filter object.
     * This is just an implementation of [list(filter: FileXFilter)][FileX#list(balti.filex.filex11.publicInterfaces.FileXFilter)].
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir = FileX.new("a_directory")
     * val allTextFileNames: Array<String>? = dir.list { file ->
     *     // Filter all text files
     *     file.extension == "txt"
     * }
     * ```
     *
     * - For [FileX11] (SAF way) - See [FileX11 Filter.list()][balti.filex.filex11.operators.Filter.list]
     * - For [FileXT] (traditional way) - See [FileXT Filter.list()][balti.filex.filexTraditional.operators.Filter.list]
     *
     * @return Array of names (strings) of files and directories under the current directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    fun list(filter: (file: FileX) -> Boolean): Array<String>? =
            list(object : FileXFilter{
                override fun accept(file: FileX): Boolean = filter(file)
            })

    /**
     * This function is made for use as arrow function, without needing to explicitly define the filter object.
     * This is just an implementation of [list(filter: FileXNameFilter)][FileX#list(balti.filex.filex11.publicInterfaces.FileXNameFilter)].
     *
     * - Example: List all files having "txt" extension.
     * ```
     * val dir = FileX.new("a_directory")
     * val allTextFilesNames: Array<String>? = dir.list { dir, name ->
     *     // Filter all text files
     *     name.endsWith(".pdf")
     *     // In this example 'dir' is fairly useless.
     * }
     * ```
     *
     * - For [FileX11] (SAF way) - See [FileX11 Filter.list()][balti.filex.filex11.operators.Filter.list]
     * - For [FileXT] (traditional way) - See [FileXT Filter.list()][balti.filex.filexTraditional.operators.Filter.list]
     *
     * @return Array of names (strings) of files and directories under the current directory, which qualify the filtering logic.
     * Returns null if the current FileX object is a file and not a directory.
     */
    fun list(filter: (dir: FileX, name: String) -> Boolean): Array<String>? =
            list(object : FileXNameFilter{
                override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
            })

    /**
     * Sometimes [list()][list] and [listFiles()][listFiles] can have huge overhead. This function was created to tackle that situation.
     * This function is comparatively much faster, especially on [FileX11] (SAF way) as it just loops once
     * using content resolver and picks up all the required information using the cursor.
     * See [FileX11 Filter.listEverything()][balti.filex.filex11.operators.Filter.listEverything]
     *
     * This is to be only run on a directory location and not a file. This function returns a set of 4 lists.
     * The lists contain information of all the containing files and directories.
     *
     * The lists in the specific order are:
     * - `1st list` - `List<String>` : List of names of all the files and directories.
     * - `2nd list` - `List<Boolean>` : Corresponding list denoting if a name (i.e. entry of the `1st list`) is a directory or not.
     * - `3rd list` - `List<Long>` : Corresponding list containing the [length] (file size in bytes) of entry of the `1st list`.
     *              This is not accurate if the entry is a directory (to be checked from the `2nd list`)
     * - `4th list` - `List<Long>` : Corresponding list denoting the [lastModified] value of each entry.
     *
     * Do note that a specific index represents properties of the same file all across the lists.
     * For example, at (say) INDEX=5,
     *
     * 1. 1st_list(5)="sample_doc.txt";
     * 2. 2nd_list(5)=false;
     * 3. 3rd_list(5)=13345;
     * 4. 4th_list(5)=1619053629000
     *
     * All these properties are for the same file "sample_doc.txt".
     *
     * - Example:
     * ```
     * val dir = FileX.new("a_directory")
     * dir.listEverything()?.let { everything ->
     *     for (i in everything.first.indices) {
     *         // loop over all the indices
     *         // NOTE: Actual size of directories will be incorrect.
     *         Log.d("DEBUG_TAG",
     *                 "Found content with name \"${everything.first[i]}\"\n" +
     *                 "This file is a directory: ${everything.second[i]}.\n" +
     *                 "Size (in bytes) of this file: ${everything.third[i]}.\n" +
     *                 "Last modified value: ${everything.fourth[i]}."
     *         )
     *     }
     * }
     * ```
     * The output is something as below:
     * ```
     * Found content with name: "Telegram.apk"
     * This file is a directory: false.
     * Size (in bytes) of this file: 57449002.
     * Last modified value: 1618589301000.
     *
     * Found content with name: "bank_statements"
     * This file is a directory: true.
     * Size (in bytes) of this file: 4096.
     * Last modified value: 1619093174000.
     *
     * Found content with name: "IMG_20210419_125842.jpg"
     * This file is a directory: false.
     * Size (in bytes) of this file: 8474609.
     * Last modified value: 1618817328000.
     * ```
     *
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listEverything()][balti.filex.filex11.operators.Filter.listEverything]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listEverything()][balti.filex.filexTraditional.operators.Filter.listEverything]
     *
     * @return Set of 4 lists containing names, boolean denoting if directory, size in bytes, and last modified.
     * Returns null if listing did not work (typically happens if the current FileX object is a file and not a directory)
     */
    abstract fun listEverything(): Quad<List<String>, List<Boolean>, List<Long>, List<Long>>?

    /**
     * This function is very similar to [listEverything]. Recommended to see its documentation first.
     *
     * This is to be only run on a directory location and not a file.
     * This function returns a single list, each element of the list being a [Quad] of 4 items.
     * Each quad item contain information of 1 file / directory. All the quads together denote all the contents of the directory.
     *
     * Each [Quad]'s 4 elements are:
     * - `first: String` - Name of an object (a file or directory).
     * - `second: Boolean` - If the corresponding object is a directory or not.
     * - `third: Long` - [length] (file size in bytes) of the object.
     *                   This is not accurate if the object is a directory (to be checked from the `second`)
     * - `fourth: Long` : [lastModified] value of the object.
     *
     * - Example:
     * ```
     * val dir = FileX.new("a_directory")
     * dir.listEverythingInQuad()?.forEach {
     *     // Each item represents 1 file or directory
     *     Log.d("DEBUG_TAG",
     *             "Found content with name: \"${it.first}\".\n" +
     *             "This file is a directory: ${it.second}.\n" +
     *             "Size (in bytes) of this file: ${it.third}.\n" +
     *             "Last modified value: ${it.fourth}."
     *     )
     * }
     * ```
     * The output is something as below:
     * ```
     * Found content with name: "Telegram.apk".
     * This file is a directory: false.
     * Size (in bytes) of this file: 57449002.
     * Last modified value: 1618589301000.
     *
     * "bank_statements".
     * This file is a directory: true.
     * Size (in bytes) of this file: 4096.
     * Last modified value: 1619093174000.
     *
     * "IMG_20210419_125842.jpg".
     * This file is a directory: false.
     * Size (in bytes) of this file: 8474609.
     * Last modified value: 1618817328000.
     * ```
     *
     * - For [FileX11] (SAF way) - See [FileX11 Filter.listEverythingInQuad()][balti.filex.filex11.operators.Filter.listEverythingInQuad]
     * - For [FileXT] (traditional way) - See [FileXT Filter.listEverythingInQuad()][balti.filex.filexTraditional.operators.Filter.listEverythingInQuad]
     *
     * @return One list containing [Quad]s of (name, boolean denoting if directory, size in bytes, last modified) of the files / directories.
     * Returns null if listing did not work (typically happens if the current FileX object is a file and not a directory)
     */
    abstract fun listEverythingInQuad(): List<Quad<String, Boolean, Long, Long>>?

    //
    // Operations
    //

    /**
     * Returns an [InputStream] from a file to read the file.
     *
     * For traditional FileX, uses [FileInputStream][kotlin.io.inputStream],
     * else uses [content resolver openInputStream()][android.content.ContentResolver.openInputStream]
     *
     * - For [FileX11] (SAF way) - See [FileX11 inputStream()][balti.filex.filex11.operators.Operations.inputStream]
     * - For [FileXT] (traditional way) - See [FileXT inputStream()][balti.filex.filexTraditional.FileXT.inputStream]
     *
     * @return An [InputStream] object to read the file.
     * Returns `null` if file does not exist, or the method is run on a directory.
     */
    abstract fun inputStream(): InputStream?

    /**
     * Returns an [OutputStream] to the document to write to.
     * This function mainly useful for [FileX11] (SAF way).
     * For [FileXT] (traditional way) please use the function `outputStream(append:Boolean)` below.
     *
     * The mode parameter is taken from [android.content.ContentProvider.openAssetFile]
     *
     * @param mode Access mode for the file.  May be "r" for read-only access,
     * "w" for write-only access (erasing whatever data is currently in
     * the file), "wa" for write-only access to append to any existing data,
     * "rw" for read and write access on any existing data, and "rwt" for read
     * and write access that truncates any existing file.
     *
     * > "rwt" wipes the file clean before writing. This is seen to be the behaviour in FileWriter.
     *
     * > "rw", "w" they start replacing the lines from top.
     * > If the file has 3 lines and only 1 line is written, the first line
     * > of the file is replaced with the new content. The rest 2 lines which
     * > were present previously remains unchanged.
     *
     * @return An [OutputStream] object to write to the file.
     * Returns `null` if stream could not be opened or the method is run on a directory.
     */
    abstract fun outputStream(mode: String): OutputStream?

    /**
     * Returns an [OutputStream] to the document to write to.
     * @param append Pass `true` to append to the end of the file. If `false`, deletes all contents of the file before writing.
     *
     * @return An [OutputStream] object to write to the file.
     * Returns `null` if stream could not be opened or the method is run on a directory.
     */
    fun outputStream(append: Boolean = false): OutputStream? = outputStream(if (append) "wa" else "rwt")

    /**
     * This function allows for easy write operations to a file.
     * This is to be only run on a file and not a directory.
     *
     * This function eliminates the use of `FileWriter` or `BufferedWriter` or similar.
     * Thus you also do not need to take care of closing the streams using `close()` method.
     * This function takes an instance of the [Writer] class which has the
     * [writeLines()][Writer.writeLines] function inside which write operations can be performed.
     *
     * - Example
     * ```
     * // create a blank file
     * val fx = FileX.new(/my_dir/my_file.txt)
     * fx.createNewFile(makeDirectories = true)
     *
     * // start writing to file
     * fx.startWriting(object : FileX.Writer() {
     *     override fun writeLines() {
     *
     *         // write strings without line breaks.
     *         writeString("a string. ")
     *         writeString("another string in the same line.")
     *
     *         // write a new line. Similar to writeString() with a line break at the end.
     *         writeLine("a new line.")
     *         writeLine("3rd line.")
     *     }
     * })
     * ```
     * For all the available functions, please see the [Writer] class.
     *
     * @param writer Instance of a [Writer] class.
     * @param append Boolean to specify if new writes are to be appended to the file.
     * @param charset Specify a [Charset] to be used. If not specified, default is [Charsets.UTF_8].
     */
    fun startWriting(writer: Writer, append: Boolean = false, charset: Charset = Charsets.UTF_8){
        if (!exists()) createNewFile()
        refreshFile()
        writer.setFileX(this, append, charset)
        writer.writeLines()
        writer.close()
    }

    /**
     * This is a specialized function. If ONLY ONE STRING is to be written in the file, then this is very useful.
     * There is no need to care about Writer classes or even the [startWriting] method.
     * The function takes care of opening an output stream, writing the supplied string and closing the output stream.
     *
     * This function is HIGHLY NOT RECOMMENDED for repetitive/multiple writes.
     * Example, if multiple writes are required in a file via a loop, then please use [startWriting] function
     * and place the loop logic inside the overridden [Writer.writeLines] function. Please see the example in [startWriting].
     *
     * - Example
     * ```
     * // create a blank file
     * val fx = FileX.new(/my_dir/my_one_line_file.txt)
     * fx.createNewFile(makeDirectories = true)
     *
     * // write the string
     * fx.writeOneLine("Write operation performed on ${System.currentTimeMillis()}. No further write operations to be done.")
     * ```
     *
     * @param string A string to be written in the file
     * @param append Boolean to specify if the string is to be appended to the file.
     * @param charset Specify a [Charset] to be used. If not specified, default is [Charsets.UTF_8].
     */
    fun writeOneLine(string: String, append: Boolean = false, charset: Charset = Charsets.UTF_8){
        if (!exists()) createNewFile()
        refreshFile()
        val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(outputStream(append), charset))
        writer.write(string)
        writer.close()
    }

    // next two functions copied straightaway from kotlin.io

    /**
     * This function logic is copied from [kotlin.io.readLines].
     *
     * Reads contents of the files and returns as a list of strings. To be only used on a file and not directory.
     * Not to be used for very big files.
     *
     * @param charset Specify a [Charset] to be used. If not specified, default is [Charsets.UTF_8].
     * @return Contents of the file as a List of String
     */
    fun readLines(charset: Charset = Charsets.UTF_8): List<String> {
        val result = ArrayList<String>()
        forEachLine(charset) { result.add(it); }
        return result
    }

    /**
     * This function logic is copied from [kotlin.io.forEachLine].
     *
     * Reads this file line by line using the specified [charset] and calls [action] for each line.
     * You may use this function on huge files.
     *
     * @param charset character set to use.
     * @param action function to process file lines.
     */
    fun forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
        // Note: close is called at forEachLine
        BufferedReader(InputStreamReader(inputStream(), charset)).forEachLine(action)
    }

    /**
     * Get actual size of a directory. This function recursively crawls over all the files inside a directory and all its subdirectories.
     * It sums up all the file sizes and returns the sum.
     *
     * This is to be run only on a directory. If run on a file / document, it works like [length].
     *
     * @return Total size of all the files, files inside sub-directories etc under the current FileX object.
     */
    fun getDirLength(): Long {
        return if (exists()) {
            if (!isDirectory) length()
            else {
                var sum = 0L
                listFiles()?.let {
                    for (f in it) sum += f.getDirLength()
                }
                sum
            }
        } else 0
    }

    /**
     * Writer class to be used with [startWriting].
     * The function [writeLines] must be overridden. Inside the function body, following methods are available.
     *
     * - [writeLine(line: String)][writeLine]: Accepts a string line to be written.
     * A line break is automatically added to the end of the string argument.
     * - [writeString(string: String)][writeString]: Accepts a string to be written. No line break is applied.
     *
     * Also functions with same function signatures as those available in [BufferedWriter] are present.
     * They work exactly as their `BufferedWriter` counterparts.
     *
     * - `write(c: Int)`: Writes a single character.
     * - `write(str: String)`: Writes a string.
     * - `write(cbuf: CharArray)`: Writes an array of characters.
     * - `write(s: String, off: Int, len: Int)`: Writes a portion of a String.
     * - `write(cbuf: CharArray, off: Int, len: Int)`: Writes a portion of an array of characters.
     */
    abstract class Writer{

        /**
         * BufferedWriter instance to be used for writing.
         * Initialised in [setFileX].
         */
        private var writer: BufferedWriter? = null

        /**
         * Initialises the [writer][Writer.writer] object on a specified FileX object [file], using a specified [charset].
         * All this takes place automatically inside the [startWriting] method.
         */
        internal fun setFileX(file: FileX, append: Boolean, charset: Charset) = file.run {
            writer = BufferedWriter(OutputStreamWriter(
                    outputStream(append), charset
            ))
        }

        /**
         * Method to be overridden. All write functions are to be performed inside.
         * Please see the example in [startWriting] method.
         */
        abstract fun writeLines()

        /**
         * Write one line with line break at the end.
         * Basically calls [writeString] with an '\n' at the end.
         */
        fun writeLine(line: String) {
            writeString("$line\n")
        }

        /**
         * Write a string without line break at the end.
         */
        fun writeString(string: String) = writer?.run {
            write(string)
        }

        // add all possible variants of BufferedWriter write()

        /**
         * Copied from [`BufferedWriter.write(int c)`][java.io.BufferedWriter#write(int)]
         *
         * Writes a single character.
         *
         * @exception  IOException  If an I/O error occurs
         */
        fun write(c: Int) = writer?.write(c)

        /**
         * Copied from [`BufferedWriter.write(String str)`][java.io.BufferedWriter#write(java.lang.String)]
         *
         * Writes a string.
         *
         * @param  str
         *         String to be written
         *
         * @throws  IOException
         *          If an I/O error occurs
         */
        fun write(str: String) = writer?.write(str)

        /**
         * Copied from `BufferedWriter.write(char cbuf[])`
         *
         * Writes an array of characters.
         *
         * @param  cbuf
         *         Array of characters to be written
         *
         * @throws  IOException
         *          If an I/O error occurs
         */
        fun write(cbuf: CharArray) = writer?.write(cbuf)

        /**
         * Copied from [`BufferedWriter.write(String s, int off, int len)`][java.io.BufferedWriter#write(java.lang.String, int, int)]
         *
         * Writes a portion of a String.
         *
         * If the value of the `len` parameter is negative then no
         * characters are written.  This is contrary to the specification of this
         * method in the [java.io.Writer#write(java.lang.String,int,int)]
         * superclass, which requires that an [IndexOutOfBoundsException] be
         * thrown.
         *
         * @param  s     String to be written
         * @param  off   Offset from which to start reading characters
         * @param  len   Number of characters to be written
         *
         * @exception  IOException  If an I/O error occurs
         */
        fun write(s: String, off: Int, len: Int) = writer?.write(s, off, len)

        /**
         * Copied from `BufferedWriter.write(char cbuf[], int off, int len)`
         *
         * Ordinarily this method stores characters from the given array into
         * this stream's buffer, flushing the buffer to the underlying stream as
         * needed.  If the requested length is at least as large as the buffer,
         * however, then this method will flush the buffer and write the characters
         * directly to the underlying stream.  Thus redundant
         * `BufferedWriter`s will not copy data unnecessarily.
         *
         * @param  cbuf  A character array
         * @param  off   Offset from which to start reading characters
         * @param  len   Number of characters to write
         *
         * @exception  IOException  If an I/O error occurs
         */
        fun write(cbuf: CharArray, off: Int, len: Int) = writer?.write(cbuf, off, len)

        /**
         * Function to close the [writer][Writer.writer] object.
         * This is automatically handled by [startWriting].
         */
        internal fun close() = writer?.close()
    }
}
