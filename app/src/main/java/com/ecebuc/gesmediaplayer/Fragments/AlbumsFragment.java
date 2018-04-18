package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.AlbumAdapter;
import com.ecebuc.gesmediaplayer.Audios.Album;
import com.ecebuc.gesmediaplayer.R;
import com.ecebuc.gesmediaplayer.Utils.RecyclerTouchListener;

import java.util.ArrayList;

public class AlbumsFragment extends Fragment {

    private OnAlbumFragmentInteractionListener albumFragmentCallback;

    private RecyclerView albumRecyclerView;
    private RecyclerView.Adapter albumRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private ArrayList<Album> albumList = new ArrayList<>();
    private AlbumLoaderAsync albumLoader;

    private final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    private String fragmentTitle;

    private final String fromArtistIDParamTAG = "Artist ID";
    private boolean paramIsFragmentDefault;
    private String paramArtistID;

    public AlbumsFragment() {
        // Required empty public constructor
    }
    public static AlbumsFragment newInstance(String param1, String param2) {
        AlbumsFragment fragment = new AlbumsFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAlbumFragmentInteractionListener) {
            albumFragmentCallback = (OnAlbumFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAlbumFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        paramIsFragmentDefault = args.getBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);

        if(paramIsFragmentDefault){
            //default behavior - display all albums
            //will be done in onCreateView
            fragmentTitle = "Albums";
        } else {
            //fragment was created with arguments - handle accordingly
            paramArtistID = args.getString(fromArtistIDParamTAG);
            if(paramArtistID == null){
                Log.e("AlbumFrag onCreate: ", "error - paramArtistID is null");
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        albumRecyclerView = (RecyclerView) rootView.findViewById(R.id.albums_recycler_view);
        albumRecyclerView.setHasFixedSize(true);

        recyclerLayoutManager = new GridLayoutManager(getActivity(), 2);
        albumRecyclerView.setLayoutManager(recyclerLayoutManager);

        albumRecyclerAdapter = new AlbumAdapter(albumList);
        albumRecyclerView.setAdapter(albumRecyclerAdapter);

        //here goes the AsyncTask
        if(paramIsFragmentDefault){
            //fragment is new, load all albums asynchronously
            albumLoader = new AlbumLoaderAsync(albumRecyclerView.getAdapter());
            initAlbumAsyncTask(albumLoader);
        }
        else{
            //albumList has albums in it, means fragment comes from an artist
            initAlbumListFromId(paramArtistID);
        }

        albumRecyclerView.setItemAnimator(new DefaultItemAnimator());
        albumRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), albumRecyclerView, new RecyclerTouchListener.ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        Album selectedAlbum = albumList.get(position);
                        String albumId = selectedAlbum.getAlbumId();
                        albumFragmentCallback.startSongsFragmentFromId(albumId);
                        //Toast.makeText(getContext(), selectedAlbum.getAlbumName() + " is selected!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        Toast.makeText(getContext(), "Long selected", Toast.LENGTH_SHORT).show();
                        //could implement the popup menu in here
                    }
                }));

        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        albumFragmentCallback.updateToolbarTitleForFragment(fragmentTitle);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();

        if(albumLoader != null && !albumLoader.isCancelled()){
            albumLoader.cancel(true);
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        albumFragmentCallback = null;
    }

    public interface OnAlbumFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
        void startSongsFragmentFromId(String albumId);
    }

    private void initAlbumListFromId(String id){
        ContentResolver contentResolver = getActivity().getContentResolver();
        String albumId, albumTitle, albumArtist, albumArt;
        long artistId = Long.parseLong(id);

        Uri albumUri = MediaStore.Audio.Artists.Albums.getContentUri("external", artistId);
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";

        final String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART};


        final Cursor cursor = contentResolver.query(
                albumUri,
                ALBUM_SUMMARY_PROJECTION,
                null, null,
                sortOrder);


        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                albumTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                albumArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                // Save to albumList
                albumList.add(new Album(albumId, albumTitle, albumArtist, albumArt));
                albumRecyclerView.getAdapter().notifyItemInserted(cursor.getPosition());
            }
            //set the fragment name to the artist of which we are displaying the albums
            cursor.moveToPrevious();
            fragmentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
        }
        cursor.close();
    }
    private void initAlbumAsyncTask(AlbumLoaderAsync albumLoader){
        ContentResolver contentResolver = getActivity().getContentResolver();
        final String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART};

        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";
        final Cursor cursor = contentResolver.query(
                albumUri,
                ALBUM_SUMMARY_PROJECTION,
                null, null,
                sortOrder);

        //begin album load asynchronously
        albumLoader.execute(cursor);
    }

    private class AlbumLoaderAsync extends AsyncTask<Cursor, Album, Void> {
        RecyclerView.Adapter recyclerAdapter;
        ContentResolver contentResolver;
        String albumId, albumTitle, albumArtist, albumArt;
        int position;

        public AlbumLoaderAsync(RecyclerView.Adapter adapter) {
            recyclerAdapter = adapter;
            contentResolver = getActivity().getContentResolver();
            position = -1;
        }

        @Override
        protected Void doInBackground(Cursor... passedCursor) {

            if (passedCursor[0] != null && passedCursor[0].getCount() > 0) {
                while (passedCursor[0].moveToNext()) {
                    albumId = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Albums._ID));
                    albumTitle = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    albumArtist = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                    albumArt = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                    position++;
                    publishProgress(new Album(albumId, albumTitle, albumArtist, albumArt));

                    //periodically check if a cancel command was given, to exit rapidly
                    if(isCancelled())break;
                }
            }
            passedCursor[0].close();
            return null;
        }
        @Override
        protected void onProgressUpdate(Album... newAlbum) {
            //super.onProgressUpdate(values);
            albumList.add(newAlbum[0]);
            recyclerAdapter.notifyItemInserted(position);
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            //super.onPostExecute(aVoid);
        }
    }
}
