package pl.sviete.dom;

/**
 * Created by andrzej on 29.01.18.
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class AisRecognitionListener implements RecognitionListener {
    public static final String TAG = AisRecognitionListener.class.getName();
    public Context context;
    public static SpeechRecognizer speechRecognizer;
    public static int mTimeoutErrorCount = 0;
    public AisRecognitionListener(Context context, SpeechRecognizer speechRecognizer) {
        this.context = context;
        this.speechRecognizer = speechRecognizer;
    }
    public static Random random = new Random();
    public static List<String> errorAnswers = Arrays.asList(
            "Nie rozpoznaję mowy, spróbuj ponownie.",
            "Brak danych głosowych, spróbuj ponownie.",
            "Nie rozumiem, spróbuj ponownie.",
            "Nie słyszę, powtórz proszę.",
            "Co mówiłeś?",
            "Przepraszam, powtórz proszę.",
            "Nie rozumiem, czy możesz powtórzyć?",
            "Powiedz jeszcze raz proszę.",
            "Nie wiem o co chodzi, proszę powtórzyć.",
            "Coś nie słychać, spróbuj ponownie.",
            "Powiedz proszę jeszcze raz.",
            "Proszę powtórzyć.",
            "Powtórz proszę jeszcze raz.",
            "Czy możesz powtórzyć?");

    @Override
    public void onReadyForSpeech(Bundle params) {

        //
        AisCoreUtils.mSpeechIsRecording = true;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "AisRecognitionListener onBeginningOfSpeech");
        Intent intent = new Intent(AisCoreUtils.BROADCAST_ON_START_SPEECH_TO_TEXT);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }


    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "AisRecognitionListener BROADCAST_ON_END_SPEECH_TO_TEXT");
        Intent intent = new Intent(AisCoreUtils.BROADCAST_ON_END_SPEECH_TO_TEXT);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);

        //
        AisCoreUtils.mSpeechIsRecording = false;
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(TAG, "AisRecognitionListener onError, FAILED " + errorMessage);
        if (!errorMessage.equals("")) {
            // end of speech to text
            onEndOfSpeech();
        }

        //
        AisCoreUtils.mSpeechIsRecording = false;
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "AisRecognitionListener onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Intent intent = new Intent(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND);
        intent.putExtra(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_COMMAND_TEXT, matches.get(0));
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);

        // publish via interface to gate
        DomWebInterface.publishMessage( matches.get(0), "speech_command", context);

        // reset the timeout error count
        mTimeoutErrorCount = 0;
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "AisRecognitionListener onPartialResults");
        ArrayList<String> matches = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Intent intent = new Intent(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS);
        intent.putExtra(AisCoreUtils.BROADCAST_EVENT_ON_SPEECH_PARTIAL_RESULTS_TEXT, matches.get(0));
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "AisRecognitionListener onEvent " + eventType + " "  + params.toString());
    }

    public static String getErrorText(int errorCode) {
        String message = "";
        Boolean bRandom = false;
        Log.e(TAG, "errorCode: " + errorCode);

        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Błąd nagrywania mowy.";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                 message = "Błąd, spróbuj ponownie.";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Niewystarczające uprawnienia";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Błąd sieci, spróbuj ponownie.";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Limit czasu sieci.";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                // "stopListening called by other caller than startListening - ignoring"
                //message = "Usługa rozpoznawania mowy jest zajęta.";
                //speechRecognizer.stopListening();
                //speechRecognizer.cancel();
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Problem z siecią, spróbuj ponownie.";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                bRandom = true;
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                bRandom = true;
                // on error code 6 - ERROR_SPEECH_TIMEOUT
                break;
            default:
                bRandom = true;
                break;
        }

        if (bRandom) {
            int index = random.nextInt(errorAnswers.size());
            message = errorAnswers.get(index);
        }


        return message;
    }

}



