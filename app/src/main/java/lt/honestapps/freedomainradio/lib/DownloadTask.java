package lt.honestapps.freedomainradio.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<Void, String, String> {

    public interface CallbackDownloadTask{
        void onProgress(String info);
        void onError(String info);
        void onCanceled();
        void onFinished();
    }

    private Context context;
    private PowerManager.WakeLock mWakeLock;
    private Podcast podcast;
    private CallbackDownloadTask callback;

    public DownloadTask(Context context, Podcast podcast, CallbackDownloadTask callbackDownloadTask) {
        this.context = context;
        this.podcast = podcast;
        this.callback = callbackDownloadTask;
    }

    //Async handlers
    @Override
    protected void onProgressUpdate(String... info) {
        callback.onProgress(info[0]);
    }

    @Override
    protected void onPostExecute(String info) {
        if(info != null){
            callback.onError(info);
        }else {
            if(isCancelled())
                callback.onCanceled();
            else
                callback.onFinished();
        }
    }

    @Override
    protected void onCancelled() {
        callback.onFinished();
    }

    @Override
    protected String doInBackground(Void... notused) {
        if(!FileOps.canWriteToStorage()){
            return "Cannot write to external storage.";
        }

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(podcast.link);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(FileOps.getStoredFile(podcast));

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1 && !isCancelled()) {
                // allow canceling with back button
                total += count;
                // publishing the progress....
                if (fileLength > 0) {
                    publishProgress(Integer.toString((int) (total * 100 / fileLength)) + "%");
                }else{
                    publishProgress(Integer.toString((int) (total / (1024 * 1024))) + "KB");
                }
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

}
