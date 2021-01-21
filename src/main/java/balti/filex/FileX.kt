package balti.filex

import android.content.Intent
import android.net.Uri
import balti.filex.filex11.FileX11
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filex11.operators.refreshFileX11
import balti.filex.filexTraditional.FileXT
import java.io.File
import java.io.InputStream
import java.io.OutputStream

abstract class FileX internal constructor(val isTraditional: Boolean) {
    abstract val path: String
    companion object {
        fun new(path: String, isTraditional: Boolean = FileXInit.fisTraditional): FileX =
                if (isTraditional) FileXT(path) else FileX11(path)
        fun new(parent: String, child: String, isTraditional: Boolean = FileXInit.fisTraditional): FileX =
                FileX.new("$parent/$child", isTraditional)
    }

    //FileX11 exclusive
    abstract val uri: Uri?
    //FileXT exclusive
    abstract val file: File?
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
    abstract fun outputStream(): OutputStream?
}