package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.SongAdapter;
import com.ecebuc.gesmediaplayer.AudioUtils.StorageUtils;
import com.ecebuc.gesmediaplayer.Audios.Audio;
import com.ecebuc.gesmediaplayer.MediaPlaybackService;
import com.ecebuc.gesmediaplayer.R;
import com.ecebuc.gesmediaplayer.Utils.SimpleDividerItemDecoration;
import com.ecebuc.gesmediaplayer.Utils.RecyclerTouchListener;

import java.util.ArrayList;

public class SongsFragment extends Fragment {

    private OnSongFragmentInteractionListener songFragmentCallback;

    private RecyclerView songRecyclerView;
    private RecyclerView.Adapter songRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private ArrayList<Audio> songList = new ArrayList<>();
    private StorageUtils storageUtils;
    private SongLoaderAsync songLoader;

    public static final String BROADCAST_AUDIO_LOAD_COMPLETE = "com.ecebuc.gesmediaplayer.AudioLoadComplete";
    private final String SONG_FRAGMENT_LOG = "SongFragment: ";
    private final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    private final String SONG_BROADCAST_IS_ASYNC = "shouldPopulateListWhole";
    private String fragmentTitle;

    private final String fromAlbumIDParamTAG = "Album ID";
    private boolean paramIsFragmentDefault;
    private String paramAlbumID;

    public SongsFragment() {
        // Required empty public constructor
    }
    public static SongsFragment newInstance(/*String param1, String param2*/) {
        SongsFragment fragment = new SongsFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSongFragmentInteractionListener) {
            songFragmentCallback = (OnSongFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSongFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        storageUtils = new StorageUtils(getContext());

        Bundle args = getArguments();
        paramIsFragmentDefault = args.getBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);

        if(paramIsFragmentDefault){
            //default behavior - display all device songs
            //will be done in onCreateView
            fragmentTitle = "All Songs";

            //clear the service audioList because all device songs are about to be load
            if(MediaPlaybackService.audioList != null){
                MediaPlaybackService.audioList.clear();
            }
        } else {
            //fragment was probably created from an album - handle accordingly
            paramAlbumID = args.getString(fromAlbumIDParamTAG);
            if(paramAlbumID == null){
                Log.e("SongsFrag onCreate: ", "error - paramAlbumID is null");
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_songs, container, false);

        //Recycler view setup for songs display
        songRecyclerView = (RecyclerView) rootView.findViewById(R.id.songs_recycler_view);
        songRecyclerView.setHasFixedSize(true);
        songRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));

        recyclerLayoutManager = new LinearLayoutManager(getActivity());
        songRecyclerView.setLayoutManager(recyclerLayoutManager);

        songRecyclerAdapter = new SongAdapter(songList);
        songRecyclerView.setAdapter(songRecyclerAdapter);

        //here goes the AsyncTask
        if(paramIsFragmentDefault){
            //fragment is new, load all songs asynchronously
            songLoader = new SongLoaderAsync(songRecyclerView.getAdapter());
            initSongAsyncTask(songLoader);
        }
        else {
            //songList has songs in it, means fragment comes from an album
            initSongListFromId(paramAlbumID);
        }

