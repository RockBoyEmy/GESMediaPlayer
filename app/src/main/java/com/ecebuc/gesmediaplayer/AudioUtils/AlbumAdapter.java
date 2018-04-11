package com.ecebuc.gesmediaplayer.AudioUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

import com.ecebuc.gesmediaplayer.Audios.Album;
import com.ecebuc.gesmediaplayer.R;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    // The data set
    private ArrayList<Album> albumList;

    //Provide a reference to the views for each data item
    //Complex data items may need more than one view per item, and
    //you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView albumRecyclerArtView;
        public TextView albumRecyclerTitleView, albumRecyclerArtistView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.albumRecyclerArtView = (ImageView) itemView.findViewById(R.id.album_item_albumCover);
            this.albumRecyclerTitleView = (TextView) itemView.findViewById(R.id.album_item_albumName);
            this.albumRecyclerArtistView = (TextView) itemView.findViewById(R.id.album_item_artistName);
        }
    }

    // Constructor
    public AlbumAdapter(ArrayList<Album> albumList){
        this.albumList = albumList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AlbumAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItemView = layoutInflater.inflate(R.layout.album_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItemView);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Album currentAlbum = albumList.get(position);
        holder.albumRecyclerTitleView.setText(currentAlbum.getAlbumName());
        holder.albumRecyclerArtistView.setText(currentAlbum.getAlbumArtist());
        if(currentAlbum.getAlbumArt() != null){
            holder.albumRecyclerArtView.setImageBitmap(BitmapFactory.decodeFile(currentAlbum.getAlbumArt()));
        }
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return albumList.size();
    }
}









/*public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private Cursor mCursor;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView albumRecyclerArtView;
        public TextView albumRecyclerTitleView, albumRecyclerArtistView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.albumRecyclerArtView = (ImageView) itemView.findViewById(R.id.album_item_albumCover);
            this.albumRecyclerTitleView = (TextView) itemView.findViewById(R.id.album_item_albumName);
            this.albumRecyclerArtistView = (TextView) itemView.findViewById(R.id.album_item_artistName);
        }
    }

    public AlbumAdapter(Context context, Cursor cursor) {
        mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mCursor = cursor;
    }

    public void setData(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    public Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        this.mCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public AlbumAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View listItemView = mInflater.inflate(R.layout.album_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItemView);
        return viewHolder;
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String albumTitle, albumArtist, albumArt;

        if (mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                albumTitle = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                albumArtist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                albumArt = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                holder.albumRecyclerTitleView.setText(albumTitle);
                holder.albumRecyclerArtistView.setText(albumArtist);
                if(albumArt != null){
                    holder.albumRecyclerArtView.setImageBitmap(BitmapFactory.decodeFile(albumArt));
                }
            }
        }
        else {
            Log.e ("onBindViewHolder: ", "Cursor is null.");
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return -1;
        }
    }
}*/
