package balti.filex

internal object Tools {
    @Suppress("NAME_SHADOWING")
    internal fun removeTrailingSlashOrColonAddFrontSlash(path: String): String {
        path.trim().let { path ->
            if (path.isBlank()) return ""
            val noFrontColon = if (path.startsWith(":")) {
                if (path.length > 1) path.substring(1)
                else ""
            } else path
            val withFrontSlash = noFrontColon.let { if (!it.startsWith("/")) "/$it" else it }
            return removeRearSlash(withFrontSlash)
        }
    }

    @Suppress("NAME_SHADOWING")
    internal fun removeRearSlash(path: String): String {
        path.trim().let { path ->
            if (path.isBlank() || path == "/") return "/"
            return if (path.last() == '/') {
                if (path.length > 1) path.substring(0, path.length - 1)
                else "/"
            } else path
        }
    }

    @Suppress("NAME_SHADOWING")
    internal fun removeDuplicateSlashes(path: String): String {
        path.trim().let{
            if (it.length < 2) return path
            // add a space at the end for cases where duplicate is at the end.
            "$it "
        }.let { path ->

            var startPtr: Char = path[0]
            var endPtr: Char = path[1]

            fun qualifyForRemoval(): Boolean {
                // this function can be modified to remove and duplicate character, not just '/'
                //    return startPtr == endPtr
                return startPtr == '/' && endPtr == '/'
            }

            val modifiedString = StringBuffer("")

            for(i in 1 until path.length){
                // startPtr is one place behind endPtr.
                // Add startPtr char to modifiedString if startPtr and endPtr are not duplicate.
                // If duplicate, freeze startPtr in its place, do not add anything to modifiedString.
                // Once duplication is over, again move startPtr behind endPtr and
                // add startPtr char to modifiedString. This will add only one instance of
                // all the adjacent duplicate characters.
                endPtr = path[i]
                if (!qualifyForRemoval()){
                    startPtr = path[i-1]
                    modifiedString.append(startPtr)
                }
            }
            return modifiedString.toString()
        }
    }
}