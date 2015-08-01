package com.example.ttins.spotifystreamer.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.List;


public class PlaybackActivity extends ActionBarActivity {

    private final static String LOG_TAG = "PlaybackActivity";
    TextView mArtistTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Intent intent = getIntent();
        TrackItemList trackItemList =  intent.getBundleExtra("INTENT_TRACK_BUNDLE").getParcelable("BUNDLE_TRACK");

        if (null == trackItemList)
            Log.d(LOG_TAG, "trackItemList is null");

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment playbackFragment = new PlaybackActivityFragment();
        Bundle bundle = new Bundle();

        bundle.putParcelable("ARG_TRACK", trackItemList);

        playbackFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.playback_container, playbackFragment).commit();

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
