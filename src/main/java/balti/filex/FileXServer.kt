package balti.filex

import android.net.Uri
import androidx.lifecycle.MutableLiveData

class FileXServer {
    companion object{
        internal val pathAndUri = MutableLiveData<Quad<Uri, String, Uri?, String?>>().apply {
            value = Quad(Uri.EMPTY, "", null, null)
        }
        internal fun setPathAndUri(rootUri: Uri, path: String, uri: Uri?, newPath: String? = null){
            pathAndUri.value = Quad(rootUri, path, uri, newPath)
        }
    }
}

public data class Quad<out A, out B, out C, out D>(
    public val first: A,
    public val second: B,
    public val third: C,
    public val fourth: D
)