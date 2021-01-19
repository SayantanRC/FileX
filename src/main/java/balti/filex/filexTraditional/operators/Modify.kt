package balti.filex.filexTraditional.operators

import balti.filex.FileX
import balti.filex.filex11.FileX11
import balti.filex.filexTraditional.FileXT
import java.io.File

internal class Modify(private val f: FileXT) {
    fun renameTo(dest: FileX): Boolean{
        return when (dest) {
            is FileXT -> renameTo(dest)
            is FileX11 -> renameTo(dest)
            else -> false
        }
    }

    private fun renameTo(dest: FileXT): Boolean = f.file.renameTo(dest.file)
    private fun renameTo(dest: FileX11): Boolean = f.run {
        val fx11Path: String? = if (dest.exists()) null else dest.canonicalPath
        return fx11Path?.let { file.renameTo(File(it)) } ?: false
    }

    fun renameTo(newFileName: String): Boolean = f.run{
        parentFile?.let {
            val newFile = File(it.file, newFileName)
            val res = file.renameTo(newFile)
            if (res) {
                file = newFile
                path = try {
                    path.let { it.substring(0, it.lastIndexOf('/')) } + "/" + newFileName
                } catch (_ : Exception){
                    file.path
                }
            }
            res
        }?: false
    }
}