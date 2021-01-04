package balti.filex.operators

import android.annotation.TargetApi
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.FileXServer

@RequiresApi(Build.VERSION_CODES.N)
@TargetApi(Build.VERSION_CODES.N)
fun FileX.renameTo(dest: FileX): Boolean {
    if (dest.exists()) return false
    val parentFile = dest.parentFile
    parentFile?.mkdirs()
    return if (parentFile?.uri == null) false
    else {
        DocumentsContract.moveDocument(fCResolver, uri!!, parentUri!!, parentFile.uri!!).let { movedUri ->
            if (movedUri != null) {
                tryIt { DocumentsContract.renameDocument(fCResolver, movedUri, dest.name) }
                dest.refreshFile()
                if (dest.exists()) FileXServer.setPathAndUri(rootUri!!, path, dest.uri, dest.path)
                else FileXServer.setPathAndUri(rootUri!!, path, movedUri, dest.parent + "/" + name)
                true
            }
            else false
        }
    }
}

fun FileX.renameTo(newFileName: String): Boolean {
    tryIt { DocumentsContract.renameDocument(fCResolver, uri!!, newFileName) }
    val newFilePath = "$parent/$newFileName"
    return FileX(newFilePath).let {
        if (exists()) {
            FileXServer.setPathAndUri(rootUri!!, path, it.uri, newFilePath)
            true
        }
        else false
    }
}