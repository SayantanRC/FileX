package balti.filex.filex11.operators

import android.os.Build
import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.FileXInit.Companion.tryIt
import balti.filex.filex11.FileX11
import balti.filex.filex11.FileXServer
import balti.filex.filexTraditional.FileXT

internal class Modify(private val f: FileX11) {

    fun renameTo(dest: FileX): Boolean {
        return when (dest) {
            is FileX11 -> renameTo(dest)
            is FileXT -> renameTo(dest)
            else -> false
        }
    }

    private fun renameTo(dest: FileX11): Boolean = f.run {
        if (dest.exists()) {
            if (dest.isFile) return false
            else if (!dest.isEmpty) return false
        }
        val parentFile = dest.parentFile
        parentFile?.mkdirs()
        return if (parentFile?.uri == null) false
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
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

        // for Android M and below, copy and then delete.
        else {
            if (balti.filex.Copy(this).copyRecursively(dest, deleteAfterCopy = true)) {
                if (deleteRecursively()) return@run true
            }
            return@run false
        }
    }

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
                directlySetUriAndPath(it.uri, newFilePath)
                FileXServer.setPathAndUri(rootUri!!, path, it.uri, newFilePath)
                true
            } else false
        }
    }
}