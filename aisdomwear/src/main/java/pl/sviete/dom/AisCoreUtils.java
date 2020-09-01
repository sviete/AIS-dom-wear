package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import static android.content.Context.BATTERY_SERVICE;

public class AisCoreUtils {

    // AIS_GATE ID
    public static String AIS_GATE_ID = null;
    public static String AIS_GATE_USER = "";
    public static String AIS_GATE_DESC = "";

    // STT
    public static final String BROADCAST_ON_END_SPEECH_TO_TEXT = "BROADCAST_ON_END_SPEECH_TO_TEXT";
    public static final String BROADCAST_ON_START_SPEECH_TO_TEXT = "BROADCAST_ON_START_SPEECH_TO_TEXT";
    public static final String  BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS = "BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS";
    public static final String  BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS_TEXT = "BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS_TEXT";
    public static final String BROADCAST_EVENT_ON_SPEECH_COMMAND = "BROADCAST_EVENT_ON_SPEECH_COMMAND";
    public static final String BROADCAST_EVENT_ON_SPEECH_COMMAND_TEXT = "BROADCAST_EVENT_ON_SPEECH_COMMAND_TEXT";

    // TTS
    public static final String BROADCAST_ACTIVITY_SAY_IT = "BROADCAST_ACTIVITY_SAY_IT";
    public static final String BROADCAST_SAY_IT_TEXT = "BROADCAST_SAY_IT_TEXT";
    public static String AIS_DOM_LAST_TTS  = "";

    // USB
    private static String TAG = "AisCoreUtils";
    private static String AIS_DOM_URL = "http://ais-dom.local";
    private static String AIS_DOM_CLOUD_WS_URL = "https://powiedz.co/ords/dom/dom/";
    private static String AIS_DOM_CLOUD_WS_URL_HTTP = "http://powiedz.co/ords/dom/dom/";

    // HA
    public static String AIS_PUSH_NOTIFICATION_KEY = "";

    // REQUEST
    private static RequestQueue mRequestQueue;
    public static final String  BROADCAST_ON_AIS_REQUEST = "BROADCAST_ON_AIS_REQUEST";

    // PERMISSION
    public static final int REQUEST_RECORD_PERMISSION = 100;
    public static final int REQUEST_HOT_WORD_MIC_PERMISSION = 200;
    public static final int REQUEST_LOCATION_PERMISSION = 300;

    // MESSAGE_CKICK_ACTION
    public static final String MESSAGE_CKICK_ACTION = "MESSAGE_CKICK_ACTION";


    public static String getAisDomUrl(){
        return AIS_DOM_URL;
    }

    public static String getAisDomCloudWsUrl(boolean http){
        if (http){
            return AIS_DOM_CLOUD_WS_URL_HTTP;
        }
        return AIS_DOM_CLOUD_WS_URL;
    }

    public static void setAisDomUrl(String url){
         AIS_DOM_URL = url;
    }


    // STT
    public static SpeechRecognizer mSpeech = null;
    public static Intent mRecognizerIntent = null;
    public static boolean mSpeechIsRecording = false;

    // get battery level
    public static String getBatteryPercentage(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
                return String.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
            } else {
                IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, iFilter);

                int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

                double batteryPct = level / (double) scale;
                return String.valueOf(batteryPct * 100);
            }
        } catch (Exception e) {
            Log.e(TAG, "getBatteryPercentage error: " + e.getMessage());
            return "100";
        }
    }

    //
    public static RequestQueue getRequestQueue(Context appCtx) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(appCtx);
        }
        return mRequestQueue;
    }

}

