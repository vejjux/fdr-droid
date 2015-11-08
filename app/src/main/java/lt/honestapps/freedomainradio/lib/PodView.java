package lt.honestapps.freedomainradio.lib;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lt.honestapps.freedomainradio.R;

/**
 * Created by vejjux on 15.10.5.
 */
public class PodView extends LinearLayout{

    public interface CallbackPodView{
        void onShrinkAll();
        void onDeleteAllOlder(Podcast podcast);
        void onDeletePodView(PodView podView);
        void onPlayPodcast(PodView podView);
    }

    private Podcast podcast;
    private DownloadTask downloadTask = null;

    private DbHelper dbHelper;
    private CallbackPodView callback;

    public PodView(final Context context, DbHelper pDbHelper, final CallbackPodView pCallbackPodView){
        super(context);
        dbHelper = pDbHelper;
        callback = pCallbackPodView;
        final PodView finalThis = this;

        //Inflate layout
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.podcast_box, this);

        //Hide the button bar
        findViewById(R.id.layout_podbuttons).setVisibility(GONE);

        //Hide the description box
        findViewById(R.id.text_poddescription).setVisibility(GONE);

        //==========================================================
        // Button handlers

        //more/less button
        ImageButton buttonExpand = (ImageButton)findViewById(R.id.button_expand);
        buttonExpand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonBarVisibility(true, true);
            }
        });

        //delete all older button
        ImageButton buttonDeleteO = (ImageButton)findViewById(R.id.button_pb_deleteolder);
        buttonDeleteO.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFactory.showConfirmDlg(
                        context, "Delete older podcasts",
                        "Are you sure you want to delete all podcasts that are older than this one and haven't been downloaded?",
                        new DialogFactory.CallbackDialogFactory() {
                            @Override
                            public void onConfirm() {
                                callback.onDeleteAllOlder(podcast);
                            }
                        });
            }
        });

        //delete button
        ImageButton buttonDelete = (ImageButton)findViewById(R.id.button_pb_delete);
        buttonDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFactory.showConfirmDlg(
                        context, "Delete podcast",
                        "Are you sure you want to delete this podcast?",
                        new DialogFactory.CallbackDialogFactory() {
                            @Override
                            public void onConfirm() {
                                deletePodcast();
                            }
                        });
            }
        });

        //restore button
        ImageButton buttonRestore = (ImageButton)findViewById(R.id.button_pb_restore);
        buttonRestore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.updatePodcastVis(podcast, true);
                updateUIStatus();
            }
        });

        //delete file
        ImageButton buttonDeleteFile = (ImageButton)findViewById(R.id.button_pb_deletefile);
        buttonDeleteFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFactory.showConfirmDlg(
                        context, "Delete podcast file",
                        "Are you sure you want to delete the downloaded podcast file?",
                        new DialogFactory.CallbackDialogFactory() {
                            @Override
                            public void onConfirm() {
                                FileOps.deleteFileIfExists(podcast);
                                FileOps.updateStatus(podcast);
                                updateUIStatus();
                            }
                        });
            }
        });

        //save button
        ImageButton buttonDownload = (ImageButton)findViewById(R.id.button_pb_download);
        buttonDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            if(!podcast.isDownloading) {
                downloadTask = new DownloadTask(context, podcast, new DownloadTask.CallbackDownloadTask() {
                    @Override
                    public void onProgress(String info) {
                        TextView text_download = (TextView) findViewById(R.id.text_download);
                        text_download.setText(info);
                    }
                    @Override
                    public void onError(String info) {
                        podcast.isDownloading = false;
                        FileOps.updateStatus(podcast);
                        updateUIStatus();
                        DialogFactory.showAlertDlg(context, "Download error!", info);
                    }
                    @Override
                    public void onCanceled(){
                        podcast.isDownloading = false;
                        FileOps.deleteFileIfExists(podcast);
                        FileOps.updateStatus(podcast);
                        updateUIStatus();
                    }
                    @Override
                    public void onFinished() {
                        podcast.isDownloading = false;
                        FileOps.updateStatus(podcast);
                        updateUIStatus();
                    }
                });
                podcast.isDownloading = true;
                downloadTask.execute();
                ((TextView)findViewById(R.id.text_download)).setText("Queued");
                updateUIStatus();
            }
            }
        });

        //cancel button
        ImageButton buttonCancel = (ImageButton)findViewById(R.id.button_pb_downloadcancel);
        buttonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelIfDownloading();
            }
        });

        //play button
        ImageButton buttonPlay = (ImageButton)findViewById(R.id.button_pb_play);
        buttonPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onPlayPodcast(finalThis);
            }
        });

        //info button
        ImageButton buttonInfo = (ImageButton)findViewById(R.id.button_pb_info);
        buttonInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(findViewById(R.id.text_poddescription).getVisibility() == GONE) {
                    findViewById(R.id.text_poddescription).setVisibility(VISIBLE);
                }else{
                    findViewById(R.id.text_poddescription).setVisibility(GONE);
                }
            }
        });

        // End of button handlers
        // End of constructor
    }

    /*=============================================================
    Basic functionality
     */

    public Podcast getPodcast(){
        return podcast;
    }

    public void setPod(Podcast podcast){
        this.podcast = podcast;
        //try to get the prefix
        String prefix = "";
        if(podcast.title.startsWith("FDR")) {
            int ispace = podcast.title.indexOf(" ");
            prefix = podcast.title.substring(0, ispace);
            podcast.title = podcast.title.replace(prefix+" ", "");
            prefix += ": ";
        }
        //set up text fields
        ((TextView)findViewById(R.id.text_podtitle)).setText(podcast.title);
        ((TextView)findViewById(R.id.text_poddescription)).setText(Html.fromHtml(podcast.description).toString());
        /*((WebView)findViewById(R.id.text_pb_description)).loadData(
                "<html><body style=\"text-align:justify;color:#888888;background-color:#333333;\">"+
                podcast.description+
                "</body></html>",
                "text/html", "utf-8"
        );*/
        //title bar
        Calendar cp = Calendar.getInstance();
        cp.setTimeInMillis(podcast.unixtime * 1000);
        Date date= new Date(podcast.unixtime * 1000);
        final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
        String pd = dateFormat.format(date);
        TextView txt_info = (TextView)findViewById(R.id.text_podinfo);
        txt_info.setText(prefix + pd + ", " + podcast.duration);
        updateUIStatus();
    }

    public void setButtonBarVisibility(boolean swap,boolean vis){
        RelativeLayout layout_buttons = (RelativeLayout)findViewById(R.id.layout_podbuttons);
        ImageButton button_more = (ImageButton)findViewById(R.id.button_expand);
        if(swap) {
            if (layout_buttons.getVisibility() == VISIBLE) {
                layout_buttons.setVisibility(GONE);
                findViewById(R.id.text_poddescription).setVisibility(GONE);
                button_more.setImageResource(R.drawable.ic_more);
            } else {
                callback.onShrinkAll();
                layout_buttons.setVisibility(VISIBLE);
                button_more.setImageResource(R.drawable.ic_less);
            }
        }else{
            findViewById(R.id.text_poddescription).setVisibility(vis?VISIBLE:GONE);
            layout_buttons.setVisibility(vis?VISIBLE:GONE);
            button_more.setImageResource(vis?R.drawable.ic_less:R.drawable.ic_more);
        }
    }



    public void updateUIStatus(){
        TextView text_progress = (TextView)findViewById(R.id.text_progress);
        TextView text_download = (TextView)findViewById(R.id.text_download);
        ImageButton button_deleteold = (ImageButton)findViewById(R.id.button_pb_download);
        ImageButton button_delete = (ImageButton)findViewById(R.id.button_pb_delete);
        ImageButton button_restore = (ImageButton)findViewById(R.id.button_pb_restore);
        ImageButton button_download = (ImageButton)findViewById(R.id.button_pb_download);
        ImageButton button_cancel = (ImageButton)findViewById(R.id.button_pb_downloadcancel);
        ImageButton button_deletefile = (ImageButton)findViewById(R.id.button_pb_deletefile);
        ImageButton button_play = (ImageButton)findViewById(R.id.button_pb_play);

        //                DelA   Del    Restr  Save   Cancl  DelF   Play
        boolean[] viss = {true,  true,  false, false, false, false, false};

        //visibility
        viss[1] = podcast.hidden ? false : true;
        viss[2] = !viss[1];

        //file stuff
        if(podcast.isDownloading){
            viss[1] = false;
            viss[4] = true;
        }else {
            if (podcast.fileSize > 0) {
                text_download.setText(Integer.toString(podcast.fileSize) + "MB");
                viss[5] = true;
                viss[6] = true;
            } else {
                text_download.setText("");
                viss[3] = true;
            }
        }

        //progress stuff
        if(podcast.position > 0){
            text_progress.setText("("+AuPlay.millisToString(podcast.position)+")");
        }else{
            text_progress.setText("");
        }

        button_deleteold.setVisibility(viss[0]?VISIBLE:GONE);
        button_delete.setVisibility(viss[1]?VISIBLE:GONE);
        button_restore.setVisibility(viss[2]?VISIBLE:GONE);
        button_download.setVisibility(viss[3]?VISIBLE:GONE);
        button_cancel.setVisibility(viss[4]?VISIBLE:GONE);
        button_deletefile.setVisibility(viss[5]?VISIBLE:GONE);
        button_play.setVisibility(viss[6]?VISIBLE:GONE);
    }

    private void cancelIfDownloading(){
        if(downloadTask!=null && !downloadTask.isCancelled() && podcast.isDownloading){
            downloadTask.cancel(false);
        }
    }

    public void cancelAndWait(){
        cancelIfDownloading();
        while(podcast.isDownloading){
            try {
                Thread.sleep(1,0);
                FileOps.deleteFileIfExists(podcast);
            } catch (InterruptedException e) {}
        }
    }

    public void deletePodcast(){
        cancelAndWait();
        FileOps.deleteFileIfExists(podcast);
        dbHelper.updatePodcastVis(podcast, false);
        FileOps.updateStatus(podcast);
        updateUIStatus();
        callback.onDeletePodView(this);
    }

    public void deletePodcastIfOlder(Podcast thenPodcast){
        if(podcast.unixtime<thenPodcast.unixtime && !FileOps.podcastFileExists(podcast)) {
            deletePodcast();
        }
    }

    public void deletePodcastNoUI(){
        cancelAndWait();
        FileOps.deleteFileIfExists(podcast);
        dbHelper.updatePodcastVis(podcast, false);
        FileOps.updateStatus(podcast);
    }

}
