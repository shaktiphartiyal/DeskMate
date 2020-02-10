package hkapp.hk009.com.deskmate;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.UnsupportedEncodingException;

public class DeskMateNotificationListener extends NotificationListenerService
{

    /*
    These are the package names of the apps. for which we want to
    listen the notifications
    */
    private static final class ApplicationPackageNames {
        public static final String FACEBOOK_PACK_NAME = "com.facebook.katana";
        public static final String FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca";
        public static final String WHATSAPP_PACK_NAME = "com.whatsapp";
        public static final String WHATSAPP_BIZ_PACK_NAME = "com.whatsapp.w4b";
        public static final String INSTAGRAM_PACK_NAME = "com.instagram.android";
        public static final String SMS_PACK_NAME = "com.google.android.apps.messaging";
    }

    /*
    These are the return codes we use in the method which intercepts
    the notifications, to decide whether we should do something or not
 */
    public static final class InterceptedNotificationCode {
        public static final int FACEBOOK_CODE = 1;
        public static final int WHATSAPP_CODE = 2;
        public static final int INSTAGRAM_CODE = 3;
        public static final int SMS_CODE = 4;
        public static final int OTHER_NOTIFICATIONS_CODE =5; // We ignore all notification with this code
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);
        if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE)
        {
            Notification mNotification=sbn.getNotification();
            Bundle extras = mNotification.extras;
            String title = extras.getString("android.title", "");
            String message = extras.getString("android.text", "");
            String packageName = getPackageName(notificationCode);
            publishToMqtt(packageName, title, message);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);

        if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            StatusBarNotification[] activeNotifications = this.getActiveNotifications();

            if(activeNotifications != null && activeNotifications.length > 0) {
                for (int i = 0; i < activeNotifications.length; i++) {
                    if (notificationCode == matchNotificationCode(activeNotifications[i]))
                    {
                        Log.d("Notification Removed !", "some notification data");
                        break;
                    }
                }
            }
        }
    }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if(packageName.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME)
                || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)){
            return(InterceptedNotificationCode.FACEBOOK_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)){
            return(InterceptedNotificationCode.INSTAGRAM_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME) || packageName.equals(ApplicationPackageNames.WHATSAPP_BIZ_PACK_NAME)){
            return(InterceptedNotificationCode.WHATSAPP_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.SMS_PACK_NAME)){
            return(InterceptedNotificationCode.SMS_CODE);
        }
        else{
            return(InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }

    private String getPackageName(int code)
    {
        String name = "Android";
        if(code == 1)
        {
            name = "FaceBook";
        }
        else if(code == 2)
        {
            name = "WhatsApp";
        }
        else if(code == 3)
        {
            name = "Instagram";
        }
        else if(code == 4)
        {
            name = "SMS";
        }
        else if(code == 5)
        {
            name = "Android";
        }
        return name;
    }



    private void publishToMqtt(final String packageName, final String title, final String message)
    {
        final String finalMessage = "{\"p\":\""+packageName+"\", \"t\":\""+title+"\",\"m\":\""+message+"\"}";
        String mqttServerUrl = SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_SERVER", "");
        String mqttPort = SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PORT", "");
        if(mqttServerUrl.equals("") || mqttPort.equals(""))
        {
            Log.d("DESKMATE", "NO MQTT SETTINGS");
            return;
        }
        String connectionString = "tcp://"+mqttServerUrl+":"+mqttPort;
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), connectionString,
                        clientId);

        try
        {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_USER", ""));
            options.setPassword(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PASSWORD", "").toCharArray());
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("DESKMATE", "onSuccess");
                    String topic = SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PUB_TOPIC", "");
                    byte[] encodedPayload = new byte[0];
                    try
                    {
                        encodedPayload = finalMessage.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        client.disconnect();
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("DESKMATE", "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
