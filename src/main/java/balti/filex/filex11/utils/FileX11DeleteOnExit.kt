package balti.filex.filex11.utils

import balti.filex.FileX
import java.util.*

/**
 * This class was created to simulate file deletion like [java.io.File.deleteOnExit].
 * This was simply copied from `java.io.DeleteOnExitHook` and converted to kotlin by Android Studio's "Convert file to kotlin" feature.
 *
 * (the class `java.io.DeleteOnExitHook` cannot be linked for some reason, please check via [deleteOnExit()][java.io.File.deleteOnExit])
 *
 * - Theoretically:
 *
 * This class holds a set of filenames to be deleted on VM exit through a shutdown hook.
 * A set is used both to prevent double-insertion of the same file as well as offer
 * quick removal.
 */
object FileX11DeleteOnExit {
    private var files: LinkedHashSet<FileX?>? = LinkedHashSet()
    @Synchronized
    fun add(file: FileX?) {
        checkNotNull(files) {
            // DeleteOnExitHook is running. Too late to add a file
            "Shutdown in progress"
        }
        files!!.add(file)
    }

    fun runHooks() {
        var theFiles: LinkedHashSet<FileX?>?
        synchronized(FileX11DeleteOnExit::class.java) {
            theFiles = files
            files = null
        }
        val toBeDeleted = ArrayList(theFiles)

        // reverse the list to maintain previous jdk deletion order.
        // Last in first deleted.
        toBeDeleted.reverse()
        for (file in toBeDeleted) {
            file?.delete();
        }
    }

    init {
        // BEGIN Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                runHooks()
            }
        })
        // END Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
    }
}