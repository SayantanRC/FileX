package balti.filex.filexTraditional

import balti.filex.FileX
import java.io.File

class FileXT(override val path: String): FileX(false) {
    constructor(parent: String, child: String): this("$parent/$child")

    val file = File(path)
}