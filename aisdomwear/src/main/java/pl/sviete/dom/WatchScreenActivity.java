package pl.sviete.dom;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;



public class WatchScreenActivity extends WearableActivity implements TextToSpeech.OnInitListener{


    final String TAG = WatchScreenActivity.class.getName();
    private LocalBroadcastManager mlocalBroadcastManager;
    private TextView mSttTextView;
    private pl.sviete.dom.MyTextClock mTimeTextView;
    private pl.sviete.dom.MyTextClock mDateTextView;
    private final int REQUEST_RECORD_PERMISSION = 100;
    private Config mConfig = null;
    private ToggleButton mBtnSpeak;
    private TextToSpeech mTts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ais_dom_watch_screen);

        //
        createSpeechEngine();

        //
        mBtnSpeak = findViewById(R.id.btnSpeak);

        mBtnSpeak.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    // volume down
                    Intent intent = new Intent(AisCoreUtils.BROADCAST_ON_START_SPEECH_TO_TEXT);
                    LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
                    bm.sendBroadcast(intent);

                    int permissionMic = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
                    if (permissionMic != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions
                                (WatchScreenActivity.this,
                                        new String[]{Manifest.permission.RECORD_AUDIO},
                                        REQUEST_RECORD_PERMISSION);
                    } else {
                        startTheSpeechToText();
                    }
                } else {
                    stopTheSpeechToText();
                }
            }
        });

        //
        mlocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //
        mSttTextView = findViewById(R.id.sttTextView);
        mTimeTextView = findViewById(R.id.timeTextView);
        mDateTextView = findViewById(R.id.dataTextView);

        ImageView mConfImageView = findViewById(R.id.go_to_config);
        mConfImageView.setOnClickListener(v -> {
            // go to settings
            Log.d(TAG, "startSettingsActivity Called");
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));

        });

        mSttTextView.setText("");
        mSttTextView.setVisibility(View.INVISIBLE);
        mTimeTextView.setVisibility(View.VISIBLE);
        mDateTextView.setVisibility(View.VISIBLE);
        mSttTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        // Enables Always-on
        setAmbientEnabled();

        createTTS();
        //

        // set gate ID
        Log.i(TAG, "set gate ID");
        AisCoreUtils.AIS_GATE_ID = "dom-" + Settings.Secure.getString(this.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.i(TAG, "AIS_GATE_ID: " + AisCoreUtils.AIS_GATE_ID);
    }

    @Override
    public void onInit(int status) {

        Log.d(TAG, "onInit");
        if (status != TextToSpeech.ERROR) {
            int result = mTts.setLanguage(new Locale("pl_PL"));
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language is not available.");
            }

            if(result == TextToSpeech.SUCCESS) {
                mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "TTS finished");

                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.d(TAG, "TTS onError");

                    }

                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "TTS onStart");
                    }
                });
            };
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }

    }

    private void startTheSpeechToText(){

        // just in case
        createSpeechEngine();

        // hide the date and clock show the stt
        mSttTextView.setVisibility(View.VISIBLE);
        mTimeTextView.setVisibility(View.INVISIBLE);
        mDateTextView.setVisibility(View.INVISIBLE);


        if (AisCoreUtils.mSpeechIsRecording) {
            stopTheSpeechToText();
            Log.e(TAG, "StopTheSpeechToText !!!");
        }

        Log.d(TAG, "startListening");
        AisCoreUtils.mSpeech.startListening(AisCoreUtils.mRecognizerIntent);

        ScaleAnimation scaleAnimation = new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.7f, Animation.RELATIVE_TO_SELF, 0.7f);
        scaleAnimation.setDuration(500);
        BounceInterpolator bounceInterpolator = new BounceInterpolator();
        scaleAnimation.setInterpolator(bounceInterpolator);
        mBtnSpeak.startAnimation(scaleAnimation);
    }

    private void stopTheSpeechToText(){
        Log.d(TAG, "stopTheSpeechToText");
        AisCoreUtils.mSpeech.stopListening();
        ScaleAnimation scaleAnimation = new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.7f, Animation.RELATIVE_TO_SELF, 0.7f);
        scaleAnimation.setDuration(500);
        BounceInterpolator bounceInterpolator = new BounceInterpolator();
        scaleAnimation.setInterpolator(bounceInterpolator);
        mBtnSpeak.startAnimation(scaleAnimation);
    }

    @Override
    protected void onStart() {
        // to check the url from settings
        mConfig = new Config(getApplicationContext());
        // get app url with discovery
        String url = mConfig.getAppLaunchUrl(true);
        Log.i(TAG, url);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AisCoreUtils.BROADCAST_ON_END_SPEECH_TO_TEXT);
        filter.addAction(AisCoreUtils.BROADCAST_ON_START_SPEECH_TO_TEXT);
        filter.addAction(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS);
        filter.addAction(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND);
        filter.addAction(AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT);
        mlocalBroadcastManager.registerReceiver(mBroadcastReceiver, filter);

        //
        super.onStart();

        // go to settings of first start
        if (url.equals("")){
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.app_add_gate_connection_info), Toast.LENGTH_LONG).show());

            Intent i = new Intent(this, SettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(i);
        }
    }


    @Override
    protected void onStop() {
        mlocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AisCoreUtils.BROADCAST_ON_END_SPEECH_TO_TEXT)) {
                Log.i(TAG, AisCoreUtils.BROADCAST_ON_END_SPEECH_TO_TEXT + "onEndSpeechToText");
                onEndSpeechToText();
            } else if (intent.getAction().equals(AisCoreUtils.BROADCAST_ON_START_SPEECH_TO_TEXT)) {
                Log.i(TAG, AisCoreUtils.BROADCAST_ON_START_SPEECH_TO_TEXT + " onStartSpeechToText.");
                onStartSpeechToText();
            } else if (intent.getAction().equals(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS)) {
                Log.i(TAG, AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS + " display text.");
                final String partialText = intent.getStringExtra(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS_TEXT);
                onSttParialResults(partialText);
            } else if (intent.getAction().equals(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND)){
                Log.i(TAG, AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND + " display text.");
                final String command = intent.getStringExtra(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND_TEXT);
                onSttFullResult(command);
            } else if (intent.getAction().equals(AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT)) {
                Log.d(TAG, AisCoreUtils.BROADCAST_ACTIVITY_SAY_IT + " going to processTTS");
                final String txtMessage = intent.getStringExtra(AisCoreUtils.BROADCAST_SAY_IT_TEXT);
                processTTS(txtMessage);
            }
        }
    };

    private void onStartSpeechToText() {
        Log.d(TAG, "onStartSpeechToText -> play animation");

        mSttTextView.setText(getString(R.string.app_i_am_listening_info));
        mSttTextView.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    private void onEndSpeechToText(){
        Log.d(TAG, "onEndSpeechToText -> stop");

        if (mSttTextView.getText().equals(getString(R.string.app_i_am_listening_info))){
            mSttTextView.setText("");
        }

        mBtnSpeak.setChecked(false);
    }

    private void onSttFullResult(final String text) {
        Log.d(TAG, "!!! onSttFullResult " + text);
        mSttTextView.setText(text);
        mSttTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //remove text after 1.5sec
                String currText = mSttTextView.getText().toString();
                if (currText.equals(text)) {
                    mSttTextView.setText("");
                    mTimeTextView.setVisibility(View.VISIBLE);
                    mDateTextView.setVisibility(View.VISIBLE);
                }
            }
        }, 2500);
    }

    private void onSttParialResults(String text) {
        Log.d(TAG, "onSttParialResults " + text);
        mSttTextView.setText(text);
        mSttTextView.setGravity(Gravity.CENTER_HORIZONTAL);
    }


    private void createSpeechEngine(){

        //if (AisCoreUtils.mSpeech == null) {
        Log.i(TAG, "createSpeechEngine -> createSpeechRecognizer");
        AisCoreUtils.mSpeech = SpeechRecognizer.createSpeechRecognizer(this);
        AisRecognitionListener listener = new AisRecognitionListener(this, AisCoreUtils.mSpeech);
        AisCoreUtils.mSpeech.setRecognitionListener(listener);
        //}

        if (AisCoreUtils.mRecognizerIntent == null) {
            Log.i(TAG, "createSpeechEngine -> starting STT initialization");
            AisCoreUtils.mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(5000));
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "pl.sviete.dom");
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            AisCoreUtils.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AisCoreUtils.mSpeech != null) {
            AisCoreUtils.mSpeech.destroy();
        }
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
    }

    // TTS
    public void stopTextToSpeech(){
        Log.i(TAG, "Speech started, stoping the tts");
        try {
            mTts.stop();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void createTTS() {
        Log.i(TAG, "starting TTS initialization");
        mTts = new TextToSpeech(this, this);
        mTts.setSpeechRate(1.0f);
    }
    private boolean processTTS(String text) {
        Log.d(TAG, "processTTS Called: " + text);

        // display text
        final String text_to_disp = text;
        mSttTextView.setText(text);
        mSttTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //remove text after 4sec
                String currText = mSttTextView.getText().toString();
                if (currText.equals(text_to_disp)) {
                    mSttTextView.setText("");
                    mTimeTextView.setVisibility(View.VISIBLE);
                    mDateTextView.setVisibility(View.VISIBLE);
                }
            }
        }, 4000);


         // shouldIsayThis Text???
        String text_to_say = text.trim();
        if (AisCoreUtils.AIS_DOM_LAST_TTS.substring(0, Math.min(AisCoreUtils.AIS_DOM_LAST_TTS.length(), 250)).equals(text_to_say.substring(0, Math.min(text_to_say.length(), 250)))){
            AisCoreUtils.AIS_DOM_LAST_TTS = text_to_say;
                return true;
            }
        AisCoreUtils.AIS_DOM_LAST_TTS = text_to_say;



        // STOP current TTS
        stopTextToSpeech();

        String voice = "pl-pl-x-oda-network";
        float pitch = 1;
        float rate = 1;

        // speak failed: not bound to TTS engine
        if (mTts == null){
            Log.w(TAG, "mTts == null");
            try {
                createTTS();
                return true;
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        Voice voiceobj = new Voice(
                voice, new Locale("pl_PL"),
                Voice.QUALITY_HIGH,
                Voice.LATENCY_NORMAL,
                false,
                null);
        mTts.setVoice(voiceobj);


        //textToSpeech can only cope with Strings with < 4000 characters
        if(text.length() >= 4000) {
            text = text.substring(0, 3999);
        }
        mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null,"123");

        return true;

    }
}
