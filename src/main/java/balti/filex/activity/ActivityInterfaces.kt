package balti.filex.activity

import android.app.Activity
import android.content.Intent

interface ToActivity {
    fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        optionalJobCode: Int = 111
    )
}