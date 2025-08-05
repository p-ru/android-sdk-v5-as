//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler) and then modified to support Android 8.0+
//

package org.ros.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Build; // CHANGE 1: Import android.os.Build for version checks
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat; // CHANGE 2: Use NotificationCompat for better compatibility

import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.ros.RosCore;
import org.ros.concurrent.ListenerGroup;
import org.ros.exception.RosRuntimeException;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import dji.sampleV5.aircraft.R; // CHANGE 3: Import your app's R file

public class NodeMainExecutorService extends Service implements NodeMainExecutor {
    private static final String TAG = "NodeMainExecutorService";
    // CHANGE 4: Use a more descriptive name and a standard notification ID
    private static final int ONGOING_NOTIFICATION_ID = 1;
    public static final String ACTION_START = "org.ros.android.ACTION_START_NODE_RUNNER_SERVICE";
    public static final String ACTION_SHUTDOWN = "org.ros.android.ACTION_SHUTDOWN_NODE_RUNNER_SERVICE";
    public static final String EXTRA_NOTIFICATION_TITLE = "org.ros.android.EXTRA_NOTIFICATION_TITLE";
    public static final String EXTRA_NOTIFICATION_TICKER = "org.ros.android.EXTRA_NOTIFICATION_TICKER";
    public static final String NOTIFICATION_CHANNEL_ID = "org.ros.android.RosServiceChannel";
    public static final String CHANNEL_NAME = "ROS Android Background Service";
    private final NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    private final IBinder binder = new LocalBinder();
    private final ListenerGroup<NodeMainExecutorServiceListener> listeners;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private RosCore rosCore;
    private URI masterUri;
    private String rosHostname = null;

    public NodeMainExecutorService() {
        this.listeners = new ListenerGroup(this.nodeMainExecutor.getScheduledExecutorService());
    }

    public void onCreate() {
        super.onCreate(); // It's good practice to call super.onCreate() first
        this.handler = new Handler();
        PowerManager powerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(1, "NodeMainExecutorService:NodeMainExecutorService");
        this.wakeLock.acquire();
        int wifiLockType = WifiManager.WIFI_MODE_FULL;

        try {
            wifiLockType = WifiManager.class.getField("WIFI_MODE_FULL_HIGH_PERF").getInt((Object)null);
        } catch (Exception var4) {
            Log.w("NodeMainExecutorService", "Unable to acquire high performance wifi lock.");
        }

        WifiManager wifiManager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wifiManager.createWifiLock(wifiLockType, "NodeMainExecutorService");
        this.wifiLock.acquire();
    }

    // --- No changes needed in these methods ---
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration, Collection<NodeListener> nodeListeneners) {
        this.nodeMainExecutor.execute(nodeMain, nodeConfiguration, nodeListeneners);
    }
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
        this.execute(nodeMain, nodeConfiguration, (Collection)null);
    }
    public ScheduledExecutorService getScheduledExecutorService() {
        return this.nodeMainExecutor.getScheduledExecutorService();
    }
    public void shutdownNodeMain(NodeMain nodeMain) {
        this.nodeMainExecutor.shutdownNodeMain(nodeMain);
    }
    // --- End of unchanged methods ---

    public void shutdown() {
        // CHANGE 5: Removed the AlertDialog part as it's problematic for services
        // and can cause crashes on modern Android. Direct shutdown is safer.
        forceShutdown();
    }

    public void forceShutdown() {
        this.signalOnShutdown();
        this.stopSelf(); // stopSelf will eventually call onDestroy
    }

    public void addListener(NodeMainExecutorServiceListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(NodeMainExecutorServiceListener listener) {
        this.listeners.remove(listener);
    }

    private void signalOnShutdown() {
        this.listeners.signal(listener -> listener.onShutdown(NodeMainExecutorService.this));
    }

    public void onDestroy() {
        this.toast("Shutting down...");
        this.stopForeground(true); // Ensure the foreground service is stopped
        this.nodeMainExecutor.shutdown();
        if (this.rosCore != null) {
            this.rosCore.shutdown();
        }

        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }

        if (this.wifiLock.isHeld()) {
            this.wifiLock.release();
        }

        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return Service.START_NOT_STICKY;
        } else {
            if (intent.getAction().equals(ACTION_START)) {
                Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TICKER));
                Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TITLE));

                // CHANGE 6: The core logic for foreground services
                createNotificationChannel(); // Ensure the channel exists

                Intent notificationIntent = new Intent(this, NodeMainExecutorService.class);
                notificationIntent.setAction(ACTION_SHUTDOWN);
                PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                Notification notification = this.buildNotification(intent, pendingIntent);
                this.startForeground(ONGOING_NOTIFICATION_ID, notification);

            } else if (intent.getAction().equals(ACTION_SHUTDOWN)) {
                this.shutdown();
            }

            return Service.START_NOT_STICKY;
        }
    }

    // --- No changes needed in the middle section ---
    public IBinder onBind(Intent intent) {
        return this.binder;
    }
    public URI getMasterUri() {
        return this.masterUri;
    }
    public void setMasterUri(URI uri) {
        this.masterUri = uri;
    }
    public void setRosHostname(String hostname) {
        this.rosHostname = hostname;
    }
    public String getRosHostname() {
        return this.rosHostname;
    }
    @Deprecated
    public void startMaster() {
        this.startMaster(true);
    }
    public void startMaster(boolean isPrivate) {
        AsyncTask<Boolean, Void, URI> task = new AsyncTask<Boolean, Void, URI>() {
            protected URI doInBackground(Boolean[] params) {
                NodeMainExecutorService.this.startMasterBlocking(params[0]);
                return NodeMainExecutorService.this.getMasterUri();
            }
        };
        task.execute(new Boolean[]{isPrivate});

        try {
            task.get();
        } catch (InterruptedException var4) {
            throw new RosRuntimeException(var4);
        } catch (ExecutionException var5) {
            throw new RosRuntimeException(var5);
        }
    }
    private void startMasterBlocking(boolean isPrivate) {
        if (isPrivate) {
            this.rosCore = RosCore.newPrivate();
        } else if (this.rosHostname != null) {
            this.rosCore = RosCore.newPublic(this.rosHostname, 11311);
        } else {
            this.rosCore = RosCore.newPublic(11311);
        }
        this.rosCore.start();
        try {
            this.rosCore.awaitStart();
        } catch (Exception var3) {
            throw new RosRuntimeException(var3);
        }
        this.masterUri = this.rosCore.getUri();
    }
    public void toast(final String text) {
        this.handler.post(new Runnable() {
            public void run() {
                Toast.makeText(NodeMainExecutorService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    // --- End of unchanged section ---

    // CHANGE 7: Re-writing the notification logic to be modern and clean
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/vibration
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(Intent intent, PendingIntent pendingIntent) {
        String ticker = intent.getStringExtra(EXTRA_NOTIFICATION_TICKER);
        String title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(ticker)
                .setSmallIcon(R.drawable.ic_confirm) // Use your app's icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {}
        public NodeMainExecutorService getService() {
            return NodeMainExecutorService.this;
        }
    }
}