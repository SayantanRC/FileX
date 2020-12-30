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