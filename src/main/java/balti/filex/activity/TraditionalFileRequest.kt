package balti.filex.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat

class TraditionalFileRequest: Activity(), ToActivity  {

    private lateinit var onResultFunction: (context: Activity, resultCode: Int, data: Intent?) -> Unit
    private val REQUEST_CODE = 11112

    val EXTRA_PERMISSIONS = "permissions"
    val EXTRA_RESULTS = "grantResults"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityFunctionDelegate.onActivityInit(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val resultCode: Int =
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                RESULT_OK
            else RESULT_CANCELED
        onResultFunction(this, resultCode, Intent().apply {
            putExtra(EXTRA_PERMISSIONS, permissions)
            putExtra(EXTRA_RESULTS, grantResults)
        })
        finish()
    }

    override fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        optionalJobCode: Int
    ) {
        onResultFunction = onResult
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
    }
}