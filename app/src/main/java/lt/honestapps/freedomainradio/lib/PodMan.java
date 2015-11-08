package lt.honestapps.freedomainradio.lib;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import lt.honestapps.freedomainradio.R;

/**
 * Created by vejjux on 15.10.5.
 */
public class PodMan extends LinearLayout implements PodView.CallbackPodView{

    final Context context;
    final PodMan  finalThis;
    final Handler handler;
    AuPlay auPlay;
    PodmanCallBacks callBacks;

    final DbHelper dbHelper;
    final Dialog splashDlg;
    boolean onlyVis = true;
    public boolean loading = false;

    private LinearLayout layout = null;

    public interface PodmanCallBacks{
        void onSetInfo(String info);
    }

    public PodMan(final Context context,Handler handler,PodmanCallBacks callBacks,AuPlay auPlay){
        super(context);
        this.context = context;
        this.finalThis = this;
        this.handler = handler;
        this.callBacks = callBacks;
        this.auPlay = auPlay;
        //init
        dbHelper = new DbHelper(context);
        splashDlg = DialogFactory.createSplashDlg(context, "FDR", "Generating podcast list...");
        splashDlg.setCancelable(false);
        splashDlg.setCanceledOnTouchOutside(false);
        this.setOrientation(VERTICAL);
    }

    //=========================================
    // private shit

    private ArrayList<PodView> getSafeList(){
        ArrayList<PodView> safeList = new ArrayList<PodView>();
        for(int i=0; i<getChildCount(); i++){
            safeList.add((PodView)getChildAt(i));
        }
        return  safeList;
    }

    private void addPodView(final ArrayList<Podcast> pods,final int i){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Podcast pod = pods.get(i);
                FileOps.updateStatus(pod);
                final PodView podView = new PodView(context,dbHelper,finalThis);
                podView.setPod(pod);
                addView(podView);
                callBacks.onSetInfo(Integer.toString(i + 1) + " podcasts");
                if(i<pods.size()-1){
                    addPodView(pods,i+1);
                }
                else{
                    loading = false;
                }
            }
        },1);
    }

    //public shit

    public void updateScreen(){
        if(!loading) {
            loading = true;
            ((TextView) splashDlg.findViewById(R.id.text_splash_info))
                    .setText("Clearing the list");
            splashDlg.show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //run
                    //clear the list on the screen
                    for (int i = getChildCount() - 1; i >= 0; i--) {
                        final PodView podView = ((PodView) getChildAt(i));
                        podView.cancelAndWait();
                        removeView(podView);
                    }
                    splashDlg.dismiss();
                    //get all podcasts and loop
                    ArrayList<Podcast> pods = dbHelper.getAllPodcasts(onlyVis);
                    if (pods.size() > 0) {
                        addPodView(pods, 0);
                    } else {
                        loading = false;
                    }
                }
            }, 500);
        }
    }

    public boolean toggleVis(){
        if(!loading) {
            onlyVis = !onlyVis;
            updateScreen();
        }
        return onlyVis;
    }

    public void updateList(final String url){
        if(!loading) {
            //check connection
            ConnectivityManager connMgr = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                DialogFactory.showAlertDlg(context, "Podcast list update", "No network connection available.");
                return;
            }
            //download xml
            ((TextView) splashDlg.findViewById(R.id.text_splash_info))
                    .setText("Downloading XML");
            splashDlg.show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final XmlHelper xmlHelper = new XmlHelper(new XmlHelper.OnPodcastFound() {
                        @Override
                        public void onPodcast(Podcast podcast) {
                            if (dbHelper.addPodcastIfNotExist(podcast)) {
                                //callBacks.onSetInfo(Integer.toString(dbHelper.getCount())+" podcasts");
                            }
                        }

                        @Override
                        public void onError(String info) {
                            splashDlg.dismiss();
                            DialogFactory.showAlertDlg(context, "Podcast list update!", info);
                        }

                        @Override
                        public void onFinished() {
                            updateScreen();
                        }
                    });
                    xmlHelper.execute(url);
                }
            }, 500);
        }
    }

    public void resetList(final String url){
        if(!loading) {
            DialogFactory.showConfirmDlg(
                    context, "Delete content",
                    "Are you sure you want to delete all content and start from fresh?",
                    new DialogFactory.CallbackDialogFactory() {
                        @Override
                        public void onConfirm() {
                            ((TextView) splashDlg.findViewById(R.id.text_splash_info))
                                    .setText("Deleting all data");
                            splashDlg.show();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    for (PodView podView : getSafeList()) {
                                        podView.deletePodcast();
                                    }
                                    dbHelper.deleteAllPodcasts();
                                    updateList(url);
                                }
                            }, 500);
                        }
                    });
        }
    }

    public AuPlay.AuPlayItem getNextPlayerItem(AuPlay.AuPlayItem source){
        int pos = indexOfChild((PodView) source.id);
        PodView nextPV = null;
        while( nextPV == null && pos > 0){
            pos--;
            nextPV = (PodView)getChildAt(pos);
            if(FileOps.podcastFileExists(nextPV.getPodcast())) {
                AuPlay.AuPlayItem item = auPlay.AuPlayItemCreate();
                item.title = nextPV.getPodcast().title;
                item.uri = FileOps.getStoredFile(nextPV.getPodcast());
                item.id = nextPV;
                return item;
            }else{
                nextPV = null;
            }
        }
        return null;
    }

    public void savePlayerItemState(AuPlay.AuPlayItem source){
        PodView podView = (PodView)source.id;
        dbHelper.updatePodcastPos(podView.getPodcast(),source.position);
        podView.updateUIStatus();
    }

    //===================================================
    //callbacks

    @Override
    public void onShrinkAll(){
        for(PodView podView : getSafeList()) {
            podView.setButtonBarVisibility(false, false);
        }
    }
    @Override
    public void onDeleteAllOlder(final Podcast podcast){
        if(!loading) {
            ((TextView) splashDlg.findViewById(R.id.text_splash_info))
                    .setText("Deleting all podcasts that are older than this one");
            splashDlg.show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (PodView podView : getSafeList()) {
                        podView.deletePodcastIfOlder(podcast);
                    }
                    splashDlg.dismiss();
                }
            }, 500);
        }
    }
    @Override
    public void onDeletePodView(PodView podView){
        if(onlyVis){
            removeView(podView);
        }
    }
    @Override
    public void onPlayPodcast(PodView podView){
        AuPlay.AuPlayItem item = auPlay.AuPlayItemCreate();
        item.title  = podView.getPodcast().title;
        item.uri = FileOps.getStoredFile(podView.getPodcast());
        item.id = podView;
        item.position = podView.getPodcast().position;
        auPlay.playNewAudio(item);
    }

}
