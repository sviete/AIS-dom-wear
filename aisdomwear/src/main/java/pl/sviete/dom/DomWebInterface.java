package pl.sviete.dom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import pl.sviete.dom.data.DomCustomRequest;

import static pl.sviete.dom.AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT;
import static pl.sviete.dom.AisCoreUtils.BROADCAST_SAY_IT_TEXT;
import static pl.sviete.dom.AisCoreUtils.getBatteryPercentage;

public class DomWebInterface {
    final static String TAG = DomWebInterface.class.getName();

    public static String getDomWsUrl(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String url = sharedPreferences.getString(context.getString(R.string.key_setting_app_launchurl), "");
        if (url.startsWith("dom-")){
            return "http://" + url + ".paczka.pro";
        }
        return pl.sviete.dom.AisCoreUtils.getAisDomUrl().replaceAll("/$", "");
    }

    private static void doPost(JSONObject message, Context context){
        Log.d(TAG, "doPost " + message);
        // do the simple HTTP post
        try {
            Config config = new Config(context);
            message.put("ais_ha_webhook_id",  config.getAisHaWebhookId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String webHookUrl = getDomWsUrl(context) + "/api/webhook/aisdomprocesscommandfromframe";
        DomCustomRequest jsObjRequest = new DomCustomRequest(Request.Method.POST, webHookUrl, message.toString(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // say the response
                if (response.has("say_it")) {
                    try {
                        String text = response.getString("say_it").trim();
                        Intent intent = null;
                        intent = new Intent(BROADCAST_ACTIVITY_SAY_IT);
                        intent.putExtra(BROADCAST_SAY_IT_TEXT, text);
                        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
                        bm.sendBroadcast(intent);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
                // save the token
                if (response.has("webhook_id")) {
                    try {
                        Config config = new Config(context);
                        String webhookId = response.getString("webhook_id");
                        config.setAisHaWebhookId(webhookId);

                        // 2. Registering a sensor - battery
                        // https://developers.home-assistant.io/docs/api/native-app-integration/sensors
                        // - create url
                        String webHookUrl = getDomWsUrl(context) + "/api/webhook/" + webhookId;
                        // create body
                        JSONObject jsonSensor = new JSONObject();
                        jsonSensor.put("type", "register_sensor");
                        JSONObject jsonSensorData = new JSONObject();
                        jsonSensorData.put("device_class", "battery");
                        jsonSensorData.put("icon", "mdi:battery");
                        jsonSensorData.put("name", "battery");
                        jsonSensorData.put("state", getBatteryPercentage(context));
                        jsonSensorData.put("type", "sensor");
                        jsonSensorData.put("unique_id", "battery");
                        jsonSensorData.put("unit_of_measurement", "%");
                        jsonSensor.put("data", jsonSensorData);
                        // call
                        DomWebInterface.doPostDomWebHoockRequest(webHookUrl, jsonSensor, context.getApplicationContext());

                        // 3. Registering a sensor - geocoded_location
                        JSONObject jsonSensor2 = new JSONObject();
                        jsonSensor2.put("type", "register_sensor");
                        JSONObject jsonSensorData2 = new JSONObject();
                        jsonSensorData2.put("icon", "mdi:map");
                        jsonSensorData2.put("name", "Geocoded Location");
                        jsonSensorData2.put("type", "sensor");
                        jsonSensorData2.put("unique_id", "geocoded_location");
                        jsonSensor2.put("data", jsonSensorData2);
                        // call
                        DomWebInterface.doPostDomWebHoockRequest(webHookUrl, jsonSensor2, context.getApplicationContext());

                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.e("AIS auth: ", response.toString());
                // try to discover gate or inform about connection problem
                Config config = new Config(context.getApplicationContext());
                String appLaunchUrl = config.getAppLaunchUrl(false);
                if (appLaunchUrl.startsWith("dom-")) {
                    appLaunchUrl = config.getAppLaunchUrl(true);
                }
                return;

            }
        });

        AisCoreUtils.getRequestQueue(context.getApplicationContext()).add(jsObjRequest);
    }

    // Sending data home
    // https://developers.home-assistant.io/docs/api/native-app-integration/sending-data
    public static void doPostDomWebHoockRequest(String url, JSONObject body, Context appContext){
        DomCustomRequest jsObjRequest = new DomCustomRequest(Request.Method.POST, url, body.toString(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {Log.d("AIS auth: ", response.toString());}
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError response) {Log.e("AIS auth: ", response.toString());}
        });
        AisCoreUtils.getRequestQueue(appContext).add(jsObjRequest);
    }


    public static void publishMessage(String message, String topicPostfix, Context context){
        // publish via http rest to local instance
        if (AisCoreUtils.AIS_GATE_CLIENT_ID == null) {
            try {
                AisCoreUtils.AIS_GATE_CLIENT_ID = "dom-" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            catch (Exception e) {
                Log.e("publishMessage", e.toString());
            }
        }

        JSONObject json = new JSONObject();
        try {
            json.put("topic", "ais/" + topicPostfix);
            json.put("ais_gate_client_id", AisCoreUtils.AIS_GATE_CLIENT_ID);
            json.put("payload", message);
        } catch (JSONException e) {
            Log.e("publishMessage", e.toString());
        }

        doPost(json, context);
    }

    // new
    public static void publishJson(JSONObject message, String topic, Context context){
        JSONObject json = new JSONObject();
        try {
            json.put("topic", "ais/" + topic);
            json.put("ais_gate_client_id", AisCoreUtils.AIS_GATE_CLIENT_ID);
            json.put("payload", message);
        } catch (JSONException e) {
            Log.e("publishJson", e.toString());
        }

        doPost(json, context);
    }

    public static void doWearOsDeviceRegistration(Context context, String pin, String userId){
        try {
            // 1. get webhook - create message body
            JSONObject webHookJson = new JSONObject();
            webHookJson.put("device_id", AisCoreUtils.AIS_GATE_CLIENT_ID);
            webHookJson.put("app_id", BuildConfig.APPLICATION_ID);
            webHookJson.put("app_name", "AIS dom");
            webHookJson.put("app_version", BuildConfig.VERSION_NAME);
            webHookJson.put("device_name", "wearos_ais_" + AisCoreUtils.AIS_GATE_CLIENT_ID.toLowerCase().replace(" ", "_"));
            webHookJson.put("manufacturer", AisNetUtils.getManufacturer());
            webHookJson.put("model", AisNetUtils.getModel() + " " + AisNetUtils.getDevice() );
            webHookJson.put("os_name", "Android");
            webHookJson.put("os_version", AisNetUtils.getApiLevel() + " " + AisNetUtils.getOsVersion());
            webHookJson.put("supports_encryption", false);
            JSONObject appData = new JSONObject();
            appData.put("push_token", AisCoreUtils.AIS_PUSH_NOTIFICATION_KEY);
            appData.put("push_url", "https://powiedz.co/ords/dom/dom/send_push_data");
            Log.e(TAG, "app_data " + appData);
            Log.e(TAG, "app_data " + appData);
            webHookJson.put("app_data", appData);
            // pin or token
            webHookJson.put("ais_dom_pin", pin);
            
            // 2. call
            publishJson(webHookJson,"register_wear_os", context);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static void doSendLogToAis(Context context){
        String logPath = context.getFilesDir().getPath() + "/log.txt";
        try {
            Runtime.getRuntime().exec("logcat -f " + logPath);

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {
                                try {
                                    Toast.makeText(context, "... ->", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });

                            //Yes button clicked
                            try {
                                File fl = new File(logPath);
                                FileInputStream fin = null;
                                fin = new FileInputStream(fl);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                                StringBuilder sb = new StringBuilder();
                                String line = null;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line).append("\n");
                                }
                                reader.close();
                                fin.close();

                                String url = AisCoreUtils.getAisDomCloudWsUrl(true) + "logs?id=" + AisCoreUtils.AIS_GATE_CLIENT_ID;
                                DomCustomRequest jsObjRequest = new DomCustomRequest(Request.Method.POST, url, sb.toString(), new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        // show the info
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.post(() -> {
                                            try {
                                                Toast.makeText(context, response.getString("status"), Toast.LENGTH_LONG).show();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        return;
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError response) {
                                        Log.e("AIS auth: ", response.toString());
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.post(() -> Toast.makeText(context, response.toString(), Toast.LENGTH_LONG).show());
                                        return;
                                    }
                                });

                                AisCoreUtils.getRequestQueue(context.getApplicationContext()).add(jsObjRequest);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder popupBuilder = new AlertDialog.Builder(context);
            popupBuilder.setMessage(R.string.are_you_sure_logs_confirmation);
            popupBuilder.setPositiveButton(R.string.are_you_sure_yes, dialogClickListener);
            popupBuilder.setNegativeButton(R.string.are_you_sure_no, dialogClickListener);
            popupBuilder.show();

        } catch (IOException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    public static void updateRegistrationPushToken(Context context) {
        // TODO
        Log.e(TAG, "updateRegistrationPushToken ");
        doWearOsDeviceRegistration(context, "updateRegistrationPushToken", "updateRegistrationPushToken");
    }

    public static void updateDeviceAddress(Context context, String address) {
        Config config = new Config(context);
        String webhookId = config.getAisHaWebhookId();
        if (!webhookId.equals("")) {
            try {
                // - create url
                String webHookUrl = getDomWsUrl(context) + "/api/webhook/" + webhookId;
                // update address state - create body
                JSONObject jsonUpdate = new JSONObject();
                jsonUpdate.put("type", "update_sensor_states");
                JSONArray sensorsDataArray = new JSONArray();
                JSONObject addressData = new JSONObject();
                addressData.put("icon", "mdi:map");
                addressData.put("state", address);
                addressData.put("type", "sensor");
                addressData.put("unique_id", "geocoded_location");
                sensorsDataArray.put(addressData);
                jsonUpdate.put("data", sensorsDataArray);
                // call
                DomWebInterface.doPostDomWebHoockRequest(webHookUrl, jsonUpdate, context.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "updateDeviceAddress error: " + e.getMessage());
            }
        }
    }

    public static void updateDeviceLocation(Context context, Location location) {
        // do the simple HTTP post
        // get ha webhook id from settings
        Config config = new Config(context);
        String webhookId = config.getAisHaWebhookId();
        if (!webhookId.equals("")) {
            try {
                // - create url
                String webHookUrl = getDomWsUrl(context) + "/api/webhook/" + webhookId;
                // create body
                JSONObject json = new JSONObject();
                json.put("type", "update_location");
                JSONObject data = new JSONObject();
                JSONArray gps = new JSONArray();
                gps.put(location.getLatitude());
                gps.put(location.getLongitude());
                data.put("gps", gps);
                data.put("gps_accuracy", location.getAccuracy());
                String batteryState = getBatteryPercentage(context);
                data.put("battery", batteryState);
                data.put("speed", location.getSpeed());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    data.put("course", location.getVerticalAccuracyMeters());
                }
                json.put("data", data);

                // call
                DomWebInterface.doPostDomWebHoockRequest(webHookUrl, json, context.getApplicationContext());

                // update battery state
                // create body
                JSONObject jsonUpdate = new JSONObject();
                jsonUpdate.put("type", "update_sensor_states");
                JSONArray sensorsDataArray = new JSONArray();
                JSONObject batteryData = new JSONObject();
                batteryData.put("icon", "mdi:battery");
                batteryData.put("state", batteryState);
                batteryData.put("type", "sensor");
                batteryData.put("unique_id", "battery");
                sensorsDataArray.put(batteryData);
                jsonUpdate.put("data", sensorsDataArray);
                // call
                DomWebInterface.doPostDomWebHoockRequest(webHookUrl, jsonUpdate, context.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "updateDeviceLocation error: " + e.getMessage());
            }
        }
    }
}
