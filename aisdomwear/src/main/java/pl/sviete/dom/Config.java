package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Config {
    public static Context myContext = null;
    public static Config mConfig = null;
    private final SharedPreferences sharedPreferences;
    public static final String TAG = Config.class.getName();

    public Config(Context appContext) {
        myContext = appContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        mConfig = this;
    }

    private String getStringPref(int resId, int defId) {
        final String def = myContext.getString(defId);
        final String pref = sharedPreferences.getString(myContext.getString(resId), "");
        return pref.length() == 0 ? def : pref;
    }

    private boolean getBoolPref(int resId, int defId) {
        return sharedPreferences.getBoolean(
                myContext.getString(resId),
                Boolean.valueOf(myContext.getString(defId))
        );
    }

    // GET the answer from server
    public static String getResponseFromServer(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }
        return "";
    }


    public static String[] getGateIdFromCloud(String pin) {
        // ask cloud for gate id for pin
        // https://powiedz.co/ords/dom/dom/gate_id_from_pin?pin=1234
        Log.i(TAG, "getGateIdFromCloud");
        String url = AisCoreUtils.getAisDomCloudWsUrl(true) + "gate_id_from_pin?pin=" + pin;
        String severAnswer = getResponseFromServer(url, 10000);
        if (!severAnswer.equals("")) {
            try {
                JSONObject jsonAnswer = new JSONObject(severAnswer);
                String gateID = jsonAnswer.getString("id");
                String userID = "";
                if (jsonAnswer.has("user_id")) {
                    userID = jsonAnswer.getString("user_id");
                }
                return new String[] {gateID, userID};
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new String[] {myContext.getString(R.string.app_get_gate_for_pin_error), ""};
    }

    public static class checkGateIdForPinJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            Log.i(TAG, "doInBackground");
            String pin = params[0];
            String [] cloudAnswer = getGateIdFromCloud(pin);
            String gateId = cloudAnswer[0];
            if (gateId.startsWith("dom-")) {
                mConfig.setAppLaunchUrl(gateId);
                String userId = cloudAnswer[1];
                // register device
                DomWebInterface.doWearOsDeviceRegistration(myContext, pin, userId);
                //
                Intent i = new Intent(myContext, WatchScreenActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                myContext.startActivity(i);
            } else {
                mConfig.setAppLaunchUrl("");
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(myContext, gateId, Toast.LENGTH_LONG).show());

            return gateId;

        }

        @Override
        protected void onPostExecute(String message) {
            //process message with url to go
            Log.i(TAG, "checkGateIdForPinJob onPostExecute " + message);
        }
    }

    public String getAppLaunchUrl() {
        String url;

        url = getStringPref(R.string.key_setting_app_launchurl, R.string.default_setting_app_launchurl);
        Log.i(TAG, "getAppLaunchUrl: " + url);
        // url.startsWith("dom-")
        url = "https://" + url + ".paczka.pro";
        return url;
    }

    public void setAppLaunchUrl(String gate) {
        // this is executed from PIN parring or Gate history only
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("setting_app_launchurl", gate);
        ed.apply();
    }

    public String getAisHaWebhookId(){
        return getStringPref(R.string.key_setting_ais_ha_webhook_id, R.string.default_setting_ais_ha_webhook_id);
    }

    public void setAisHaWebhookId(String webhookId){
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("ais_ha_webhook_id", webhookId);
        ed.apply();
    }

}
