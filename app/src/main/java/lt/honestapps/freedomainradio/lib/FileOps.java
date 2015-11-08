package lt.honestapps.freedomainradio.lib;

import android.os.Environment;

import java.io.File;

/**
 * Created by vejjux on 15.10.9.
 */
public class FileOps {

    public static boolean canWriteToStorage(){
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean canReadFromStorage(){
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state)||Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static String getStoredFile(Podcast podcast){
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/FreeDomainRadio");
        dir.mkdirs();
        File file = new File(dir, podcast.idPodcast+".mp3");
        return file.getAbsolutePath();
    }

    public static void updateStatus(Podcast podcast){
        File file = new File(FileOps.getStoredFile(podcast));
        if(file.exists()){
            podcast.fileSize = (int)(file.length()/(1024*1024));
        }else{
            podcast.fileSize = -1;
        }
    }

    public static void deleteFileIfExists(Podcast podcast){
        File file = new File(FileOps.getStoredFile(podcast));
        if(file.exists()){
            file.delete();
        }
    }

    public static boolean podcastFileExists(Podcast podcast){
        File file = new File(FileOps.getStoredFile(podcast));
        return file.exists();
    }

}
