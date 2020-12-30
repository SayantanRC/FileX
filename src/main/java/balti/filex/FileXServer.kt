package balti.filex

import android.net.Uri
import androidx.lifecycle.MutableLiveData

class FileXServer {
    companion object{
        internal val pathAndUri = MutableLiveData<Triple<Uri, String, Uri?>>().apply {
            value = Triple(Uri.EMPTY, "", null)
        }
        internal fun setPathAndUri(rootUri: Uri, path: String, uri: Uri?){
            pathAndUri.value = Triple(rootUri, path, uri)
        }
    }
}