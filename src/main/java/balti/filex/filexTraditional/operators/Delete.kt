package balti.filex.filexTraditional.operators

import balti.filex.filexTraditional.FileXT

fun FileXT.delete() = file.delete()
fun FileXT.deleteRecursively() = file.deleteRecursively()
fun FileXT.deleteOnExit() = file.deleteOnExit()