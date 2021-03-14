# FileX
[![JitPack](https://img.shields.io/jitpack/v/github/SayantanRC/FileX?color=green)](https://jitpack.io/#SayantanRC/FileX)  
[Build instructions](build_instructions.md)  

Quick links  
- [Philosophy](#philosophy)  
  - [How paths are interpreted?](#how-paths-are-interpreted)  
- [Internal classification](#internal-classification-based-on-istraditional)
- [Getting started](#getting-started)  
  - [Get the library from jitpack.io](#get-the-library-from-jitpackio)  
  - [Use the AAR file from this repository](#use-the-aar-file-from-this-repository)  
- [Initialize the library](#initialize-the-library)  
- [Init methods](#init-methods)
  - [Check for read-write permission](#check-for-file-read-write-permissions)
  - [Request for read-write access](#request-for-read-write-access)
  - [Refresh storage volumes](#refresh-storage-volumes)
- [Public attributes](#public-attributes-for-filex)  
- [Public methods](#public-methods-for-filex)  
- [Easy writing to files](#easy-writing-to-files)  

# Philosophy
From Android 11 onward, it is mandatory to use `DocumentsContract` or similar approach to write to shared storage, because of enforcement of Storage Access Framework. Our old and beloved Java File no longer works unless you are writing on private storage.  
Hence, there are two different ways to write a file: 1. Use Java File to write to internal storage. 2. Use DocumentsContract to write to shared storage.  
This means extra code to write. Also, DocumentsContract is not very friendly to work with, as it is completely a Uri based approach than the file path based approach we are generally aware of.  

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
val f = FileX.new("my/path/on/shared/storage")
// ignoring the second parameter defaults to false.
```
You may call `resetRoot()` on the object `f` to open the Documents UI which will allow the user to select a root directory on the shared storage. Once a root directory is chosen by the user, the path mentioned by you will be relative to that root.  
Assume in the above case, the user selects a directory as `[Internal storage]/dir1/dir2`. Then `f` here refers to `[Internal storage]/dir1/dir2/my/path/on/shared/storage`.  
This can also be seen by calling `canonicalPath` on `f`.
```
Log.d("Tag", f.canonicalPath)  
//   Output:  /storage/emulated/0/dir1/dir2/my/path/on/shared/storage
```
Once a root is set, you can peacefully use methods like `createNewFile()` to create the document, and other known methods for further operation and new file/document creation.  
Please check the sections:
[Check for file read write permissions](#check-for-file-read-write-permissions)  

# Internal classification (based on `isTraditional`)

![Classification](/doc_assets/FX_classification.png)  

This picture shows how FileX internally classifies itself as two different types based on the `isTraditional` argument. This is internal classification, and you as user do not have to worry.  
However, based on this classification, some specific methods and attributes are available to specific types. Example `createFileUsingPicker()` is a method available to `FileX11` objects, i.e. if `isTraditional` = false. But this method will throw an exception if used on `FileXT` object. These exclusive methods are expanded in a following section.

# Getting started
You can import the library in your project in any of the below ways.

## Get the library from [jitpack.io](https://jitpack.io/#SayantanRC/FileX/)  
1. In top-level `build.gradle` file, in `allprojects` section, add jitpack as shown below.
<pre>
allprojects {
    repositories {
        google()
        jcenter()
        ...
        <b>maven { url 'https://jitpack.io' }</b>
    }
}
</pre>
2. In the "app" level `build.gradle` file, add the dependency.
<pre>
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    ...
    <b>implementation 'com.github.SayantanRC:FileX:<i>alpha-4</i>'</b>
}
</pre>

Perform a gradle sync. Now you can use the library in the project.

## Use the AAR file from this repository.
1. Get the latest released AAR file from the [Releases](https://github.com/SayantanRC/FileX/releases) page.  
2. In your `app` module directory of the project, there should exist a directory named `libs`. If not, create it.  
3. Place the downloaded AAR file inside the `libs` directory.
4. In top-level `build.gradle` file, in `allprojects` section, add the below lines.
<pre>
allprojects {
    repositories {
        google()
        jcenter()
        ...
        <b>
        flatDir {
            dirs 'libs'
        }
        </b>
    }
}
</pre>
5. In the "app" level `build.gradle` file, add the dependency.
<pre>
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    ...
    <b>implementation(name:'FileX-release', ext:'aar')</b>
}
</pre>

Perform a gradle sync to use the library.

# Initialization

## Initialize the library
In your `MainActivity` class, in `onCreate()` add the below line. This is only needed once in the entire app.  
<b>This has to be before any `FileX` related operation or object creation is performed!!</b>  
```
FileXInit(this, false)
```
- The first argument is the context of the class. `FileXInit` will internally get the application context from this context.  
- The second argument is a <b>global</b> `isTraditional` attribute. All new FileX objects will take this value if not explicitly mentioned.  

Alternately you can also initialise the `FileXInit()` method from a subclass of the `Application()` class if you have it in your app. 
### Manifest
```
<application
        ...
        android:name=".AppInstance"
        ...
        >
        ...
        
</application>
```
### Application class
<pre>
class AppInstance: Application() {
    override fun onCreate() {
        ...
        <b>FileXInit(this, false)</b>
    }
}
</pre>

## Create FileX objects to work with
Working with FileX objects is similar to working with Java File objects.
```
val fx = FileX.new("my/path")
```
Here, the object `fx` gets its `isTraditional` parameter from the global parameter defined in `FileXInit()`. If you wish to override it, you may declare as below:
```
val fx = FileX.new("my/path", true)
```
This creates a `FileXT` object i.e. with `isTraditional` = true even though the global value may be false.

# Init methods
These are public methods available from `FileXInit` class.
## Check for file read-write permissions.
```
fun isUserPermissionGranted(): Boolean
```
For FileXT, the above method checks if the `Manifest.permission.READ_EXTERNAL_STORAGE` and `Manifest.permission.WRITE_EXTERNAL_STORAGE` are granted by the system.  
For FileX11, it checks if user has selected a root directory via the system ui and if the root exists now.  
#### Usage  
```
val isPermissionGranted = FileXInit.isUserPermissionGranted()
if (isPermissionGranted){
    // ... create some files
}
```
## Request for read-write access.
```
fun requestUserPermission(reRequest: Boolean = false, onResult: ((resultCode: Int, data: Intent?) -> Unit)? = null)
```
For FileXT, this method requests `Manifest.permission.READ_EXTERNAL_STORAGE` and `Manifest.permission.WRITE_EXTERNAL_STORAGE` from the `ActivityCompat.requestPermissions()` method.  
For FileX11, this method starts the system ui to let the user select a global root directory. The uri from the selected root directory is internally stored.  
All new FileX objects will consider this user selected directory as the root.     
#### Arguments:  
`reRequest`: Only applicable for FileX11, defunct for FileXT. Default is "false". If "false" and global root is already selected by user, and exists now, then user is not asked again. If "true" user is prompted to select a new global root directory. Root of all previously created FileX objects will remain unchanged.  
`onResult: ((resultCode: Int, data: Intent?) -> Unit)`: Optional callback function called once permission is granted or denied.
  - `resultCode`: If success, it is `Activity.RESULT_OK` else usually `Activity.RESULT_CANCELED`.  
  - `data`: Intent with some information.  
    - For FileXT  
    `data.getStringArrayExtra("permissions")` = Array is requested permissions. Equal to array consisting `Manifest.permission.READ_EXTERNAL_STORAGE`, `Manifest.permission.WRITE_EXTERNAL_STORAGE`    
    `data.getStringArrayExtra("grantResults")` = Array is granted permissions. If granted, should be equal to array of `PackageManager.PERMISSION_GRANTED`, `PackageManager.PERMISSION_GRANTED`  
    - For FileX11  
    `data.data` = Uri of the selected root directory.  
#### Usage  
```
FileXInit.requestUserPermission() { resultCode, data ->

    // this will be executed once user grants read-write permission (or selects new global root).
    // this block will also be executed if permission was already granted.
    // if permission was not previously granted (or global root is null or deleted), user will be prompted, 
    // and this block will be executed once user takes action.

    Log.d("DEBUG_TAG", "result code: $resultCode")
    if (!FileXInit.isTraditional) {
        Log.d("DEBUG_TAG", "root uri: ${data?.data}")
    }
    // create some files
}
```
## Refresh storage volumes
```
fun refreshStorageVolumes()
```
Useful only for FileX11 and above Android M. Detects all attached storage volumes. Say a new USB OTG drive is attached, then this may be helpful. In most cases, manually calling this method is not required as it is done automatically by the library.  
Usage: `FileXInit.refreshStorageVolumes()`

# Public attributes for `FileX`

| Attribute name       | Return type<br>(`?` - null return possible) | Exclusively for                        | Description                                                                                                                                                                                                                                               |
|----------------------|---------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| uri                  | String?                                     | FileX11<br>(`isTraditional`<br>=false) | Returns Uri of the document.<br>If used on `FileX11`, returns the tree uri.  <br>If used on `FileXT`, returns `Uri.fromFile()`                                                                                                                            |
| file                 | File?                                       | FileXT<br>(`isTraditional`<br>=true)   | Returns raw Java File.<br>Maybe useful for `FileXT`. But usually not of much use for `FileX11` as the returned File object cannot be read from or written to.                                                                                             |
| path                 | String                                      | -                                      | Path of the document. Formatted with leading slash (`/`) and no trailing slash.                                                                                                                                                                           |
| canonicalPath        | String                                      | -                                      | Canonical path of the object.<br>For `FileX11` returns complete path for any physical storage location (including SD cards) only from <b>Android 11+</b>. On lower versions, returns complete path for any location inside the Internal shared storage.   |
| absolutePath         | String                                      | -                                      | Absolute path of the object.<br>For `FileX11` it is same as `canonicalPath`                                                                                                                                                                               |
| isDirectory          | Boolean                                     | -                                      | Returns if the document referred to by the FileX object is directory or not. Returns false if document does not exist already.                                                                                                                            |
| isFile               | Boolean                                     | -                                      | Returns if the document is a file or not (like text, jpeg etc). Returns false if document does not exist.                                                                                                                                                 |
| name                 | String                                      | -                                      | Name of the document.                                                                                                                                                                                                                                     |
| parent               | String?                                     | -                                      | Path of the parent directory. This is not `canonicalPath` of the parent. Null if no parent.                                                                                                                                                               |
| parentFile           | FileX?                                      | -                                      | A FileX object pointing to the parent directory. Null if no parent.                                                                                                                                                                                       |
| parentCanonical      | String                                      | -                                      | `canonicalPath` of the parent directory.                                                                                                                                                                                                                  |
| freeSpace            | Long                                        | -                                      | Number of bytes of free space available in the storage location.                                                                                                                                                                                          |
| usableSpace          | Long                                        | -                                      | Number of bytes of usable space to write data. This usually takes care of permissions and other restrictions and more accurate than `freeSpace`                                                                                                           |
| totalSpace           | Long                                        | -                                      | Number of bytes representing total storage of the medium.                                                                                                                                                                                                 |
| isHidden             | Boolean                                     | -                                      | Checks if the document is hidden.<br>For `FileX11` checks if the name begins with a `.`                                                                                                                                                                   |
| extension            | String                                      | -                                      | Extension of the document                                                                                                                                                                                                                                 |
| nameWithoutExtension | String                                      | -                                      | The name of the document without the extension part.                                                                                                                                                                                                      |
| storagePath          | String?                                     | FileX11<br>(`isTraditional`<br>=false) | Returns the path of the document from the root of the storage.<br><b>Returns null for `FileXT`</b><br><br>Example: A document with user selected root = `[Internal storage]/dir1/dir2` and having a path `my/path`.<br>storagePath = `/dir1/dir2/my/path` |
| volumePath           | String?                                     | FileX11<br>(`isTraditional`<br>=false) | Returns the canonical path of the root of the storage.<br><b>Returns null for `FileXT`</b><br><br>Example: A document with user selected root = `[Internal storage]/dir1/dir2` and having a path `my/path`.<br>volumePath = `/storage/emulated/0`         |
| rootPath             | String?                                     | FileX11<br>(`isTraditional`<br>=false) | Returns the canonical path upto the root selected by the user.<br><b>Returns null for `FileXT`</b><br><br>Example: In the above scenario, rootPath = `/storage/emulated/0/dir1/dir2`                                                                      |
| parentUri            | Uri?                                        | FileX11<br>(`isTraditional`<br>=false) | Returns the tree uri of the parent directory if present, else null.<br><b>Returns null for `FileXT`</b>                                                                                                                                                   |
| isEmpty              | Boolean                                     | -                                      | Applicable on directories. Returns true if the directory is empty.                                                                                                                                                                                        |

# Public methods for `FileX`

| Method name                                                                                                                  | Return type<br>(`?` - null return possible) | Exclusively for                        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| refreshFile()                                                                                                                | -                                           | FileX11<br>(`isTraditional`<br>=false) | <b>Not required by `FileXT`</b><br><br><br>If the document was not present during declaration of the FileX object, and the document is later created by any other app, then call `refreshFile()` on it to update the Uri pointing to the file.<br>Do note that if your app is itself creating the document, you need not call `refreshFile()` again.<br><br>Example:<br><br>`val fx1 = FileX.new("aFile")`<br>`val fx2 = FileX.new("/aFile")`<br>`fx2.createNewFile()`<br><br>In this case you need not call `refreshFile()` on `fx1`. However if any other app creates the document, then you will not be able to refer to it unless the file is refreshed. |
| exists()                                                                                                                     | Boolean                                     | -                                      | Returns if the document exist. For `FileX11`, internally calls `refreshFile()` before checking.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| length()                                                                                                                     | Long                                        | -                                      | Length of the file in bytes.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| lastModified()                                                                                                               | Long                                        | -                                      | Value representing the time the file was last modified, measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| canRead()                                                                                                                    | Boolean                                     | -                                      | Returns if the document can be read from. Usually always true for `FileX11`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| canWrite()                                                                                                                   | Boolean                                     | -                                      | Returns if the document can be written to. Usually always true for `FileX11`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| canExecute()                                                                                                                 | Boolean                                     | FileXT<br>(`isTraditional`<br>=true)   | Returns if the Java File pointed by a FileX object is executable. Always false for `FileX11`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| delete()                                                                                                                     | Boolean                                     | -                                      | Deletes a single document. Does not delete a directory. Returns true if successful, else false.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| deleteRecursively()                                                                                                          | Boolean                                     | -                                      | Deletes a directory and all documents and other directories inside it. Returns true if successful.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| deleteOnExit()                                                                                                               | -                                           | FileXT<br>(`isTraditional`<br>=true)   | Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.<br>Same as `java.io.File.deleteOnExit()`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| createNewFile()                                                                                                              | Boolean                                     | -                                      | Creates document referred to by the FileX object. Throws error if the whole directory path is not present.<br>A safer alternative is a new variant of the method described below.                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| createNewFile(<br>  makeDirectories:Boolean=false, <br>  overwriteIfExists:Boolean=false, <br>  optionalMimeType:String<br>) | Boolean                                     | -                                      | Create a document.<br>If `makeDirectories` = true (Default: false) -> Creates the whole directory tree before the document if not present.<br>If `overwriteIfExist` = true (Default: false) -> Deletes the document if already present and creates a blank document.<br>For `FileX11`:<br>`optionalMimeType` as string can be specified. Ignored for `FileXT`<br><br>Returns true, if document creation is successful.                                                                                                                                                                                                                                       |
| createFileUsingPicker(<br>  optionalMimeType: String,<br>  afterJob:<br>    (resultCode: Int, data: Intent?)<br>)            | -                                           | FileX11<br>(`isTraditional`<br>=false) | Invoke the System file picker to create the file. Only applicable on `FileX11`<br><br>mime type can be spcified in `optionalMimeType`<br>`afterJob()` - custom function can be passed to execute after document is created.<br>    `resultCode` = `Activity.RESULT_OK` if document is successfully created.<br>    `data` = Intent data returned by System after document creation.                                                                                                                                                                                                                                                                          |
| mkdirs()                                                                                                                     | Boolean                                     | -                                      | Make all directories specified by the path of the FileX object (including the last element of the path and other non-existing parent directories.).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| mkdir()                                                                                                                      | Boolean                                     | -                                      | Creates only the last element of the path as a directory. Parent directories must be already present.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| renameTo(dest: FileX)                                                                                                        | Boolean                                     | -                                      | Move the current document to the path mentioned by the FileX parameter `dest`<br>For `FileX11` this only works for Android 7+ (API 24) due to Android limitations.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| renameTo(newFileName: String)                                                                                                | Boolean                                     | -                                      | Rename the document in place. This is used to only change the name and cannot move the document.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| inputStream()                                                                                                                | InputStream?                                | -                                      | Returns an `InputStream` to the document to write to.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| outputStream(mode:String="w")                                                                                                | OutputStream?                               | -                                      | Returns an `OutputStream` to the document to read from.<br><br>The `mode` argument is mainly useful for `FileX11`. It can be<br>`"r"` for read-only access,<br>`"w"` for write-only access (erasing whatever data is currently in the file),<br>`"wa"` for write-only access to append to any existing data,<br>`"rw"` for read and write access on any existing data,<br>and `"rwt"` for read and write access that truncates any existing file.<br><br>For `FileXT`, pass `"wa"` to get a `FileOutputStream` in append mode.                                                                                                                               |
| list()                                                                                                                       | Array-String?                               | -                                      | Returns a String array of all the contents of a directory.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| list(filter: FileXFilter)                                                                                                    | Array-String?                               | -                                      | Returns the list filtering with a `FileXFilter`. This is similar to `FileFilter` in Java.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| list(filter: FileXNameFilter)                                                                                                | Array-String?                               | -                                      | Returns the list filtering with a `FileXNameFilter`. This is similar to `FilenameFilter` in Java.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| listFiles()                                                                                                                  | Array-FileX?                                | -                                      | Returns an array of FileX pointing to all the contents of a directory.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| listFiles(filter: FileXFilter)                                                                                               | Array-FileX?                                | -                                      | Returns FileX elements array filtering with a `FileXFilter`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| listFiles(filter: FileXNameFilter)                                                                                           | Array-FileX?                                | -                                      | Returns FileX elements array filtering with a `FileXNameFilter`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| copyTo(<br>  target:FileX,<br>  overwrite:Boolean=false,<br>  bufferSize:Int<br>)                                            | FileX                                       | -                                      | Copies a file and returns the target. Logic is completely copied from File.copyTo() of kotlin.io.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| copyRecursively(<br>  target:FileX,<br>  overwrite:Boolean=false,<br>  onError:<br>    (FileX, Exception)<br>)               | Boolean                                     | -                                      | Directory copy recursively, return true if success else false.<br>Logic is completely copied from File.copyRecursively() of kotlin.io.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
# Easy writing to files.
You can easily write to a newly created file without having to deal with input or output streams. Check the below example:
```
// create a blank file
val fx = FileX.new(/my_dir/my_file.txt)
fx.createNewFile(makeDirectories = true)

// start writing to file
fx.startWriting(object : FileX.Writer() {
  override fun writeLines() {
  
    // write strings without line breaks.
    writeString("a string. ")
    writeString("another string in the same line.")
    
    // write a new line. Similar to writeString() with a line break at the end.
    writeLine("a new line.")
    writeLine("3rd line.")
  }
})
```
