package com.example.ttins.spotifystreamer.app;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.ArrayList;
import java.util.List;

public class TopTenActivity extends ActionBarActivity implements TopTenFragment.OnTopTenFragmentInteractionListener {

    private final static String LOG_TAG = "TopTenActivity";
    private MenuItem mPlayActionButton;
    List<TrackItemList> mTracks = new ArrayList<>();
    public static final String ACTION_NOW_PLAY = "com.example.ttins.spotifystreamer.MainActivity.INTENT_NOW_PLAYING_TRACK";

    private final static String LIST_KEY = "PARCEABLE_LIST_KEY";

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