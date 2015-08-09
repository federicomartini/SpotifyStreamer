package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.MyArtist;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.OnArtistSelectedListener, TopTenFragment.OnTopTenFragmentInteractionListener, PlaybackActivityFragment.OnFragmentClickListener {

    private boolean mTwoPane;
    private List<TrackItemList> mTracks = new ArrayList<TrackItemList>();
    Fragment mTopTenFragment;

    @Override
    public void onArtistSelected(MyArtist artist, ArrayList<TrackItemList> tracks) {
        //TODO: Manage the artist selection behavior
        if (mTwoPane){
            mTopTenFragment = new TopTenFragment();
            Bundle argsBundle = new Bundle();
            mTracks = tracks;
            argsBundle.putString("ARG_ARTIST_NAME", artist.name);
            argsBundle.putString("ARG_ARTIST_IMAGE", artist.image);
            argsBundle.putParcelableArrayList("ARG_TOP_TEN_LIST", tracks);

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

        /* Starting Playback Service */
        Intent playbackServiceIntent = new Intent(PlaybackService.ACTION_PLAY);
        playbackServiceIntent.putExtra("INTENT_PREVIEW_URL", trackItemList.getTrackPreview_url());
        /*playbackServiceIntent.putExtra("INTENT_TRACK_POSITION", position);
        playbackServiceIntent.putParcelableArrayListExtra("INTENT_TOP_TEN_LIST", (ArrayList<TrackItemList>) mTracks);*/
        playbackServiceIntent.setClass(this, PlaybackService.class);
        startService(playbackServiceIntent);

        if (mTwoPane) {
            PlaybackActivityFragment playbackActivityFragment = new PlaybackActivityFragment();
            FragmentManager fragmentManager = getFragmentManager();
            Bundle bundleFragment = new Bundle();

            bundleFragment.putParcelable("ARG_TRACK", trackItemList);
            bundleFragment.putParcelableArrayList("ARG_TOP_TEN_LIST", (ArrayList<TrackItemList>) mTracks);
            bundleFragment.putInt("ARG_TRACK_POSITION", position);

            playbackActivityFragment.setArguments(bundleFragment);
            playbackActivityFragment.show(fragmentManager, "Play Now");

            /*FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            PlaybackActivityFragment playbackFragment = new PlaybackActivityFragment();

            fragmentTransaction.addToBackStack(null);

            fragmentTransaction.add(R.id.playback_container, playbackFragment).commit();*/

        } else {
            /* Starting Playback UI */
            bundle.putParcelable("BUNDLE_TRACK", trackItemList);
            bundle.putInt("INTENT_TRACK_POSITION", position);
            bundle.putParcelableArrayList("INTENT_TOP_TEN_LIST", (ArrayList<TrackItemList>) mTracks);
            intent.putExtra("INTENT_TRACK_BUNDLE", bundle);
            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null != savedInstanceState) {
            mTracks = savedInstanceState.getParcelableArrayList("TRACK_LIST");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);

        saveInstanceState.putParcelableArrayList("TRACK_LIST", (ArrayList<TrackItemList>) mTracks);


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
        intent.putExtra("INTENT_ARTIST_NAME", artist.name);
        intent.putExtra("INTENT_ARTIST_IMAGE", artist.image);
        intent.putParcelableArrayListExtra("TOP_TEN_LIST", tracks);

        return intent;
    }


}
