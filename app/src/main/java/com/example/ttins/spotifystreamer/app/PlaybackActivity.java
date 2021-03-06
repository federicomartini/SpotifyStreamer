package com.example.ttins.spotifystreamer.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.ArrayList;
import java.util.List;


public class PlaybackActivity extends ActionBarActivity implements PlaybackActivityFragment.OnFragmentClickListener {

    private final static String LOG_TAG = "PlaybackActivity";
    public static final String FRAG_ARG_TOP_TEN_LIST = "ARG_TOP_TEN_LIST";
    public static final String FRAG_ARG_TRACK = "ARG_TRACK";
    public static final String FRAG_ARG_TRACK_POSITION = "ARG_TRACK_POSITION";
    public static final String FRAG_ARG_NOW_PLAYING = "ARG_NOW_PLAYING";
    public static final String INTENT_TRACK_POSITION = "INTENT_TRACK_POSITION";
    public static final String INTENT_TOP_TEN_LIST = "INTENT_TOP_TEN_LIST";
    public static final String BUNDLE_TRACK = "BUNDLE_TRACK";
    public static final String INTENT_TRACK_BUNDLE = "INTENT_TRACK_BUNDLE";

    private TrackItemList mTrackItemList;
    private int mPosition;
    private List<TrackItemList> mTracks = new ArrayList<TrackItemList>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        /* Toolbar handler */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarPlayback);
        setToolbar(toolbar);
        //toolbar.setSubtitle(artistName);
        toolbar.setTitle(R.string.playback_activity_name);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference_activity, false);

        Intent intent = getIntent();

        //Check if the Intent comes from the Now Playing button or from Top Ten Item Click
        if (!intent.hasExtra(INTENT_TRACK_BUNDLE)) {
            //do nothing
        } else {
            mTrackItemList =  intent.getBundleExtra(INTENT_TRACK_BUNDLE).getParcelable(BUNDLE_TRACK);
            mPosition = intent.getBundleExtra(INTENT_TRACK_BUNDLE).getInt(INTENT_TRACK_POSITION);
            mTracks = intent.getBundleExtra(INTENT_TRACK_BUNDLE).getParcelableArrayList(INTENT_TOP_TEN_LIST);

            if (null == mTrackItemList)
                Log.d(LOG_TAG, "trackItemList is null");
        }

        if (null == savedInstanceState) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment playbackFragment = new PlaybackActivityFragment();
            Bundle bundle = new Bundle();

            if (intent.hasExtra(INTENT_TRACK_BUNDLE)) {
                bundle.putParcelable(FRAG_ARG_TRACK, mTrackItemList);
                bundle.putParcelableArrayList(FRAG_ARG_TOP_TEN_LIST, (ArrayList<TrackItemList>) mTracks);
                bundle.putInt(FRAG_ARG_TRACK_POSITION, mPosition);
            } else {
                bundle.putBoolean(FRAG_ARG_NOW_PLAYING, true);
            }

            playbackFragment.setArguments(bundle);
            fragmentTransaction.replace(R.id.playback_container, playbackFragment).commit();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /* Customize the toolbar */
    private boolean setToolbar(Toolbar toolbar) {

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            assert getSupportActionBar() != null;
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        else
            return false;

        /* Logo image */
        toolbar.setLogo(R.drawable.spotify_icon_36);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        return true;
    }


    public void onFragmentDismiss() {
        this.finish();
    }

}
