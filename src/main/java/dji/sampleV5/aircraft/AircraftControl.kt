package dji.sampleV5.aircraft



import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager

class AircraftControl {


    fun startTakeOff(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        FlightControllerKey.KeyStartTakeoff.create().action({
            callback.onSuccess(it)
        }, { e: IDJIError ->
            callback.onFailure(e)
        })
    }

    fun startLanding(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        FlightControllerKey.KeyStartAutoLanding.create().action({
            callback.onSuccess(it)
        }, { e: IDJIError ->
            callback.onFailure(e)
        })
    }

    fun enableVirtualStick(callback: CommonCallbacks.CompletionCallback) {
        VirtualStickManager.getInstance().enableVirtualStick(callback)
    }

    fun disableVirtualStick(callback: CommonCallbacks.CompletionCallback) {
        VirtualStickManager.getInstance().disableVirtualStick(callback)
    }
    fun enableVirtualStickAdvancedMode() {
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)
    }

    fun disableVirtualStickAdvancedMode() {
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(false)
    }

    fun sendVirtualStickAdvancedParam(param: VirtualStickFlightControlParam) {
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)
    }


    fun setGimbal(gimbalParam: GimbalAngleRotation, callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        GimbalKey.KeyRotateByAngle.create().action(
            gimbalParam,
            {callback.onSuccess(it)},
            {e: IDJIError -> callback.onFailure(e)})
    }



    fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        VirtualStickManager.getInstance().clearAllVirtualStickStateListener()
    }
}