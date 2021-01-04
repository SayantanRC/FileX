package balti.filex.interfaces

import balti.filex.FileX

interface FileXFilter {
    fun accept(file: FileX): Boolean
}