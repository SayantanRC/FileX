package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filex11.utils.Tools.getChildrenUri

val FileX11.isEmpty: Boolean get() {
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

fun FileX11.listFiles(filter: FileXFilter? = null): Array<FileX11>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<FileX11>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                val f = FileX11(this@listFiles.path, getString(0))
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

fun FileX11.listFiles(filter: FileXNameFilter): Array<FileX11>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<FileX11>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                getString(0).let { name ->
                    if (filter.accept(this@listFiles, name)) {
                        val f = FileX11(this@listFiles.path, name)
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

fun FileX11.listFiles(): Array<FileX11>? = listFiles(null)

fun FileX11.listFiles(filter: ((file: FileX11) -> Boolean)): Array<FileX11>? =
    listFiles(object : FileXFilter {
        override fun accept(file: FileX11): Boolean = filter(file)
    })

fun FileX11.listFiles(filter: ((dir: FileX11, name: String) -> Boolean)): Array<FileX11>? =
    listFiles(object : FileXNameFilter {
        override fun accept(dir: FileX11, name: String): Boolean = filter(dir, name)
    })

fun FileX11.list(filter: FileXFilter? = null): Array<String>?{
    if (!this.isDirectory || documentId == null) return null
    val qualifyingList = ArrayList<String>(0)
    val childrenUri = getChildrenUri(documentId!!)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    try {
        fCResolver.query(childrenUri, projection, null, null, null)?.run {
            while (moveToNext()) {
                getString(0).let {
                    val f = FileX11(this@list.path, it)
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

fun FileX11.list(filter: FileXNameFilter): Array<String>?{
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

fun FileX11.list(): Array<String>? = list(null)

fun FileX11.list(filter: ((file: FileX11) -> Boolean)): Array<String>? =
    list(object : FileXFilter {
        override fun accept(file: FileX11): Boolean = filter(file)
    })

fun FileX11.list(filter: ((dir: FileX11, name: String) -> Boolean)): Array<String>? =
    list(object : FileXNameFilter {
        override fun accept(dir: FileX11, name: String): Boolean = filter(dir, name)
    })