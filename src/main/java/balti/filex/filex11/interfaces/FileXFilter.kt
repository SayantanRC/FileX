package balti.filex.filex11.interfaces

import balti.filex.filex11.FileX11

interface FileXFilter {
    fun accept(file: FileX11): Boolean
}