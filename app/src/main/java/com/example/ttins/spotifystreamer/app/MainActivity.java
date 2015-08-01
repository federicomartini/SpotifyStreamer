package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.ttins.spotifystreamer.app.utils.MyArtist;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.OnArtistSelectedListener, TopTenFragment.OnTopTenFragmentInteractionListener {

    private boolean mTwoPane;
    Fragment mTopTenFragment;

    @Override
    public void onTopTenFragmentItemClick(TrackItemList trackItemList){

    }

    @Override
    public void onArtistSelected(MyArtist artist, ArrayList<TrackItemList> tracks) {
        //TODO: Manage the artist selection behavior
        if (mTwoPane){
            mTopTenFragment = new TopTenFragment();
            Bundle argsBundle = new Bundle();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
