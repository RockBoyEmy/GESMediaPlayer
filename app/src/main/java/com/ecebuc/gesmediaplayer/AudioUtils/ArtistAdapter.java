package com.ecebuc.gesmediaplayer.AudioUtils;

import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ecebuc.gesmediaplayer.Audios.Album;
import com.ecebuc.gesmediaplayer.Audios.Artist;
import com.ecebuc.gesmediaplayer.R;

import java.util.ArrayList;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    //the dataset
    private ArrayList<Artist> artistList;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView artistRecyclerNameView, artistRecyclerAlbumsNumber, artistRecyclerTracksNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            this.artistRecyclerNameView = (TextView) itemView.findViewById(R.id.artist_item_artistName);
            this.artistRecyclerAlbumsNumber = (TextView) itemView.findViewById(R.id.artist_item_albumsNumber);
            this.artistRecyclerTracksNumber = (TextView) itemView.findViewById(R.id.artist_item_tracksNumber);
        }
    }

    // Constructor
    public ArtistAdapter(ArrayList<Artist> artistList){
        this.artistList = artistList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ArtistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItemView = layoutInflater.inflate(R.layout.artist_list_item, parent, false);
        ArtistAdapter.ViewHolder viewHolder = new ArtistAdapter.ViewHolder(listItemView);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ArtistAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Artist currentArtist = artistList.get(position);
        String albumsNumber = currentArtist.getArtistAlbumsNo();
        String tracksNumber = currentArtist.getArtistTracksNo();
        albumsNumber = albumsNumber.concat(" albums");
        tracksNumber = tracksNumber.concat(" total tracks");
        holder.artistRecyclerNameView.setText(currentArtist.getArtistName());
        holder.artistRecyclerAlbumsNumber.setText(albumsNumber);
        holder.artistRecyclerTracksNumber.setText(tracksNumber);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return artistList.size();
    }
}
