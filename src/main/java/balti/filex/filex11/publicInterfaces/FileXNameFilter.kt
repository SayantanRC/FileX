package balti.filex.filex11.publicInterfaces

import balti.filex.FileX
import balti.filex.filexTraditional.FileXT
import balti.filex.filex11.FileX11

/**
 * This interface is made to replicate logic of [java.io.FilenameFilter].
 * It is used to provide logic to filter files in all FileX listing methods. This logic is provided in the [accept] method.
 *
 * This interface provides a unified way to deal with both [FileXT] and [FileX11] types.
 */
interface FileXNameFilter {

    /**
     * Function to be overridden to define the logic to filter files in file lists.
     *
     * @param dir Directory in which the file to be evaluated for the logic was found in.
     * @param name Name of the file to be evaluated with the filtering logic.
     * If actual FileX object is required for the logic, please check out [FileXFilter], rather than creating a FileX instance for every new file.
     *
     * @return `true` if the file qualifies to be included in the filtered list of files, `false` otherwise.
     */
    fun accept(dir: FileX, name: String): Boolean
}