package lt.honestapps.freedomainradio.lib;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

/**
 * Created by g on 25/10/2015.
 */
public class AudioPlayer extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener {

    public final static int STATUS_STOPPED = 0;
    public final static int STATUS_ACTIVE = 1;

    //public final static String ACTION_NEW = "action.new";
    //public final static String ACTION_PLAY = "action.play";

    private Context context = null;
    private int pos = 0;
    private MediaPlayer player = null;
    private int status = STATUS_STOPPED;

    // service on create
    public void onCreate(){
        super.onCreate();
        pos = 0;
        player = new MediaPlayer();
    }

    // initialisation and killing
    public void releasePlayer(){
        if(player != null){
            try {
                player.stop();
                player.release();
                player = null;
            }catch(Exception e){}
        }
        status = STATUS_STOPPED;
    }

    // intialisation
    public void initAudioPlayer(Context context){
        this.context = context;
        try {
            releasePlayer();
            player = new MediaPlayer();
            player.setOnErrorListener(this);
            player.setOnErrorListener(this);
            player.setOnPreparedListener(this);
            player.setOnSeekCompleteListener(this);
            player.setOnBufferingUpdateListener(this);
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setLooping(false);
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        }catch(Exception e){
            DialogFactory.showAlertDlg(context, "Player error!", e.toString());
        }
    }

    // service binder interface
    public class AudioPlayerBinder extends Binder {
        AudioPlayer getService() {
            return AudioPlayer.this;
        }
    }



    // media player listeners

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        DialogFactory.showAlertDlg(context,
                "Player error!",
                "Error numer " + Integer.toString(i) + " / " + Integer.toString(i1));
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        status = STATUS_ACTIVE;
        mediaPlayer.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    //service listeners

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(ACTION_NEW)) {

            mMediaPlayer = ... // initialize it here
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.prepareAsync(); // prepare async to not block main thread
        }
        return 0;
    }*/

    //========================================================================

    public int getStatus(){
        return status;
    }

    public boolean getIsPlaying(){
        if(player!=null) return player.isPlaying();
        return false;
    }

    public int getPosition(){
        if(player!=null) return player.getCurrentPosition();
        return 0;
    }

    public int getDuration(){
        if(player!=null) return player.getDuration();
        return 0;
    }



    public void play(){
        player.start();
    }

    public void pause(){
        if(player.isPlaying()) player.pause();
    }

    public void seekTo(int pos){
        player.seekTo(pos);
    }

    public void seekBy(int delta){
        int pos = player.getCurrentPosition() + delta;
        if(pos<0) pos = 0;
        if(pos>player.getDuration()) pos = player.getDuration();
        player.seekTo(pos);
    }

    public void initMediaPlayer(String uri) {
        try {
            releasePlayer();
            player = new MediaPlayer();
            player.setOnErrorListener(this);
            player.setOnErrorListener(this);
            player.setOnPreparedListener(this);
            player.setOnSeekCompleteListener(this);
            player.setOnBufferingUpdateListener(this);
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(uri);
            player.setLooping(false);
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            player.prepareAsync();
        }catch(Exception e){
            DialogFactory.showAlertDlg(context, "Player error!", e.toString());
        }
    }



}
