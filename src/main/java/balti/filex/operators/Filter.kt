package balti.filex.operators

import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.interfaces.FileXFilter
import balti.filex.interfaces.FileXNameFilter
import balti.filex.utils.Tools.getChildrenUri
import java.io.FilenameFilter

val FileX.isEmpty: Boolean get() {
    if (!this.isDirectory || documentId == null) return false
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    var isEmpty = false
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            isEmpty = this.count <= 0
            close()
        }
    }
    catch (e: Exception){
        e.printStackTrace()
    }
    return isEmpty
}

fun FileX.listFiles(filter: FileXFilter? = null): Array<FileX>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<FileX>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                val f = FileX(this@listFiles.path, getString(0))
                if (filter == null || filter.accept(f))
                    qualifyingList.add(f)
            }
            close()
        }
    }
    catch (e: Exception){
        e.printStackTrace()
    }
    return qualifyingList.toTypedArray()
}

fun FileX.listFiles(filter: FileXNameFilter): Array<FileX>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<FileX>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                getString(0).let { name ->
                    if (filter.accept(this@listFiles, name)) {
                        val f = FileX(this@listFiles.path, name)
                        qualifyingList.add(f)
                    }
                }
            }
            close()
        }
    }
    catch (e: Exception){
        e.printStackTrace()
    }
    return qualifyingList.toTypedArray()
}

fun FileX.listFiles(): Array<FileX>? = listFiles(null)

fun FileX.listFiles(filter: ((file: FileX) -> Boolean)): Array<FileX>? =
    listFiles(object : FileXFilter{
        override fun accept(file: FileX): Boolean = filter(file)
    })

fun FileX.listFiles(filter: ((dir: FileX, name: String) -> Boolean)): Array<FileX>? =
    listFiles(object : FileXNameFilter{
        override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
    })

fun FileX.list(filter: FileXFilter? = null): Array<String>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<String>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                getString(0).let {
                    val f = FileX(this@list.path, it)
                    if (filter == null || filter.accept(f))
                        qualifyingList.add(it)
                }
            }
            close()
        }
    }
    catch (e: Exception){
        e.printStackTrace()
    }
    return qualifyingList.toTypedArray()
}

fun FileX.list(filter: FileXNameFilter): Array<String>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<String>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                getString(0).let { name ->
                    if (filter.accept(this@list, name)) {
                        qualifyingList.add(name)
                    }
                }
            }
            close()
        }
    }
    catch (e: Exception){
        e.printStackTrace()
    }
    return qualifyingList.toTypedArray()
}

fun FileX.list(): Array<String>? = list(null)

fun FileX.list(filter: ((file: FileX) -> Boolean)): Array<String>? =
    list(object : FileXFilter{
        override fun accept(file: FileX): Boolean = filter(file)
    })

fun FileX.list(filter: ((dir: FileX, name: String) -> Boolean)): Array<String>? =
    list(object : FileXNameFilter{
        override fun accept(dir: FileX, name: String): Boolean = filter(dir, name)
    })