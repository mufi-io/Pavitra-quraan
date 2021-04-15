package com.scalosphere.labs.kquran.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.common.QuranAyah;
import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.data.QuranInfo;
import com.scalosphere.labs.kquran.service.util.AudioRequest;
import com.scalosphere.labs.kquran.service.util.DownloadAudioRequest;

import java.io.File;
import java.util.Locale;

public class AudioUtils {
   private static final String TAG = "AudioUtils";
   public static final String DB_EXTENSION = ".db";
   public static final String AUDIO_EXTENSION = ".mp3";
   public static final String ZIP_EXTENSION = ".zip";
   private static String AUDIO_DIRECTORY = "audio";


   public final static class LookAheadAmount {
      public static final int PAGE = 1;
      public static final int SURA = 2;
      public static final int JUZ = 3;

      // make sure to update these when a lookup type is added
      public static final int MIN = 1;
      public static final int MAX = 3;
   }

   private static String[] mQariBaseUrls = null;
   private static String[] mQariFilePaths = null;
   private static String[] mQariDatabaseFiles = null;

   public static String getQariUrl(Context context, int position,
                                   boolean addPlaceHolders){
      if (mQariBaseUrls == null){
         mQariBaseUrls = context.getResources()
                 .getStringArray(R.array.quran_readers_urls);
      }

      if (position >= mQariBaseUrls.length || 0 > position){ return null; }
      String url = mQariBaseUrls[position];
      if (addPlaceHolders){
         if (isQariGapless(context, position)){
            Log.d(TAG, "qari is gapless...");
            url += "%03d" + AudioUtils.AUDIO_EXTENSION;
         }
         else { url += "%03d%03d" + AudioUtils.AUDIO_EXTENSION; }
      }
      return url;
   }

   public static String getLocalQariUrl(Context context, int position){
      if (mQariFilePaths == null){
         mQariFilePaths = context.getResources()
                 .getStringArray(R.array.quran_readers_path);
      }

      String rootDirectory = getAudioRootDirectory(context);
      return rootDirectory == null? null :
              rootDirectory + mQariFilePaths[position];
   }

   public static boolean isQariGapless(Context context, int position){
      return getQariDatabasePathIfGapless(context, position) != null;
   }

   public static String getQariDatabasePathIfGapless(Context context,
                                                     int position){
      if (mQariDatabaseFiles == null){
         mQariDatabaseFiles = context.getResources()
                 .getStringArray(R.array.quran_readers_db_name);
      }

      if (position > mQariDatabaseFiles.length){ return null; }

      String dbname = mQariDatabaseFiles[position];
      Log.d(TAG, "got dbname of: " + dbname + " for qari");
      if (TextUtils.isEmpty(dbname)){ return null; }

      String path = getLocalQariUrl(context, position);
      if (path == null){ return null; }
      String overall = path + File.separator +
              dbname + DB_EXTENSION;
      Log.d(TAG, "overall path: " + overall);
      return overall;
   }

   public static boolean shouldDownloadGaplessDatabase(
           Context context, DownloadAudioRequest request){
      if (!request.isGapless()){ return false; }
      String dbPath = request.getGaplessDatabaseFilePath();
      if (TextUtils.isEmpty(dbPath)){ return false; }

      File f = new File(dbPath);
      return !f.exists();
   }

   public static String getGaplessDatabaseUrl(
           Context context, DownloadAudioRequest request){
      if (!request.isGapless()){ return null; }
      int qariId = request.getQariId();

      if (mQariDatabaseFiles == null){
         mQariDatabaseFiles = context.getResources()
                 .getStringArray(R.array.quran_readers_db_name);
      }

      if (qariId > mQariDatabaseFiles.length){ return null; }

      String dbname = mQariDatabaseFiles[qariId] + ZIP_EXTENSION;
      return QuranFileUtils.getGaplessDatabaseRootUrl() + "/" + dbname;
   }

   public static QuranAyah getLastAyahToPlay(QuranAyah startAyah,
                                             int page, int mode,
                                             boolean isDualPages){
      if (isDualPages && mode == LookAheadAmount.PAGE && (page % 2 == 1)){
         // if we download page by page and we are currently in tablet mode
         // and playing from the right page, get the left page as well.
         page++;
      }

      int pageLastSura = 114;
      int pageLastAyah = 6;
      if (page > Constants.PAGES_LAST || page < 0){ return null; }
      if (page < Constants.PAGES_LAST){
         int nextPageSura = QuranInfo.PAGE_SURA_START[page];
         int nextPageAyah = QuranInfo.PAGE_AYAH_START[page];

         pageLastSura = nextPageSura;
         pageLastAyah = nextPageAyah - 1;
         if (pageLastAyah < 1){
            pageLastSura--;
            if (pageLastSura < 1){ pageLastSura = 1; }
            pageLastAyah = QuranInfo.getNumAyahs(pageLastSura);
         }
      }

      if (mode == LookAheadAmount.SURA){
         int sura = startAyah.getSura();
         int lastAyah = QuranInfo.getNumAyahs(sura);
         if (lastAyah == -1){ return null; }

         // if we start playback between two suras, download both suras
         if (pageLastSura > sura){
            sura = pageLastSura;
            lastAyah = QuranInfo.getNumAyahs(sura);
         }
         return new QuranAyah(sura, lastAyah);
      }
      else if (mode == LookAheadAmount.JUZ){
         int juz = QuranInfo.getJuzFromPage(page);
         if (juz == 30){
            return new QuranAyah(114, 6);
         }
         else if (juz >= 1 && juz < 30){
            int[] endJuz = QuranInfo.QUARTERS[juz * 8];
            if (pageLastSura > endJuz[0]){
               // ex between jathiya and a7qaf
               endJuz = QuranInfo.QUARTERS[(juz+1) * 8];
            }
            else if (pageLastSura == endJuz[0] &&
                     pageLastAyah > endJuz[1]){
               // ex surat al anfal
               endJuz = QuranInfo.QUARTERS[(juz+1) * 8];
            }

            return new QuranAyah(endJuz[0], endJuz[1]);
         }
      }

      // page mode (fallback also from errors above)
      return new QuranAyah(pageLastSura, pageLastAyah);
   }

