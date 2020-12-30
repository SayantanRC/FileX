package balti.filex.operators

import android.net.Uri
import android.provider.DocumentsContract
import balti.filex.FileX
import balti.filex.FileXInit

/*val FileX.isEmpty: Boolean get() = documentFile?.listFiles()?.isEmpty()?: false

fun FileX.list(filter: ((dir: FileX, child: FileX) -> Boolean)): ArrayList<FileX>{
    val immediateList = ArrayList<FileX>(0)
    documentFile?.listFiles()?.forEach {fx ->
        FileX(fx).let { if (filter(this, it)) immediateList.add(it) }
    }
    return immediateList
}

fun FileX.list(): ArrayList<FileX> =
    documentFile?.listFiles()?.map { FileX(it) }.let { if (it != null) ArrayList(it) else ArrayList(0) }*/

