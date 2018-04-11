package com.ecebuc.gesmediaplayer.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ecebuc.gesmediaplayer.AudioUtils.SongAdapter;
import com.ecebuc.gesmediaplayer.AudioUtils.StorageUtils;
import com.ecebuc.gesmediaplayer.Audios.Audio;
import com.ecebuc.gesmediaplayer.R;

import java.util.ArrayList;

public class SongsFragment extends Fragment {

    private OnSongFragmentInteractionListener songFragmentCallback;

    private RecyclerView songRecyclerView;
    private RecyclerView.Adapter songRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private Context context;

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
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
        if(getActivity() != null){
            context = getActivity();
        }
        else
        {
            Log.e("SongFrag onCreate: ", "getActivity was null");
            Log.d("SongFrag onCreate: ", "getActivity was null");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_songs, container, false);

        //get the songs list for the recyclerview adapter
        ArrayList<Audio> songList;
        StorageUtils storageUtils = new StorageUtils(getActivity());
        songList = storageUtils.loadAudio();

        //Recycler view setup for songs display
        songRecyclerView = (RecyclerView) rootView.findViewById(R.id.songs_recycler_view);
        songRecyclerView.setHasFixedSize(true);

        recyclerLayoutManager = new LinearLayoutManager(getActivity());
        songRecyclerView.setLayoutManager(recyclerLayoutManager);

        songRecyclerAdapter = new SongAdapter(songList);
        songRecyclerView.setAdapter(songRecyclerAdapter);
        songRecyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        songFragmentCallback.updateToolbarTitleForFragment("All Songs");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        songFragmentCallback = null;
    }

    public interface OnSongFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
    }
}
