package balti.filex

import balti.filex.exceptions.*
import java.io.IOException

internal class Copy(private val f: FileX) {

    /**
     * Logic completely copied from [kotlin.io.copyTo]
     *
     * This function is used to copy a single file / document.
     * If run on a directory, then only a blank directory is created at the [target].
     *
     * @param target Location where the current file / document is to be copied.
     * @param overwrite If `true`, if a file exists at the [target] location,
     * then that file / document is deleted and the current file is copied in the target location.
     * Default value is `false`.
     * @param bufferSize Int specifying the buffer size to be used while copying. Default value is [DEFAULT_BUFFER_SIZE].
     *
     * @return the [target] file.
     *
     * @throws NoSuchFileXException If the source file doesn't exist.
     * @throws FileXAlreadyExistsException If the destination file already exists and [overwrite] argument is set to `false`.
     * @throws FileXSystemException If the source is a directory, this function only creates and empty directory at [target].
     * This exception is thrown if creating the empty directory fails.
     * @throws NullPointerException If the input stream (from source) or output stream (to target) is null.
     * @throws IOException If any errors occur while copying.
     */
    fun copyTo(target: FileX, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): FileX = f.run {

        refreshFile()
        target.refreshFile()

        if (!exists()) {
            throw NoSuchFileXException(file = this, reason = "The source file doesn't exist.")
        }

        if (target.exists()){
            if (!overwrite)
                throw FileXAlreadyExistsException(file = this, other = target, "The destination file already exists.")
            else if (!target.delete())
                throw FileXAlreadyExistsException(file = this, other = target, "Tried to overwrite the destination, but failed to delete it.")
        }

        if (isDirectory) {
            if (!target.mkdirs())
                throw FileXSystemException(this, target, "Failed to create target directory.")
        } else {

            target.createNewFile(makeDirectories = true)

            val inputStream = f.inputStream()
            val outputStream = target.outputStream()

            if (inputStream == null) throw NullPointerException("Input stream is null")
            if (outputStream == null) throw NullPointerException("Output stream is null")

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output, bufferSize)
                }
            }
        }

        return target
    }

    /**
     * Logic completely copied from [kotlin.io.copyRecursively]
     *
     * This function is used to recursively copy a directory with files and subdirectories inside.
     *
     * @param target Location where the current directory is to be copied.
     * @param overwrite If `true`, then if conflicting files and directories exists inside [target] location,
     * then they are deleted and the source file / subdirectories are copied.
     * Default value is `false`.
     * @param onError If any errors occur during the copying, then further actions will depend on the result of the call
     * to `onError(File, IOException)` function, that will be called with arguments,
     * specifying the file that caused the error and the exception itself.
     * By default this function rethrows exceptions.
     *
     * Exceptions that can be passed to the `onError` function:
     *
     * - [NoSuchFileXException] - if there was an attempt to copy a non-existent file
     * - [FileAlreadyExistsException] - if there is a conflict
     * - [AccessDeniedException] - if there was an attempt to open a directory that didn't succeed.
     * - [IOException] - if some problems occur when copying.
     *
     * @return the [target] file.
     * @throws NoSuchFileXException if the source file doesn't exist.
     * @throws FileAlreadyExistsException if the destination file already exists and [overwrite] argument is set to `false`.
     * @throws IOException if any errors occur while copying.
     */
    fun copyRecursively(
            target: FileX,
            overwrite: Boolean = false,
            onError: (FileX, Exception) -> OnErrorAction = { _, exception -> throw exception },
            deleteAfterCopy: Boolean = false  // internal flag used to move files.
    ): Boolean = f.run {

        this.refreshFile()
        target.refreshFile()

        if (!exists()) {
            return onError(this, NoSuchFileXException(file = this, reason = "The source file doesn't exist.")) !=
                    OnErrorAction.TERMINATE
        }
        try {
            // We cannot break for loop from inside a lambda, so we have to use an exception here
            for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw FileXSystemException(f) }) {
                if (!src.exists()) {
                    if (onError(src, NoSuchFileXException(file = src, reason = "The source file doesn't exist.")) ==
                            OnErrorAction.TERMINATE)
                        return false
                } else {

                    // own logic: different from kotlin.io.copyRecursively()
                    val relPath = src.canonicalPath.substring(this.canonicalPath.length).let {
                        // remove leading '/'
                        if (it.startsWith('/')) it.substring(1)
                        else it
                    }
                    val targetPath = target.path.let {
                        // add trailing '/'
                        if (it.endsWith('/')) it else "$it/"
                    }
                    val dstFile = FileX.new("${targetPath}$relPath")
                    // *****

                    if (dstFile.exists() && !(src.isDirectory && dstFile.isDirectory)) {
                        val stillExists = if (!overwrite) true else {
                            if (dstFile.isDirectory)
                                !dstFile.deleteRecursively()
                            else
                                !dstFile.delete()
                        }

                        if (stillExists) {
                            if (onError(dstFile, FileXAlreadyExistsException(file = src,
                                            other = dstFile,
                                            reason = "The destination file already exists.")) == OnErrorAction.TERMINATE)
                                return false

                            continue
                        }
                    }

                    if (src.isDirectory) {
                        dstFile.mkdirs()
                    } else {
                        if (src.copyTo(dstFile, overwrite).length() != src.length()) {
                            if (onError(src, IOException("Source file wasn't copied completely, length of destination file differs.")) == OnErrorAction.TERMINATE)
                                return false
                        }

                        // deleteAfterCopy is an internal flag used to move files.
                        else if (deleteAfterCopy) src.delete()
                    }
                }
            }
            return true
        } catch (e: FileXSystemException) {
            return false
        }
    }

}