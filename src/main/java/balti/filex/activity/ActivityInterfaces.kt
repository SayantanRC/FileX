package balti.filex.activity

import android.app.Activity
import android.content.Intent

/**
 * Interface to pass `trigger` and `onResult` methods to an activity class.
 * This interface acts like a middle man, as functions cannot be passed as intent extras to an activity.
 *
 * Please see [ActivityFunctionDelegate] to understand how this interface is used.
 */
interface ToActivity {

    /**
     * This function is used to pass the `trigger` and `onResult` methods as function arguments to an activity.
     * The activity class implements this interface. When it calls [ActivityFunctionDelegate.onActivityInit] function,
     * the `trigger` and `onResult` methods are sent via this function.
     *
     * @param trigger The function (a block of code) to be executed on an activity context.
     * @param onResult The function to be executed with results of executing [trigger] function.
     * @param optionalJobCode Request code for cases where [Activity.startActivityForResult] needs to be called as trigger function.
     */
    fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        optionalJobCode: Int = 111
    )
}