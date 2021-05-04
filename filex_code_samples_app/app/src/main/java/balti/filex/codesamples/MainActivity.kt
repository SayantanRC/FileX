package balti.filex.codesamples

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import balti.filex.FileX
import balti.filex.FileXInit
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize FileX
        FileXInit(this, false)

        request_permission.setOnClickListener {

            // Get root location if permission granted.
            fun getRootLocation(): String {
                val root = FileX.new("/")
                return root.canonicalPath
            }

            // Check FileX permission
            if (!FileXInit.isUserPermissionGranted()) {

                // Not granted. Request for access.
                FileXInit.requestUserPermission { resultCode, data ->
                    // for permission granted
                    if (resultCode == Activity.RESULT_OK) {
                        request_result.text = "${getString(R.string.granted)}.\n"
                        request_result.append("Root location: ${getRootLocation()}")
                    }
                    else {
                        // On system file picker, if the user keeps pressing back and exits out of it, then this block works.
                        request_result.text = "${getString(R.string.access_denied)}\n"
                    }
                }
            }
            else {

                // Already granted
                request_result.text = "${getString(R.string.already_granted)}\n"
                request_result.append("Root location: ${getRootLocation()}")
            }
        }
    }
}