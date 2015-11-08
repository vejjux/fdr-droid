package lt.honestapps.freedomainradio.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by vejjux on 15.10.4.
 */
public class XmlHelper extends AsyncTask<String, Podcast, String>{

    OnPodcastFound callback;

    public interface OnPodcastFound{
        void onPodcast(Podcast podcast);
        void onError(String info);
        void onFinished();
    }

    public XmlHelper(OnPodcastFound callbackPodcast ){
        this.callback = callbackPodcast;
    }

    //Async handlers
    @Override
    protected void onProgressUpdate(Podcast... podcasts) {
        callback.onPodcast(podcasts[0]);
    }

    @Override
    protected void onPostExecute(String info) {
        if(info != null){
            callback.onError(info);
        }else {
            callback.onFinished();
        }
    }

    @Override
    protected void onCancelled() {
        callback.onFinished();
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(sUrl[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // download the file
            input = connection.getInputStream();

            // start xml parser
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(input, null);
            parser.nextTag();
            readFeed(parser);

        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (input != null)
                    input.close();
                if (connection != null)
                    connection.disconnect();
            } catch (Exception e) {}
        }
        return null;
    }

    private void readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        Podcast podcast = null;
        String name = null;
        final DateFormat dFParse  = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss");
        final DateFormat dFFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        while (eventType != XmlPullParser.END_DOCUMENT){
            switch (eventType){
                case XmlPullParser.START_DOCUMENT:
                    //---
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase("item")){
                        podcast = new Podcast();
                    } else if (podcast != null){
                        if (name.equalsIgnoreCase("title")){
                            podcast.title = parser.nextText();
                            podcast.idPodcast = MD5(podcast.title);
                        } else if (name.equalsIgnoreCase("itunes:summary")){
                            podcast.description = parser.nextText();
                        } else if (name.equalsIgnoreCase("link")){
                            podcast.link = parser.nextText();
                        } else if (name.equalsIgnoreCase("pubdate")){
                            try {
                                Date date = dFParse.parse(parser.nextText().replace(" GMT", ""));
                                podcast.pubdate = dFFormat.format(date).toString();
                            }catch(Exception e){}
                        } else if (name.equalsIgnoreCase("itunes:duration")){
                            podcast.duration = parser.nextText();
                        } else if (name.equalsIgnoreCase("itunes:keywords")){
                            podcast.keywords = parser.nextText();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase("item") && podcast != null){
                        publishProgress(podcast);
                    }
            }
            eventType = parser.next();
        }
    }

    private String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

}
