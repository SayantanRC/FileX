package balti.filex.filex11.interfaces

import balti.filex.FileX
import balti.filex.filex11.FileX11

interface FileXNameFilter {
    fun accept(dir: FileX, name: String): Boolean
}