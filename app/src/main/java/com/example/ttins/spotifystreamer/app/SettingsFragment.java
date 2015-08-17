package com.example.ttins.spotifystreamer.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;

import java.util.zip.Inflater;


public class SettingsFragment extends PreferenceFragment
                implements Preference.OnPreferenceChangeListener{

    private static final String LOG_TAG = "SettingsFragment";
    private static final String ACTION_ENABLE_NOTIFY = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_ENABLE_NOTIFY";
    private static final String ACTION_DISABLE_NOTIFY = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_DISABLE_NOTIFY";
    private PlaybackService mPlaybackService;
    private boolean mBound;
    private Boolean mNotifyEnable;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;

            Log.d(LOG_TAG, "Bound to Playback service");

            if(mNotifyEnable) {
                Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                serviceIntent.setAction(ACTION_ENABLE_NOTIFY);
                getActivity().startService(serviceIntent);
            } else {
                Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                serviceIntent.setAction(ACTION_DISABLE_NOTIFY);
                getActivity().startService(serviceIntent);
            }

        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound=false;
            Log.d(LOG_TAG, "Unbound to Playback service");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_activity);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_list_lang_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_checkbox_notification_key)));

    }


    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof ListPreference) {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } else if (preference instanceof CheckBoxPreference) {
            // Trigger the listener immediately with the preference's
            // current value.
            onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getBoolean(preference.getKey(), true));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference instanceof ListPreference) {
            String stringValue = newValue.toString();
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            if (index >= 0) {
                preference.setSummary(listPreference.getEntries()[index]);
            }
            else {
                preference.setSummary(stringValue);
            }
        }

        if (preference instanceof CheckBoxPreference) {
            mNotifyEnable = (Boolean) newValue;

            if (null == mPlaybackService) {
                Intent intent = new Intent(getActivity(), PlaybackService.class);
                if (!getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                    Log.d(LOG_TAG, "Bind to Service failed");
                }
            } else {

                if(mNotifyEnable) {
                    Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                    serviceIntent.setAction(ACTION_ENABLE_NOTIFY);
                    getActivity().startService(serviceIntent);
                } else {
                    Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                    serviceIntent.setAction(ACTION_DISABLE_NOTIFY);
                    getActivity().startService(serviceIntent);
                }
            }

        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

}
