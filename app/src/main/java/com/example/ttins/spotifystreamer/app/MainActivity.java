package com.example.ttins.spotifystreamer.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.MyArtist;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.OnArtistSelectedListener, TopTenFragment.OnTopTenFragmentInteractionListener, PlaybackActivityFragment.OnFragmentClickListener {

    private static final String LOG_TAG = "MainActivity";
    public static final String ACTION_NOW_PLAY = "com.example.ttins.spotifystreamer.MainActivity.INTENT_NOW_PLAYING_TRACK";
    public static final String FRAG_ARG_ARTIST_NAME = "ARG_ARTIST_NAME";
    public static final String FRAG_ARG_ARTIST_IMAGE = "ARG_ARTIST_IMAGE";
    public static final String FRAG_ARG_TOP_TEN_LIST = "ARG_TOP_TEN_LIST";
    public static final String FRAG_ARG_TRACK = "ARG_TRACK";
    public static final String FRAG_ARG_TRACK_POSITION = "ARG_TRACK_POSITION";
    public static final String FRAG_ARG_NOW_PLAYING = "ARG_NOW_PLAYING";

    public static final String INTENT_PREVIEW_URL = "INTENT_PREVIEW_URL";
    public static final String INTENT_TRACK_POSITION = "INTENT_TRACK_POSITION";
    public static final String INTENT_TOP_TEN_LIST = "INTENT_TOP_TEN_LIST";
    public static final String BUNDLE_TRACK = "BUNDLE_TRACK";
    public static final String INTENT_TRACK_BUNDLE = "INTENT_TRACK_BUNDLE";
    public static final String INTENT_ARTIST_NAME = "INTENT_ARTIST_NAME";
    public static final String INTENT_ARTIST_IMAGE = "INTENT_ARTIST_IMAGE";
    public static final String TOP_TEN_LIST = "TOP_TEN_LIST";

    public static final String SAVED_TRACK_LIST = "TRACK_LIST";
    public static final String SAVED_PREVIEW_URL = "PREVIEW_URL";


    private boolean mTwoPane;
    private List<TrackItemList> mTracks = new ArrayList<TrackItemList>();
    private String mTrackPreview_Url;
    Fragment mTopTenFragment;
    private PlaybackService mPlaybackService;
    private boolean mBound;
    MenuItem mPlayActionButton;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;

            Log.d(LOG_TAG, "Bound to Playback service");

            if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                sendShareIntent(mPlaybackService.getPreviewUrl());
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

    void sendShareIntent(String previewUrl) {
        Intent sendIntent = new Intent();

        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, previewUrl);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_intent_menu_text)));

    }

    @Override
    public void onArtistSelected(MyArtist artist, ArrayList<TrackItemList> tracks) {
        //Tablet version
        if (mTwoPane){
            mTopTenFragment = new TopTenFragment();
            Bundle argsBundle = new Bundle();
            mTracks = tracks;
            argsBundle.putString(FRAG_ARG_ARTIST_NAME, artist.name);
            argsBundle.putString(FRAG_ARG_ARTIST_IMAGE, artist.image);
            argsBundle.putParcelableArrayList(FRAG_ARG_TOP_TEN_LIST, tracks);

            mTopTenFragment.setArguments(argsBundle);
            getFragmentManager().beginTransaction().replace(R.id.topten_container, mTopTenFragment).commit();

        } else {
            startActivity(makeTopTenIntent(artist, tracks));
        }
    }

    @Override
    public void onTopTenFragmentItemClick(@NonNull TrackItemList trackItemList, @NonNull int position){
        Intent intent = new Intent(this, PlaybackActivity.class);
        Bundle bundle = new Bundle();
        mTrackPreview_Url = trackItemList.getTrackPreview_url();

        /* Starting Playback Service */
        Intent playbackServiceIntent = new Intent(PlaybackService.ACTION_INIT);
        playbackServiceIntent.putExtra(INTENT_PREVIEW_URL, trackItemList.getTrackPreview_url());
        playbackServiceIntent.putExtra(INTENT_TRACK_POSITION, position);
        playbackServiceIntent.putParcelableArrayListExtra(INTENT_TOP_TEN_LIST, (ArrayList<TrackItemList>) mTracks);
        playbackServiceIntent.setClass(this, PlaybackService.class);
        startService(playbackServiceIntent);

        Intent playSongIntent = new Intent(PlaybackService.ACTION_PLAY);
        playSongIntent.setClass(this, PlaybackService.class);
        startService(playSongIntent);

        if (mTwoPane) {
            PlaybackActivityFragment playbackActivityFragment = new PlaybackActivityFragment();
            FragmentManager fragmentManager = getFragmentManager();
            Bundle bundleFragment = new Bundle();

            bundleFragment.putParcelable(FRAG_ARG_TRACK, trackItemList);
            bundleFragment.putParcelableArrayList(FRAG_ARG_TOP_TEN_LIST, (ArrayList<TrackItemList>) mTracks);
            bundleFragment.putInt(FRAG_ARG_TRACK_POSITION, position);

            playbackActivityFragment.setArguments(bundleFragment);
            playbackActivityFragment.show(fragmentManager, "Play Now");

        } else {
            /* Starting Playback UI */
            bundle.putParcelable(BUNDLE_TRACK, trackItemList);
            bundle.putInt(INTENT_TRACK_POSITION, position);
            bundle.putParcelableArrayList(INTENT_TOP_TEN_LIST, (ArrayList<TrackItemList>) mTracks);
            intent.putExtra(INTENT_TRACK_BUNDLE, bundle);
            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null != savedInstanceState) {
            mTracks = savedInstanceState.getParcelableArrayList("TRACK_LIST");
            mTrackPreview_Url = savedInstanceState.getString("PREVIEW_URL");
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setToolbar(toolbar);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference_activity, false);

        if (findViewById(R.id.topten_container) != null) {
            mTwoPane = true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (null == mPlaybackService && !mTwoPane) {
            Intent intent = new Intent(this, PlaybackService.class);
            if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(LOG_TAG, "Bind to Service failed");
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mPlayActionButton = menu.findItem(R.id.now_playing_action_button);

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);

        saveInstanceState.putParcelableArrayList(SAVED_TRACK_LIST, (ArrayList<TrackItemList>) mTracks);
        saveInstanceState.putString(SAVED_PREVIEW_URL, mTrackPreview_Url);
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

        if (id == R.id.now_playing_action_button) {
            if (mTwoPane) {
                if (mTrackPreview_Url != null) {
                    PlaybackActivityFragment playbackActivityFragment = new PlaybackActivityFragment();
                    FragmentManager fragmentManager = getFragmentManager();
                    Bundle bundleFragment = new Bundle();
                    bundleFragment.putBoolean(FRAG_ARG_NOW_PLAYING, true);

                    playbackActivityFragment.setArguments(bundleFragment);
                    playbackActivityFragment.show(fragmentManager, "Play Now");
                } else {
                    Toast.makeText(this, "No Url recently played by Spotify Streamer", Toast.LENGTH_SHORT).show();
                }

            } else {
                Intent intent = new Intent(ACTION_NOW_PLAY);
                startActivity(intent);
                return true;
            }
        }

        if (id == R.id.menu_item_share) {
            if (mTwoPane) {
                if (null == mPlaybackService) {
                    Intent intent = new Intent(this, PlaybackService.class);
                    if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                        Log.d(LOG_TAG, "Bind to Service failed");
                    }
                } else {
                    if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                        sendShareIntent(mPlaybackService.getPreviewUrl());
                    } else {
                        Toast.makeText(this, "No Url found", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                    sendShareIntent(mPlaybackService.getPreviewUrl());
                } else {
                    Toast.makeText(this, "No Url found", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /* Customize toolbar */
    private boolean setToolbar(Toolbar toolbar) {

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        else {
            return false;
        }

        toolbar.setLogo(R.drawable.spotify_icon_36);
        return true;
    }

    /* Create intent for TopTenActivity */
    private Intent makeTopTenIntent(MyArtist artist, ArrayList<TrackItemList> tracks) {
        Intent intent = new Intent(this, TopTenActivity.class);
        intent.putExtra(INTENT_ARTIST_NAME, artist.name);
        intent.putExtra(INTENT_ARTIST_IMAGE, artist.image);
        intent.putParcelableArrayListExtra(TOP_TEN_LIST, tracks);

        return intent;
    }

    public void onFragmentDismiss() {
        Toast.makeText(this, "No song is playing in background", Toast.LENGTH_SHORT).show();
    }
}
