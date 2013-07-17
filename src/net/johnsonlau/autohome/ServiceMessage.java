package net.johnsonlau.autohome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;

public class ServiceMessage extends Service {
	private static final String TAG = "AutoHome_ServiceMessage";

	private String mMqttConnectionString = "tcp://tools.johnson.uicp.net:1883";
	
	private static final String MqttServer = "tools.johnson.uicp.net";
	private static final int MqttPort = 1883;
	
	private String mUserId = "admin";
	private String mPassword = "admin";

	private MessageThread mMessageThread = null;

	private int mNotificationIcon = R.drawable.notification_icon;
	private int mNotificationID = 0;

	private Notification mNotification = null;
	private NotificationManager mNotificatioManager = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mMessageThread == null) {
			mNotification = new Notification();
			mNotification.icon = mNotificationIcon;
			mNotification.defaults = Notification.DEFAULT_SOUND;
			mNotification.flags = Notification.FLAG_AUTO_CANCEL;
			mNotificatioManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			mMessageThread = new MessageThread();
			mMessageThread.start();
		}
		return START_STICKY;
	}

	private class MessageThread extends Thread {
		public void run() {
			startMqttClient();
		}
	}

	private class MyMqttClient extends MqttClient {
		MyMqttClient(String theConnection) throws MqttException {
			super(theConnection);
		}

		@Override
		protected void publishArrived(String thisTopicName,
				byte[] thisPayload,
				int QoS,
				boolean retained) throws java.lang.Exception
		{
			String[] items = thisTopicName.split("/");

			if(items.length == 3 && (items[2].equals("online") || items[2].equals("offline"))){
				String deviceName = "";
				if(items[1].equals("000000000002")){
					deviceName = "Arduino";
				}
				else if(items[1].equals("000000000003")){
					deviceName = "Windows";
				}
				else if(items[1].equals("000000000004")){
					deviceName = "Linux";
				}
				else {
					deviceName = items[1];
				}

				String msg = deviceName + " is " + items[2];
				showNotification(msg);
			}
			else
			{
				showNotification(thisTopicName);
			}
		}
	}

	private void startMqttClient() {
		String clientId = getLocalMacAddress();
		showNotification("Client id: " + clientId);

		boolean cleanstart = true;
		short keepalive = 3600;

		try{
			MyMqttClient mqttClient = new MyMqttClient(mMqttConnectionString);
			mqttClient.connect(clientId, cleanstart, keepalive);
		} catch (MqttException e) {
			Log.i(ServiceMessage.TAG, e.getMessage());
		}
	}

	private void showNotification(String msg) {
		showNotification(msg, "message");
	}

	private void showNotification(String msg, String title) {
		try {
			if (msg != null && !"".equals(msg)) {
				PendingIntent pendingIntent = buildPendingIntent(msg);
				mNotification.tickerText = msg;
				mNotification.setLatestEventInfo(ServiceMessage.this, title, msg, pendingIntent);
				mNotificatioManager.notify(mNotificationID++, mNotification);
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
			}
		} catch (Exception e) {
			Log.i(ServiceMessage.TAG, e.getMessage());
		}
	}

	private PendingIntent buildPendingIntent(String message) {
		Intent intent = new Intent(this, AutoHome.class);
		intent.putExtra("Message", message);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pendingIntent;
	}

	public String getLocalMacAddress() {  
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
		WifiInfo info = wifi.getConnectionInfo();  
		return info.getMacAddress().replace(":", "");  
	} 

	@Override
	public void onDestroy() {
		System.exit(0);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
