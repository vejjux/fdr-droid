package lt.honestapps.freedomainradio.lib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import lt.honestapps.freedomainradio.R;

public class DialogFactory {

    public interface CallbackDialogFactory{
        void onConfirm();
    }

    //generic error dialog
    public static void showAlertDlg(Context context,String title,String msg){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //generic confirmation dialog
    public static void showConfirmDlg(Context context,String title,String msg, final CallbackDialogFactory callback){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onConfirm();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //help dialog
    public static void showHelpDlg(Context context){
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.help_box);
        dialog.setTitle("Podcast player usage");
        dialog.findViewById(R.id.button_dismiss_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    //splash dialog
    public static Dialog createSplashDlg(Context context, String title, String msg){
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.splash_box);
        dialog.setTitle(title);
        dialog.getWindow().setGravity(Gravity.CENTER_HORIZONTAL);
        ((TextView)dialog.findViewById(R.id.text_splash_info)).setText(msg);
        //((WebView)dialog.findViewById(R.id.webview_splashlogo))
        //        .loadUrl("file:///android_res/raw/fdr.gif");
        return dialog;
    }

    //settings dialog
    public static Dialog createSettingsDlg(Context context,View.OnClickListener resetClickListener){
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.settings_box);
        dialog.setTitle("Settings");
        dialog.getWindow().setGravity(Gravity.CENTER_HORIZONTAL);
        ((WebView)dialog.findViewById(R.id.web_settingslogo))
                .loadUrl("file:///android_res/raw/fdr.gif");
        dialog.findViewById(R.id.button_settings_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.button_reset_app).setOnClickListener(resetClickListener);
        return dialog;
    }

}
