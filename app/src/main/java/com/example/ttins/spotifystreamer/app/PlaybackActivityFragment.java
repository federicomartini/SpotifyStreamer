package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends DialogFragment implements PlaybackService.OnPlaybackServiceListener{

    private final static String LOG_TAG = "PlaybackFragment";
    TextView mArtistTextView;
    TextView mAlbumTextView;
    TextView mSongTextView ;
    ImageView mAlbumImageView;
    SeekBar mTimeSeekBar;
    ImageButton mPrevTrackImageButton;
    ImageButton mPlayStopTrackImageButton;
    ImageButton mNextTrackImageButton;
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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;
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
            if (false == getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(LOG_TAG, "Bind to Service failed");
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        Bundle argsBundle = getArguments();

        if (null == argsBundle) {
            Log.d(LOG_TAG, "argsBundle is null");
        }

        mTrackItemList = argsBundle.getParcelable("ARG_TRACK");
        mPosition = argsBundle.getInt("ARG_TRACK_POSITION");
        mTracks = argsBundle.getParcelableArrayList("ARG_TOP_TEN_LIST");

        mArtistTextView = (TextView) rootView.findViewById(R.id.playback_artist_textview);
        mAlbumTextView = (TextView) rootView.findViewById(R.id.playback_album_textview);
        mSongTextView = (TextView) rootView.findViewById(R.id.playback_song_textview);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.playback_album_imageview);
        mTimeSeekBar = (SeekBar) rootView.findViewById(R.id.playback_time_seekbar);
        mPrevTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_prev_track_imagebutton);
        mPlayStopTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_playstop_track_imagebutton);
        mNextTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_next_track_imagebutton);

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

                        if(mPlaybackService.onMediaCompleted()) {

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

                mPlaybackService.setPlaybackPosition(mPlaybackService.getPlaybackPosition());

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
                if (mPosition < 0) mPosition = 9;
                mTrackItemList = mTracks.get(mPosition);
                serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                serviceIntent.setClass(getActivity(), PlaybackService.class);
                getActivity().startService(serviceIntent);

                if (mTrackItemList != null) {
                    mAlbumTextView.setText(mTrackItemList.getAlbumName());
                    Picasso.with(getActivity()).load(mTrackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
                    mSongTextView.setText(mTrackItemList.getName());
                    mTimeSeekBar.setMax(mPlaybackService.getDuration());

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
                if (mPosition >= 10) mPosition = 0;
                mTrackItemList = mTracks.get(mPosition);
                serviceIntent.putExtra("INTENT_PREVIEW_URL", mTrackItemList.getTrackPreview_url());
                serviceIntent.setClass(getActivity(), PlaybackService.class);
                getActivity().startService(serviceIntent);

                if (mTrackItemList != null) {
                    mAlbumTextView.setText(mTrackItemList.getAlbumName());
                    Picasso.with(getActivity()).load(mTrackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
                    mSongTextView.setText(mTrackItemList.getName());
                    mTimeSeekBar.setMax(mPlaybackService.getDuration());

                } else {
                    Log.d(LOG_TAG, "TrackItemList is null");
                }
            }
        });

        return rootView;
    }

    private Runnable updateSeekBarTime = new Runnable() {
        @Override
        public void run() {

            try {
                if(mPlaybackService.isMediaPlayerReady())
                    mTimeSeekBar.setMax(mPlaybackService.getDuration());

                if (null != mPlaybackService) {
                    mTimeElapsed = mPlaybackService.getPlaybackPosition();
                    mTimeSeekBar.setProgress((int) mTimeElapsed);

                }
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "mediaPlayer not ready (null)");
            }

            durationHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onPause() {
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

    public interface OnFragmentClickListener {

    }

    @Override
    public void onMediaPlay() {
        mIsPlay=true;
    }

}
