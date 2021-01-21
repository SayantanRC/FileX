package balti.filex.filexTraditional

import android.content.Intent
import android.net.Uri
import balti.filex.FileX
import balti.filex.Tools.removeTrailingSlashOrColonAddFrontSlash
import balti.filex.exceptions.ImproperFileXType
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filexTraditional.operators.Filter
import balti.filex.filexTraditional.operators.Modify
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class FileXT(path: String): FileX(false) {

    internal constructor(file: File): this(file.canonicalPath)

    override var path: String = ""
    internal set

    override lateinit var file: File

    init {
        this.path = removeTrailingSlashOrColonAddFrontSlash(path)
        file = File(this.path)
    }

    override val uri: Uri? get() = Uri.fromFile(file)
    override val canonicalPath: String = file.canonicalPath
    override val absolutePath: String = file.absolutePath
    override fun exists(): Boolean = file.exists()
    override val isDirectory: Boolean = file.isDirectory
    override val isFile: Boolean = file.isFile
    override val name: String = file.name
    override val parent: String? =  file.parent
    override val parentFile: FileX? = file.parentFile?.let { FileXT(it.canonicalPath)}
    override val storagePath: String? = null
    override val volumePath: String? = null
    override val parentCanonical: String = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "/" }
    override fun length(): Long = file.length()
    override fun lastModified(): Long = file.lastModified()
    override fun canRead(): Boolean = file.canRead()
    override fun canWrite(): Boolean = file.canWrite()
    override val freeSpace: Long = file.freeSpace
    override val usableSpace: Long = file.usableSpace
    override val totalSpace: Long = file.totalSpace
    override val isHidden: Boolean = file.isHidden
    override val extension: String = file.extension
    override val nameWithoutExtension: String = file.nameWithoutExtension
    override val parentUri: Uri? = null
    override fun canExecute(): Boolean = file.canExecute()
    override val rootPath: String? = null

    override fun delete(): Boolean = file.delete()
    override fun deleteOnExit() = file.deleteOnExit()
    override fun deleteRecursively(): Boolean = file.deleteRecursively()

    override fun createNewFile(): Boolean = file.createNewFile()
    override fun createNewFile(makeDirectories: Boolean, overwriteIfExists: Boolean, optionalMimeType: String): Boolean {
        if (makeDirectories){
            parentFile.let { it?.mkdirs() }
        }
        if (overwriteIfExists) file.delete()
        return file.createNewFile()
    }

    override fun mkdir(): Boolean = file.mkdir()
    override fun mkdirs(): Boolean = file.mkdirs()
    override fun createFileUsingPicker(optionalMimeType: String, afterJob: ((resultCode: Int, data: Intent?) -> Unit)?) {
        throw ImproperFileXType("Not applicable on traditional FileX")
    }

    private val Modify = Modify(this)
    override fun renameTo(dest: FileX): Boolean = Modify.renameTo(dest)
    override fun renameTo(newFileName: String): Boolean = Modify.renameTo(newFileName)

    private val Filter = Filter(this)

    override val isEmpty: Boolean = Filter.isEmpty
    override fun listFiles(): Array<FileX>? = Filter.listFiles()
    override fun listFiles(filter: FileXFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun listFiles(filter: FileXNameFilter): Array<FileX>? = Filter.listFiles(filter)
    override fun list() = Filter.list()
    override fun list(filter: FileXFilter): Array<String>? = Filter.list(filter)
    override fun list(filter: FileXNameFilter): Array<String>? = Filter.list(filter)

    override fun inputStream(): InputStream = file.inputStream()
    override fun outputStream(): OutputStream = file.outputStream()
}