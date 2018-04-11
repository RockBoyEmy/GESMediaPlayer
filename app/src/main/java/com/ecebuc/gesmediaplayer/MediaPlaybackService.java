package com.ecebuc.gesmediaplayer;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.app.NotificationManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.ecebuc.gesmediaplayer.AudioUtils.StorageUtils;
import com.ecebuc.gesmediaplayer.Audios.Audio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,

        AudioManager.OnAudioFocusChangeListener {

    public static final int NOTIFICATION_ID = 123;
    public static boolean isServiceStarted = false;

    private static int audioIndex = -1;
    private ArrayList<Audio> audioList;
    private Audio activeAudio = null;
    private int pausedPosition;

    private MediaPlayer gesMediaPlayer;
    private MediaSessionCompat gesMediaSession;
    private MediaControllerCompat.TransportControls transportControls;


    public static final String ACTION_PLAY = "com.example.gesmediaplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.gesmediaplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.gesmediaplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.gesmediaplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.gesmediaplayer.ACTION_STOP";
    public static final String CHANNEL_ID = "com.example.gesmediaplayer.NOTIFICATION_CHANNEL_ID";



    //-------------------------------------Lifecycle methods--------------------------------------//

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("service onCreate: ", "Service created");
        registerAudioLoadCompleteReceiver();
        initMediaPlayer();
        initMediaSession();
        callStateListener();

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        // Very much needed to avoid lots of NullPtrExceptions with getPlaybackState()
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_PLAY_PAUSE);
        gesMediaSession.setPlaybackState(playbackStateBuilder.build()); //translates to STATE_NONE

        //create a notification channel for displaying notifications on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            String name = "GES Notification Channel";
            String description = "This is the GES Media Player notification channel - test";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            long vibrationP[] = {0}; //to eliminate vibration for every command on notification

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            mChannel.setVibrationPattern(vibrationP);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
        }
        Log.d("service onCreate: ", "exited service onCreate");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("service onDestroy: ", "onDestroy just entered");
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        /*//clear cached playlist
        //new StorageUtils(getApplicationContext()).clearCachedAudioPlaylist();*/

        gesMediaSession.release();
        unregisterReceiver(audioLoadComplete);

        /*handle app crash when it is started but no music is played, as
        * the broadcast receiver is never being registered, giving error
        * when entering onDestroy with an unregistered receiver*/
        if(isServiceStarted){
            unregisterReceiver(notificationPlaybackCommand);
        }
        Log.d("service onDestroy: ", "everything unregistered and cleared in service, exiting...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("onStartCommand: ", "Service has been started");
        isServiceStarted = true;
        registerNotificationCommandReceiver();

        if(activeAudio == null){
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        }

        try {
            //sets current song as data source for media player
            gesMediaPlayer.setDataSource(activeAudio.getData());
            gesMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MUSIC SERVICE", "Error setting data source", e);
            stopSelf();
        }

        PlaybackStateCompat pbState = gesMediaSession.getController().getPlaybackState();
        Log.d("onPlay: ", "current playback state is: " + pbState.getState());

        MediaButtonReceiver.handleIntent(gesMediaSession, intent);
        //createNotification();
        //handleIncomingActions(intent);
        Log.d("onStartCommand: ", "exited onStartCommand method");
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if( gesMediaPlayer != null ) {
            //gesMediaPlayer.reset();
            transportControls.skipToNext();
            //should not reset in here because it breaks controls of the
            //notification as the song changes, should only skip to next
        }
    }
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error for an asynchronous operation.
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error",
                        "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error",
                        "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error",
                        "MEDIA ERROR UNKNOWN " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.d("MediaPlayer Error",
                        "MEDIA ERROR UNSUPPORTED " + extra);
                break;
        }
        return false;
        //TODO: documentation talks about mediaPlayer needing reset here before
        //being able to use it again
    }
    @Override
    public void onPrepared(MediaPlayer mp){
        Log.d("onPrepared: ", "Media Player is ready for playback");
        gesMediaPlayer.start();
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        createNotification();
        registerNoisyReceiver();
    }



    //----------------------------------------Initializers----------------------------------------//

    private void initMediaPlayer() {
        gesMediaPlayer = new MediaPlayer();
        gesMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        gesMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        gesMediaPlayer.setVolume(1.0f, 1.0f);

        gesMediaPlayer.setOnErrorListener(this);
        gesMediaPlayer.setOnPreparedListener(this);
        gesMediaPlayer.setOnCompletionListener(this);
    }
    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        gesMediaSession = new MediaSessionCompat(getApplicationContext(), "GESMediaService",
                mediaButtonReceiver, null);

        gesMediaSession.setCallback(mediaSessionCallbacks);
        gesMediaSession.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );
        transportControls = gesMediaSession.getController().getTransportControls();

        //this is for pre-Lollipop media button handling on those devices
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        gesMediaSession.setMediaButtonReceiver(pendingIntent);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(gesMediaSession.getSessionToken());
    }
    private void initMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Display Title");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle");
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        gesMediaSession.setMetadata(metadataBuilder.build());
    }
    private void registerNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter noisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, noisyFilter);
    }
    private void registerAudioLoadCompleteReceiver() {
        //Register playNewAudio receiver
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_AUDIO_LOAD_COMPLETE);
        registerReceiver(audioLoadComplete, filter);
    }
    private void registerNotificationCommandReceiver(){
        IntentFilter notificationFilter = new IntentFilter();
        notificationFilter.addAction(ACTION_PLAY);
        notificationFilter.addAction(ACTION_PAUSE);
        notificationFilter.addAction(ACTION_NEXT);
        notificationFilter.addAction(ACTION_PREVIOUS);

        registerReceiver(notificationPlaybackCommand, notificationFilter);
    }
    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.skepty_face); //image cover in ../res/drawable/
        // Update the current metadata
        gesMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }


    //-----------------------------------Media Playback functions---------------------------------//

    //TODO: read about the AssetFileDescriptor, and the ResultReceiver for onPlayFromMediaId

    private MediaSessionCompat.Callback mediaSessionCallbacks = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            if(!requestAudioFocus()) {
                Log.d("onPlay: ", "Failed to gain focus");
                return;
            }
            //check if service is started, not only bound
            //will call onStartCommand when service is started, otherwise no
            if(!isServiceStarted){
                Log.d("onPlay: ", "service was called to be started");
                startService(new Intent(getApplicationContext(), MediaPlaybackService.class));
            }

            if(gesMediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
                gesMediaSession.setActive(true);
                gesMediaPlayer.start();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                registerNoisyReceiver();
                createNotification();
            }
        }
        @Override
        public void onPause() {
            super.onPause();

            if( gesMediaPlayer.isPlaying() ) {
                gesMediaPlayer.pause();
                unregisterReceiver(becomingNoisyReceiver);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                createNotification();
            }
        }
        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            //if last in playlist, set index to first in audioList
            if (audioIndex == audioList.size() - 1) {
                audioIndex = 0;
                activeAudio = audioList.get(audioIndex);
            } else {
                //get next in playlist
                activeAudio = audioList.get(++audioIndex);
            }

            //Update stored index
            new StorageUtils(getApplicationContext()).storeAudioIndex(audioIndex);

            //reset mediaPlayer
            gesMediaPlayer.pause();
            gesMediaPlayer.reset();
            initMediaPlayer();

            try {
                //sets current song as data source for media player
                gesMediaPlayer.setDataSource(activeAudio.getData());
                gesMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }

            //updateMetaData();
            //createNotification();
        }
        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            if (audioIndex == 0) {
                audioIndex = audioList.size() - 1;
                activeAudio = audioList.get(audioIndex);
            } else {
                //get previous in playlist
                activeAudio = audioList.get(--audioIndex);
            }

            //Update stored index
            new StorageUtils(getApplicationContext()).storeAudioIndex(audioIndex);

            //reset mediaPlayer
            gesMediaPlayer.pause();
            gesMediaPlayer.reset();
            initMediaPlayer();

            try {
                //sets current song as data source for media player
                gesMediaPlayer.setDataSource(activeAudio.getData());
                gesMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }

            //updateMetaData();
            //createNotification();
        }
        @Override
        public void onStop() {
            super.onStop();

            // Abandon audio focus
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(MediaPlaybackService.this);

            stopSelf();
            // Set the session inactive  (and update metadata and state)
            gesMediaSession.setActive(false);
            gesMediaPlayer.stop();
            removeNotification();
        }
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);

            try {
                AssetFileDescriptor afd = getResources().openRawResourceFd(Integer.valueOf(mediaId));
                if( afd == null ) {
                    Log.d("afd: ", "afd in onPlayFromMediaId is null");
                    return;
                }

                try {
                    gesMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                } catch( IllegalStateException e ) {
                    gesMediaPlayer.release();
                    initMediaPlayer();
                    gesMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }

                afd.close();
                initMediaSessionMetadata();

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                gesMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("onPlayFromId: ", "mediaPlayer failed to prepare");
            }

            //Work with extras here if you want
        }
        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent){
            //on default, KEYCODE_MEDIA_PLAY calls onPlay
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
    };

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PLAY);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        gesMediaSession.setPlaybackState(playbackstateBuilder.build());
    }



    //------------------------------AudioFocus, Calls and Broadcasts-------------------------------//

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private boolean requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_GAIN;
    }
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (gesMediaPlayer != null && gesMediaPlayer.isPlaying()) {
                            gesMediaPlayer.pause();
                            pausedPosition = gesMediaPlayer.getCurrentPosition();
                            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start/resume playing.
                        if (gesMediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                gesMediaPlayer.seekTo(pausedPosition);
                                gesMediaPlayer.start();
                                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                // Lost focus for an unbounded amount of time:
                // stop playback and (maybe) release media player
                if( gesMediaPlayer.isPlaying() ) {
                    gesMediaPlayer.pause();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    createNotification();
                    //gesMediaPlayer.stop();
                    //transportControls.stop();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                // Lost focus for a short time; Pause only and do not
                // release the media player as playback is likely to resume
                if (gesMediaPlayer.isPlaying()) {
                    gesMediaPlayer.pause();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    createNotification();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                // Lost focus for a short time (ex. notification sound)
                // but it's ok to keep playing at a temporarily attenuated level
                if( gesMediaPlayer != null ) {
                    gesMediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                //Invoked when the audio focus of the system is updated.
                if( gesMediaPlayer != null ) {
                    if( !gesMediaPlayer.isPlaying() ) {
                        gesMediaPlayer.start();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        createNotification();
                    }
                    gesMediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            }
        }
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( gesMediaPlayer != null && gesMediaPlayer.isPlaying() ) {
                gesMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
        }
    };

    private BroadcastReceiver audioLoadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("audioLoadReceiver: ", "entered onReceive");
            StorageUtils storageUtils = new StorageUtils(getApplicationContext());
            audioList = storageUtils.loadAudio();
            if(audioList.isEmpty()){
                Log.e("onReceive: ", "audioList is empty after loading");
                //TODO: will have to handle scenario of no songs on device (app mustn't crash)
                return;
            }
            Log.d("audioLoadReceiver: ", "exited onReceive");
        }
    };

    private BroadcastReceiver notificationPlaybackCommand = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("notifPlayCommand: ", "entered onReceive");

            if (intent == null || intent.getAction() == null) return;

            String actionString = intent.getAction();
            if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
                transportControls.play();
            } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
                transportControls.pause();
            } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
                transportControls.skipToNext();
            } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
                transportControls.skipToPrevious();
            } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
                transportControls.stop();
            }

            Log.d("notifPlayCommand: ", "exited onReceive");
        }
    };



    //--------------------------------------------------------------------------------------------//

    private void createNotification(){
        //Playback actions for the notification buttons
        Intent playPauseSongIntent = new Intent();
        Intent previousSongIntent = new Intent(ACTION_PREVIOUS);
        Intent nextSongIntent = new Intent(ACTION_NEXT);
        Intent stopIntent = new Intent(ACTION_STOP);

        PendingIntent playPauseSongPendingIntent = null;
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0,
                                                                    stopIntent, 0);
        PendingIntent previousSongPendingIntent = PendingIntent.getBroadcast(this, 0,
                                                                    previousSongIntent, 0);
        PendingIntent nextSongPendingIntent = PendingIntent.getBroadcast(this, 0,
                                                                    nextSongIntent, 0);


        PlaybackStateCompat state = gesMediaSession.getController().getPlaybackState();
        Log.d("createNotification: ", "playbackState is " + state.getState());

        //notification needs to be initialized - so it will initially be the pause icon
        int playPauseNotificationIcon = R.drawable.ic_pause_black_24dp;/*android.R.drawable.ic_media_pause;*/

        //Build a new notification according to the current state of the MediaPlayer
        if(state.getState() == PlaybackStateCompat.STATE_PAUSED){
            Log.d("createNotification: ", "should be Paused");
            playPauseNotificationIcon = R.drawable.ic_play_arrow_black_24dp;/*android.R.drawable.ic_media_play;*/
            playPauseSongIntent.setAction(ACTION_PLAY);
            playPauseSongPendingIntent = PendingIntent.getBroadcast(this, 0,
                                                                    playPauseSongIntent, 0);

        } if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
            Log.d("createNotification: ", "should be Playing");
            playPauseNotificationIcon = R.drawable.ic_pause_black_24dp;/*android.R.drawable.ic_media_pause*/
            playPauseSongIntent.setAction(ACTION_PAUSE);
            playPauseSongPendingIntent = PendingIntent.getBroadcast(this, 0,
                                                                    playPauseSongIntent, 0);
        }

        //set a default icon for song cover
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.skepty_face); //replace with your own default image/cover
        if(activeAudio.getAlbumArt() != null){
            largeIcon = BitmapFactory.decodeFile(activeAudio.getAlbumArt());
        }


        //TODO this doesn't work for now - starting the activity when clicking the notification
        Intent notificationClickIntent = new Intent(this, MainActivity.class);
        notificationClickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickActivityStart = PendingIntent.getActivity(this, 0,
                                            notificationClickIntent , PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder)
                new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder
                // Enable launching the player by clicking the notification
                //.setContentIntent(gesMediaSession.getController().getSessionActivity())
                .setContentIntent(clickActivityStart) //TODO doesn't work well for now, restarts, leaking receivers

                // Stop the service when the notification is swiped away //TODO: not working, see cancelButton
                .setDeleteIntent(stopPendingIntent)

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Set the Notification style
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token - to display artwork in lock screen
                        .setMediaSession(gesMediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2)
                        /*.setShowCancelButton(true)
                        .setCancelButtonIntent(stopPendingIntent)*/)

                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimaryDark))

                // Set the large and small icons
                //.setSmallIcon(android.R.drawable.stat_sys_headset)
                .setSmallIcon(R.drawable.ic_headset_black_24dp)
                .setLargeIcon(largeIcon)

                // Set Notification content information
                .setContentText(activeAudio.getArtist())
                .setSubText(activeAudio.getAlbum())
                .setContentTitle(activeAudio.getTitle())
                .setShowWhen(false) //don't display timestamp

                // Set notification action buttons for music playback
                .addAction(R.drawable.ic_skip_previous_black_24dp/*android.R.drawable.ic_media_previous*/,
                        "Previous",
                        previousSongPendingIntent)
                .addAction(playPauseNotificationIcon, "PlayPause", playPauseSongPendingIntent)
                .addAction(R.drawable.ic_skip_next_black_24dp/*android.R.drawable.ic_media_next*/,
                        "Next",
                        nextSongPendingIntent);

        //.build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notificationBuilder.build());
    }
    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    //------------------------------------Less important methods----------------------------------//

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    //Not important for general audio service, required for class
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }
}
