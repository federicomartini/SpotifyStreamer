package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import kaaes.spotify.webapi.android.models.Track;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TopTenFragment.OnTopTenFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TopTenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TopTenFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_ARTIST_NAME = "ARG_ARTIST_NAME";
    private static final String ARG_ARTIST_IMAGE = "ARG_ARTIST_IMAGE";
    private static final String ARG_TOP_TEN_LIST = "ARG_TOP_TEN_LIST";
    private final static String LIST_KEY = "PARCEABLE_LIST_KEY";
    private final static String LOG_TAG = "TopTenActivity_Fragment";
    private OnTopTenFragmentInteractionListener mCallback;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    TopTenListAdapter mTopTenArrayAdapter;
    List<TrackItemList> mTracks = new ArrayList<>();
    ImageView mImageViewTop;
    TextView mTextViewTopArtist;
    RelativeLayout mRelativeLayoutTitle;
    String mArtistName;
    String mArtistImage;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TopTenFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TopTenFragment newInstance(String param1, String param2) {
        TopTenFragment fragment = new TopTenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TopTenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_activity, false);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mArtistName = getArguments().getString(ARG_ARTIST_NAME);
        mArtistImage = getArguments().getString(ARG_ARTIST_IMAGE);
        mTracks = getArguments().getParcelableArrayList(ARG_TOP_TEN_LIST);

        if (mArtistImage == null || mArtistName == null || mTracks == null)
            Log.d(LOG_TAG, "Some arguments are null");

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);

        saveInstanceState.putParcelableArrayList(LIST_KEY, (ArrayList<TrackItemList>) mTracks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_top_ten, container, false);

        /* Take references to views */
        mImageViewTop = (ImageView) rootView.findViewById(R.id.imageview_top_artist_topten);
        mTextViewTopArtist = (TextView) rootView.findViewById(R.id.textview_top_title_topten);
        mRelativeLayoutTitle = (RelativeLayout) rootView.findViewById(R.id.layout_top_topten_activity);

        /* Preparing the target BMP for Picasso framework */
        Target target;
        target = makeBitmapTarget();

        if (mArtistImage != null) {
            Picasso.with(getActivity()).load(mArtistImage).into(target);
        }

        /* Blur image on Image View at top of activity */
        blurImageView(mImageViewTop, mArtistImage);

        /* Setting Artist name as Title of the Top side of the activity */
        mTextViewTopArtist.setText(mArtistName);

        ListView topTenListView = (ListView) rootView.findViewById(R.id.listView_topTen);
        mTopTenArrayAdapter = new TopTenListAdapter(getActivity(), R.layout.list_item_topten, new ArrayList<TrackItemList>());
        topTenListView.setAdapter(mTopTenArrayAdapter);

        //if there are no saved instances (e.g. configuration change) server has to be queried
        if (null == savedInstanceState) {

            showTopTenList(mTracks);
        } else {

            mTracks = savedInstanceState.getParcelableArrayList(LIST_KEY);
            showTopTenList(mTracks);
        }

        /* Click handlers */
        topTenListItemClick(topTenListView);

        return rootView;
    }

    public void topTenListItemClick(ListView topTenListView){
        topTenListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TrackItemList trackItemList = mTopTenArrayAdapter.getItem(position);

                if (trackItemList != null) {
                    mCallback.onTopTenFragmentItemClick(trackItemList);
                } else {
                    Log.d(LOG_TAG, "track list null on callback");
                }


            }
        });
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
                    mTopTenArrayAdapter.add(new TrackItemList(track.getArtists(), track.getAlbumName(), track.getName(), null, null, track.getId(), track.getTrackPreview_url()));
                else
                    mTopTenArrayAdapter.add(new TrackItemList(track.getArtists(), track.getAlbumName(), track.getName(), track.getImage(), track.getAlbumImages(), track.getId(), track.getTrackPreview_url()));

            }

        } catch (NullPointerException e) {
            Log.d(LOG_TAG, "mTopTenArrayAdapter is null");
        }

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

    /* Blur Relative Layout of Top Ten Activity */
    protected void blurImageView(ImageView imageView, String artistImage) {

        final String localArtistImage = artistImage;

        imageView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Picasso.with(getActivity()).load(localArtistImage).into(new Target() {
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

        RenderScript renderScript = RenderScript.create(getActivity());

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


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnTopTenFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTopTenFragmentInteractionListener {
        void onTopTenFragmentItemClick(TrackItemList trackItemList);
    }

}
