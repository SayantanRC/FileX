package balti.filex.filexTraditional.operators

import balti.filex.filexTraditional.FileXT
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter

val FileXT.isEmpty: Boolean get() =
    if (isDirectory) {
        file.list()?.isEmpty() ?: false
    } else false

fun FileXT.listFiles(filter: FileFilter): Array<FileXT>? = convertToFileXT(file.listFiles(filter))
fun FileXT.listFiles(filter: FilenameFilter): Array<FileXT>? = convertToFileXT(file.listFiles(filter))
fun FileXT.listFiles(filter: (file: FileXT) -> Boolean): Array<FileXT>? {
    return convertToFileXT(file.listFiles { pathname ->
        filter(FileXT(pathname.canonicalPath))
    })
}
fun FileXT.listFiles(filter: (dir: FileXT, name: String) -> Boolean): Array<FileXT>? {
    return convertToFileXT(file.listFiles { dir, name ->
        filter(FileXT(dir.canonicalPath), name)
    })
}
fun FileXT.listFiles() = convertToFileXT(file.listFiles())

fun FileXT.list(filter: FileFilter): Array<String>? = file.listFiles()?.filter { filter.accept(it) }?.map { it.name }?.toTypedArray()
fun FileXT.list(filter: FilenameFilter): Array<String>? = convertToStringArray(file.listFiles(filter))
fun FileXT.list(filter: (file: FileXT) -> Boolean): Array<String>? {
    return convertToStringArray(file.listFiles { pathname ->
        filter(FileXT(pathname.canonicalPath))
    })
}
fun FileXT.list(filter: (dir: FileXT, name: String) -> Boolean): Array<String>? {
    return convertToStringArray(file.listFiles { dir, name ->
        filter(FileXT(dir.canonicalPath), name)
    })
}
fun FileXT.list() = convertToStringArray(file.listFiles())

private fun convertToFileXT(files: Array<File>?): Array<FileXT>? = files?.map { FileXT(it.canonicalPath) }?.toTypedArray()
private fun convertToStringArray(files: Array<File>?): Array<String>? = files?.map { it.name }?.toTypedArray()