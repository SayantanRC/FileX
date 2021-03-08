package balti.filex

import balti.filex.exceptions.*
import java.io.IOException

internal class Copy(private val f: FileX) {

    fun copyTo(target: FileX, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): FileX {

        if (!f.exists()) {
            throw FileXNotFoundException("The source file doesn't exist.")
        }

        if (target.exists()){
            if (!overwrite)
                throw FileXAlreadyExists("The destination file already exists.")
            else if (!target.delete())
                throw FileXAlreadyExists("Tried to overwrite the destination, but failed to delete it.")
        }

        if (f.isDirectory) {
            if (!target.mkdirs())
                throw Exception("Failed to create target directory.")
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

    fun copyRecursively(
            target: FileX,
            overwrite: Boolean = false,
            onError: (FileX, Exception) -> OnErrorAction = { _, exception -> throw exception }
    ): Boolean = f.run {
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
                    }
                }
            }
            return true
        } catch (e: FileXSystemException) {
            return false
        }
    }

}