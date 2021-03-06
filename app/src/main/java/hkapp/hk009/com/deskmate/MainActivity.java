package hkapp.hk009.com.deskmate;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    public boolean settingsSaved = false;
    private TextView displayArea;
    private EditText mqttServer;
    private EditText mqttUser;
    private EditText mqttPassword;
    private EditText mqttPort;
    private EditText mqttPubTopic;
    private Button saveSettingsBtn;
    private CheckBox passwordShowCheck;
    private CheckBox hideContentCheck;
    private CheckBox showOTPCheck;
    private EditText displayDuration;





    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadControls();
        checkForSavedSettings();

        boolean isNotificationServiceRunning = isNotificationServiceRunning();
        if(!isNotificationServiceRunning){
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
        startTheService();
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        startTheService();
    }

    /**
     * @author Shakti Phartiyal
     * @desc Method to check if the service is running or else start it
     */
    public void startTheService()
    {
        if(checkServiceRunning(DeskMateNotificationListener.class) == false)
        {
            startService(new Intent(getApplicationContext(), DeskMateNotificationListener.class)); //this may crash the app
        }
    }

    /**
     * @author Shakti Phartiyal
     * @desc Check if notification listener service is running
     */
    public boolean checkServiceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * @author Shakti Phartiyal
     * @desc initialize controls
     */
    private void loadControls()
    {
        displayArea = (TextView) findViewById(R.id.displayArea);
        mqttServer = (EditText) findViewById(R.id.mqttServer);
        mqttUser = (EditText) findViewById(R.id.mqttUser);
        mqttPassword = (EditText) findViewById(R.id.mqttPassword);
        mqttPort = (EditText) findViewById(R.id.mqttPort);
        mqttPubTopic = (EditText) findViewById(R.id.mqttPubTopic);
        saveSettingsBtn = (Button) findViewById(R.id.saveSettingBtn);
        passwordShowCheck = (CheckBox) findViewById(R.id.passwordShowCheck);
        hideContentCheck = (CheckBox) findViewById(R.id.hideContentCheck);
        showOTPCheck = (CheckBox) findViewById(R.id.showOTPCheck);
        displayDuration = (EditText) findViewById(R.id.displayDuration);
        saveSettingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveConfiguration();
            }
        });
        passwordShowCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(((CompoundButton) view).isChecked())
                {
                    mqttPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                else
                {
                    mqttPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });
    }


    /**
     * @author Shakti Phartiyal
     * @desc check if settings are stored in shared preferences
     */
    private void checkForSavedSettings()
    {
        settingsSaved = SharedPreferenceHelper.getSharedPreferenceBoolean(this, "SETTINGS_SAVED", false);
        if(settingsSaved == true)
        {
            displayArea.setTextColor(Color.GREEN);
            displayArea.setText("Settings configured");
            populateSavedSettings();
        }
        else
        {
            displayArea.setTextColor(Color.RED);
            displayArea.setText("Settings not configured");
        }
    }


    /**
     * @author Shakti Phartiyal
     * @desc Method to save settings in shared preferences after validation
     */
    private void saveConfiguration()
    {
        String inputMqttServer = mqttServer.getText().toString();
        String inputMqttUser = mqttUser.getText().toString();
        String inputMqttPassword = mqttPassword.getText().toString();
        String inputMqttPort = mqttPort.getText().toString();
        String inputMqttPubTopic = mqttPubTopic.getText().toString();
        boolean hideContent = hideContentCheck.isChecked();
        boolean showOtp = showOTPCheck.isChecked();
        String displayDurationSeconds = displayDuration.getText().toString();

        if(inputMqttServer.equalsIgnoreCase(""))
        {
            Toast.makeText(getApplicationContext(),"Invalid server",Toast.LENGTH_LONG).show();
            return;
        }
        if(inputMqttUser.equalsIgnoreCase(""))
        {
            Toast.makeText(getApplicationContext(),"Invalid user",Toast.LENGTH_LONG).show();
            return;
        }
        if(inputMqttPassword.equalsIgnoreCase(""))
        {
            Toast.makeText(getApplicationContext(),"Invalid password",Toast.LENGTH_LONG).show();
            return;
        }
        if(inputMqttPort.equalsIgnoreCase(""))
        {
            Toast.makeText(getApplicationContext(),"Invalid port",Toast.LENGTH_LONG).show();
            return;
        }
        if(inputMqttPubTopic.equalsIgnoreCase(""))
        {
            Toast.makeText(getApplicationContext(),"Invalid topic",Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "MQTT_SERVER", inputMqttServer);
        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "MQTT_USER", inputMqttUser);
        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "MQTT_PASSWORD", inputMqttPassword);
        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "MQTT_PORT", inputMqttPort);
        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "MQTT_PUB_TOPIC", inputMqttPubTopic);
        SharedPreferenceHelper.setSharedPreferenceBoolean(getApplicationContext(), "HIDE_CONTENT", hideContent);
        SharedPreferenceHelper.setSharedPreferenceBoolean(getApplicationContext(), "SHOW_OTP", showOtp);
        SharedPreferenceHelper.setSharedPreferenceString(getApplicationContext(), "DISPLAY_DURATION", displayDurationSeconds);
        SharedPreferenceHelper.setSharedPreferenceBoolean(getApplicationContext(), "SETTINGS_SAVED", true);
        checkForSavedSettings();
        if(settingsSaved)
        {
            Toast.makeText(getApplicationContext(),"Settings saved",Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(),"Unable to save settings",Toast.LENGTH_LONG).show();
        }
    }


    /**
     * @author Shakti Phartiyal
     * @desc Method to pre populate saved settings in form
     */
    private void populateSavedSettings()
    {
        mqttServer.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_SERVER", ""));
        mqttUser.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_USER", ""));
        mqttPassword.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PASSWORD", ""));
        mqttPort.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PORT", ""));
        mqttPubTopic.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "MQTT_PUB_TOPIC", ""));
        hideContentCheck.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(getApplicationContext(), "HIDE_CONTENT", false));
        showOTPCheck.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(getApplicationContext(), "SHOW_OTP", false));
        displayDuration.setText(SharedPreferenceHelper.getSharedPreferenceString(getApplicationContext(), "DISPLAY_DURATION", "5"));
    }


    /**
     * @author Shakti Phartiyal
     * @desc method to check if notifiation permission is granted
     */
    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }
}
