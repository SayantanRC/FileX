package balti.filex.filexTraditional.operators

import balti.filex.filex11.FileX11
import balti.filex.filex11.operators.canonicalPath
import balti.filex.filex11.operators.exists
import balti.filex.filexTraditional.FileXT
import java.io.File

fun FileXT.renameTo(dest: FileXT): Boolean = file.renameTo(dest.file)
fun FileXT.renameTo(dest: FileX11): Boolean {
    val fx11Path: String? = if (dest.exists()) null else dest.canonicalPath
    return fx11Path?.let { file.renameTo(File(it)) }?: false
}