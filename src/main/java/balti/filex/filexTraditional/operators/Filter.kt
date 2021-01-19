package balti.filex.filexTraditional.operators

import balti.filex.FileX
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filexTraditional.FileXT
import java.io.File

internal class Filter(private val f: FileXT) {

    val isEmpty: Boolean
        get() = f.run {
            if (isDirectory) {
                file.list()?.isEmpty() ?: false
            } else false
        }

    fun listFiles(filter: FileXFilter): Array<FileX>? = f.file.listFiles()?.let {
        convertToFileX(it.filter { filter.accept(FileXT(it)) })
    }
    fun listFiles(filter: FileXNameFilter): Array<FileX>? = convertToFileX(f.file.listFiles {
        dir, name -> filter.accept(FileXT(dir), name)
    })

    fun listFiles() = convertToFileX(f.file.listFiles())

    fun list(filter: FileXFilter): Array<String>? = f.run {
        file.listFiles()?.filter { filter.accept(FileXT(it)) }?.map { it.name }?.toTypedArray()
    }
    fun list(filter: FileXNameFilter): Array<String>? = convertToStringArray(f.file.listFiles{
        dir, name -> filter.accept(FileXT(dir), name)
    })

    fun list() = convertToStringArray(f.file.listFiles())

    private fun convertToFileX(files: Array<File>?): Array<FileX>? = files?.map { FileXT(it.canonicalPath) }?.toTypedArray()
    private fun convertToFileX(files: List<File>?): Array<FileX>? = files?.map { FileXT(it.canonicalPath) }?.toTypedArray()
    private fun convertToStringArray(files: Array<File>?): Array<String>? = files?.map { it.name }?.toTypedArray()
}