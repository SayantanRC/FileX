package balti.filex.filexTraditional.operators

import balti.filex.FileX
import balti.filex.Quad
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
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

    fun listEverything(): ArrayList<Quad<String, Boolean, Long, Long>>? = f.run {
        val results = ArrayList<Quad<String, Boolean, Long, Long>>(0)

        f.file.listFiles()?.forEach {
            val name = it.name
            val isDirectory = it.isDirectory
            val size = it.length()
            val lastModified = it.lastModified()
            val entry = Quad(name, isDirectory, size, lastModified)
            results.add(entry)
        } ?: return null

        return results
    }

    private fun convertToFileX(files: Array<File>?): Array<FileX>? = files?.map { FileXT(it.canonicalPath) }?.toTypedArray()
    private fun convertToFileX(files: List<File>?): Array<FileX>? = files?.map { FileXT(it.canonicalPath) }?.toTypedArray()
    private fun convertToStringArray(files: Array<File>?): Array<String>? = files?.map { it.name }?.toTypedArray()
}