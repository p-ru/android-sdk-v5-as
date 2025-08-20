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
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode



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
        return GraphName.of("DJI/service_server")
    }

    override fun onStart(connectedNode: ConnectedNode) {
        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>("takeoff", std_srvs.Trigger._TYPE) {
            _, response ->

            //val tree: ParameterTree = connectedNode.parameterTree
            //tree.set("test", 0)

            val result = DroneServiceUtils.takeoff(aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "takeoff response: $response")
        }


        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>("landing", std_srvs.Trigger._TYPE) {
            _, response ->
            val result = DroneServiceUtils.landing(aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "landing response: $response")
        }

        connectedNode.newServiceServer<dji_srvs.SetValueRequest, dji_srvs.SetValueResponse>("set_gimbal_pitch", dji_srvs.SetValue._TYPE) {
            request, response ->

            val value = request.value.toDouble()
            Log.d(TAG, "received: $value")
            gimbalParam.duration = 0.0
            gimbalParam.pitch = value
            gimbalParam.yawIgnored = true
            val result = DroneServiceUtils.setGimbal(gimbalParam, aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "set_gimbal response: $response")
        }

        connectedNode.newServiceServer<dji_srvs.SetValueRequest, dji_srvs.SetValueResponse>("set_gimbal_yaw", dji_srvs.SetValue._TYPE) {
                request, response ->

            val value = request.value.toDouble()
            Log.d(TAG, "received: $value")
            gimbalParam.yawIgnored = false
            gimbalParam.yaw = value
            gimbalParam.duration = 1.0
            val result = DroneServiceUtils.setGimbal(gimbalParam, aircraftController)
            response.success = result.success
            response.message = result.message
            Log.d(TAG, "set_gimbal response: $response")
        }
    }
}