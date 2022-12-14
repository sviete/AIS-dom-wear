package pl.sviete.dom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import android.util.Log;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import static pl.sviete.dom.DomWebInterface.doSendLogToAis;


public class SettingsActivity extends AppCompatActivity {

    static final String TAG = SettingsActivity.class.getName();
    private static Config mConfig = null;
    private static Context myContext = null;




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
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    return false;
                    }
            });

            EditTextPreference prefAppUrl = (EditTextPreference) findPreference(getString(R.string.key_setting_app_launchurl));
            prefAppUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // pin was provided
                    if (newValue.toString().length() == 6) {
                        Log.i(TAG, newValue.toString());
                        // get gate id for pin
                        Config.checkGateIdForPinJob checkConnectionUrlJob = new Config.checkGateIdForPinJob();
                        checkConnectionUrlJob.execute(newValue.toString());
                    }
                    return true;
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
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            // TODO
            super.onDisplayPreferenceDialog(preference);

        }

    }


}

