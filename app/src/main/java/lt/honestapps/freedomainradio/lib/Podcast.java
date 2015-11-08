package lt.honestapps.freedomainradio.lib;

import java.util.Date;

/**
 * Created by budulis on 03/10/15.
 */
public class Podcast {

    public String idPodcast = null;
    public String title = null;
    public String description = null;
    public String link = null;
    public String pubdate = null;
    public String duration = null;
    public String keywords = null;

    public int position = 0;
    public boolean played = false;
    public boolean hidden = false;
    public long  unixtime = 0;

    public int fileSize = 0;
    public boolean isDownloading = false;

    public Podcast(){}

    public Podcast(String idPodcast){
        this.idPodcast = idPodcast;
    }

}
