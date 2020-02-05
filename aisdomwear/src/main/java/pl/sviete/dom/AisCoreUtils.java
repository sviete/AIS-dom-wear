package pl.sviete.dom;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

public class AisCoreUtils {

    // AIS_GATE ID
    public static String AIS_GATE_ID = null;
    public static String AIS_GATE_USER = "";
    public static String AIS_GATE_DESC = "";
    public static String AIS_DEVICE_TYPE = "MOB";
    public static int AIS_LAST_KEY_CODE = 0;

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

    // IP
    public static final String BROADCAST_ON_IP_CHANGE = "BROADCAST_ON_IP_CHANGE";
    public static final String BROADCAST_ON_IP_CHANGE_NEW_IP = "BROADCAST_ON_IP_CHANGE_NEW_IP";
    public static final String BROADCAST_ON_IP_CHANGE_NEW_ICON = "BROADCAST_ON_IP_CHANGE_NEW_ICON";
    public static final String BROADCAST_ON_IP_CHANGE_NEW_INFO = "BROADCAST_ON_IP_CHANGE_NEW_INFO";


    // USB
    private static String TAG = "AisCoreUtils";
    private static String AIS_DOM_URL = "http://127.0.0.1:8180";
    private static String AIS_DOM_CLOUD_WS_URL = "https://powiedz.co/ords/dom/dom/";
    private static String AIS_DOM_CLOUD_WS_URL_HTTP = "http://powiedz.co/ords/dom/dom/";
    private static String mCurrentRemoteControllerMode = AisCoreUtils.mOnDisplay;



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

    public static String getRemoteControllerMode(){
        return mCurrentRemoteControllerMode;
    }

    public static void setRemoteControllerMode(String mode){
        mCurrentRemoteControllerMode = mode;
    }

    public static Activity mBrowserActivity = null;
    public static Activity mSplashScreenActivity = null;
    public static WebView mWebView = null;

    // STT
    public static SpeechRecognizer mSpeech = null;
    public static Intent mRecognizerIntent = null;
    public static boolean mSpeechIsRecording = false;
    //
    // Control mode
    public static String mOnDisplay = "ON_DISPLAY";
    public static String mOffDisplay = "OFF_DISPLAY";
    public static String mByGesture = "BY_GESTURE";


    // IOT and IP
    public static int    mLastLongPressKeyCode = -1;
    public static long   mLastSwitchControlModeTime = System.currentTimeMillis();
    public static long   mLastWifiDisconnectInfoTime = System.currentTimeMillis();
    public static int    mLastConnectedNetworkId = -1;
    public static boolean mReconnectToEthernet = false;
    public static String G_Last_IP_Address = " ";
    public static int G_Last_Network_Type = -2;
    public static boolean mIOTparringInProgress = false;
    public static boolean mSayInfoConnectionBack = false;

    // Audio source
    public static String mAudioSourceSpotify = "Spotify";
    public static String mAudioSourceRadio = "Radio";
    public static String mAudioSourcePodcast = "Podcast";
    public static String mAudioSourceMusic = "Music";
    public static String mAudioSourceAudioBook = "AudioBook";
    public static String mAudioSourceNews = "News";
    public static String mAudioSourceLocal = "Local";
    public static String mAudioSourceAndroid = "Android";


}

