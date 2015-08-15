package com.example.ttins.spotifystreamer.app;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.ArrayList;
import java.util.List;

public class TopTenActivity extends ActionBarActivity implements TopTenFragment.OnTopTenFragmentInteractionListener {

    private final static String LOG_TAG = "TopTenActivity";
    private MenuItem mPlayActionButton;
    List<TrackItemList> mTracks = new ArrayList<>();
    private ShareActionProvider mShareActionProvider;
    private PlaybackService mPlaybackService;
    private boolean mBound;
    public static final String ACTION_NOW_PLAY = "com.example.ttins.spotifystreamer.MainActivity.INTENT_NOW_PLAYING_TRACK";

    private final static String LIST_KEY = "PARCEABLE_LIST_KEY";

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;

            Log.d(LOG_TAG, "Bound to Playback service");

            if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mPlaybackService.getPreviewUrl());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "SendTo"));
            } else {
                Toast.makeText(getApplicationContext(), "No Url found", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound=false;
            Log.d(LOG_TAG, "Unbound to Playback service");
        }
    };

    @Override
    public void onTopTenFragmentItemClick(@NonNull TrackItemList trackItemList, @NonNull int position){
        Intent intent = new Intent(this, PlaybackActivity.class);
        Bundle bundle = new Bundle();

        /* Starting Playback Service */
        Intent playbackServiceIntent = new Intent(PlaybackService.ACTION_INIT);
        playbackServiceIntent.putExtra("INTENT_PREVIEW_URL", trackItemList.getTrackPreview_url());
        playbackServiceIntent.putExtra("INTENT_TRACK_POSITION", position);
        playbackServiceIntent.putParcelableArrayListExtra("INTENT_TOP_TEN_LIST", (ArrayList<TrackItemList>) mTracks);
        playbackServiceIntent.setClass(this, PlaybackService.class);
        startService(playbackServiceIntent);

        Intent playSongIntent = new Intent(PlaybackService.ACTION_PLAY);
        playSongIntent.setClass(this, PlaybackService.class);
        startService(playSongIntent);

        /* Starting Playback UI */
        bundle.putParcelable("BUNDLE_TRACK", trackItemList);
        bundle.putInt("INTENT_TRACK_POSITION", position);
        bundle.putParcelableArrayList("INTENT_TOP_TEN_LIST", (ArrayList<TrackItemList>) mTracks);
        intent.putExtra("INTENT_TRACK_BUNDLE", bundle);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_ten_activity, menu);
        mPlayActionButton = menu.findItem(R.id.now_playing_action_button_topten);

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

        if (id == R.id.now_playing_action_button_topten) {
            Intent intent = new Intent(ACTION_NOW_PLAY);
            startActivity(intent);
            return true;

        }

        if (id == R.id.menu_item_share) {
            if (null == mPlaybackService) {
                Intent intent = new Intent(this, PlaybackService.class);
                if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                    Log.d(LOG_TAG, "Bind to Service failed");
                }
            } else {
                if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mPlaybackService.getPreviewUrl());
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "SendTo"));
                } else {
                    Toast.makeText(this, "No Url found", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);

        saveInstanceState.putParcelableArrayList(LIST_KEY, (ArrayList<TrackItemList>) mTracks);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onCreate(Bundle saveInstanceState) {

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(saveInstanceState);
        setContentView(R.layout.top_ten_activity);

         /* Toolbar handler */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarTopTen);
        setToolbar(toolbar);
        //toolbar.setSubtitle(artistName);
        toolbar.setTitle(R.string.topten_activity_name);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference_activity, false);

        String artistName = getIntent().getStringExtra("INTENT_ARTIST_NAME");
        final String artistImage = getIntent().getStringExtra("INTENT_ARTIST_IMAGE");
        mTracks = getIntent().getParcelableArrayListExtra("TOP_TEN_LIST");

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        Fragment topTenFragment = new TopTenFragment();
        Bundle argsBundle = new Bundle();

        argsBundle.putString("ARG_ARTIST_NAME", getIntent().getStringExtra("INTENT_ARTIST_NAME"));
        argsBundle.putString("ARG_ARTIST_IMAGE", getIntent().getStringExtra("INTENT_ARTIST_IMAGE"));
        argsBundle.putParcelableArrayList("ARG_TOP_TEN_LIST", getIntent().getParcelableArrayListExtra("TOP_TEN_LIST"));

        topTenFragment.setArguments(argsBundle);

        fragmentTransaction.replace(R.id.topten_container, topTenFragment);
        fragmentTransaction.commit();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
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




}