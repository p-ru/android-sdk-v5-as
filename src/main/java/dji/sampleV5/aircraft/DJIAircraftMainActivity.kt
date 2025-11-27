package dji.sampleV5.aircraft

import android.util.Log
import dji.sampleV5.aircraft.models.TAG
import dji.v5.common.utils.GeoidManager
import dji.v5.ux.core.communication.DefaultGlobalPreferences
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.util.UxSharedPreferencesUtil
import dji.v5.ux.sample.showcase.defaultlayout.DefaultLayoutActivity
import dji.v5.ux.sample.showcase.widgetlist.WidgetsActivity

import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor



//import org.ros.rosjava_tutorial_pubsub.Listener
import java.net.URI
/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
//http://192.168.43.76:11311
//http://192.168.3.139:11311
class DJIAircraftMainActivity : RosActivity("Pubsub Tutorial", "Pubsub Tutorial",  URI.create("http://192.168.43.76:11311")) {
    private var talker: Talker? = null
    private var listener: Listener? = null
    private var server: Server? = null
    override fun prepareUxActivity() {
        UxSharedPreferencesUtil.initialize(this)
        GlobalPreferencesManager.initialize(DefaultGlobalPreferences(this))
        GeoidManager.getInstance().init(this)

        enableDefaultLayout(DefaultLayoutActivity::class.java)
        enableWidgetList(WidgetsActivity::class.java)
    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(AircraftTestingToolsActivity::class.java)
    }

    override fun init(nodeMainExecutor: NodeMainExecutor?) {
        //nodeMainExecutorService.rosHostname = "192.168.43.129"
        talker = Talker()
        listener = Listener()
        server = Server()


        if(masterUri.toString() == "http://192.168.43.76:11311"){nodeMainExecutorService.rosHostname = "192.168.43.129"}
        val nodeConfiguration = NodeConfiguration.newPublic(rosHostname)

        nodeConfiguration.setMasterUri(masterUri)

        nodeMainExecutor?.execute(listener, nodeConfiguration)
        nodeMainExecutor?.execute(talker, nodeConfiguration)
        nodeMainExecutor?.execute(server, nodeConfiguration)
    }
    override fun onDestroy() {
        // Shutdown the NodeMainExecutorService when the Activity is destroyed
        nodeMainExecutorService.shutdown()
        super.onDestroy()
    }
}