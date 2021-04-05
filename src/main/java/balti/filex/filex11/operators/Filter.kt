package balti.filex.filex11.operators

import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.FileXInit.Companion.fCResolver
import balti.filex.Quad
import balti.filex.filex11.FileX11
import balti.filex.filex11.publicInterfaces.FileXFilter
import balti.filex.filex11.publicInterfaces.FileXNameFilter
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
                    val f = FileX11(path + "/" + getString(0), rootUri)
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
                            val f = FileX11("$path/$name", rootUri)
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
                        val f = FileX11("$path/$it", rootUri)
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

    fun listEverything(): ArrayList<Quad<String, Boolean, Long, Long>>? = f.run {
        val results = ArrayList<Quad<String, Boolean, Long, Long>>(0)

        if (!this.isDirectory || documentId == null) return null
        val childrenUri = getChildrenUri(documentId!!)

        val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        try {
            fCResolver.query(childrenUri, projection, null, null, null)?.run {
                while (moveToNext()) {
                    val name = getString(0)
                    val isDirectory = getString(1) == DocumentsContract.Document.MIME_TYPE_DIR
                    val size = try { getString(2).toLong() } catch (_: Exception) { 0L }
                    val lastModified = try { getString(3).toLong() } catch (_: Exception) { 0L }
                    val entry = Quad(name, isDirectory, size, lastModified)
                    results.add(entry)
                }
                close()
            }
            return results
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

}