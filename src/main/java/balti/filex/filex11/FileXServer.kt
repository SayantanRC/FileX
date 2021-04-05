package balti.filex.filex11

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import balti.filex.Quad

internal class FileXServer {
    companion object{
        internal val pathAndUri = MutableLiveData<Quad<Uri, String, Uri?, String?>>().apply {
            value = Quad(Uri.EMPTY, "", null, null)
        }
        internal fun setPathAndUri(rootUri: Uri, path: String, uri: Uri?, newPath: String? = null){
            val runnable = Runnable {
                pathAndUri.value = Quad(rootUri, path, uri, newPath)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) runnable.run()
            else Handler(Looper.getMainLooper()).post(runnable)
        }
    }
}