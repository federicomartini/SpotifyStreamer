package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.example.ttins.spotifystreamer.app.utils.KeyboardUtil;
import com.example.ttins.spotifystreamer.app.utils.MyArtist;
import com.example.ttins.spotifystreamer.app.utils.ResultListAdapter;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    ResultListAdapter mResultListAdapter;
    ListView mListView;
    EditText mEditText;
    List<Artist> mResultListArtist;
    List<TrackItemList> mTracks = new ArrayList<>();
    private long mLastClickTime = 0; // variable to track event time
    OnArtistSelectedListener mCallback;

    private static final String LOG_TAG = "MainActivity";

    /**** Callbacks ****/
    public interface OnArtistSelectedListener {
        void onArtistSelected(MyArtist artist, ArrayList<TrackItemList> tracks);
    }


    /**** Methods ****/

    public MainActivityFragment() {
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        try{
            mCallback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement OnArtistSelectedListener interface.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onPause();

        setRetainInstance(true);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_activity, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Calling Super method for backward compatibility
        super.onCreateView(inflater, container, savedInstanceState);

        //Loading and declaring UI fragment components
        View viewRoot = inflater.inflate(R.layout.fragment_main, container, false);

        //Views references
        Button buttonClearText = (Button) viewRoot.findViewById(R.id.button_clear_search);
        mEditText = (EditText) viewRoot.findViewById(R.id.edittext_search_artist);
        mListView = (ListView) viewRoot.findViewById(R.id.listView_artist);
        mResultListAdapter = new ResultListAdapter(getActivity(), R.layout.list_item_artist, new ArrayList<MyArtist>());
        mListView.setAdapter(mResultListAdapter);

        /**** Click Handlers ****/
        /* Clear Edit Text */
        buttonClearClick(buttonClearText);

        /* Handling soft keyboard Enter Button click */
        searchTextEnterClick(mEditText);

        /* Artist list item click */
        artistListItemClick(mListView, mTracks);

        //Reload artist result list if present
        if (null != savedInstanceState && mResultListArtist != null)
            showArtists(mResultListArtist);

        return viewRoot;
    }

    /* Artist Item click handler */
    protected void artistListItemClick(ListView listView, List<TrackItemList> trackItemList) {

        final List<TrackItemList> localTrackItemList = trackItemList;

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                final MyArtist artist = mResultListAdapter.getItem(position);
                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                Map<String, Object> queryMap = new HashMap<String, Object>();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String language = sharedPref.getString(getString(R.string.pref_list_lang_key), getString(R.string.pref_list_lang_default_value));
                queryMap.put("country", language);
                queryMap.put("limit", "10");

                /* Spotify Top Tracks handler */
                spotifyService.getArtistTopTrack(artist.id, queryMap, new Callback<Tracks>() {
                    @Override
                    public void success(final Tracks tracks, Response response) {

                        localTrackItemList.clear();

                        if (null == tracks || tracks.tracks.size() == 0) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "Top Ten List not available. Please, refine the search.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else {
                            /* Populating Top Ten Track list */
                            populateTopTenTracks(tracks.tracks);

                            // Preventing multiple clicks, using threshold of 2 seconds
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 2000) {
                                return;
                            }

                            mLastClickTime = SystemClock.elapsedRealtime();

                            /* Start TopTenActivity */
                            //startActivity(makeTopTenIntent(artist));

                            /* Passing info to parent activity */
                            mCallback.onArtistSelected(artist, (ArrayList<TrackItemList>) mTracks);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d("TopTen failure", error.toString());
                    }
                });
            }
        });
    }

    /* Search Button click handler (Enter button of the Soft Keyboard) */
    protected void searchTextEnterClick(EditText editText) {

        final EditText localEditText = editText;

        localEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                // Preventing multiple clicks, using threshold of 2 seconds
                if (SystemClock.elapsedRealtime() - mLastClickTime < 2000) {
                    return false;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    SpotifyApi api = new SpotifyApi();
                    SpotifyService spotify = api.getService();

                    KeyboardUtil.hideKeyboard(getActivity());

                    spotify.searchArtists(localEditText.getText().toString(), new Callback<ArtistsPager>() {
                        @Override
                        public void success(ArtistsPager artistsPager, Response response) {
                            mResultListArtist = artistsPager.artists.items;

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showArtists(mResultListArtist);
                                }
                            });
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.d("Artist failure", error.toString());
                        }
                    });
                    return true;
                }

                return false;
            }
        });
    }

    /* Clear button click handler */
    protected void buttonClearClick(Button button) {
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Preventing multiple clicks, using threshold of 2 seconds
                if (SystemClock.elapsedRealtime() - mLastClickTime < 2000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                mEditText.setText("");
                InputMethodManager inputMgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMgr.toggleSoftInput(0, 0);
            }
        });
    }

    /* Populate Top Ten Tracks list */
    private void populateTopTenTracks(List<Track> tracks) {
        for (Track element : tracks) {
            TrackItemList trackItemList = new TrackItemList(getTrackArtists(element),
                    element.album.name,
                    element.name,
                    element.album.images.get(0).url,
                    getAlbumImages(element.album.images),
                    element.id,
                    element.preview_url
            );

            mTracks.add(trackItemList);
        }
    }

    private List<String> getTrackArtists(Track track) {
        List<String> artists = new ArrayList<String>();

        /* Avoid processing null track parameter */
        if (null == track)
            return null;

        for (ArtistSimple artist: track.artists) {
            artists.add(artist.name);
        }

        return artists;
    }


    /* Populating Artists list for local mResultListAdapter */
    private void showArtists(List<Artist> artists) {

        int pos;
        String imagePath;

        mResultListAdapter.clear();

        if (artists.size() == 0)
        {
            Toast.makeText(getActivity(), "Artist not found. Please, refine your search.", Toast.LENGTH_SHORT).show();
        }

        for (Artist element : artists) {

            Log.d("Name", element.name);

            if (!element.images.isEmpty()) {
                List<String> images = getAlbumImages(element.images);
                pos = images.size() - 1;
                imagePath = element.images.get(pos).url;
                mResultListAdapter.add(new MyArtist(element.name, imagePath, element.id, element.followers.total, element.popularity));
                Log.d("Image URL:", imagePath);
            } else {
                mResultListAdapter.add(new MyArtist(element.name, null, element.id, element.followers.total, element.popularity));
            }
        }
    }

    /* Return a list of strings about images to be used by the APP */
    private List<String> getAlbumImages(List<Image> images) {

        List<String> albumImages = new ArrayList<>();
        int thumbSize = Integer.MAX_VALUE;
        int smallSize = 0;
        String thumbImage = null;
        String smallImage = null;

        /* If there are no images return null */
        if (null == images || images.size() == 0)
            return null;

        /* If there is only one image there's no need to parse the list */
        if(images.size() == 1) {
            albumImages.add(images.get(0).url);
            return albumImages;
        }

        /* Parsing list of Images looking for 640px and 200px sizes */
        for(Image image: images) {
            if (image.height >= 640 && image.height <= thumbSize)
                thumbImage = image.url;
            else if (image.height >= smallSize && image.height <= 200) {
                smallImage = image.url;
            }
        }

        /* If no url images are stored I can store the first one as biggest considering the
        * descendant order */
        if (thumbImage == null)
            albumImages.add(images.get(0).url);
        else
            albumImages.add(thumbImage);

        /* If no url images are stored I can store the last one as smallest considering the
        * descendant order */
        if (smallImage == null)
            albumImages.add(images.get(images.size() - 1).url);
        else
            albumImages.add(smallImage);

        return albumImages;
    }


}




