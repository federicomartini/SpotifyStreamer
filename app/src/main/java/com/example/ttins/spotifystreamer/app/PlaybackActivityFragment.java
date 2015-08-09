package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Artist;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends DialogFragment implements PlaybackService.OnPlaybackServiceListener{

    private final static String LOG_TAG = "PlaybackFragment";
    RelativeLayout mLayoutContainer;
    TextView mArtistTextView;
    TextView mAlbumTextView;
    TextView mSongTextView ;
    ImageView mAlbumImageView;
    SeekBar mTimeSeekBar;
    TextView mStartTimeTextView;
    TextView mStopTimeTextView;
    ImageButton mPrevTrackImageButton;
    static ImageButton mPlayStopTrackImageButton;
    ImageButton mNextTrackImageButton;
    private int mDuration;
    private PlaybackService mPlaybackService;
    boolean mBound;
    private int mPosition;
    private List<TrackItemList> mTracks = new ArrayList<TrackItemList>();
    private TrackItemList mTrackItemList;
    private OnFragmentClickListener mCallback;
    private boolean mIsPlay;
    private Handler durationHandler = new Handler();
    private double mTimeElapsed;
    private boolean mIsSeekBarTouched = true;;
    private boolean mIsNewTrack;
    private BroadcastReceiver mUiUpdated= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mDuration = intent.getExtras().getInt("duration");

            String sDuration = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(mDuration),
                    TimeUnit.MILLISECONDS.toSeconds(mDuration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mDuration))
            );

            mStopTimeTextView.setText(sDuration);
            mTimeSeekBar.setMax(mDuration);
            mTimeSeekBar.setProgress(mPlaybackService.getPlaybackPosition());

            if (mPlaybackService.isMediaCompleted() || mPlaybackService.isMediaPlayerPaused()) {
                mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
            }


        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;

            if (mPlaybackService.isMediaPlayerReady())
                mPlaybackService.getPlaybackServiceIntent();

            Log.d(LOG_TAG, "Bound to Playback service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound=false;
            Log.d(LOG_TAG, "Unbound to Playback service");
        }
    };

    public PlaybackActivityFragment() {
    }

    public void onStart() {
        super.onStart();


        if (null == mPlaybackService) {
            Intent intent = new Intent(getActivity(), PlaybackService.class);
            if (!getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(LOG_TAG, "Bind to Service failed");
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreateView()");

        final String START_TIME = "0:00";
        List<String> artists = new ArrayList<String>();
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        Bundle argsBundle = getArguments();

        if (null == argsBundle) {
            Log.d(LOG_TAG, "argsBundle is null");
        }

        if(null == savedInstanceState) {
            mTrackItemList = argsBundle.getParcelable("ARG_TRACK");
            mPosition = argsBundle.getInt("ARG_TRACK_POSITION");
            mTracks = argsBundle.getParcelableArrayList("ARG_TOP_TEN_LIST");
        } else {
            mTrackItemList = savedInstanceState.getParcelable("TRACK_ITEM_LIST");
            mPosition = savedInstanceState.getInt("PLAY_POSITION");
            mTracks = savedInstanceState.getParcelableArrayList("TRACK_LIST");
        }

        mLayoutContainer = (RelativeLayout) rootView.findViewById(R.id.dialog_fragment_container);
        mArtistTextView = (TextView) rootView.findViewById(R.id.playback_artist_textview);
        mAlbumTextView = (TextView) rootView.findViewById(R.id.playback_album_textview);
        mSongTextView = (TextView) rootView.findViewById(R.id.playback_song_textview);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.playback_album_imageview);
        mTimeSeekBar = (SeekBar) rootView.findViewById(R.id.playback_time_seekbar);
        mStartTimeTextView = (TextView) rootView.findViewById(R.id.start_time_textview);
        mStopTimeTextView = (TextView) rootView.findViewById(R.id.stop_time_textview);
        mPrevTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_prev_track_imagebutton);
        mPlayStopTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_playstop_track_imagebutton);
        mNextTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_next_track_imagebutton);

        artists = mTrackItemList.getArtists();
        String artistsStringList = "";

        for (String artistName: artists) {
            artistsStringList = artistsStringList + artistName + "";
        }

        mArtistTextView.setText(artistsStringList);

        mStartTimeTextView.setText(START_TIME);

        if (mTrackItemList != null) {
            mAlbumTextView.setText(mTrackItemList.getAlbumName());
            Picasso.with(getActivity()).load(mTrackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
            mSongTextView.setText(mTrackItemList.getName());
            durationHandler.postDelayed(updateSeekBarTime, 100);
        } else {
            Log.d(LOG_TAG, "TrackItemList is null");
        }

        mTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    if (mPlaybackService.isMediaPlayerReady() && fromUser) {
                        mPlaybackService.setPlaybackPosition(progress);

                        if(mPlaybackService.isMediaCompleted()) {

                            Log.d(LOG_TAG, "DEBUG :: Media completed. Restarting the track.");
                            Intent serviceIntent = new Intent(PlaybackService.ACTION_PLAY);
                            serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                            serviceIntent.setClass(getActivity(), PlaybackService.class);
                            getActivity().startService(serviceIntent);
                        }
                    }
                } catch (NullPointerException e) {
                    Log.d(LOG_TAG, "PlaybackService Null");
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mPlayStopTrackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stringAction;

                if(!mPlaybackService.isMediaCompleted())
                    mPlaybackService.setPlaybackPosition(mPlaybackService.getPlaybackPosition());
                else
                    mPlaybackService.setPlaybackPosition(0);

                if (mPlaybackService.isMediaStarted()) {
                    stringAction = PlaybackService.ACTION_STOP;
                    Log.d(LOG_TAG, "ACTION_STOP received");
                    mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    stringAction = PlaybackService.ACTION_PLAY;
                    Log.d(LOG_TAG, "ACTION_PLAY received");
                    mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_pause);
                }

                Intent serviceIntent = new Intent(stringAction);
                serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                serviceIntent.setClass(getActivity(), PlaybackService.class);
                getActivity().startService(serviceIntent);
            }
        });

        mPrevTrackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(PlaybackService.ACTION_PREV);
                mPosition = mPosition - 1;
                if (mPosition < 0) mPosition = mTracks.size() - 1;
                mTrackItemList = mTracks.get(mPosition);
                serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                serviceIntent.setClass(getActivity(), PlaybackService.class);
                getActivity().startService(serviceIntent);

                if (mTrackItemList != null) {
                    mAlbumTextView.setText(mTrackItemList.getAlbumName());
                    Picasso.with(getActivity()).load(mTrackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
                    mSongTextView.setText(mTrackItemList.getName());
                    mTimeSeekBar.setMax(mPlaybackService.getDuration());
                    mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_pause);

                } else {
                    Log.d(LOG_TAG, "TrackItemList is null");
                }
            }
        });

        mNextTrackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(PlaybackService.ACTION_NEXT);
                mPosition = mPosition + 1;
                if (mPosition >= mTracks.size()) mPosition = 0;
                mTrackItemList = mTracks.get(mPosition);
                serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                serviceIntent.setClass(getActivity(), PlaybackService.class);
                getActivity().startService(serviceIntent);

                if (mTrackItemList != null) {
                    mAlbumTextView.setText(mTrackItemList.getAlbumName());
                    Picasso.with(getActivity()).load(mTrackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
                    mSongTextView.setText(mTrackItemList.getName());
                    mTimeSeekBar.setMax(mPlaybackService.getDuration());
                    mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_pause);

                } else {
                    Log.d(LOG_TAG, "TrackItemList is null");
                }
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelable("TRACK_ITEM_LIST", mTrackItemList);
        savedInstanceState.putInt("PLAY_POSITION", mPosition);
        savedInstanceState.putParcelableArrayList("TRACK_LIST", (ArrayList<TrackItemList>) mTracks);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mUiUpdated, new IntentFilter("DURATION_UPDATED"));

        super.onResume();

        if (getDialog() != null) {
            int height = (int) getResources().getDimension(R.dimen.fragment_container_height);
            int width = (int) getResources().getDimension(R.dimen.fragment_container_width);
            mLayoutContainer.getLayoutParams().height = height;
            mLayoutContainer.getLayoutParams().width = width;

            height = (int) getResources().getDimension(R.dimen.dialog_container_height);
            width = (int) getResources().getDimension(R.dimen.dialog_container_width);
            getDialog().getWindow().setLayout(width, height);
        }

    }

    private Runnable updateSeekBarTime = new Runnable() {
        @Override
        public void run() {

            try {
                if (null != mPlaybackService ) {
                    if (!mPlaybackService.isMediaCompleted() && !mPlaybackService.isMediaPlayerPaused()) {
                        mTimeElapsed = mPlaybackService.getPlaybackPosition();
                        mTimeSeekBar.setProgress((int) mTimeElapsed);
                    }
                }
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "mediaPlayer not ready (null)");
            }

            durationHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onPause() {
        try {
            if (mUiUpdated != null)
                getActivity().unregisterReceiver(mUiUpdated);
            Log.d(LOG_TAG, "onPause(): unregister Receiver");
        }
        catch (IllegalArgumentException e) {
            Log.d(LOG_TAG, "Receiver not registered to Broadcast event");
        }
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnFragmentClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnFragmentClickListener {

    }

    @Override
    public void onMediaCompleted() {
        if (mPlayStopTrackImageButton != null) {
            mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
            Log.d(LOG_TAG, "onMediaCompleted called");
        } else {
            Log.d(LOG_TAG, "onMediaCompleted found null button");
        }
    }

    @Override
    public void onMediaPlaying() {
        if (mPlayStopTrackImageButton != null) {
            mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_pause);
            Log.d(LOG_TAG, "onMediaPlaying called");
        } else {
            Log.d(LOG_TAG, "onMediaPlaying found null button");
        }
    }

}