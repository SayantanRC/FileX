package balti.filex.operators

import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver

fun FileX.delete(): Boolean {
    return uri?.let {
        if (isFile || isEmpty) DocumentsContract.deleteDocument(fCResolver, it)
        else false
    } ?: false
}

fun FileX.deleteRecursively(): Boolean {
    return uri?.let {
        DocumentsContract.deleteDocument(fCResolver, it)
    } ?: false
}