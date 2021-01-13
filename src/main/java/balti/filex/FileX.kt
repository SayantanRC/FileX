package balti.filex

import balti.filex.filex11.FileX11
import balti.filex.filexTraditional.FileXT

abstract class FileX internal constructor(val isTraditional: Boolean) {
    abstract val path: String
    companion object {
        fun new(path: String): FileX11 = FileX11(path)
        fun new(parent: String, child: String): FileX11 = FileX11(parent, child)
        fun old(path: String): FileXT = FileXT(path)
        fun old(parent: String, child: String): FileXT = FileXT(parent, child)
    }
}