package balti.filex


public data class Quad<out A, out B, out C, out D>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D
)

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
        try {
            path.trim().let {
                if (it.length < 2) return path
                // add a space at the end for cases where duplicate is at the end.
                "$it "
            }.let { path ->

                var lastDuplicatePtr: Char = path[0]
                var ptr: Char = path[1]

                fun qualifyForRemoval(startPtr: Char, endPtr: Char): Boolean {
                    // this function can be modified to remove and duplicate character, not just '/'
                    //    return startPtr == endPtr
                    return startPtr == '/' && endPtr == '/'
                }

                val modifiedString = StringBuffer("")

                for (i in 1 until path.length) {
                    ptr = path[i]
                    val behindPtr = path[i-1]
                    // behindPtr is one place behind ptr.
                    // Add lastDuplicatePtr char to modifiedString if ptr and behindPtr are not duplicate.
                    // If duplicate, freeze lastDuplicatePtr in its place, do not add anything to modifiedString.
                    // Once duplication is over, again move lastDuplicatePtr behind ptr and
                    // add lastDuplicatePtr char to modifiedString. This will add only one instance of
                    // all the adjacent duplicate characters.
                    if (!qualifyForRemoval(ptr, behindPtr)) {
                        lastDuplicatePtr = behindPtr
                        modifiedString.append(lastDuplicatePtr)
                    }
                }
                return modifiedString.toString()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            return path
        }
    }
}