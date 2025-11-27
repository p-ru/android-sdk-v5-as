package dji.sampleV5.aircraft


//import org.ros.concurrent.CancellableLoop

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import dji.sampleV5.aircraft.models.TAG
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager.FrameFormat
import org.jboss.netty.buffer.ChannelBufferOutputStream
import org.ros.internal.message.MessageBuffers
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode

class Talker : AbstractNodeMain() {




    override fun getDefaultNodeName(): GraphName {
        return GraphName.of("MSDK_App/talker")
    }


    override fun onStart(connectedNode: ConnectedNode) {
        Log.d(TAG, "publisher node started and publishing")
        val resolver = connectedNode.resolver.newChild("Mavic_3M")


        val imagePublisher = connectedNode.newPublisher<sensor_msgs.CompressedImage>(resolver.resolve("image/compressed"), sensor_msgs.CompressedImage._TYPE)
        MediaDataCenter.getInstance().cameraStreamManager.addFrameListener(
            ComponentIndexType.LEFT_OR_MAIN,
            FrameFormat.YUY2
        ) { frameData, offset, length, width, height, format ->
            //Log.d(TAG, "onFrame: Offset = $offset, Width=$width, Height=$height, Format=$format, Data Length=$length, Frame Data Size=${frameData.size}")
            if (imagePublisher.numberOfSubscribers > 0)
            {
                try {
                    val image: sensor_msgs.CompressedImage = imagePublisher.newMessage()
                    val yuvImage = YuvImage(frameData, ImageFormat.YUY2, width, height, null)

                    val rect = Rect(0, 0, width, height)
                    val stream = ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())


                    image.header.stamp = connectedNode.currentTime // Set current ROS time as timestamp
                    image.header.frameId = "drone_frame"
                    image.format = "jpeg"


                    yuvImage.compressToJpeg(rect, 80, stream)
                    image.data = stream.buffer().copy()

                    stream.buffer().clear()

                    imagePublisher.publish(image)

                    //Log.d(TAG, "image publishing")
                } catch (e: Exception) {
                    Log.d(TAG, "image publishing failed")
                }
                // Because only one frame needs to be saved, you need to call removeOnFrameListener here
                // If you need to read frame data for a long time, you can choose to actually call remove OnFrameListener according to your needs
                //MediaDataCenter.getInstance().cameraStreamManager.removeFrameListener(this)
            }
        }


        // note: simPublisher should change message type
        val simPublisher = connectedNode.newPublisher<geometry_msgs.TwistStamped>(resolver.resolve("state/sim"), geometry_msgs.TwistStamped._TYPE)
        SimulatorManager.getInstance().addSimulatorStateListener { newValue ->
            // note: simPublisher should change message type
            val simMsg: geometry_msgs.TwistStamped = simPublisher.newMessage()
            simMsg.header.stamp = connectedNode.currentTime // Set current ROS time as timestamp
            simMsg.header.frameId = "drone_frame" // You can set a frame_id if relevant, e.g., "drone_frame", or leave it empty ""

            simMsg.twist.linear.x = newValue.positionX.toDouble()
            simMsg.twist.linear.y = newValue.positionY.toDouble()
            simMsg.twist.linear.z = newValue.positionZ.toDouble()

            simMsg.twist.angular.x = newValue.roll.toDouble()
            simMsg.twist.angular.y = newValue.pitch.toDouble()
            simMsg.twist.angular.z = newValue.yaw.toDouble()
            simPublisher.publish(simMsg)
        }

//        val velocityPublisher = connectedNode.newPublisher<geometry_msgs.Vector3Stamped>(resolver.resolve("state/velocity"), geometry_msgs.Vector3Stamped._TYPE)
//        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), this
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
//
//            }
//        }
//
//        val attitudePublisher = connectedNode.newPublisher<geometry_msgs.Vector3Stamped>(resolver.resolve("state/attitude"), geometry_msgs.Vector3Stamped._TYPE)
//        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), this
//        ) { _, newValue ->
//            if (newValue != null) {
//
//                val vectorStampedMsg: geometry_msgs.Vector3Stamped = attitudePublisher.newMessage() // Create Vector3Stamped message
//
//                // Populate the header
//                vectorStampedMsg.header.stamp = connectedNode.currentTime // Set current ROS time as timestamp
//                vectorStampedMsg.header.frameId = "drone_frame" // You can set a frame_id if relevant, e.g., "drone_frame", or leave it empty ""
//
//                // Populate the vector3 part with velocity data
//                vectorStampedMsg.vector.x = newValue.pitch.toDouble()
//                vectorStampedMsg.vector.y = newValue.roll.toDouble()
//                vectorStampedMsg.vector.z = newValue.yaw.toDouble()
//
//                attitudePublisher.publish(vectorStampedMsg)
//
//            }
//        }

    }

}









