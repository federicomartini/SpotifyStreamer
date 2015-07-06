package com.example.ttins.spotifystreamer.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

public class TopTenListAdapter extends ArrayAdapter<TrackItemList> {

    Context mContext;
    int mResource;
    List<TrackItemList> mTrackItemList;
    TrackHolder holder = null;

    public TopTenListAdapter(Context context, int resource, List<TrackItemList> objects) {
        super(context, resource, objects);

        mContext = context;
        mResource = resource;
        mTrackItemList = objects;

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View itemView = convertView;

        if (null == convertView) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            itemView = inflater.inflate(mResource, parent, false);

            holder = new TrackHolder();
            holder.imageViewPreview = (ImageView) itemView.findViewById(R.id.list_item_top_ten_imageView);
            holder.textViewAlbumName = (TextView) itemView.findViewById(R.id.list_item_top_ten_album_textView);
            holder.textViewName = (TextView) itemView.findViewById(R.id.list_item_top_ten_textView);

            itemView.setTag(holder);

        }
        else {

            holder = (TrackHolder)itemView.getTag();

        }


        TrackItemList trackItem = getItem(position);
        String albumName;

        if(trackItem.getAlbumName().length() > 40)
            albumName = trackItem.getAlbumName().substring(0, 37).concat("...");
        else
            albumName = trackItem.getAlbumName();

        holder.textViewAlbumName.setText(albumName);
        holder.textViewName.setText(trackItem.getName());

        if (trackItem.getAlbumImages() != null) {
            int pos = trackItem.getAlbumImages().size() - 1;
            String trackName = trackItem.getAlbumImages().get(pos);

            if (null != trackName) {
                Picasso.with(mContext).load(trackName).into(holder);
            }
            else {
                Picasso.with(mContext).load(R.drawable.unknown_artist).into(holder);
            }
        }

        return itemView;
    }


    static class TrackHolder implements Target{

        TextView textViewAlbumName;
        TextView textViewName;
        ImageView imageViewPreview;

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            RoundImage roundImage = new RoundImage(bitmap);
            imageViewPreview.setImageDrawable(roundImage);

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }

    }
}
