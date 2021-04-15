package com.scalosphere.labs.kquran;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.util.QuranSettings;

import java.util.Locale;

//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;

/**
 * Created by ahmedre on 8/3/13.
 */
public class QuranApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Fabric.with(this, new Crashlytics());
        if (Constants.CRASH_REPORTING_ENABLED) {
            //TODO enable it with new code mapping scalosphere
            //Crashlytics.start(this);
        }
        refreshLocale(false);
    }

    public void refreshLocale(boolean force) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String language = QuranSettings.isArabicNames(this) ? "ar" : null;

        Locale locale = null;
        if ("ar".equals(language)) {
            locale = new Locale("ar");
        } else if (force) {
            // get the system locale (since we overwrote the default locale)
            locale = Resources.getSystem().getConfiguration().locale;
        }

        if (locale != null) {
            Locale.setDefault(locale);
            Configuration config = getResources().getConfiguration();
            config.locale = locale;
            getResources().updateConfiguration(config,
                    getResources().getDisplayMetrics());
        }
    }
}
