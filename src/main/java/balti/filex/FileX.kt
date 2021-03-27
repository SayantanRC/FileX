package balti.filex

import android.content.Intent
import android.net.Uri
import balti.filex.filex11.FileX11
import balti.filex.filex11.operators.refreshFileX11
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
import balti.filex.filexTraditional.FileXT
import java.io.*
import java.nio.charset.Charset
import java.util.*

abstract class FileX internal constructor(val isTraditional: Boolean) {
    abstract val path: String
    companion object {
        fun new(path: String, isTraditional: Boolean = FileXInit.globalIsTraditional): FileX =
                if (isTraditional) FileXT(path) else FileX11(path)
        fun new(parent: String, child: String, isTraditional: Boolean = FileXInit.globalIsTraditional): FileX =
                FileX.new("$parent/$child", isTraditional)
    }

    //FileX11 exclusive
    abstract val uri: Uri?
    //FileXT exclusive
    abstract val file: java.io.File
    fun refreshFile() {
        if (this is FileX11) this.refreshFileX11()
    }
    fun resetRoot(onResult: ((resultCode: Int, data: Intent?) -> Unit)){
        if (this is FileX11) this.setLocalRootUri(onResult)
    }

    //
    // Info
    //

    abstract val canonicalPath: String
    abstract val absolutePath: String
    abstract fun exists(): Boolean
    abstract val isDirectory: Boolean
    abstract val isFile: Boolean
    abstract val name: String
    abstract val parent: String?
    abstract val parentFile: FileX?
    abstract val parentCanonical: String
    abstract fun length(): Long
    abstract fun lastModified(): Long
    abstract fun canRead(): Boolean
    abstract fun canWrite(): Boolean
    abstract val freeSpace: Long
    abstract val usableSpace: Long
    abstract val totalSpace: Long
    abstract val isHidden: Boolean
    abstract val extension: String
    abstract val nameWithoutExtension: String
    //FileX11 exclusive
    abstract val storagePath: String?
    abstract val volumePath: String?
    abstract val rootPath: String?
    abstract val parentUri: Uri?
    //FileXT exclusive
    abstract fun canExecute(): Boolean

    //
    // Delete
    //

    abstract fun delete(): Boolean
    abstract fun deleteRecursively(): Boolean
    //FileXT exclusive
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
        writer.setFileX(this, append)
        writer.writeLines()
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
        internal fun close() = writer?.close()
    }
}