   public static boolean shouldDownloadBasmallah(Context context,
                                                 DownloadAudioRequest request){
      if (request.isGapless()){ return false; }
      String baseDirectory = request.getLocalPath();
      if (!TextUtils.isEmpty(baseDirectory)){
         File f = new File(baseDirectory);
         if (f.exists()){
            String filename = 1 + File.separator + 1 + AUDIO_EXTENSION;
            f = new File(baseDirectory + File.separator + filename);
            if (f.exists()){
               android.util.Log.d(TAG, "already have basmalla...");
               return false; }
         }
         else {
            f.mkdirs();
         }
      }

      return doesRequireBasmallah(request);
   }

   public static boolean haveSuraAyahForQari(String baseDir, int sura, int ayah){
      String filename = baseDir + File.separator + sura +
              File.separator + ayah + AUDIO_EXTENSION;
      File f = new File(filename);
      return f.exists();
   }

   private static boolean doesRequireBasmallah(AudioRequest request){
      QuranAyah minAyah = request.getMinAyah();
      int startSura = minAyah.getSura();
      int startAyah = minAyah.getAyah();

      QuranAyah maxAyah = request.getMaxAyah();
      int endSura = maxAyah.getSura();
      int endAyah = maxAyah.getAyah();

      android.util.Log.d(TAG, "seeing if need basmalla...");

      for (int i = startSura; i <= endSura; i++){
         int lastAyah = QuranInfo.getNumAyahs(i);
         if (i == endSura){ lastAyah = endAyah; }
         int firstAyah = 1;
         if (i == startSura){ firstAyah = startAyah; }

         for (int j = firstAyah; j < lastAyah; j++){
            if (j == 1 && i != 1 && i != 9){
               android.util.Log.d(TAG, "need basmalla for " + i + ":" + j);

               return true;
            }
         }
      }

      return false;
   }

   public static boolean haveAllFiles(DownloadAudioRequest request){
      String baseDirectory = request.getLocalPath();
      if (TextUtils.isEmpty(baseDirectory)){ return false; }

      boolean isGapless = request.isGapless();
      File f = new File(baseDirectory);
      if (!f.exists()){
         f.mkdirs();
         return false;
      }

      QuranAyah minAyah = request.getMinAyah();
      int startSura = minAyah.getSura();
      int startAyah = minAyah.getAyah();

      QuranAyah maxAyah = request.getMaxAyah();
      int endSura = maxAyah.getSura();
      int endAyah = maxAyah.getAyah();

      for (int i = startSura; i <= endSura; i++){
         int lastAyah = QuranInfo.getNumAyahs(i);
         if (i == endSura){ lastAyah = endAyah; }
         int firstAyah = 1;
         if (i == startSura){ firstAyah = startAyah; }

         if (isGapless){
            if (i == endSura && endAyah == 0){ continue; }
            String p = request.getBaseUrl();
            String fileName = String.format(Locale.US, p, i);
            Log.d(TAG, "gapless, checking if we have " + fileName);
            f = new File(fileName);
            if (!f.exists()){ return false; }
            continue;
         }

         Log.d(TAG, "not gapless, checking each ayah...");
         for (int j = firstAyah; j <= lastAyah; j++){
            String filename = i + File.separator + j + AUDIO_EXTENSION;
            f = new File(baseDirectory + File.separator + filename);
            if (!f.exists()){ return false; }
         }
      }

      return true;
   }

   public static String getAudioRootDirectory(Context context){
      String s = QuranFileUtils.getQuranBaseDirectory(context);
      return (s == null)? null : s + AUDIO_DIRECTORY + File.separator;
   }

   public static String getOldAudioRootDirectory(Context context){
      File f;
      String path;
      String sep = File.separator;

      if (android.os.Build.VERSION.SDK_INT >= 8){
         f = getExternalFilesDirectoryFroyo(context);
         path = sep + "audio" + sep;
      }
      else {
         f = Environment.getExternalStorageDirectory();
         path = sep + "Android" + sep + "data" + sep +
                 context.getPackageName() + sep + "files" + sep + "audio" + sep;
      }

      if (f == null){ return null; }
      return f.getAbsolutePath() + path;
   }

  @TargetApi(Build.VERSION_CODES.FROYO)
  private static File getExternalFilesDirectoryFroyo(Context context) {
    return context.getExternalFilesDir(null);
  }
}
