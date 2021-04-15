package com.scalosphere.labs.kquran.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.ui.QuranActivity;


public class QuranSettings {

    private static int textSize =Constants.DEFAULT_TEXT_SIZE;

    public static int getTextSize() {
        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(QuranActivity.getSharedContext());
        int prefSize= myPrefs.getInt(Constants.PREF_TRANSLATION_TEXT_SIZE, Constants.DEFAULT_TEXT_SIZE);
        //Log.i("QuranSettings", "size from Preferences in getTextSize "+prefSize);
        textSize=prefSize;
        return textSize;
    }

   public static boolean isArabicNames(Context context){
      return getBooleanPreference(context,
              Constants.PREF_USE_ARABIC_NAMES, false);
   }

   public static boolean isLockOrientation(Context context){
      return getBooleanPreference(context,
              Constants.PREF_LOCK_ORIENTATION, false);
   }

   public static boolean isLandscapeOrientation(Context context){
      return getBooleanPreference(context,
              Constants.PREF_LANDSCAPE_ORIENTATION, false);
   }

   public static boolean shouldStream(Context context){
      return getBooleanPreference(context,
              Constants.PREF_PREFER_STREAMING, false);
   }

   public static boolean needArabicFont(Context context){
     return Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
   }

   public static boolean isReshapeArabic(Context context){
      boolean defValue =
              (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH);
      return getBooleanPreference(context, Constants.PREF_USE_ARABIC_RESHAPER,
              defValue);
   }

   public static boolean isNightMode(Context context){
      return getBooleanPreference(context,
              Constants.PREF_NIGHT_MODE, false);
   }

   public static int getNightModeTextBrightness(Context context){
      SharedPreferences prefs =
              PreferenceManager.getDefaultSharedPreferences(context);
      return prefs.getInt(Constants.PREF_NIGHT_MODE_TEXT_BRIGHTNESS,
              Constants.DEFAULT_NIGHT_MODE_TEXT_BRIGHTNESS);
   }

   public static boolean shouldOverlayPageInfo(Context context){
      return getBooleanPreference(context,
            Constants.PREF_OVERLAY_PAGE_INFO, true);
   }

   public static boolean shouldDisplayMarkerPopup(Context context){
      return getBooleanPreference(context,
              Constants.PREF_DISPLAY_MARKER_POPUP, true);
   }

   public static boolean shouldHighlightBookmarks(Context context){
      return getBooleanPreference(context,
              Constants.PREF_HIGHLIGHT_BOOKMARKS, true);
   }

   public static boolean wantArabicInTranslationView(Context context){
      return getBooleanPreference(context,
              Constants.PREF_AYAH_BEFORE_TRANSLATION, true);
   }

   private static boolean getBooleanPreference(Context context,
                                               String pref,
                                               boolean defaultValue){
      SharedPreferences prefs =
              PreferenceManager.getDefaultSharedPreferences(context);
      return prefs.getBoolean(pref, defaultValue);
   }

   public static int getPreferredDownloadAmount(Context context){
      SharedPreferences prefs =
              PreferenceManager.getDefaultSharedPreferences(context);
      String str = prefs.getString(Constants.PREF_DOWNLOAD_AMOUNT,
                  "" + AudioUtils.LookAheadAmount.PAGE);
      int val = AudioUtils.LookAheadAmount.PAGE;
      try { val = Integer.parseInt(str); }
      catch (Exception e){}

      if (val > AudioUtils.LookAheadAmount.MAX ||
              val < AudioUtils.LookAheadAmount.MIN){
         return AudioUtils.LookAheadAmount.PAGE;
      }
      return val;
   }

   public static int getTranslationTextSize(Context context){
     SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
     textSize= myPrefs.getInt(Constants.PREF_TRANSLATION_TEXT_SIZE, Constants.DEFAULT_TEXT_SIZE);
     return textSize;
   }

   public static void setTranslationTextSize(int fontSize) {
       if(fontSize <Constants.DEFAULT_TEXT_SIZE-5){
           fontSize=Constants.DEFAULT_TEXT_SIZE-5;
       }
       SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(QuranActivity.getSharedContext());
       int prefSize= myPrefs.getInt(Constants.PREF_TRANSLATION_TEXT_SIZE, Constants.DEFAULT_TEXT_SIZE);
       if(fontSize!=prefSize){
           myPrefs.edit().putInt(Constants.PREF_TRANSLATION_TEXT_SIZE,fontSize).commit();
       }
       textSize = fontSize;
   }

    public static void setTwoLanguageView(boolean flag) {
        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(QuranActivity.getSharedContext());
        boolean twoLanguages= myPrefs.getBoolean(Constants.PREF_AYAH_BEFORE_TRANSLATION, true);
       // Log.i("Quran Settings ", " twoLanguages before updating "+twoLanguages);
        myPrefs.edit().putBoolean(Constants.PREF_AYAH_BEFORE_TRANSLATION,flag).commit();
        //Log.i("Quran Settings ", " twoLanguages after updating "+wantArabicInTranslationView(QuranActivity.getSharedContext()));
   }

  public static int getLastPage(Context context){
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getInt(Constants.PREF_LAST_PAGE, Constants.NO_PAGE_SAVED);
  }

   public static void setLastPage(Context context, int page){
      SharedPreferences prefs =
              PreferenceManager.getDefaultSharedPreferences(context);
      prefs.edit().putInt(Constants.PREF_LAST_PAGE, page).commit();
   }

   public static String getAppCustomLocation(Context context) {
      SharedPreferences prefs =
          PreferenceManager.getDefaultSharedPreferences(context);
      return prefs.getString(context.getString(R.string.prefs_app_location),
            Environment.getExternalStorageDirectory().getAbsolutePath());
   }

   public static void setAppCustomLocation(Context context, String newLocation) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
       prefs.edit().putString(context.getString(R.string.prefs_app_location), newLocation).commit();
   }
}
