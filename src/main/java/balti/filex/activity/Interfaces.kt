package balti.filex.activity

import android.app.Activity
import android.content.Intent

interface ToActivity {
    fun toActivity(trigger: (context: Activity) -> Unit,
                   onResult: (context: Activity, jobCode: Int, resultCode: Int, data: Intent?) -> Unit)
}

interface FromActivity {
    fun onActivityInit()
}