package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver

fun FileX11.delete(): Boolean {
    return uri?.let {
        if (isFile || isEmpty) DocumentsContract.deleteDocument(fCResolver, it)
        else false
    } ?: false
}

fun FileX11.deleteRecursively(): Boolean {
    return uri?.let {
        DocumentsContract.deleteDocument(fCResolver, it)
    } ?: false
}