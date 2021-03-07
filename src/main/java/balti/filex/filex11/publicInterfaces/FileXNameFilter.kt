package balti.filex.filex11.publicInterfaces

import balti.filex.FileX

interface FileXNameFilter {
    fun accept(dir: FileX, name: String): Boolean
}