package balti.filex.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class SysFilePickerActivity: Activity(), ToActivity {

    companion object{
        internal lateinit var context: Context
        private lateinit var onResultFunction: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        ActivityFunctionDelegate.onActivityInit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onResultFunction(this, requestCode, resultCode, data)
        finish()
    }

    override fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit
    ) {
        onResultFunction = onResult
        trigger(this)
    }
}