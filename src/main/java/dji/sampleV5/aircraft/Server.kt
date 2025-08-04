package dji.sampleV5.aircraft

import android.util.Log
import dji.sampleV5.aircraft.models.TAG
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.parameter.ParameterTree


class Server : AbstractNodeMain() {
    override fun getDefaultNodeName(): GraphName {
        return GraphName.of("DJI/service_server")
    }

    override fun onStart(connectedNode: ConnectedNode) {
        connectedNode.newServiceServer<std_srvs.TriggerRequest, std_srvs.TriggerResponse>("takeoff", std_srvs.Trigger._TYPE) {
            _, response ->
            response.success = true
            response.message = "success"
            val tree: ParameterTree = connectedNode.parameterTree
            tree.set("test", 0)
            Log.d(TAG, "take off success")
        }
    }
}