package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import pl.sviete.dom.data.DomCustomRequest;

import static pl.sviete.dom.AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT;
import static pl.sviete.dom.AisCoreUtils.BROADCAST_SAY_IT_TEXT;

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
        // do the simple HTTP post
        try {
            message.put("ais_ha_webhook_id", AisCoreUtils.AIS_HA_WEBHOOK_ID);
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

    public static void publishMessage(String message, String topicPostfix, Context context){
        // publish via http rest to local instance
        if (AisCoreUtils.AIS_GATE_ID == null) {
            try {
                AisCoreUtils.AIS_GATE_ID = "dom-" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            catch (Exception e) {
                Log.e("publishMessage", e.toString());
            }
        }

        JSONObject json = new JSONObject();
        try {
            json.put("topic", "ais/" + topicPostfix);
            json.put("ais_gate_client_id", AisCoreUtils.AIS_GATE_ID);
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
            json.put("ais_gate_client_id", AisCoreUtils.AIS_GATE_ID);
            json.put("payload", message);
        } catch (JSONException e) {
            Log.e("publishJson", e.toString());
        }

        doPost(json, context);
    }

}
