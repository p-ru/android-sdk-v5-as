package dji.sampleV5.aircraft

import android.util.Log
import dji.sampleV5.aircraft.models.TAG
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import dji.sampleV5.aircraft.rosutils.DroneServiceUtils
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.manager.KeyManager


class Server : AbstractNodeMain() {

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

    private val gimbalParam = GimbalAngleRotation().apply {
        pitch = 0.0
        roll = 0.0
        yaw = 0.0
        mode = GimbalAngleRotationMode.ABSOLUTE_ANGLE
        pitchIgnored = false
        rollIgnored = false
        yawIgnored = false
        duration = 0.0
        jointReferenceUsed = false
        timeout = 0
    }

    override fun getDefaultNodeName(): GraphName {
        return GraphName.of("MSDK_App/service_server")
    }

    override fun onStart(connectedNode: ConnectedNode) {
        val resolver = connectedNode.resolver.newChild("Mavic_3M")
        val droneServiceUtils = DroneServiceUtils(connectedNode)
        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>(resolver.resolve("takeoff"), std_srvs.Trigger._TYPE) {
            _, response ->

            //val tree: ParameterTree = connectedNode.parameterTree
            //tree.set("test", 0)

            val result = droneServiceUtils.takeoff(aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "takeoff response: $response")
        }


        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>(resolver.resolve("landing"), std_srvs.Trigger._TYPE) {
            _, response ->
            val result = droneServiceUtils.landing(aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "landing response: $response")
        }

        connectedNode.newServiceServer<dji_srvs.SetValueRequest, dji_srvs.SetValueResponse>(resolver.resolve("set_gimbal_pitch"), dji_srvs.SetValue._TYPE) {
            request, response ->
            val service = "set_gimbal_pitch"
            val value = request.value.toDouble()
            Log.d(TAG, "received: $value")
            gimbalParam.duration = 0.0
            gimbalParam.pitch = value
            gimbalParam.yawIgnored = true
            val result = droneServiceUtils.setGimbal(gimbalParam, aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "$service: $response")
        }

        connectedNode.newServiceServer<dji_srvs.SetValueRequest, dji_srvs.SetValueResponse>(resolver.resolve("set_gimbal_yaw"), dji_srvs.SetValue._TYPE) {
                request, response ->
            val service = "set_gimbal_yaw"
            val value = request.value.toDouble()
            Log.d(TAG, "received: $value")
            gimbalParam.yawIgnored = false
            gimbalParam.yaw = value
            gimbalParam.duration = 1.0
            val result = droneServiceUtils.setGimbal(gimbalParam, aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "$service: $response")
        }





        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>(resolver.resolve("pull_channel_list"), std_srvs.Trigger._TYPE) {
                _, response ->
            val service = "pull_channel_list"
            Log.d(TAG, "received: $service command")


            val result = droneServiceUtils.pullChannelList()
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "$service: $response")
        }



        connectedNode.newServiceServer<dji_srvs.SetStringRequest, dji_srvs.SetStringResponse>(resolver.resolve("pull_key_list"), dji_srvs.SetString._TYPE) {
                request, response ->
            val service = "pull_key_list"
            Log.d(TAG, "received: $service command")
            val result = droneServiceUtils.pullKeyList(request.value)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "$service: ${response.message}")
        }


        connectedNode.newServiceServer<dji_srvs.SetStringRequest, dji_srvs.SetStringResponse>(resolver.resolve("pull_key_info"), dji_srvs.SetString._TYPE) {
                request, response ->
            val service = "pull_key_info"
            Log.d(TAG, "received: $service command")


            val (channelType, keyItem) = request.value.split('.', limit = 2)

            val result = droneServiceUtils.pullKeyInfo("listen", channelType, keyItem)

            response.success = result.success
            response.message = result.message


            Log.d(TAG, "$service: $response")
        }




        connectedNode.newServiceServer<dji_srvs.SetStringRequest, dji_srvs.SetStringResponse>(resolver.resolve("call_API"), dji_srvs.SetString._TYPE) {
                request, response ->
            val service = "call_API"
            Log.d(TAG, "received: $service command")


            val (operation, channelType, keyItem, params) = request.value.split('.', limit = 4)

            val result = droneServiceUtils.callAPI(operation, channelType, keyItem, params)

            response.success = result.success
            response.message = result.message


            Log.d(TAG, "$service: $response")
        }





//        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>("listen_velocity", std_srvs.Trigger._TYPE) {
//                _, response ->
//            val service = "listen_velocity"
//            Log.d(TAG, "received: $service command")
//
//
//            val result = DroneServiceUtils.pullKeyInfo(request.value)
//
//            response.success = result.success
//            response.message = result.message
//            Log.d(TAG, "$service: $response")
//        }
//
//        val velocityKey = KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity)
//        KeyManager.getInstance().listen(
//            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), this
//        ) { _, newValue ->
//            if (newValue != null) {
//                Log.d(TAG, "velocity reading received")
//                val vectorStampedMsg: geometry_msgs.Vector3Stamped = velocityPublisher.newMessage() // Create Vector3Stamped message
//
//                // Populate the header
//                vectorStampedMsg.header.stamp = connectedNode.currentTime // Set current ROS time as timestamp
//                vectorStampedMsg.header.frameId = "drone_frame" // You can set a frame_id if relevant, e.g., "drone_frame", or leave it empty ""
//
//                // Populate the vector3 part with velocity data
//                vectorStampedMsg.vector.x = newValue.x.toDouble() // Convert Float to Double if needed for Vector3
//                vectorStampedMsg.vector.y = newValue.y.toDouble()
//                vectorStampedMsg.vector.z = newValue.z.toDouble()
//
//                velocityPublisher.publish(vectorStampedMsg)
//                /*
//                if (DJIApplication.isCameraStreamRunning) {
//                    velocityPublisher.publish(vectorStampedMsg)}
//                */
//            }
//        }

    }
}