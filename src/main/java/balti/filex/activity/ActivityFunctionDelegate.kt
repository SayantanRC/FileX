package balti.filex.activity

import android.app.Activity
import android.content.Intent
import balti.filex.FileXInit.Companion.fContext

class ActivityFunctionDelegate(trigger: (context: Activity) -> Unit, onResult: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit){

    companion object: FromActivity {
        private lateinit var triggerFunction: (context: Activity) -> Unit
        private lateinit var onResultFunction: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit
        override fun onActivityInit() {
            (SysFilePickerActivity.context as ToActivity).toActivity(triggerFunction, onResultFunction)
        }
    }

    constructor(jobCode: Int, intent: Intent, onResult: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit):
            this({ it.startActivityForResult(intent, jobCode) }, onResult)

    init {
        triggerFunction = trigger
        onResultFunction = onResult
        fContext.run {
            startActivity(Intent(this, SysFilePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}