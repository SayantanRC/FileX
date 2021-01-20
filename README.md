# FileX
# Philosophy
From Android 11 onwards, it is mandatory to use `DocumentsContract` or similar approach to write to shared storage, because of enforcement of Storage Access Framework. Our old and beloved Java File no longer works unless you are writing on private storage.  
Hence there are two different ways to write a file: 1. Use Java File to write to internal storage. 2. Use DocumentsContract to write to shared storage.  
This causes excess code and also DocumentsContract is not very friendly to work with, as it is completely a Uri based approach than the file path based approach we are generally aware of.  
Hence `FileX` was created. FileX tries to address these problems:
1. It is mostly file path based. You as a user of the library do not have to think about Uris. They are handled in background.
2. FileX also wraps around old Java File. You only need to mention one parameter `isTraditional` to use the Java File way, or the DocumentsContract way.
3. Known syntax is used. You will find methods like `mkdirs()`, `delete()`, `canonicalPath` just like old Java File had.

## How paths are interpreted?
If you use the `isTraditional` parameter as below:
```
FileX.new("my/example/path", isTraditional = true)
```
then it is similar to declaring:
```
File("my/example/path")
```
This can be used to access private storage of the app. This also lets you access shared storage on Android 10 and below.  
However, for accessing shared storage on Android 11+, you cannot declare the `isTraditional` parameter as true.  
```
val f = FileX("my/path/on/shared/storage")
```
You may call `resetRoot()` on the object `f` to open the Documents UI which will allow the user to select a root directory on the shared storage. Once a root directory is chosen by the user, the path mentioned by you will be relative to that root.  
Assume in the above case, the user selects a directory as `[Internal storage]/dir1/dir2`. Then `f` here refers to `[Internal storage]/dir1/dir2/my/path/on/shared/storage`.  
This can also be seen by calling `canonicalPath` on `f`
```
Log.d("Tag", f.canonicalPath)  
//   Output:  /storage/emulated/0/dir1/dir2/my/path/on/shared/storage
```
Once a root is set, you can peacefully use methods like `createNewFile()` to create the document, and other known methods for further operation and new file/document creation.  
