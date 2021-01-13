package balti.filex.filex11.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class SysFilePickerActivity: Activity(), ToActivity {

    private lateinit var onResultFunction: (context: Activity, resultCode: Int, data: Intent?) -> Unit
    private var jobCode: Int = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityFunctionDelegate.onActivityInit(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == jobCode) {
            onResultFunction(this, resultCode, data)
            finish()
        }
    }

    override fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        optionalJobCode: Int
    ) {
        jobCode = optionalJobCode
        onResultFunction = onResult
        trigger(this)
    }
}