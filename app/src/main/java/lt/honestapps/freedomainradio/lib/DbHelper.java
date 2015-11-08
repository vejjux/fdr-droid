package lt.honestapps.freedomainradio.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;

import lt.honestapps.freedomainradio.lib.Podcast;

/**
 * Created by budulis on 03/10/15.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "FdrPodcasts";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(
            "CREATE TABLE Podcast (" +
            "    idPodcast TEXT PRIMARY KEY," +
            "    title TEXT," +
            "    description TEXT," +
            "    link TEXT," +
            "    pubdate TEXT," +
            "    duration TEXT," +
            "    keywords TEXT," +
            "    position INTEGER," +
            "    played INTEGER," +
            "    hidden INTEGER" +
            ");"
        );
        db.execSQL(
                "CREATE UNIQUE INDEX `ix_idPodcast`" +
                        "ON `Podcast` (`idPodcast` ASC);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Podcast;");
        db.execSQL("DROP INDEX IF EXISTS ix_idPodcast;");
        this.onCreate(db);
    }

    public int getCount(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =
                db.query("Podcast",
                        new String[] { "idPodcast" },
                        null,
                        null,
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        null); // h. limit
        if (cursor != null) return cursor.getCount();
        return 0;
    }

    public boolean isPodcast(String idPodcast){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =
                db.query("Podcast",
                        new String[] { "idPodcast" },
                        "idPodcast = ?",
                        new String[] { idPodcast },
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        "1"); // h. limit

        if (cursor != null && cursor.getCount() > 0) return true;
        else return false;
    }

    /*public Podcast getPodcast(String idPodcast){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =
                db.query("Podcast",
                        new String[] { "*" },
                        "idPodcast = ?",
                        new String[] { idPodcast },
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        "1"); // h. limit
        if (cursor == null) return null;
        cursor.moveToFirst();
        Podcast podcast = new Podcast(idPodcast);
        podcast.title = cursor.getString(cursor.getColumnIndex("title"));
        podcast.description = cursor.getString(cursor.getColumnIndex("description"));
        podcast.link = cursor.getString(cursor.getColumnIndex("link"));
        podcast.pubdate = cursor.getString(cursor.getColumnIndex("pubdate"));
        podcast.duration = cursor.getString(cursor.getColumnIndex("duration"));
        podcast.keywords = cursor.getString(cursor.getColumnIndex("keywords"));
        podcast.position = cursor.getInt(cursor.getColumnIndex("position"));
        podcast.played = cursor.getInt(cursor.getColumnIndex("played")) == 0 ? false : true;
        podcast.hidden = cursor.getInt(cursor.getColumnIndex("hidden")) == 0 ? false : true;
        return podcast;
    }*/

    public boolean addPodcastIfNotExist(Podcast podcast){
        if(isPodcast(podcast.idPodcast) == false) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("idPodcast",podcast.idPodcast);
            values.put("title",podcast.title);
            values.put("description",podcast.description);
            values.put("link",podcast.link);
            values.put("pubdate",podcast.pubdate);
            values.put("duration",podcast.duration);
            values.put("keywords",podcast.keywords);
            values.put("position",0);
            values.put("played",0);
            values.put("hidden",0);
            db.insert("Podcast", null, values);
            db.close();
            return true;
        }
        return false;
    }

    public void deleteAllPodcasts(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Podcast",null,null);
    }

    public void updatePodcastVis(Podcast podcast,boolean vis){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hidden",vis?"0":"1");
        db.update("Podcast",
                values,
                "`idPodcast`=?",
                new String[]{podcast.idPodcast});
        podcast.hidden = !vis;
    }

    public void updatePodcastPos(Podcast podcast,int pos){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("position",pos);
        db.update("Podcast",
                values,
                "`idPodcast`=?",
                new String[]{podcast.idPodcast});
        podcast.position = pos;
    }

    public ArrayList<Podcast> getAllPodcasts(boolean visible){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =
                db.query("Podcast",
                        new String[] { "*,strftime('%s',pubdate) AS `unixtime`" },
                        visible ? "hidden = ?" : null,
                        visible ? new String[] {  "0" } : null,
                        null, // e. group by
                        null, // f. having
                        "`unixtime` DESC", // g. order by
                        null); // h. limit
        if (cursor == null) return null;
        ArrayList<Podcast> pods = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                Podcast podcast = new Podcast();
                podcast.idPodcast = cursor.getString(cursor.getColumnIndex("idPodcast"));
                podcast.title = cursor.getString(cursor.getColumnIndex("title"));
                podcast.description = cursor.getString(cursor.getColumnIndex("description"));
                podcast.link = cursor.getString(cursor.getColumnIndex("link"));
                podcast.pubdate = cursor.getString(cursor.getColumnIndex("pubdate"));
                podcast.duration = cursor.getString(cursor.getColumnIndex("duration"));
                podcast.keywords = cursor.getString(cursor.getColumnIndex("keywords"));
                podcast.position = cursor.getInt(cursor.getColumnIndex("position"));
                podcast.played = cursor.getInt(cursor.getColumnIndex("played")) == 0 ? false : true;
                podcast.hidden = cursor.getInt(cursor.getColumnIndex("hidden")) == 0 ? false : true;
                podcast.unixtime = cursor.getLong(cursor.getColumnIndex("unixtime"));
                pods.add(podcast);
            } while (cursor.moveToNext());
        }
        return pods;
    }

}
