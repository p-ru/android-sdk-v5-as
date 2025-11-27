package dji.sampleV5.aircraft.rosutils


import android.util.Log
import com.google.gson.Gson
import dji.sampleV5.aircraft.AircraftControl
import dji.sampleV5.aircraft.keyvalue.EnumItem
import dji.sampleV5.aircraft.keyvalue.KeyItem
import dji.sampleV5.aircraft.models.TAG
import dji.sampleV5.aircraft.util.ToastUtils.showToast
import dji.sdk.keyvalue.converter.DJIValueConverter
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.manager.KeyManager
import dji.v5.manager.capability.CapabilityManager
import dji.v5.utils.common.LogUtils
import kotlinx.serialization.json.*
import org.ros.node.ConnectedNode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


data class DroneResult(val success: Boolean, val message: String)

class DroneServiceUtils(connectedNode: ConnectedNode) {
    /**
     * Block until startLanding completes or times out.
     * @param controller  the aircraftController you’re using
     * @param timeoutSec  how many seconds to wait before giving up
     */
    val resolver = connectedNode.resolver.newChild("Mavic_3M")
    val universalPublisher = connectedNode.newPublisher<std_msgs.String>(resolver.resolve("state/other"), std_msgs.String._TYPE)
    val myString: std_msgs.String = universalPublisher.newMessage()

    val batteryKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val gimbalKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val cameraKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val flightControlKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> =
        ArrayList()
    val airlinkKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val remoteControllerKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> =
        ArrayList()

    var currentKeyItem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>? = null

    val currentKeyItemList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()




    private val keyItemOperateCallBack: dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<Any> =
        dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<Any> { t ->
            //showToast("$t")
            myString.data = t.toString()
            universalPublisher.publish(myString)
        }







    private fun getComponentIndex(componentName: String): Int {
        return when (componentName) {
            ComponentIndexType.LEFT_OR_MAIN.name -> ComponentIndexType.LEFT_OR_MAIN.value()
            ComponentIndexType.RIGHT.name -> ComponentIndexType.RIGHT.value()
            ComponentIndexType.UP.name -> ComponentIndexType.UP.value()
            ComponentIndexType.AGGREGATION.name -> ComponentIndexType.AGGREGATION.value()
            ComponentIndexType.UP_TYPE_C.name -> ComponentIndexType.UP_TYPE_C.value()
            ComponentIndexType.UP_TYPE_C_EXT_ONE.name -> ComponentIndexType.UP_TYPE_C_EXT_ONE.value()

            else -> {
                ComponentIndexType.UNKNOWN.value()
            }
        }
    }

    private fun getCameraSubIndex(lensName: String): Int {
        CameraLensType.entries.forEach {
            if (lensName == it.name) {
                return it.value()
            }
        }
        return CameraLensType.UNKNOWN.value()
    }


    fun callAPI(
        operation: String,
        channelType: String,
        keyItem: String,
        params: String
    ): DroneResult {

        var result: DroneResult
        var message = ""
        try {
            initializeKeyLists()
            currentKeyItemList.clear()
            initCurrentKeyItemList(channelType)
            if (currentKeyItemList.isEmpty()) {
                result = DroneResult(false, "no key list, call pullKeyList first")
                return result
            }
            currentKeyItem = currentKeyItemList.firstOrNull { it.name == keyItem }
            if (currentKeyItem != null) {
                val index = getComponentIndex("LEFT_OR_MAIN")
                currentKeyItem!!.componetIndex = index
                CapabilityManager.getInstance().setComponetIndex(index)

                currentKeyItem!!.subComponetType = 65534

//                    if (!item.canGet()) {
//                        showToast("not support get")
//                        message += ", not support get"
//
//                    }

                val componentIndexType = ComponentIndexType.LEFT_OR_MAIN.value()
                val theKey = KeyTools.createKey(currentKeyItem!!.keyInfo, componentIndexType)
                currentKeyItem!!.setKeyOperateCallBack(keyItemOperateCallBack)

                when (operation) {
                    "get" -> {
                        val myResult = KeyManager.getInstance().getValue(theKey)

                        val keyParamElement = Json.parseToJsonElement(Gson().toJson(myResult))
                        val json = buildJsonObject {
                            put("operation", operation)
                            put("channel", channelType)
                            put("key", keyItem)
                            put("param", keyParamElement)
                        }

                        myString.data = json.toString()
                        universalPublisher.publish(myString)

                    }
                    "set" -> {
//                        //logging the subItemMap
//                        currentKeyItem!!.subItemMap.forEach { (key, enumList) ->
//                            Log.d(TAG, "Key: $key")
//                            enumList.forEach { enumItem ->
//                                Log.d(TAG, enumItem.hasName("YAW_FOLLOW").toString() +": "+enumItem.getName().toString())
//                            }
//                        }
//                        Log.d(TAG,  Gson().toJson(currentKeyItem!!.subItemMap).toString())
                        val (_, enumList) = currentKeyItem!!.subItemMap.entries.first()
                        val valueIndex = enumList.indexOfFirst { it.getName() == params }
                        Log.d(TAG, "Key: $valueIndex")
                        currentKeyItem!!.doSet("{\"value\":$valueIndex}")



                    }
                    "listen" -> {

                    }
                    "action" -> {
                        //Log.d(TAG, currentKeyItem!!.paramJsonStr)
                        currentKeyItem!!.doAction(params)
                    }
                    else -> {message = "no such operation"}
                }
                message = "performed $operation, $message"
            } else {
                message = "no such key in current key list"
            }


            result = DroneResult(true, message)
        } catch (e: RuntimeException) {
            Log.e(TAG, "error", e)
            result = DroneResult(false, e.message.toString())
        }
        return result
    }



