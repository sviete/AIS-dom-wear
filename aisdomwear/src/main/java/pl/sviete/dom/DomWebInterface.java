package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.JSONObjectBody;

import static pl.sviete.dom.AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT;
import static pl.sviete.dom.AisCoreUtils.BROADCAST_SAY_IT_TEXT;

public class DomWebInterface {
    final static String TAG = DomWebInterface.class.getName();

    private static void doPost(JSONObject message, Context context){
        // do the simple HTTP post
        String url =  pl.sviete.dom.AisCoreUtils.getAisDomUrl() + "/api/webhook/aisdomprocesscommandfromframe";
        AsyncHttpPost post = new AsyncHttpPost(url);
        JSONObjectBody body = new JSONObjectBody(message);
        post.addHeader("Content-Type", "application/json");
        post.setBody(body);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {

            void say(String text){
                Intent intent = null;
                intent = new Intent(BROADCAST_ACTIVITY_SAY_IT);
                intent.putExtra(BROADCAST_SAY_IT_TEXT, text);
                LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
                bm.sendBroadcast(intent);
            }

            // Callback is invoked with any exceptions/errors, and the result, if available.
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                if (e != null) {
                    // try to discover gate or inform about connection problem
                    Config config = new Config(context.getApplicationContext());
                    String appLaunchUrl = config.getAppLaunchUrl(false);
                    if (appLaunchUrl.startsWith("dom-")) {
                        // sprawdzam połączenie
                        //say("Sprawdzam połączenie.");
                        appLaunchUrl = config.getAppLaunchUrl(true);
                    }
                    return;
                }
                // say the response
                if (result.has("say_it")){
                    try {
                        String text = result.getString("say_it").trim();
                        say(text);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
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
