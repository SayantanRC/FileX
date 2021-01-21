```
fun FileX.listFiles(): ArrayList<String> {
    val files = ArrayList<String>(0)
    val uri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, DocumentsContract.getTreeDocumentId(rootUri))
    //files.add(uri.toString())
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
    val cursor = cResolver.query(uri, projection, null, null, null)
    while (cursor?.moveToNext() == true){
        files.add(cursor.getString(0).toString())
        files.add(cursor.getString(1).toString())
        /*files.add(cursor.getString(2).toString())
        files.add(cursor.getInt(3).toString())
        files.add(cursor.getInt(4).toString())
        files.add(cursor.getInt(5).toString())*/
        files.add("             ")
    }

    //cursor?.columnNames?.let { it.forEach { files.add(it.toString()) } }      // document_id, mime_type, _display_name, last_modified, flags, _size
    //                                                                  Types:     String       String     String         Int            Int    Int
    //files.add(DocumentsContract.getDocumentId(uri))                           // primary:ADM/aaa/bb/aaa1111
    //files.add(DocumentsContract.getTreeDocumentId(uri))                       // primary:ADM
    //files.add(DocumentsContract.getRootId(rootUri))

    cursor?.close()
    return files
}
```
```
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
```

delete()            -- done 20210104
deleteRecursively() -- done 20210104
list()              -- done 20210104
listFiles()         -- done 20210104
extension           -- done 20210104
nameWithoutExtension-- done 20210104
renameTo            -- done 20210104

inputStream()       -- done 20210121
outputStream()      -- done 20210121

To build AAR: (AAR is located under - build/outputs/aar/FileX-release.aar)
```
export JAVA_HOME="$HOME/android-studio/jre/"
./gradlew assembleRelease -xtest -xlint
```

