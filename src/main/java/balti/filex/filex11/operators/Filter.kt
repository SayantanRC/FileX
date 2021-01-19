package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.filex11.FileX11
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.filex11.interfaces.FileXFilter
import balti.filex.filex11.interfaces.FileXNameFilter
import balti.filex.filex11.utils.Tools.getChildrenUri

internal class Filter(private val f: FileX11) {

    val isEmpty: Boolean
        get() = f.run{
            if (!this.isDirectory || documentId == null) return false
            val childrenUri = getChildrenUri(documentId!!)
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            var isEmpty = false
            try {
                fCResolver.query(childrenUri, projection, null, null, null)?.run {
                    isEmpty = this.count <= 0
                    close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return isEmpty
        }

    fun listFiles(filter: FileXFilter? = null): Array<FileX>? = f.run {
        if (!this.isDirectory || documentId == null) return null
        val qualifyingList = ArrayList<FileX11>(0)
        val childrenUri = getChildrenUri(documentId!!)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    val f = FileX11(path + "/" + getString(0))
                    if (filter == null || filter.accept(f))
                        qualifyingList.add(f)
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return qualifyingList.toTypedArray()
    }

    fun listFiles(filter: FileXNameFilter): Array<FileX>? = f.run {
        if (!this.isDirectory || documentId == null) return null
        val qualifyingList = ArrayList<FileX11>(0)
        val childrenUri = getChildrenUri(documentId!!)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    getString(0).let { name ->
                        if (filter.accept(f, name)) {
                            val f = FileX11("$path/$name")
                            qualifyingList.add(f)
                        }
                    }
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return qualifyingList.toTypedArray()
    }

    fun listFiles(): Array<FileX>? = listFiles(null)

    fun list(filter: FileXFilter? = null): Array<String>? = f.run {
        if (!this.isDirectory || documentId == null) return null
        val qualifyingList = ArrayList<String>(0)
        val childrenUri = getChildrenUri(documentId!!)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    getString(0).let {
                        val f = FileX11("$path/$it")
                        if (filter == null || filter.accept(f))
                            qualifyingList.add(it)
                    }
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return qualifyingList.toTypedArray()
    }

    fun list(filter: FileXNameFilter): Array<String>? = f.run {
        if (!this.isDirectory || documentId == null) return null
        val qualifyingList = ArrayList<String>(0)
        val childrenUri = getChildrenUri(documentId!!)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    getString(0).let { name ->
                        if (filter.accept(f, name)) {
                            qualifyingList.add(name)
                        }
                    }
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return qualifyingList.toTypedArray()
    }

    fun list(): Array<String>? = list(null)

}