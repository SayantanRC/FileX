package balti.filex.filexTraditional.operators

import balti.filex.filexTraditional.FileXT

fun FileXT.createNewFile(makeDirectories: Boolean = false, overwriteIfExists: Boolean = false): Boolean {
    if (makeDirectories){
        parentFile.let { it?.mkdirs() }
    }
    if (overwriteIfExists) file.delete()
    return file.createNewFile()
}

fun FileXT.createNewFile() = createNewFile(false, overwriteIfExists = false)

fun FileXT.mkdirs(): Boolean = file.mkdirs()
fun FileXT.mkdir(): Boolean = file.mkdir()