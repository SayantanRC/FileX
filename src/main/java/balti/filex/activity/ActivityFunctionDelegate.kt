package balti.filex.activity

import android.app.Activity
import android.content.Intent
import balti.filex.FileXInit.Companion.fContext

/**
 * Some jobs require to be executed with an activity context. This class facilitates such jobs.
 * This is a class which launches an activity, then passes a function to launch inside the activity.
 * It works on Interface based callback methods.
 * This class works as a middle-man because functions cannot be directly sent to Activities by Intent extras.
 *
 * - `trigger()`: It is a block of code (as a function) which is to be run on an activity context
 * - `onResult()`: Another block of code (as a function), which is to be run after executing the `trigger` function.
 *
 * How the flow of control works:
 * 1. A different caller class (say for example, in [resetGlobalRootUri()][balti.filex.filex11.utils.RootUri.resetGlobalRootUri])
 *   instantiates this class with a [trigger] function, an [onResult] function and a [launchingActivity].
 *   Both these two functions will be executed inside the [launchingActivity].
 * 2. Immediately upon instantiating an object of this class, inside `init` the [trigger] and [onResult] functions are stored in the
 *   companion object's [triggerFunction] and [onResultFunction] respectively.
 * 3. Immediately after, the Activity specified by [launchingActivity] is started. This activity implements [ToActivity] interface.
 * 4. When the Activity starts, in `onCreate()` it calls this class's companion object's [onActivityInit] method.
 * 5. The [onActivityInit] function now calls the Activity's [toActivity(trigger, onResult)][ToActivity.toActivity] method.
 *   This is an overridden method from the [ToActivity] interface.
 *   This `toActivity` method delivers the actual [triggerFunction] (=[trigger]) and [onResultFunction] (=[onResult]) to the activity.
 * 6. Finally, the [launchingActivity] executes the `trigger` function and then executes the `onResult` code block with the result from `trigger.`
 *
 * @param trigger Function sent to activity to execute after activity start.
 * @param onResult Function sent to activity to execute after executing [trigger].
 * This function runs on the result from executing [trigger] function.
 * @param launchingActivity Activity class to launch to execute [trigger].
 * This activity must implement the [ToActivity] interface. Default is [SysFilePickerActivity].
 *
 * @see ToActivity
 */
class ActivityFunctionDelegate(
        private val trigger: (context: Activity) -> Unit,
        private val onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit,
        private val launchingActivity: Class<*> = SysFilePickerActivity::class.java
){

    /**
     * This is used to hold the `trigger` and `onResult` functions, and they are sent to the [launchingActivity] after
     * it calls [onActivityInit] function. This the activity usually does on `onCreate()`,
     * basically after the activity is fully "ready" to execute the `trigger` function.
     *
     * The [launchingActivity] implements [ToActivity], hence overrides [ToActivity.toActivity].
     * When the activity calls the [ActivityFunctionDelegate.onActivityInit] function, it receives the `trigger` and `onResult`
     * function in its overridden `toActivity()` function.
     * The activity then runs the `trigger` function and the results of it are sent to `onResult`.
     */
    companion object {

        /**
         * If the `trigger` function requires calling the Activity's [Activity.startActivityForResult] function,
         * then it is called with this job code. It is also validated inside [Activity.onActivityResult].
         *
         * This is not done by actually analyzing the trigger function.
         * The logic is embedded in the different activities set in [launchingActivity].
         *
         * For example:
         * - [SysFilePickerActivity] is mainly written to launch System file picker UI (an activity) for selecting a root location for SAF storage.
         * Hence it is associated with [Activity.startActivityForResult], and uses this parameter.
         * - But [TraditionalFileRequest] is created to ask for file permissions in the old way, prior to how it was done in Android 9.
         * This is not related to any activity. Hence this parameter is useless.
         */
        private var optionalJobcode: Int = 111

        /**
         * Stores the [trigger] function.
         */
        private lateinit var triggerFunction: (context: Activity) -> Unit

        /**
         * Stores the [onResult] function.
         */
        private lateinit var onResultFunction: (context: Activity, resultCode: Int, data: Intent?) -> Unit

        /**
         * Function called by the [launchingActivity], to receive the [triggerFunction] and [onResultFunction].
         * @param activity The running activity (which is an instance of [ToActivity]) to which
         * the [triggerFunction] and [onResultFunction] is to be passed.
         */
        fun onActivityInit(activity: ToActivity) {
            activity.toActivity(triggerFunction, onResultFunction, optionalJobcode)
        }
    }

    /**
     * An new constructor created to specially for SAF uses. Used for setting the root location.
     * For SAF uses, mainly an [Intent] is launched to start the system file picker activity.
     * Hence this constructor is focused to receive an intent and launch in the [launchingActivity] with the provided [jobCode].
     *
     * @param jobCode Integer code, functions as `requestCode` for [Activity.startActivityForResult].
     * @param intent Intent (to launch system file picker ui) to be launched from the activity using [Activity.startActivityForResult].
     * This intent can have different flags and extras depending on the type of SAF based work to be performed.
     * @param onResult Function to be executed after receiving the result in the activity's [Activity.onActivityResult] method.
     */
    constructor(jobCode: Int, intent: Intent, onResult: (context: Activity, resultCode: Int, data: Intent?) -> Unit):
            this({ it.startActivityForResult(intent, jobCode) }, onResult) {
                // Store the provided jobCode.
                optionalJobcode = jobCode
            }

    init {
        // store the trigger and onResult functions.
        triggerFunction = trigger
        onResultFunction = onResult

        // launch the required activity.
        fContext.run {
            startActivity(Intent(this, launchingActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}