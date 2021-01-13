package balti.filex.filexTraditional

import balti.filex.FileX
import balti.filex.Tools.removeLeadingTrailingSlashOrColon
import java.io.File

class FileXT(path: String): FileX(false) {
    constructor(parent: String, child: String): this("$parent/$child")

    override var path: String = ""
    private set

    init {
        this.path = removeLeadingTrailingSlashOrColon(path)
    }

    val file = File(path)
}