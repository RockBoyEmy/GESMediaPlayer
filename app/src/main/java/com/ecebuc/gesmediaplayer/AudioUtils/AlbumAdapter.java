package com.ecebuc.gesmediaplayer.AudioUtils;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

import com.ecebuc.gesmediaplayer.Audios.Album;
import com.ecebuc.gesmediaplayer.R;

import java.util.ArrayList;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    // The dataset
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
        holder.albumRecyclerArtView.setImageBitmap(BitmapFactory.decodeFile(currentAlbum.getAlbumArt()));
        holder.albumRecyclerTitleView.setText(currentAlbum.getAlbumName());
        holder.albumRecyclerArtistView.setText(currentAlbum.getAlbumArtist());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return albumList.size();
    }

}
