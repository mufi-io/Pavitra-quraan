package com.scalosphere.labs.kquran;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.util.QuranFileUtils;
import com.scalosphere.labs.kquran.util.QuranSettings;
import com.scalosphere.labs.kquran.util.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuranPreferenceActivity extends SherlockPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
  private static final String TAG =
      "QuranPreferenceActivity";

  private ListPreference mListStorageOptions;
  private MoveFilesAsyncTask mMoveFilesTask;
  //private ReadLogsTask mReadLogsTask;
  private List<StorageUtils.Storage> mStorageList;
  private LoadStorageOptionsTask mLoadStorageOptionsTask;
  private int mAppSize;
  private boolean mIsPaused;
  private String mInternalSdcardLocation;
  private AlertDialog mDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ((QuranApplication)getApplication()).refreshLocale(false);

    setTheme(R.style.QuranAndroid);
    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.menu_settings);
    }

    // add preferences
    addPreferencesFromResource(R.xml.quran_preferences);

    // remove the tablet mode preference if it doesn't exist
//    if (!QuranScreenInfo.getOrMakeInstance(this).isTablet(this)) {
//      Preference tabletModePreference =
//          findPreference(Constants.PREF_TABLET_ENABLED);
//      PreferenceCategory category =
//          (PreferenceCategory) findPreference(
//              Constants.PREF_DISPLAY_CATEGORY);
//      category.removePreference(tabletModePreference);
//    }

    Preference advancedPrefs = findPreference(
        getString(R.string.prefs_advanced_settings));
    advancedPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        mLoadStorageOptionsTask = new LoadStorageOptionsTask();
        mLoadStorageOptionsTask.execute();
        return false;
      }
    });

    /*
    // can enable this to get logs from users until we move it to
    // a setting. needs READ_LOGS permissions below jellybean. this
    // is a work around until we properly use the Logger framework
    // to roll our own log files.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mReadLogsTask = new ReadLogsTask();
      mReadLogsTask.execute();
    }
    */

    mInternalSdcardLocation =
        Environment.getExternalStorageDirectory().getAbsolutePath();

    mListStorageOptions = (ListPreference) findPreference(
        getString(R.string.prefs_app_location));

    final File[] mountPoints = ContextCompat.getExternalFilesDirs(this, null);
    if (mountPoints.length > 1) {
      mStorageList = new ArrayList<StorageUtils.Storage>();
      for (int i=0; i<mountPoints.length; i++) {
        final StorageUtils.Storage s;
        if (i == 0) {
          s = new StorageUtils.Storage(
              getString(R.string.prefs_sdcard_internal),
              mInternalSdcardLocation);
        } else if (mountPoints[i] != null) {
          s = new StorageUtils.Storage(
              getString(R.string.prefs_sdcard_external),
              mountPoints[i].getAbsolutePath());
        } else {
          s = null;
        }

        if (s != null) {
          mStorageList.add(s);
        }
      }
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      try {
        mStorageList = StorageUtils
            .getAllStorageLocations(getApplicationContext());
      } catch (Exception e) {
        mStorageList = new ArrayList<StorageUtils.Storage>();
      }
    }

    // Hide Advanced Preferences Screen if there is no storage option
    // except for the normal Environment.getExternalStorageDirectory
    if (mStorageList == null || mStorageList.size() <= 1) {
      Log.d(TAG, "removing advanced settings from preferences");
      getPreferenceScreen().removePreference(advancedPrefs);
    }
  }

  @Override
  protected void onDestroy() {
    if (mDialog != null) {
      mDialog.dismiss();
    }
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void loadStorageOptions() {
    try {
      CharSequence[] values = new CharSequence[mStorageList.size()];
      CharSequence[] displayNames = new CharSequence[mStorageList.size()];
      int i = 0;
      final HashMap<String, Integer> storageEmptySpaceMap =
          new HashMap<String, Integer>(mStorageList.size());
      for (StorageUtils.Storage storage : mStorageList) {
        values[i] = storage.getMountPoint();
        displayNames[i] = storage.getLabel() + " " +
            storage.getFreeSpace()
            + " " + getString(R.string.prefs_megabytes);
        i++;
        storageEmptySpaceMap.put(storage.getMountPoint(),
            storage.getFreeSpace());
      }

      String msg = getString(R.string.prefs_app_location_summary) + "\n"
          + getString(R.string.prefs_app_size) + " " + mAppSize
          + " " + getString(R.string.prefs_megabytes);
      mListStorageOptions.setSummary(msg);
      mListStorageOptions.setEntries(displayNames);
      mListStorageOptions.setEntryValues(values);

      String current = QuranSettings.getAppCustomLocation(this);
      if (TextUtils.isEmpty(current)){
        current = values[0].toString();
      }
      mListStorageOptions.setValue(current);

      mListStorageOptions.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          if (TextUtils.isEmpty(QuranSettings
              .getAppCustomLocation(QuranPreferenceActivity.this)) &&
              Environment.getExternalStorageDirectory().equals(newValue)){
            // do nothing since we're moving from empty settings to
            // the default sdcard setting, which are the same, but write it.
            return false;
          }

          // this is called right before the preference is saved
          String newLocation = (String)newValue;
          String current = QuranSettings.getAppCustomLocation(
              QuranPreferenceActivity.this);
          if (mAppSize < storageEmptySpaceMap.get(newLocation)) {
            if (current == null || !current.equals(newLocation)){
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ||
                  newLocation.equals(mInternalSdcardLocation)) {
                moveFiles(newLocation);
              } else {
                showKitKatConfirmation(newLocation);
              }
            }
          }
          else {
            // We have not included the feature of _moving files yet
            Toast.makeText(QuranPreferenceActivity.this,
                getString(
                    R.string.prefs_no_enough_space_to_move_files),
                Toast.LENGTH_LONG).show();
          }
          // this says, "don't write the preference"
          return false;
        }
      });
    }
    catch (Exception e) {
      Log.e(TAG, "error loading storage options", e);
    }
  }

  private void showKitKatConfirmation(final String newLocation) {
    final AlertDialog.Builder b = new AlertDialog.Builder(this)
        .setTitle(R.string.kitkat_external_title)
        .setMessage(R.string.kitkat_external_message)
        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            moveFiles(newLocation);
            dialog.dismiss();
            mDialog = null;
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            mDialog = null;
          }
        });
    mDialog = b.create();
    mDialog.show();
  }

  private void moveFiles(String newLocation) {
    mMoveFilesTask = new MoveFilesAsyncTask(newLocation);
    mMoveFilesTask.execute();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mIsPaused = false;
    getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    mIsPaused = true;
    getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
    super.onPause();
  }

  @Override
  public void onBackPressed() {
    // disable back press while task in progress
    if (mMoveFilesTask == null)
      super.onBackPressed();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    if (key.equals(Constants.PREF_USE_ARABIC_NAMES)) {
      ((QuranApplication) getApplication()).refreshLocale(true);
      Intent intent = getIntent();
      finish();
      startActivity(intent);
    }
  }

  private class MoveFilesAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private String newLocation;
    private ProgressDialog dialog;

    private MoveFilesAsyncTask(String newLocation) {
      this.newLocation = newLocation;
    }

    @Override
    protected void onPreExecute() {
      dialog = new ProgressDialog(QuranPreferenceActivity.this);
      dialog.setMessage(getString(R.string.prefs_copying_app_files));
      dialog.setCancelable(false);
      dialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      return QuranFileUtils.moveAppFiles(
          QuranPreferenceActivity.this, newLocation);
    }

    @Override
    protected void onPostExecute(Boolean result) {
      if (!mIsPaused){
        dialog.dismiss();
        if (result) {
          QuranSettings.setAppCustomLocation(
              QuranPreferenceActivity.this, newLocation);
          if (mListStorageOptions != null){
            mListStorageOptions.setValue(newLocation);
          }
        } else {
            // We have not included the feature of _moving files yet
          Toast.makeText(QuranPreferenceActivity.this,
              getString(R.string.prefs_err_moving_app_files),
              Toast.LENGTH_LONG).show();
        }
        dialog = null;
        mMoveFilesTask = null;
      }
    }
  }

  private class LoadStorageOptionsTask extends AsyncTask<Void, Void, Void> {

    private ProgressDialog dialog;

    @Override
    protected void onPreExecute() {
      dialog = new ProgressDialog(QuranPreferenceActivity.this);
      dialog.setMessage(getString(R.string.prefs_calculating_app_size));
      dialog.setCancelable(false);
      dialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
      mAppSize = QuranFileUtils.getAppUsedSpace(QuranPreferenceActivity.this);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (!mIsPaused){
        loadStorageOptions();
        mLoadStorageOptionsTask = null;
        dialog.dismiss();
        dialog = null;
      }
    }
  }

  /*
  private class ReadLogsTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... voids) {
      StringBuilder logs = new StringBuilder();
      try {
        Process process = Runtime.getRuntime().exec("logcat -d");
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
          logs.append(line).append("\n");
        }
      } catch (Exception e) {
        Log.d(TAG, "error reading logs", e);
      }
      return logs.toString();
    }

    @Override
    protected void onPostExecute(String logs) {
      if (mIsPaused){
        return;
      }

      Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
      emailIntent.setType("plain/text");

      String body = "\n\n";
      try {
        PackageInfo pInfo = getPackageManager()
            .getPackageInfo(getPackageName(), 0);
        body = pInfo.packageName + " Version: " + pInfo.versionName;
      } catch (Exception e) {
        // no handling for now
      }

      body += "\nPhone: " + android.os.Build.MANUFACTURER +
          " " + android.os.Build.MODEL;
      body += "\nAndroid Version: " + android.os.Build.VERSION.CODENAME + " "
          + android.os.Build.VERSION.RELEASE;

      body += QuranUtils.getDebugInfo(QuranPreferenceActivity.this);


      body += "\n\n";
      body += "\nLogs: " + logs;
      body += "\n\n";

      emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
      emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
          getString(R.string.email_logs_subject));
      emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
          new String[]{getString(R.string.email_to)});
      startActivity(Intent.createChooser(emailIntent,
          getString(R.string.send_email)));
    }
  }
  */
}
