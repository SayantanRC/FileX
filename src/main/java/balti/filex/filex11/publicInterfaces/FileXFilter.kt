package balti.filex.filex11.publicInterfaces

import balti.filex.FileX

interface FileXFilter {
    fun accept(file: FileX): Boolean
}