package balti.filex

import android.content.Intent
import android.net.Uri
import balti.filex.filex11.FileX11
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
import balti.filex.filexTraditional.FileXT
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*
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
     * Else this variable will contain relative path from the user selected root location (from system picker).
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
     */
    abstract fun refreshFile()

    /**
     * `FileX11 exclusive` (SAF way)
     *
     * Opens system picker UI allow user select a new root for this FileX object. The method [FileXInit.requestUserPermission] is used to set a global root.
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
     * @return Boolean representing file / document can be read or not.
     */
    abstract fun canRead(): Boolean

    /**
     * Returns if the document can be written to. Usually always true for `FileX11`.
     * - For [FileX11] (SAF way) - checks if the [Uri][android.net.Uri] of the file has the write permission
     * via [UriPermission isWritePermission()][android.content.UriPermission.isWritePermission].
     * See [Info.canWrite()][balti.filex.filex11.operators.Info.canWrite].
     * - For [FileXT] (traditional way) - See [Java File canWrite()][java.io.File.canWrite].
     *
     * @return Boolean representing file / document can be written to or not.
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
     * This path, in most cases, is not readable or writable unless the user picks selects it from the system picker.
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
     * Returns the canonical path upto the root selected by the user from the system picker. Returns null for `FileXT`.
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
     * @return True if the file is executable, else false.
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
     * @return True is the file / document could be deleted, else false.
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

    abstract fun createNewFile(): Boolean
    abstract fun createNewFile(makeDirectories: Boolean = false, overwriteIfExists: Boolean = false, optionalMimeType: String = "*/*"): Boolean
    abstract fun mkdirs(): Boolean
    abstract fun mkdir(): Boolean
    //FileXT exclusive
    abstract fun createFileUsingPicker(optionalMimeType: String = "*/*", afterJob: ((resultCode: Int, data: Intent?) -> Unit)? = null)

    //
    // Modify
    //

    abstract fun renameTo(dest: FileX): Boolean
    abstract fun renameTo(newFileName: String): Boolean

    //
    // Copy
    //

    private val Copy = Copy(this)

    fun copyTo(target: FileX, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): FileX = Copy.copyTo(target, overwrite, bufferSize)
    fun copyRecursively(
        target: FileX,
        overwrite: Boolean = false,
        onError: (FileX, Exception) -> OnErrorAction = { _, exception -> throw exception }
    ): Boolean = Copy.copyRecursively(target, overwrite, onError)

    //
    // Filter
    //

    abstract val isEmpty: Boolean
    abstract fun listFiles(): Array<FileX>?
    abstract fun listFiles(filter: FileXFilter): Array<FileX>?
    abstract fun listFiles(filter: FileXNameFilter): Array<FileX>?
    fun listFiles(filter: (file: FileX) -> Boolean): Array<FileX>? =
            listFiles(object : FileXFilter {
                override fun accept(file: FileX): Boolean = filter(file)
            })
    fun listFiles(filter: (dir: FileX, name: String) -> Boolean): Array<FileX>? =
            listFiles(object : FileXNameFilter {
                override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
            })
    abstract fun list(): Array<String>?
    abstract fun list(filter: FileXFilter): Array<String>?
    abstract fun list(filter: FileXNameFilter): Array<String>?
    fun list(filter: (file: FileX) -> Boolean): Array<String>? =
            list(object : FileXFilter{
                override fun accept(file: FileX): Boolean = filter(file)
            })
    fun list(filter: (dir: FileX, name: String) -> Boolean): Array<String>? =
            list(object : FileXNameFilter{
                override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
            })
    abstract fun listEverythingInQuad(): ArrayList<Quad<String, Boolean, Long, Long>>?
    abstract fun listEverything(): Quad<List<String>, List<Boolean>, List<Long>, List<Long>>?

    //
    // Operations
    //

    abstract fun inputStream(): InputStream?

    /**
     * Taken from [android.content.ContentProvider.openAssetFile]
     *
     * @param mode Access mode for the file.  May be "r" for read-only access,
     * "w" for write-only access (erasing whatever data is currently in
     * the file), "wa" for write-only access to append to any existing data,
     * "rw" for read and write access on any existing data, and "rwt" for read
     * and write access that truncates any existing file.
     */
    abstract fun outputStream(mode: String = "w"): OutputStream?

    fun startWriting(writer: Writer, append: Boolean = false){
        if (!exists()) createNewFile()
        refreshFile()
        writer.setFileX(this, append)
        writer.writeLines()
        writer.close()
    }

    fun writeOneLine(string: String, append: Boolean = false){
        if (!exists()) createNewFile()
        refreshFile()
        val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(outputStream(if (append) "wa" else "rwt")))
        writer.write(string)
        writer.close()
    }

    // next two functions copied straightaway from kotlin.io
    fun readLines(charset: Charset = Charsets.UTF_8): List<String> {
        val result = ArrayList<String>()
        forEachLine(charset) { result.add(it); }
        return result
    }
    fun forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
        // Note: close is called at forEachLine
        BufferedReader(InputStreamReader(inputStream(), charset)).forEachLine(action)
    }

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

    abstract class Writer{
        private var writer: BufferedWriter? = null
        internal fun setFileX(file: FileX, append: Boolean) = file.run {
            writer = BufferedWriter(OutputStreamWriter(
                    outputStream(
                            if (append) "wa" else "rwt"
                            // "rwt" wipes the file clean before writing.
                            // This is seen to be the behaviour in FileWriter.

                            // "rw", "w" they start replacing the lines from top. If the file has
                            // 3 lines and append = false and only 1 line is written, the first line
                            // of the file is replaced with the new content. The rest 2 lines which
                            // were present previously remains unchanged.
                    )
            ))
        }
        abstract fun writeLines()
        fun writeLine(line: String) {
            writeString("$line\n")
        }
        fun writeString(line: String) = writer?.run {
            write(line)
        }

        // add all possible variants of BufferedWriter write()
        fun write(c: Int) = writer?.write(c)
        fun write(str: String) = writer?.write(str)
        fun write(cbuf: CharArray) = writer?.write(cbuf)
        fun write(s: String, off: Int, len: Int) = writer?.write(s, off, len)
        fun write(cbuf: CharArray, off: Int, len: Int) = writer?.write(cbuf, off, len)

        internal fun close() = writer?.close()
    }
}
