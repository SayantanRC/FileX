package balti.filex.exceptions

import balti.filex.FileX

/**
 * A base exception class for file system exceptions.
 * @property fileX the file on which the failed operation was performed.
 * @property otherFileX the second file involved in the operation, if any (for example, the target of a copy or move)
 * @property reasonString the description of the error
 */

open public class FileXSystemException(
        val fileX: FileX,
        val otherFileX: FileX? = null,
        val reasonString: String? = null
): kotlin.io.FileSystemException(fileX.file, otherFileX?.file, reasonString)

/**
 * An exception class which is used when some file to create or copy to already exists.
 */
public class FileXAlreadyExistsException(
        file: FileX,
        other: FileX? = null,
        reason: String? = null
) : FileXSystemException(file, other, reason)

/**
 * An exception class which is used when we have not enough access for some operation.
 */
public class FileXAccessDeniedException(
        file: FileX,
        other: FileX? = null,
        reason: String? = null
) : FileXSystemException(file, other, reason)

/**
 * An exception class which is used when file to copy does not exist.
 */
public class NoSuchFileXException(
        file: FileX,
        other: FileX? = null,
        reason: String? = null
) : FileXSystemException(file, other, reason)