package balti.filex.filex11.operators

import android.annotation.TargetApi
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.filex11.FileXServer
import balti.filex.filexTraditional.FileXT
import balti.filex.filexTraditional.operators.canonicalPath
import balti.filex.filexTraditional.operators.renameTo

@RequiresApi(Build.VERSION_CODES.N)
@TargetApi(Build.VERSION_CODES.N)
fun FileX11.renameTo(dest: FileX11): Boolean {
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

@RequiresApi(Build.VERSION_CODES.N)
@TargetApi(Build.VERSION_CODES.N)
fun FileX11.renameTo(dest: FileXT): Boolean {
    return if (dest.canonicalPath.startsWith(rootPath)) {
        val relativePath = dest.canonicalPath.substring(rootPath.length)
        renameTo(FileX11(relativePath))
    }
    else FileXT(canonicalPath).renameTo(dest)
}

fun FileX11.renameTo(newFileName: String): Boolean {
    tryIt { DocumentsContract.renameDocument(fCResolver, uri!!, newFileName) }
    val newFilePath = "$parent/$newFileName"
    return FileX11(newFilePath).let {
        if (exists()) {
            balti.filex.filex11.FileXServer.setPathAndUri(rootUri!!, path, it.uri, newFilePath)
            true
        }
        else false
    }
}