package com.ecebuc.gesmediaplayer.AudioUtils;

import android.content.Context;
import android.content.SharedPreferences;

import com.ecebuc.gesmediaplayer.Audios.Audio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class StorageUtils {

    private final String STORAGE = " com.example.gesmediaplayer.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtils(Context context) {
        this.context = context;
    }

    public void storeAudio(ArrayList<Audio> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioFilesArrayList", json);
        //editor.apply();
        editor.commit();

    }

    public void storeSingleAudio(Audio song, int positionId){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String singleJson = gson.toJson(song);
        editor.putString(Integer.toString(positionId), singleJson);
        editor.commit();
    }
    public Audio loadSingleAudio(int positionId){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String singleJson = preferences.getString(Integer.toString(positionId), null);
        Type type = new TypeToken<Audio>() {
        }.getType();
        return gson.fromJson(singleJson, type);
    }

    public ArrayList<Audio> loadAudio() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioFilesArrayList", null);
        Type type = new TypeToken<ArrayList<Audio>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    /*public void storeAudioIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioFileIndex", index);
        editor.commit();
    }
    public int loadAudioIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioFileIndex", -1);//return -1 if no data found
    }*/

    public void clearCachedAudioPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }
}