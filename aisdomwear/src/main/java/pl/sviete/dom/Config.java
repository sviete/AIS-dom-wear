package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import pl.sviete.dom.connhist.AisConnectionHistJSON;

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



    public boolean canUseLocalConnection(String localIP, String gateId) {
        // no if demo gate
        if (gateId.startsWith("dom-demo")){
            return false;
        }
        // no if no connection
        ConnectivityManager connectivityManager = (ConnectivityManager) myContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (null == activeNetwork) {
            return false;
        }
        // no if no wifi connection
        if (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
            return false;
        }

        // check local IP
        String url = "http://" + localIP + ":8122";
        String severAnswer = getResponseFromServer(url, 3000);
        if (!severAnswer.equals("")) {
            try {
                JSONObject jsonAnswer = new JSONObject(severAnswer);
                String localGateID = jsonAnswer.getString("gate_id");
                if (gateId.equals(localGateID)) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public String getLocalIpFromCloud(String gateId) {
        // ask cloud for local IP
        // https://powiedz.co/ords/dom/dom/gate_ip_full_info?id=dom-aba
        AisCoreUtils.AIS_GATE_USER = "";
        AisCoreUtils.AIS_GATE_DESC = "";
        String url = AisCoreUtils.getAisDomCloudWsUrl(true) + "gate_ip_full_info?id=" + gateId;
        String severAnswer = getResponseFromServer(url, 10000);
        if (!severAnswer.equals("")) {
            try {
                JSONObject jsonAnswer = new JSONObject(severAnswer);
                String localGateIP = jsonAnswer.getString("ip");
                AisCoreUtils.AIS_GATE_USER = jsonAnswer.getString("user");
                AisCoreUtils.AIS_GATE_DESC = jsonAnswer.getString("desc");
                if (!gateId.equals("ais-dom")) {
                    return localGateIP;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    public static String getGateIdFromCloud(String pin) {
        // ask cloud for gate id for pin
        // https://powiedz.co/ords/dom/dom/gate_id_from_pin?pin=1234
        Log.i(TAG, "getGateIdFromCloud");
        String url = AisCoreUtils.getAisDomCloudWsUrl(true) + "gate_id_from_pin?pin=" + pin;
        String severAnswer = getResponseFromServer(url, 10000);
        if (!severAnswer.equals("")) {
            try {
                JSONObject jsonAnswer = new JSONObject(severAnswer);
                String gateID = jsonAnswer.getString("id");
                return gateID;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return myContext.getString(R.string.app_get_gate_for_pin_error);
    }



    private class checkConnectionUrlJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            String gateID = params[0];
            String localIpHist = params[1];
            String userHist = params[2];
            String descHist = params[3];
            String urlToGo = "";

            // Check if the local IP from history is still OK
            if (!localIpHist.equals("") && canUseLocalConnection(localIpHist, gateID)){
                    urlToGo = "http://" + localIpHist + ":8180";
                    saveConnToHistory(localIpHist, urlToGo, gateID, userHist, descHist);
                    return urlToGo;
            } else {
                    // Get the new local IP from the Cloud
                    String localIpFromCloud = getLocalIpFromCloud(gateID);
                    if (!localIpFromCloud.equals("")) {
                        // check if new local IP from cloud is now OK
                        if (canUseLocalConnection(localIpFromCloud, gateID)){
                            urlToGo = "http://" + localIpFromCloud + ":8180";
                            saveConnToHistory(localIpFromCloud, urlToGo, gateID, AisCoreUtils.AIS_GATE_USER, AisCoreUtils.AIS_GATE_DESC);
                            return urlToGo;
                        } else {
                            // try the tunnel connection
                            urlToGo = "https://" + gateID + ".paczka.pro";
                            saveConnToHistory(localIpFromCloud, urlToGo, gateID, AisCoreUtils.AIS_GATE_USER, AisCoreUtils.AIS_GATE_DESC);
                            return urlToGo;
                        }
                    } else {
                        // try tunnel
                        urlToGo = "https://" + gateID + ".paczka.pro";
                        saveConnToHistory(localIpHist, urlToGo, gateID, AisCoreUtils.AIS_GATE_USER, AisCoreUtils.AIS_GATE_DESC);
                        return urlToGo;
                    }
            }
        }

        @Override
        protected void onPostExecute(String message) {
            //process message with url to go
            if (!message.equals("")){
                AisCoreUtils.setAisDomUrl(message);
            }
        }
    }


    public static class checkGateIdForPinJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            Log.i(TAG, "doInBackground");
            String pin = params[0];
            String gateId = getGateIdFromCloud(pin);

            if (gateId.startsWith("dom-")) {
                mConfig.setAppLaunchUrl(gateId);
                // register device

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


    public void saveConnToHistory(String localIP, String url, String gate, String user, String desc) {
        try {
            JSONObject mNewConn = new JSONObject();
            mNewConn.put("gate", gate);
            mNewConn.put("url", url);
            mNewConn.put("ip", localIP);
            mNewConn.put("user", user);
            mNewConn.put("desc", desc);
            AisConnectionHistJSON.addConnection(myContext, mNewConn.toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public String getAppLaunchUrl(boolean disco) {
        String url;

        url = getStringPref(R.string.key_setting_app_launchurl, R.string.default_setting_app_launchurl);
        Log.i(TAG, "getAppLaunchUrl: " + url);

        if (url.startsWith("dom-") && disco) {
            String gateID = url;
            String localIpHist = AisConnectionHistJSON.getLocalIpForGate(myContext, gateID);
            String userHist = AisConnectionHistJSON.getUserForGate(myContext, gateID);
            String descHist = AisConnectionHistJSON.getDescForGate(myContext, gateID);
            checkConnectionUrlJob checkConnectionUrlJob = new checkConnectionUrlJob();
            checkConnectionUrlJob.execute(gateID, localIpHist, userHist, descHist);
        } else {
            // the url is set by hand, save it for interface communication with gate
            AisCoreUtils.setAisDomUrl(url);
        }
        return url;
    }


    public void setAppLaunchUrl(String gate) {
        // this is executed from PIN parring or Gate history only
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("setting_app_launchurl", gate);
        ed.apply();
    }

}
