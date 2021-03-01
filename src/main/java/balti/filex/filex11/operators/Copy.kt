package balti.filex.filex11.operators

import android.os.Build
import androidx.annotation.RequiresApi
import balti.filex.FileX
import balti.filex.exceptions.FileXAlreadyExists
import balti.filex.exceptions.FileXNotFoundException
import balti.filex.filex11.FileX11

internal class Copy(private val f: FileX11) {

    @RequiresApi(Build.VERSION_CODES.N)
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

}