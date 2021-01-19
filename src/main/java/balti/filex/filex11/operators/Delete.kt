package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver

internal class Delete(private val f: FileX11) {
    fun delete(): Boolean = f.run {
        return uri?.let {
            if (isFile || isEmpty) DocumentsContract.deleteDocument(fCResolver, it)
            else false
        } ?: false
    }

    fun deleteRecursively(): Boolean = f.run {
        return uri?.let {
            DocumentsContract.deleteDocument(fCResolver, it)
        } ?: false
    }
}