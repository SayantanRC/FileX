package balti.filex.filexTraditional.operators

import balti.filex.filexTraditional.FileXT

val FileXT.canonicalPath: String get() = file.canonicalPath
val FileXT.absolutePath: String get() = file.absolutePath
fun FileXT.exists(): Boolean = file.exists()
val FileXT.isDirectory: Boolean get() = file.isDirectory
val FileXT.isFile: Boolean get() = file.isFile
val FileXT.name: String get() = file.name
val FileXT.parent: String? get() = file.parent
val FileXT.parentFile: FileXT? get() = parent?.let { FileXT(it) }
val FileXT.parentCanonical: String get() = canonicalPath.let { if (it.isNotBlank()) it.substring(0, it.lastIndexOf("/")) else "/" }
fun FileXT.length(): Long = file.length()
fun FileXT.lastModified(): Long = file.lastModified()
fun FileXT.canRead(): Boolean = file.canRead()
fun FileXT.canWrite(): Boolean = file.canWrite()
fun FileXT.canExecute(): Boolean = file.canExecute()
val FileXT.freeSpace: Long get() = file.freeSpace
val FileXT.usableSpace: Long get() = file.usableSpace
val FileXT.totalSpace: Long get() = file.totalSpace
val FileXT.isHidden: Boolean get() = file.isHidden
val FileXT.extension: String get() = file.extension
val FileXT.nameWithoutExtension: String get() = file.nameWithoutExtension