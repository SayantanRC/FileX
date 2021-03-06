package balti.filex

/**
 * Inspired from [kotlin.Pair].
 * Represents a set of four values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Quad exhibits value semantics, i.e. two quads are equal if both components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 *
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 *
 * @constructor Creates a new instance of Quad.
 */
public data class Quad<out A, out B, out C, out D>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D
)

/**
 * Some useful functions, mainly used to format file paths.
 */
internal object Tools {

    /**
     * Adds a slash ('/') at front of the file path if not already present.
     * Removes trailing slash ('/') of present at the end.
     */
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

    /**
     * Removes slash ('/') at end of the file path if present.
     */
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

    /**
     * Removes multiple slashes ('/') if present in the path.
     * Whenever a new [FileX] object is created, the path is passed through this function.
     *
     * Examples:
     * - "//aaa////bbb/ccc//ddd/" will be converted to "/aaa/bbb/ccc/ddd/"
     * - "a/b/bbb//c///dd/dde///" will be converted to "a/b/bbb/c/dd/dde/"
     */
    @Suppress("NAME_SHADOWING")
    internal fun removeDuplicateSlashes(path: String): String {
        try {
            path.trim().let {
                if (it.length < 2) return path
                // add a space at the end for cases where duplicate is at the end.
                "$it "
            }.let { path ->

                var lastConsideredPtr: Char = path[0]
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
                    // Normally, lastConsideredPtr = behindPtr and added to modifiedString.
                    // Add lastConsideredPtr char to modifiedString if ptr and behindPtr are not duplicate.
                    // If duplicate, freeze lastConsideredPtr in its place, do not add anything to modifiedString.
                    // Once duplication is over, again move lastConsideredPtr at behindPtr and
                    // add lastConsideredPtr char to modifiedString. This will add only one instance of
                    // all the adjacent duplicate characters.
                    if (!qualifyForRemoval(ptr, behindPtr)) {
                        lastConsideredPtr = behindPtr
                        modifiedString.append(lastConsideredPtr)
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