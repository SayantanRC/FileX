package balti.filex.activity

import android.app.Activity
import android.content.Intent
import balti.filex.FileXInit.Companion.fContext

class ActivityFunctionDelegate(
    trigger: (context: Activity) -> Unit,
    onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
    launchingActivity: Class<*> = SysFilePickerActivity::class.java
){

    companion object {
        private var optionalJobcode: Int = 111
        private lateinit var triggerFunction: (context: Activity) -> Unit
        private lateinit var onResultFunction: (context: Activity, resultCode: Int, data: Intent?) -> Unit
        fun onActivityInit(activity: ToActivity) {
            activity.toActivity(triggerFunction, onResultFunction, optionalJobcode)
        }
    }


    constructor(jobCode: Int, intent: Intent, onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit):
            this({ it.startActivityForResult(intent, jobCode) }, onResult) {
                optionalJobcode = jobCode
            }

    init {

        triggerFunction = trigger
        onResultFunction = onResult
        fContext.run {
            startActivity(Intent(this, launchingActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}