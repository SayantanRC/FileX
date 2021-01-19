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
}