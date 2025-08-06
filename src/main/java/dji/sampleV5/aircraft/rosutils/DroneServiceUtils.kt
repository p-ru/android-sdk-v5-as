package dji.sampleV5.aircraft.rosutils


import dji.v5.common.error.IDJIError
import android.util.Log
import dji.sampleV5.aircraft.AircraftControl
import dji.sampleV5.aircraft.models.TAG
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.common.callback.CommonCallbacks
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class DroneResult(val success: Boolean, val message: String)

object DroneServiceUtils {
    /**
     * Block until startLanding completes or times out.
     * @param controller  the aircraftController you’re using
     * @param timeoutSec  how many seconds to wait before giving up
     */


    private val gimbalParam = GimbalAngleRotation()

    fun landing(
        controller: AircraftControl,
        timeoutSec: Long = 10
    ): DroneResult {
        val latch = CountDownLatch(1)
        // this will be updated by the callbacks:
        var result = DroneResult(false, "no result")

        controller.startLanding(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>  {
            override fun onSuccess(t: EmptyMsg?) {
                result = DroneResult(true, "landing succeeded")
                latch.countDown()
            }
            override fun onFailure(error: IDJIError) {
                result = DroneResult(false, "landing failed: $error")
                latch.countDown()
            }
        })

        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                result = DroneResult(false, "landing timed out")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            result = DroneResult(false, "interrupted while waiting")
        }

        return result
    }


    fun takeoff(
        controller: AircraftControl,
        timeoutSec: Long = 10
    ): DroneResult {
        val latch = CountDownLatch(1)
        // this will be updated by the callbacks:
        var result = DroneResult(false, "no result")

        controller.startTakeOff(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>  {
            override fun onSuccess(t: EmptyMsg?) {
                result = DroneResult(true, "takeoff succeeded")
                latch.countDown()
            }
            override fun onFailure(error: IDJIError) {
                result = DroneResult(false, "takeoff failed: $error")
                latch.countDown()
            }
        })

        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                result = DroneResult(false, "takeoff timed out")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            result = DroneResult(false, "interrupted while waiting")
        }
        return result
    }



    fun setGimbal(
        gimbalParam: GimbalAngleRotation,
        controller: AircraftControl,
        timeoutSec: Long = 10
    ): DroneResult {
        val latch = CountDownLatch(1)
        // this will be updated by the callbacks:
        var result = DroneResult(false, "no result")

        controller.setGimbal(gimbalParam, object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>  {
            override fun onSuccess(t: EmptyMsg?) {
                result = DroneResult(true, "set_gimbal succeeded")
                latch.countDown()
            }
            override fun onFailure(error: IDJIError) {
                result = DroneResult(false, "set_gimbal failed: $error")
                latch.countDown()
            }
        })

        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                result = DroneResult(false, "set_gimbal timed out")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            result = DroneResult(false, "interrupted while waiting")
        }
        return result
    }
}