    fun pullChannelList(

    ): DroneResult {

        val channelList = arrayOf(
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_BATTERY,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_AIRLINK,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_GIMBAL,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL
        )
        val result = DroneResult(true, channelList.contentToString())
        return result
    }


    fun pullKeyList(
        channelType: String
    ): DroneResult {
        var result: DroneResult
        try {
            initializeKeyLists()
            currentKeyItemList.clear()
            initCurrentKeyItemList(channelType)

            result = DroneResult(true, currentKeyItemList.toString())

        } catch (e: RuntimeException) {
            Log.e(TAG, "error", e)
            result = DroneResult(false, e.message.toString())
        }
        return result
    }




    fun pullKeyInfo(
        operation: String,
        channelType: String,
        keyItem: String
    ): DroneResult {

        var result: DroneResult
        var message = ""




        try {
            initializeKeyLists()
            currentKeyItemList.clear()
            initCurrentKeyItemList(channelType)
            if (currentKeyItemList.isEmpty()) {
                result = DroneResult(false, "no key list, call pullKeyList first")
                return result
            }

            for (item in currentKeyItemList) {
                if (keyItem == item.name) {
                    //message = "found key"
                    if (!item.canGet()) {
                        showToast("not support get")
                        message += ", not support get"

                    } else {
                        val theKey = KeyTools.createKey(item.keyInfo)

                        KeyManager.getInstance().listen(
                            theKey, this
                        ) { _, newValue ->
                            val myString: std_msgs.String = universalPublisher.newMessage()

                            val keyParamElement = Json.parseToJsonElement(Gson().toJson(newValue))
                            val json = buildJsonObject {
                                put("operation", operation)
                                put("channel", channelType)
                                put("key", keyItem)
                                put("param", keyParamElement)
                            }

                            myString.data = json.toString()
                            universalPublisher.publish(myString)
                            Log.e(TAG, myString.data)
                        }
                        //message += keyItem

                        // get operation
                        // "${KeyManager.getInstance().getValue(theKey)}"
                    }


                    val keyParamElement = Json.parseToJsonElement(Gson().toJson(item.param))
                    val myJsonMessage = buildJsonObject {
                        put("keyName", keyItem)
                        put("keyInfo", item.keyInfo.toString())
                        put("keyParam", keyParamElement)

                    }


                    message = myJsonMessage.toString()
                    break
                } else {
                    message = "no such key in current key list"
                }
            }
            result = DroneResult(true, message)
        } catch (e: RuntimeException) {
            Log.e(TAG, "error", e)
            result = DroneResult(false, e.message.toString())
        }
        return result
    }


    //private val gimbalParam = GimbalAngleRotation()

    fun landing(
        controller: AircraftControl,
        timeoutSec: Long = 10
    ): DroneResult {
        val latch = CountDownLatch(1)
        // this will be updated by the callbacks:
        var result = DroneResult(false, "no result")

        controller.startLanding(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
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

        controller.startTakeOff(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
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

        controller.setGimbal(
            gimbalParam,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
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



    private fun initializeKeyLists()
    {
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initBatteryKeyList(batteryKeyList)
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initGimbalKeyList(gimbalKeyList)
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initCameraKeyList(cameraKeyList)
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initFlightControllerKeyList(flightControlKeyList)
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initAirlinkKeyList(airlinkKeyList)
        dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initRemoteControllerKeyList(remoteControllerKeyList)
    }

    private fun initCurrentKeyItemList(channelType: String)
    {
        val currentChannelType =
            dji.sampleV5.aircraft.keyvalue.ChannelType.valueOf("CHANNEL_TYPE_$channelType")
        when (currentChannelType) {
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_BATTERY -> {
                currentKeyItemList.addAll(batteryKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_GIMBAL -> {
                currentKeyItemList.addAll(gimbalKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA -> {
                currentKeyItemList.addAll(cameraKeyList)
            }


            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL -> {
                currentKeyItemList.addAll(flightControlKeyList)
            }


            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_AIRLINK -> {
                currentKeyItemList.addAll(airlinkKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                currentKeyItemList.addAll(remoteControllerKeyList)
            }

            else -> {
                LogUtils.d(TAG, "nothing to do")
            }
        }
    }
}
