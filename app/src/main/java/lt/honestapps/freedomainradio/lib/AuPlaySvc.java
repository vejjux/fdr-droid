package lt.honestapps.freedomainradio.lib;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Created by g on 26/10/2015.
 */
public class AuPlaySvc extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{

    public final static int STATUS_STOPPED = 0;
    public final static int STATUS_PREPING = 1;
    public final static int STATUS_ACTIVE = 2;
    public final static int STATUS_COMPLETE = 3;

    private MediaPlayer player = null;
    AudioManager audioManager = null;
    TelephonyManager telephonyManager = null;
    private Context context = null;
    private int status = STATUS_STOPPED;
    private String errorMsg = null;
    private int startAt = 0;

    @Override
    public void onCreate(){
        context =  getApplicationContext();
        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        status = STATUS_STOPPED;
    }

    public int getStatus(){
        return status;
    }

    public boolean getPlaying(){
        if(player != null){
            return player.isPlaying();
        }
        return false;
    }

    public int getMax(){
        if(player != null){
            return player.getDuration();
        }
        return 0;
    }

    public int getPos(){
        if(player != null){
            return player.getCurrentPosition();
        }
        return 0;
    }

    //================================================================================
    //control handlers
    public void playerStartNew(String uri,int stratAt){
        try {
            playerStop();
            this.startAt = stratAt;
            player = new MediaPlayer();
            player.setOnPreparedListener(this);
            player.setOnErrorListener(this);
            //player.setOnSeekCompleteListener(this);
            //player.setOnBufferingUpdateListener(this);
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setLooping(false);
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(uri);
            status = STATUS_PREPING;
            player.prepare();
        }catch(Exception e){
            //DialogFactory.showAlertDlg(context, "Player error!", e.toString());
            errorMsg = e.toString();
        }
    }
    public void playerStop(){
        looseAudioFocus();
        if(player != null){
            try {
                player.stop();
                player.release();
            }catch(Exception e){}
        }
        player = null;
        status = STATUS_STOPPED;
    }
    //regular control
    public void playerStart(){
        if(player != null && status == STATUS_ACTIVE) {
            player.start();
        }
    }
    public void playerPause(){
        if(player != null && status == STATUS_ACTIVE) {
            player.pause();
        }
    }
    public void playerSeekTo(int pos){
        if(player != null && status == STATUS_ACTIVE) {
            if(pos<0) pos = 0;
            if(pos>player.getDuration()) pos = player.getDuration();
            player.seekTo(pos);
        }
    }
    public void playerSeekBy(int delta){
        if(player != null && status == STATUS_ACTIVE) {
            int pos = player.getCurrentPosition() + delta;
            if(pos<0) pos = 0;
            if(pos>player.getDuration()) pos = player.getDuration();
            player.seekTo(pos);
        }
    }

    //====================================================================
    //media player listeners
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if( gainAudioFocus() ) {
            status = STATUS_ACTIVE;
            mediaPlayer.start();
            if(startAt>0){
                mediaPlayer.seekTo(startAt);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        looseAudioFocus();
        status = STATUS_STOPPED;
        //DialogFactory.showAlertDlg(context, "Player error!", Integer.toString(i) + "/" + Integer.toString(i1));
        errorMsg = Integer.toString(i) + "/" + Integer.toString(i1);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        status = STATUS_COMPLETE;
    }

    //==================================================================
    // Audio focus handlers

    private boolean gainAudioFocus(){
        if( audioManager.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ){
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
            registerReceiver(afBroadcastReceiver, intentFilter);
            try{telephonyManager.listen(afPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);}catch (Exception e){}
            //switch to foreground service
            /*PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification();
            notification.tickerText = text;
            notification.icon = R.drawable.play0;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
                    "Playing: " + songName, pi);
            startForeground(NOTIFICATION_ID, notification);*/
            return true;
        }
        return false;
    }

    private void looseAudioFocus(){
        try{unregisterReceiver(afBroadcastReceiver);}catch (Exception e){}
        try{telephonyManager.listen(afPhoneStateListener,PhoneStateListener.LISTEN_NONE);}catch (Exception e){}
        audioManager.abandonAudioFocus(afChangeListener);
        //stopForeground(true);
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        private boolean isTransient = false;
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if(getPlaying()) {
                    isTransient = true;
                    player.setVolume(0.2f, 0.2f);
                }
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if(getPlaying()) {
                    isTransient = true;
                    playerPause();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if( isTransient ){
                    player.setVolume(1f, 1f);
                    playerStart();
                }
                isTransient = false;
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                playerStop();
                isTransient = false;
            }
        }
    };

    BroadcastReceiver afBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                playerPause();
            }else if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())){
                playerPause();
            }
        }
    };

    PhoneStateListener afPhoneStateListener = new PhoneStateListener() {
        private boolean isPlaying;
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                isPlaying = player.isPlaying();
                playerPause();
            } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                //if(isPlaying) playerStart();
            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                isPlaying = player.isPlaying();
                playerPause();
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    //=================================================================
    // Service binder
    private AuPlaySvcBinder auPlaySvcBinder = new AuPlaySvcBinder();
    public class AuPlaySvcBinder extends Binder {
        public AuPlaySvc getAuPlaySvc(){
            return AuPlaySvc.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return auPlaySvcBinder;
    }

}
