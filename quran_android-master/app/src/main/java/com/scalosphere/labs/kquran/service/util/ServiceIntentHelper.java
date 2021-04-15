package com.scalosphere.labs.kquran.service.util;

import android.content.Context;
import android.content.Intent;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.service.QuranDownloadService;

public class ServiceIntentHelper {

   public static Intent getDownloadIntent(Context context, String url,
                                          String destination,
                                          String notificationTitle,
                                          String key, int type){
      Intent intent = new Intent(context, QuranDownloadService.class);
      intent.putExtra(QuranDownloadService.EXTRA_URL, url);
      intent.putExtra(QuranDownloadService.EXTRA_DESTINATION,
              destination);
      intent.putExtra(QuranDownloadService.EXTRA_NOTIFICATION_NAME,
              notificationTitle);
      intent.putExtra(QuranDownloadService.EXTRA_DOWNLOAD_KEY,
              key);
      intent.putExtra(QuranDownloadService.EXTRA_DOWNLOAD_TYPE,
              type);
      intent.setAction(QuranDownloadService.ACTION_DOWNLOAD_URL);
      return intent;
   }

   public static int getErrorResourceFromDownloadIntent(Intent intent,
                                                        boolean willRetry){
      int errorCode = intent.getIntExtra(
              QuranDownloadService.ProgressIntent.ERROR_CODE, 0);
      return getErrorResourceFromErrorCode(errorCode, willRetry);
   }

   public static int getErrorResourceFromErrorCode(int errorCode,
                                                   boolean willRetry){
      int errorId = 0;

      switch (errorCode){
         case QuranDownloadService.ERROR_DISK_SPACE:
            errorId = R.string.download_error_disk;
            break;
         case QuranDownloadService.ERROR_NETWORK:
            errorId = R.string.download_error_network;
            if (willRetry){
               errorId = R.string.download_error_network_retry;
            }
            break;
         case QuranDownloadService.ERROR_PERMISSIONS:
            errorId = R.string.download_error_perms;
            break;
         case QuranDownloadService.ERROR_INVALID_DOWNLOAD:
            errorId = R.string.download_error_invalid_download;
            if (willRetry){
               errorId = R.string.download_error_invalid_download_retry;
            }
            break;
         case QuranDownloadService.ERROR_CANCELLED:
            errorId = R.string.notification_download_canceled;
            break;
         case QuranDownloadService.ERROR_GENERAL:
         default:
            errorId = R.string.download_error_general;
      }

      return errorId;
   }
}