        songRecyclerView.setItemAnimator(new DefaultItemAnimator());
        songRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), songRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // If current fragment is created from an album notify service to reload songList
                if(!paramIsFragmentDefault){
                    //tell service to load songs from Shared Preferences because album was selected
                    //Intent songUploadBroadcastIntent = new Intent(BROADCAST_AUDIO_LOAD_COMPLETE);
                    //songUploadBroadcastIntent.putExtra(SONG_BROADCAST_IS_ASYNC, false);
                    //getActivity().sendBroadcast(songUploadBroadcastIntent);
                }
                songFragmentCallback.playSelectedSong(position);
            }
            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(getContext(), "Long selected", Toast.LENGTH_SHORT).show();
            }
        }));


        return rootView;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        songFragmentCallback.updateToolbarTitleForFragment(fragmentTitle);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();

        if(songLoader != null && !songLoader.isCancelled()){
            songLoader.cancel(true);
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        songFragmentCallback = null;
    }

    public interface OnSongFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
        void playSelectedSong(int position);
    }

    private void initSongListFromId(String albumId){
        ContentResolver contentResolver = getActivity().getContentResolver();
        String data, title, album, artist, albumArt, id;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        selection += " and album_id = " + albumId;
        String sortOrder = MediaStore.Audio.AudioColumns.TRACK + " ASC";

        String[] SONG_SUMMARY_PROJECTION = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media._ID
        };

        Cursor cursor = contentResolver.query(uri,
                            SONG_SUMMARY_PROJECTION,
                            selection, null,
                            sortOrder);

        Cursor albumArtCursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[] {String.valueOf(albumId)},
                null);
        albumArtCursor.moveToFirst();
        albumArt = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        albumArtCursor.close();

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                // Save to audioList
                songList.add(new Audio(data, id, title, album, artist, albumArt));
                songRecyclerView.getAdapter().notifyItemInserted(cursor.getPosition());
            }
            //set the fragment name to the artist of which we are displaying the albums
            cursor.moveToPrevious();
            fragmentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        }
        cursor.close();

        // Upload songs, but do not broadcast, will be done onClick
        storageUtils.clearCachedAudioPlaylist();
        storageUtils.storeAudio(songList);

        Intent i = new Intent(BROADCAST_AUDIO_LOAD_COMPLETE);
        i.putExtra(SONG_BROADCAST_IS_ASYNC, false);
        getActivity().sendBroadcast(i);
    }
    private void initSongAsyncTask(SongLoaderAsync songLoader){
        ContentResolver contentResolver = getActivity().getContentResolver();
        final String[] SONG_SUMMARY_PROJECTION = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media._ID
        };

        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        final Cursor cursor = contentResolver.query(
                songUri,
                SONG_SUMMARY_PROJECTION,
                selection, null,
                sortOrder);

        //begin song load asynchronously
        songLoader.execute(cursor);
    }

    private class SongLoaderAsync extends AsyncTask<Cursor, Audio, Void> {
        RecyclerView.Adapter recyclerAdapter;
        ContentResolver contentResolver;
        String data, title, album, artist, albumArt, id, albumId;
        int position, countBeforeSongUpload;
        final int TIMES_BEFORE_UPLOAD = 30;

        public SongLoaderAsync(RecyclerView.Adapter adapter) {
            recyclerAdapter = adapter;
            contentResolver = getActivity().getContentResolver();
            position = -1;
            countBeforeSongUpload = TIMES_BEFORE_UPLOAD;
        }

        @Override
        protected Void doInBackground(Cursor... passedCursor) {
            if (passedCursor[0] != null && passedCursor[0].getCount() > 0) {
                while (passedCursor[0].moveToNext()) {
                    id = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media._ID));
                    albumId = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    data = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.DATA));
                    title = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.TITLE));
                    album = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    artist = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ARTIST));

                    Cursor albumArtCursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                            MediaStore.Audio.Albums._ID + "=?",
                            new String[]{String.valueOf(albumId)},
                            null);
                    albumArtCursor.moveToFirst();
                    albumArt = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                    //create new Audio object and put it individually in SharedPreferences
                    //using the position variable as identifier for it
                    position++;
                    Audio newSong = new Audio(data, id, title, album, artist, albumArt);
                    publishProgress(newSong);
                    storageUtils.storeSingleAudio(newSong, position);

                    //periodically check if a cancel command was given, to exit rapidly
                    if(isCancelled())break;

                    //add some items to songList, upload to SharedPreferences and reset counter
                    if(countBeforeSongUpload == 0){
                        Intent songUploadBroadcastIntent = new Intent(BROADCAST_AUDIO_LOAD_COMPLETE);
                        songUploadBroadcastIntent.putExtra(SONG_BROADCAST_IS_ASYNC, true);
                        getActivity().sendBroadcast(songUploadBroadcastIntent);
                        countBeforeSongUpload = TIMES_BEFORE_UPLOAD;
                    }
                    else{
                        countBeforeSongUpload--;
                    }
                }
            }
            passedCursor[0].close();
            return null;
        }
        @Override
        protected void onProgressUpdate(Audio... newSong) {
            //super.onProgressUpdate(newSong);
            songList.add(newSong[0]);
            recyclerAdapter.notifyItemInserted(position);
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            // Upload song list one last time
            Intent songUploadBroadcastIntent = new Intent(BROADCAST_AUDIO_LOAD_COMPLETE);
            songUploadBroadcastIntent.putExtra(SONG_BROADCAST_IS_ASYNC, true);
            getActivity().sendBroadcast(songUploadBroadcastIntent);
        }
    }
}
