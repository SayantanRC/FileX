package balti.filex.interfaces

import balti.filex.FileX

interface FileXNameFilter {
    fun accept(dir: FileX, name: String): Boolean
}