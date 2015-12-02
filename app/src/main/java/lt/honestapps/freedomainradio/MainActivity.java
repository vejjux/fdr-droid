package lt.honestapps.freedomainradio;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import lt.honestapps.freedomainradio.lib.AuPlay;
import lt.honestapps.freedomainradio.lib.DialogFactory;
import lt.honestapps.freedomainradio.lib.PodMan;
import lt.honestapps.freedomainradio.lib.PodcastPlayer;

public class MainActivity extends AppCompatActivity {

    PodMan podMan;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        handler = new Handler(this.getMainLooper());
        final Context finalThis = this;

        // audio player
        AuPlay auPlay = new AuPlay(this,getApplicationContext());
        auPlay.setOnAuPlayListener(new AuPlay.OnAuPlayListener() {
            @Override
            public AuPlay.AuPlayItem onNeedNextItem(AuPlay.AuPlayItem source) {
                return podMan.getNextPlayerItem(source);
            }
            @Override
            public void onSaveState(AuPlay.AuPlayItem source) {
                podMan.savePlayerItemState(source);
            }
        });
        LinearLayout layoutBottom = (LinearLayout) findViewById(R.id.layout_bottom);
        layoutBottom.addView(auPlay,0);

        //create the tutorial webview
        WebView wvTut = ((WebView) findViewById(R.id.webview_tut));
        wvTut.loadUrl("file:///android_res/raw/tutorial.html");
        wvTut.setVisibility(View.GONE);

        // PodMan
        podMan = new PodMan(this, handler, new PodMan.PodmanCallBacks() {
            @Override
            public void onSetInfo(String info) {
                TextView tv = (TextView) findViewById(R.id.text_info);
                tv.setText(info);
            }
            @Override
            public void onSetTutorialVis(boolean vis) {
                findViewById(R.id.webview_tut).setVisibility( vis ? View.VISIBLE : View.GONE );
            }
        }, auPlay);
        ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view);
        scrollView.addView(podMan);
        podMan.updateScreen();

        //buttons

        // update xml
        findViewById(R.id.button_getxml).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                podMan.updateList("http://feeds.feedburner.com/FreedomainRadioVolume6");
            }
        });

        // delete content
        findViewById(R.id.button_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dlg = DialogFactory.createSettingsDlg(finalThis, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        podMan.resetList("http://feeds.feedburner.com/FreedomainRadioVolume6");
                    }
                });
                dlg.show();
            }
        });

        // show hide deleted
        findViewById(R.id.button_screen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( podMan.toggleVis() ){
                    ((ImageButton)findViewById(R.id.button_screen)).setImageResource(R.drawable.ic_view);
                }else{
                    ((ImageButton)findViewById(R.id.button_screen)).setImageResource(R.drawable.ic_view_off);
                }
            }
        });

        // show help
        findViewById(R.id.button_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFactory.showHelpDlg(finalThis);
            }
        });


    }

}