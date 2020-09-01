package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import android.os.Environment;
import android.util.Log;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.wearable.intent.RemoteIntent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static pl.sviete.dom.DomWebInterface.doSendLogToAis;


public class SettingsActivity extends AppCompatActivity {

    static final String TAG = SettingsActivity.class.getName();
    private static Config mConfig = null;
    private static Context myContext = null;


    private static final Preference.OnPreferenceChangeListener sBindPreferenceChangeListener = (preference, value) -> {
        String stringValue = value.toString();

        // preference is url
        if (preference.getKey().equals("setting_app_launchurl")) {
            // pin was provided
            if (stringValue.length() == 6) {
                Log.i(TAG, stringValue);
                // get gate id for pin
                Config.checkGateIdForPinJob checkConnectionUrlJob = new Config.checkGateIdForPinJob();
                checkConnectionUrlJob.execute(stringValue);
            }
        }

        return true;
    };

    /**
     * Binds a preference's value change.
     *
     * @see #sBindPreferenceChangeListener
     */
    private static void bindPreferenceChangeListener(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        sBindPreferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
    }


    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        myContext = getApplicationContext();

        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }


    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onResume() {
            super.onResume();
        }


        @Override
        public void onStart() {
            super.onStart();
            mConfig = new Config(myContext);

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceScreen prefEmailLogs = (PreferenceScreen) findPreference("pref_ais_dom_email_logs");
            prefEmailLogs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        doSendLogToAis(getContext());
                        // Intent remoteIntent = new Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(Uri.parse("market://details?id=pl.sviete.dom"));
                        //RemoteIntent.startRemoteActivity(getContext(),remoteIntent,null);

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Log.e(TAG, e.getMessage());
                    }
                    return false;
                    }
            });

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            // set info in version
            String versionName = BuildConfig.VERSION_NAME;
            Preference preferenceVersion = findPreference("pref_ais_dom_version");
            preferenceVersion.setSummary(versionName + " (wear app)\n");

            //
            bindPreferenceChangeListener(findPreference(getString(R.string.key_setting_app_launchurl)));
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            // TODO
            super.onDisplayPreferenceDialog(preference);

        }

    }


}

