package dji.sampleV5.aircraft


import android.util.Log
import dji.sampleV5.aircraft.models.TAG
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.parameter.ParameterTree

//test
class Listener : AbstractNodeMain() {
    private val aircraftController = AircraftControl()
    private val params = VirtualStickFlightControlParam().apply {
        pitch = 0.0
        roll = 0.0
        yaw = 0.0
        verticalThrottle = 0.0
        verticalControlMode = VerticalControlMode.VELOCITY
        rollPitchControlMode = RollPitchControlMode.VELOCITY
        yawControlMode = YawControlMode.ANGULAR_VELOCITY
        rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
    }
    override fun getDefaultNodeName(): GraphName {
        return GraphName.of("MSDK_App/listener")
    }

    override fun onStart(connectedNode: ConnectedNode) {
        Log.d(TAG, "Listener node started and subscribing to /cmd_vel")


        val tree: ParameterTree = connectedNode.parameterTree
        tree.addParameterListener("/test") { value ->
            if (value != null) {
                Log.d(TAG, "test: $value")
            } else {
                Log.d(TAG, "no parameter received")
            }
        }

        // controlling
        val subscriber = connectedNode.newSubscriber<geometry_msgs.Twist>("command/cmd_vel", "geometry_msgs/Twist")
        subscriber.addMessageListener { message ->
            Log.d(TAG, "yaw: ${message.angular.x}, x: ${message.linear.x}")



            //params.pitch = - message.linear.y
            //params.yaw = - message.angular.z

            params.roll = message.linear.x
            params.pitch = message.linear.y
            params.verticalThrottle = - message.linear.z


            params.yaw = message.angular.z


            aircraftController.sendVirtualStickAdvancedParam(params)
        }

//        // taking off
//        val takeoffSubscriber = connectedNode.newSubscriber<std_msgs.Empty>("command/takeoff", "std_msgs/Empty")
//        takeoffSubscriber.addMessageListener { newValue ->
//            if (newValue != null) {
//                Log.d(TAG, "take off message received")
//                aircraftController.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
//                    override fun onSuccess() {
//                        ToastUtils.showToast("enableVirtualStick success.")
//                    }
//
//                    override fun onFailure(error: IDJIError) {
//                        ToastUtils.showToast("enableVirtualStick error,$error")
//                    }
//                })
//                aircraftController.enableVirtualStickAdvancedMode()
//                aircraftController.startTakeOff(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
//                    override fun onSuccess(t: EmptyMsg?) {
//                        ToastUtils.showToast("start takeOff onSuccess.")
//                    }
//
//                    override fun onFailure(error: IDJIError) {
//                        ToastUtils.showToast("start takeOff onFailure,$error")
//                    }
//                })
//            } else {
//                Log.d(TAG, "no take off message received")
//            }
//
//        }


//
//        // landing
//        val landSubscriber = connectedNode.newSubscriber<std_msgs.Empty>("command/land", "std_msgs/Empty")
//        landSubscriber.addMessageListener {
//            aircraftController.disableVirtualStickAdvancedMode()
//            aircraftController.startLanding(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
//                override fun onSuccess(t: EmptyMsg?) {
//                    ToastUtils.showToast("start landing onSuccess.")
//                }
//
//                override fun onFailure(error: IDJIError) {
//                    ToastUtils.showToast("start landing onFailure,$error")
//                }
//            })
//            aircraftController.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
//                override fun onSuccess() {
//                    ToastUtils.showToast("disableVirtualStick success.")
//                }
//
//                override fun onFailure(error: IDJIError) {
//                    ToastUtils.showToast("disableVirtualStick error,${error})")
//                }
//            })
//        }
    }
}