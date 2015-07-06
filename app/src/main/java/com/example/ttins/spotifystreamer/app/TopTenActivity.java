package com.example.ttins.spotifystreamer.app;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.utils.RoundImage;
import com.example.ttins.spotifystreamer.app.utils.TopTenListAdapter;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class TopTenActivity extends ActionBarActivity {

    private final static String LOG_TAG = "TopTenActivity";
    TopTenListAdapter mTopTenArrayAdapter;
    List<TrackItemList> mTracks = new ArrayList<>();
    ImageView mImageViewTop;
    TextView mTextViewTopArtist;
    RelativeLayout mRelativeLayoutTitle;

    private final static String LIST_KEY = "PARCEABLE_LIST_KEY";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_ten_activity, menu);
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
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference_activity, false);

        Target target;
        String artistName = getIntent().getStringExtra("INTENT_ARTIST_NAME");
        final String artistImage = getIntent().getStringExtra("INTENT_ARTIST_IMAGE");
        mTracks = getIntent().getParcelableArrayListExtra("TOP_TEN_LIST");

        /* Toolbar handler */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarTopTen);
        setToolbar(toolbar);
        toolbar.setSubtitle(artistName);
        toolbar.setTitle(R.string.topten_activity_name);

        /* Take references to views */
        mImageViewTop = (ImageView) findViewById(R.id.imageview_top_artist_topten);
        mTextViewTopArtist =(TextView) findViewById(R.id.textview_top_title_topten);
        mRelativeLayoutTitle = (RelativeLayout) findViewById(R.id.layout_top_topten_activity);

        /* Preparing the target BMP for Picasso framework */
        target = makeBitmapTarget();

        if (artistImage != null) {
            Picasso.with(this).load(artistImage).into(target);
        }

        /* Blur image on Image View at top of activity */
        blurImageView(mImageViewTop, artistImage);

        /* Setting Artist name as Title of the Top side of the activity */
        mTextViewTopArtist.setText(artistName);

        ListView topTenListView = (ListView) findViewById(R.id.listView_topTen);
        mTopTenArrayAdapter = new TopTenListAdapter(this, R.layout.list_item_topten, new ArrayList<TrackItemList>());
        topTenListView.setAdapter(mTopTenArrayAdapter);

        //if there are no saved instances (e.g. configuration change) server has to be queried
        if (null == saveInstanceState) {

            showTopTenList(mTracks);
        } else {

            mTracks = saveInstanceState.getParcelableArrayList(LIST_KEY);
            showTopTenList(mTracks);
        }

    }

    /* Blur Relative Layout of Top Ten Activity */
    protected void blurImageView(ImageView imageView, String artistImage) {

        final String localArtistImage = artistImage;

        imageView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Picasso.with(getParent()).load(localArtistImage).into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            BitmapDrawable bluredBitmap = new BitmapDrawable(createBitmap_ScriptIntrinsicBlur(bitmap, 25.0f));

                            if (Build.VERSION.SDK_INT >= 16)
                                mRelativeLayoutTitle.setBackground(bluredBitmap);
                            else
                                mRelativeLayoutTitle.setBackgroundDrawable(bluredBitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
                } catch (Exception e) {
                    Log.d(LOG_TAG, "Bluring error - " + e.getMessage());
                }


            }
        });
    }

    /* Picasso Target builder for BMP elaboration */
    private Target makeBitmapTarget() {
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                RoundImage roundImage = new RoundImage(bitmap);
                mImageViewTop.setImageDrawable(roundImage);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        return target;
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

    /* Populate the array list adapter of Top Ten Tracks */
    private void showTopTenList(List<TrackItemList> tracks) {

        try {
            mTopTenArrayAdapter.clear();

            for (TrackItemList track : tracks) {

                Log.d("Track AlbumName:", track.getAlbumName());
                Log.d("Track name:", track.getName());
                Log.d("Track image:", track.getImage());

                if (track.getImage().equals(""))
                    mTopTenArrayAdapter.add(new TrackItemList(track.getAlbumName(), track.getName(), null, null, track.getId(), track.getTrackPreview_url()));
                else
                    mTopTenArrayAdapter.add(new TrackItemList(track.getAlbumName(), track.getName(), track.getImage(), track.getAlbumImages(), track.getId(), track.getTrackPreview_url()));

            }

        } catch (NullPointerException e) {
            Log.d(LOG_TAG, "mTopTenArrayAdapter is null");
        }

    }

    /* Bluring image */
    private Bitmap createBitmap_ScriptIntrinsicBlur(Bitmap src, float r) {

        //Radius range (0 < r <= 25)
        if(r <= 0){
            r = 0.1f;
        }else if(r > 25){
            r = 25.0f;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(this);

        Allocation blurInput = Allocation.createFromBitmap(renderScript, src);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(r);
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();
        return bitmap;
    }

}