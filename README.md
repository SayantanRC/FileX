# FileX
## Philosophy
From Android 11 onwards, it is mandatory to use `DocumentsContract` or similar methods to write to shared storage, because of enforcement of Storage Access Framework. Our old and beloved Java File no longer works unless you are writing on private storage.  
Hence there are two different ways to write a file: 1. Use Java File to write to internal storage. 2. Use DocumentsContract to write to shared storage.  
This causes excess code and also DocumentsContract is not very friendly to work with, as it is completely a Uri based approach than the file path based approach we are generally aware of.  
Hence `FileX` was created. FileX tries to these problems:
1. It is mostly file path based. You as a user of the library do not have to think about Uris. They are handled in background.
2. FileX also wraps around old Java File. You only need to mention one parameter `isTraditional` to use the Java File way, or the DocumentsContract way.
3. Known syntax is used. You will find methods like `mkdirs()`, `delete()`, `canonicalPath` just like old Java File had.
