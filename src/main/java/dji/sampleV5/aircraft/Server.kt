package dji.sampleV5.aircraft

import android.util.Log
import dji.sampleV5.aircraft.models.TAG
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import dji.sampleV5.aircraft.rosutils.DroneServiceUtils

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

    }
}