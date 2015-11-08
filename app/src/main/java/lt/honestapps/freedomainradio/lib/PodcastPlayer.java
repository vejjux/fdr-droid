package lt.honestapps.freedomainradio.lib;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.Handler;

import lt.honestapps.freedomainradio.R;

/**
 * Created by vejjux on 15.10.10.
 */
public class PodcastPlayer extends LinearLayout{

    // interface to caller
    public class PodcastPlayerItem{
        public String uri = null;
        public String title = null;
        public Object id = null;
    }
    public PodcastPlayerItem PodcastPlayerItemCreate(){
        return new PodcastPlayerItem();
    }
    public interface PodcastPlayerCallback{
        PodcastPlayerItem onGetNextSource(PodcastPlayerItem source);
    }

    // local vars
    private AudioPlayer audioPlayer;
    private Intent playIntent = null;
    private boolean musicBound = false;
    private PodcastPlayerItem source = null;
    private PodcastPlayerCallback callback = null;
    private Handler handler = new Handler();
    private SeekBar seekBar;
    private boolean isSeeking = false;

    private Context context;

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayer.AudioPlayerBinder binder = (AudioPlayer.AudioPlayerBinder)service;
            //get service
            audioPlayer = binder.getService();
            //pass list
            audioPlayer.initMediaPlayer(source.uri);
            handler.postDelayed(updatePlayTime,100);
            updateUi();
            musicBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void playNewPodcast(PodcastPlayerItem source){
        this.source = source;
        ((TextView)findViewById(R.id.text_player_title)).setText(source.title);
        if(playIntent==null) {
            playIntent = new Intent(context, AudioPlayer.class);
        }
        context.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        context.startService(playIntent);
    }



    public PodcastPlayer(final Context context, final PodcastPlayerCallback callback){
        super(context);
        this.context = context;
        this.callback = callback;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_box, this);

        //seekbar
        seekBar = (SeekBar)findViewById(R.id.seeker_play);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeekText();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioPlayer.seekTo(seekBar.getProgress());
                isSeeking = false;
            }
        });

        //buttons

        // Play button
        findViewById(R.id.button_play_play).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.play();
                updateUi();
            }
        });

        // Pause button
        findViewById(R.id.button_play_pause).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.pause();
                updateUi();
            }
        });

        // To beginning
        findViewById(R.id.button_play_prev).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.seekTo(0);
                updateUi();
            }
        });

        // Rev
        findViewById(R.id.button_play_rev).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.seekBy(0 - (1000 * 60));
                updateUi();
            }
        });

        // Ff
        findViewById(R.id.button_play_ff).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.seekBy(1000*60);
                updateUi();
            }
        });

        // Next
        findViewById(R.id.button_play_next).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PodcastPlayerItem src = callback.onGetNextSource(source);
                if(src!=null){
                    source = src;
                    playNewPodcast(src);
                }
            }
        });

    }

    private void updateUi(){
        if(audioPlayer.getIsPlaying()){
            findViewById(R.id.button_play_play).setVisibility(GONE);
            findViewById(R.id.button_play_pause).setVisibility(VISIBLE);
        }
        else{
            findViewById(R.id.button_play_play).setVisibility(VISIBLE);
            findViewById(R.id.button_play_pause).setVisibility(GONE);
        }
    }

    private void updateSeekText(){
        ((TextView)findViewById(R.id.text_play_pos)).setText(millisToString(seekBar.getProgress()));
        ((TextView)findViewById(R.id.text_play_total)).setText(millisToString(audioPlayer.getDuration()));
    }

    private Runnable updatePlayTime = new Runnable() {
        public void run() {
            if (audioPlayer.getIsPlaying()) {
                if(!isSeeking) {
                    seekBar.setMax(audioPlayer.getDuration());
                    seekBar.setProgress(audioPlayer.getPosition());
                    updateSeekText();
                }
                handler.postDelayed(this, 300);
            }
        }
    };



    private String millisToString(long milliseconds){
        String s = "";
        // Convert total duration into time
        int minutes = (int)(milliseconds / (1000*60));
        int seconds = (int)((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        //minutes
        if(minutes < 10){ s += "00"; }
        s += String.valueOf(minutes) + ":";
        //seconds
        if(seconds < 10){ s+= "0"; }
        s += String.valueOf(seconds);
        // return timer string
        return s;
    }

}
