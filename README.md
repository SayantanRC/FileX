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

# Internal classification (based on `isTraditional`)

![Classification](/doc_assets/FX_classification.png)  

This picture shows how FileX internally classifies itself as two different types based on the `isTraditional` argument. This is internal classification and you as user do not have to worry.  
However, based on this classification, some specific methods and attributes are available based on this classification. Example `createFileUsingPicker()` is a method availble to `FileX11` objects, i.e. if `isTraditional` = false. But this method will throw an exception if used on `FileXT` object. These exclusive methods are expanded in a following section.

# Getting started

The library is available on [jitpack.io](https://jitpack.io/#SayantanRC/FileX/)  
1. In top level `build.gradle` file, in `allprojects` section, add jitpack as shown below.
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
2. Add dependency.
<pre>
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation 'androidx.core:core-ktx:1.3.2'
    ...
    <b>implementation 'com.github.SayantanRC:FileX:<i>alpha-2</i>'</b>
}
</pre>

Now you can use the library in the project.

Or you can get the AAR files from the [Releases](https://github.com/SayantanRC/FileX/releases) page.  

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

