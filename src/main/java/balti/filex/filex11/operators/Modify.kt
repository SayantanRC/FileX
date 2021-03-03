package balti.filex.filex11.operators

import android.annotation.TargetApi
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.filex11.FileX11
import balti.filex.filex11.FileXServer
import balti.filex.filexTraditional.FileXT

internal class Modify(private val f: FileX11) {

    @RequiresApi(Build.VERSION_CODES.N)
    @TargetApi(Build.VERSION_CODES.N)
    fun renameTo(dest: FileX): Boolean {
        return when (dest) {
            is FileX11 -> renameTo(dest)
            is FileXT -> renameTo(dest)
            else -> false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @TargetApi(Build.VERSION_CODES.N)
    private fun renameTo(dest: FileX11): Boolean = f.run {
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
                } else false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @TargetApi(Build.VERSION_CODES.N)
    private fun renameTo(dest: FileXT): Boolean = f.run {
        return if (dest.canonicalPath.startsWith(rootPath)) {
            val relativePath = dest.canonicalPath.substring(rootPath.length)
            renameTo(FileX11(relativePath, rootUri))
        } else FileXT(canonicalPath).renameTo(dest)
    }

    fun renameTo(newFileName: String): Boolean = f.run{
        tryIt { DocumentsContract.renameDocument(fCResolver, uri!!, newFileName) }
        val newFilePath = "$parent/$newFileName"
        return FileX11(newFilePath, rootUri).let {
            if (exists()) {
                FileXServer.setPathAndUri(rootUri!!, path, it.uri, newFilePath)
                true
            } else false
        }
    }
}