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

    fun listFiles(filter: FileXFilter): Array<FileX>? = convertToFileXArray(f.file.listFiles { file ->
        filter.accept(FileXT(file))
    })

    fun listFiles(filter: FileXNameFilter): Array<FileX>? = convertToFileXArray(f.file.listFiles {
        dir, name -> filter.accept(FileXT(dir), name)
    })

    fun listFiles() = convertToFileXArray(f.file.listFiles())

    fun list(filter: FileXFilter): Array<String>? = f.run {
        file.listFiles()?.filter { filter.accept(FileXT(it)) }?.map { it.name }?.toTypedArray()
    }
    fun list(filter: FileXNameFilter): Array<String>? = convertToStringArray(f.file.listFiles{
        dir, name -> filter.accept(FileXT(dir), name)
    })

    fun list(): Array<String>? = f.file.list()

    fun listEverythingInQuad(): List<Quad<String, Boolean, Long, Long>>? = f.run {
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

    fun listEverything(): Quad<List<String>, List<Boolean>, List<Long>, List<Long>>? = f.run {
        val resultNames = ArrayList<String>(0)
        val resultDirectory = ArrayList<Boolean>(0)
        val resultSize = ArrayList<Long>(0)
        val resultLastModified = ArrayList<Long>(0)

        f.file.listFiles()?.forEach {
            val name = it.name
            val isDirectory = it.isDirectory
            val size = it.length()
            val lastModified = it.lastModified()

            resultNames.add(name)
            resultDirectory.add(isDirectory)
            resultSize.add(size)
            resultLastModified.add(lastModified)
        } ?: return null

        return Quad(resultNames, resultDirectory, resultSize, resultLastModified)
    }

    private fun convertToFileXArray(files: Array<File>?): Array<FileX>? = files?.map { FileXT(it) }?.toTypedArray()
    private fun convertToStringArray(files: Array<File>?): Array<String>? = files?.map { it.name }?.toTypedArray()
}