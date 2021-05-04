package balti.filex.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Class created for Storage Access Framework (SAF) usage.
 *
 * Depending on `trigger` function, is used for various purposes like
 * opening the system file picker activity to set a root storage location.
 */
class SysFilePickerActivity: Activity(), ToActivity {

    /**
     * Stores the block of code to be executed after executing the `trigger` function.
     * This is also the `onResult` function.
     *
     * This class is made assuming that the `trigger` function will start the system file picker UI which is an activity.
     * Hence the results from the user's interaction will be received in [onActivityResult].
     * Thus this function is run on the results received inside `onActivityResult`.
     */
    private lateinit var onResultFunction: (context: Activity, resultCode: Int, data: Intent?) -> Unit

    /**
     * Integer request code with which the system file picker activity is called.
     * The `trigger` function is expected to contain some form of [Activity.startActivityForResult] with this code.
     */
    private var jobCode: Int = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * This activity is called by [ActivityFunctionDelegate] class, which at this point,
         * already contains `trigger` and `onResult` functions.
         *
         * On calling this function, the [ActivityFunctionDelegate] passes those
         * `trigger` and `onResult` functions to this activity class, via the [toActivity] method.
         */
        ActivityFunctionDelegate.onActivityInit(this)
    }

    /**
     * Receive results from system file picker activity and pass
     * it to [onResultFunction], if the request code is equal to the [jobCode].
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == jobCode) {
            onResultFunction(this, resultCode, data)
            // close the activity after running `onResult`
            finish()
        }
    }

    /**
     * Overridden function of the [ToActivity] interface.
     *
     * - Receives the [trigger] function (which is expected to start system file picker activity).
     * - Also receives [onResult] function which is stored in [onResultFunction].
     * - [optionalJobCode] is stored in [jobCode].
     *
     * Finally, the [trigger] function is executed.
     */
    override fun toActivity(
        trigger: (context: Activity) -> Unit,
        onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        optionalJobCode: Int
    ) {
        jobCode = optionalJobCode
        onResultFunction = onResult

        // execute the trigger function with the current activity context.
        trigger(this)
    }
}