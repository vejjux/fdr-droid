package lt.honestapps.freedomainradio.lib;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import lt.honestapps.freedomainradio.R;

/**
 * Created by g on 26/10/2015.
 */
public class AuPlay extends LinearLayout implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    //=============================================
    // interface to caller
    public class AuPlayItem{
        public String uri = null;
        public String title = null;
        public Object id = null;
        public int position;
    }
    public AuPlayItem AuPlayItemCreate(){
        return new AuPlayItem();
    }
    //listeners
    public interface OnAuPlayListener{
        AuPlayItem onNeedNextItem(AuPlayItem source);
        void onSaveState(AuPlayItem source);
    }
    OnAuPlayListener onAuPlayListener = null;
    public void setOnAuPlayListener(OnAuPlayListener onAuPlayListener){
        this.onAuPlayListener = onAuPlayListener;
    }
    //---------------------------------------------
    //===================================================================================

    private AuPlaySvc auPlaySvc = null;
    private boolean svcBound = false;
    private AuPlayItem auPlayItem = null;
    private Context appContext = null;
    private Context uiContext = null;
    private Handler handler = new Handler();
    private boolean monitoring = false;
    private boolean seeking = false;

    //-------------------------------------------------------------------
    // public control

    public AuPlay(Context uiContext,Context appContext) {
        super(uiContext);
        this.appContext = appContext;
        this.uiContext = uiContext;
        //inflate layout
        LayoutInflater inflater = (LayoutInflater) uiContext
                .getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_box, this);
        //this.setVisibility(GONE);
        //add button listeners
        findViewById(R.id.button_play_play).setOnClickListener(this);
        findViewById(R.id.button_play_pause).setOnClickListener(this);
        findViewById(R.id.button_play_prev).setOnClickListener(this);
        findViewById(R.id.button_play_rev).setOnClickListener(this);
        findViewById(R.id.button_play_ff).setOnClickListener(this);
        findViewById(R.id.button_play_next).setOnClickListener(this);
        findViewById(R.id.auplay_button_stop).setOnClickListener(this);
        //add seekbar listener
        ((SeekBar)findViewById(R.id.seeker_play)).setOnSeekBarChangeListener(this);
        //bind to the service
        Intent intent = new Intent(appContext, AuPlaySvc.class);
        if( appContext.bindService(intent, auPlaySvcConnection, Context.BIND_AUTO_CREATE) ){
            appContext.startService(intent);
        }
        //hide itself initialy
        setVisibility(GONE);
    }

    public void playNewAudio(AuPlayItem auPlayItem){
        if(svcBound && auPlaySvc != null) {
            this.auPlayItem = auPlayItem;
            ((TextView) findViewById(R.id.text_player_title)).setText(auPlayItem.title);
            auPlaySvc.playerStartNew(auPlayItem.uri,auPlayItem.position);
            startMonitor();
        }
    }

    private void startMonitor(){
        if(!monitoring) {
            monitoring = true;
            continueMonitor();
        }
    }

    private void continueMonitor(){
        updateUi();
        saveState(false);
        //rerun
        if(svcBound && auPlaySvc != null && monitoring && (
                auPlaySvc.getStatus() == auPlaySvc.STATUS_ACTIVE ||
                auPlaySvc.getStatus() == auPlaySvc.STATUS_PREPING
        )){
            monitoring = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    continueMonitor();
                }
            }, 300);
        }else{
            monitoring = false;
        }
    }

    private void updateUi(){
        if(svcBound && auPlaySvc != null && auPlaySvc.getStatus() != auPlaySvc.STATUS_STOPPED){
            //---
            if(!seeking) {
                SeekBar seekBar = ((SeekBar) findViewById(R.id.seeker_play));
                seekBar.setMax(auPlaySvc.getMax());
                seekBar.setProgress(auPlaySvc.getPos());
                ((TextView) findViewById(R.id.text_play_total)).setText(millisToString(auPlaySvc.getMax()));
                ((TextView) findViewById(R.id.text_play_pos)).setText(millisToString(auPlaySvc.getPos()));
            }
            //---
            findViewById(R.id.button_play_play).setVisibility(auPlaySvc.getPlaying()?GONE:VISIBLE);
            findViewById(R.id.button_play_pause).setVisibility(auPlaySvc.getPlaying()?VISIBLE:GONE);
            //---
            this.setVisibility(VISIBLE);
        }else{
            this.setVisibility(GONE);
        }
    }

    private void saveState(boolean force){
        if(svcBound && auPlaySvc != null && (auPlaySvc.getPlaying() || force)) {
            auPlayItem.position = auPlaySvc.getPos();
            onAuPlayListener.onSaveState(auPlayItem);
        }
    }

    //-----------------------------------------------------------------------
    // event handlers

    @Override
    public void onClick(View view) {
        if(svcBound && auPlaySvc != null) {
            switch (view.getId()) {
                case R.id.button_play_play: {
                    auPlaySvc.playerStart();
                    break;
                }
                case R.id.button_play_pause: {
                    auPlaySvc.playerPause();
                    break;
                }
                case R.id.button_play_prev: {
                    auPlaySvc.playerSeekTo(0);
                    break;
                }
                case R.id.button_play_rev: {
                    auPlaySvc.playerSeekBy(-60000);
                    break;
                }
                case R.id.button_play_ff: {
                    auPlaySvc.playerSeekBy(60000);
                    break;
                }
                case R.id.button_play_next: {
                    if(onAuPlayListener!=null) {
                        AuPlayItem newItem = onAuPlayListener.onNeedNextItem(auPlayItem);
                        if(newItem!=null) {
                            playNewAudio(newItem);
                        }
                    }
                    break;
                }
                case R.id.auplay_button_stop: {
                    auPlaySvc.playerStop();
                    break;
                }
            }
            startMonitor();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seeking){
            ((TextView) findViewById(R.id.text_play_pos)).setText(millisToString(seekBar.getProgress()));
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seeking = true;
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(svcBound && auPlaySvc != null) {
            auPlaySvc.playerSeekTo(seekBar.getProgress());
            saveState(true);
        }
        seeking = false;
    }

    //==============================================================
    // connect to the service
    private ServiceConnection auPlaySvcConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AuPlaySvc.AuPlaySvcBinder binder = (AuPlaySvc.AuPlaySvcBinder)service;
            auPlaySvc = binder.getAuPlaySvc();
            svcBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            svcBound = false;
        }
    };

    //extras
    public static String millisToString(long milliseconds){
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
