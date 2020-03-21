package com.example.voice_chatting;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;

public class SoundPoolManager {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private float actualVolume;
    private float maxVolume;
    private float volume;
    private AudioManager audioManager;
    private SoundPool soundPool;
    private int ringingSoundId;
    private int ringingStreamId;
    private int disconnectSoundId;

    private static SoundPoolManager instance;

    private SoundPoolManager(Context context){
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float) audioManager.getStreamMaxVolume((AudioManager.STREAM_MUSIC));
        volume = actualVolume / maxVolume;

        // Load the sounds
        int maxStreams = 2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(maxStreams)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }else {
            soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
        }

        ringingSoundId = soundPool.load(context, R.raw.incoming, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect, 1);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled){
                    playRinging();
                    playingCalled = false;
                }
            }
        });
    }

    public static SoundPoolManager getInstance(Context context){
        if (instance == null){
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    public void playRinging(){
        if (loaded && !playing){
            ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
            soundPool.autoPause();
            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void stopRinging(){
        if (playing){
            soundPool.stop(ringingStreamId);
        }
    }

    public void playDisconnect(){
        if (loaded && !playing){
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            soundPool.autoPause();
            playing = false;
        }
    }

    public void release(){
        if (soundPool != null){
            soundPool.unload(ringingSoundId);
            soundPool.unload(disconnectSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }
}
