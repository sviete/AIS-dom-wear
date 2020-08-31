package pl.sviete.dom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import android.os.Environment;
import android.util.Log;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.io.File;
import java.io.IOException;


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
                    // email
                        // save logcat in file
                        File outputFile = new File(Environment.getExternalStorageDirectory(),
                                "logcat.txt");
                        try {
                            Runtime.getRuntime().exec(
                                    "logcat -f " + outputFile.getAbsolutePath());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        //send file using email
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        // Set type to "email"
                        emailIntent.setType("vnd.android.cursor.dir/email");
                        String to[] = {"info@gsviete.pl"};
                        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
                        // the attachment
                        emailIntent .putExtra(Intent.EXTRA_STREAM, outputFile.getAbsolutePath());
                        // the mail subject
                        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Wear Os Logs");
                        startActivity(Intent.createChooser(emailIntent , "Logs..."));

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

