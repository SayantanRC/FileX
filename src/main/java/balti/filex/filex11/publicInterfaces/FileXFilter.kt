package balti.filex.filex11.publicInterfaces

import balti.filex.FileX
import balti.filex.filex11.FileX11
import balti.filex.filexTraditional.FileXT

/**
 * This interface is made to replicate logic of [java.io.FileFilter].
 * It is used to provide logic to filter files in all FileX listing methods. This logic is provided in the [accept] method.
 *
 * This interface provides a unified way to deal with both [FileXT] and [FileX11] types.
 */
interface FileXFilter {

    /**
     * Function to be overridden to define the logic to filter files in file lists.
     *
     * @param file The FileX object, pointinig to a file / directory to be evaluated with the filtering logic inside the method.
     * An alternate interface [FileXNameFilter] is also present, which focuses on the string name of the files, rather than FileX objects.
     *
     * @return `true` if the file qualifies to be included in the filtered list of files, `false` otherwise.
     */
    fun accept(file: FileX): Boolean
}