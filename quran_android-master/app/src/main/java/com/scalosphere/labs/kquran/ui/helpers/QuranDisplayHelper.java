package com.scalosphere.labs.kquran.ui.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.common.Response;
import com.scalosphere.labs.kquran.data.QuranInfo;
import com.scalosphere.labs.kquran.ui.PagerActivity;
import com.scalosphere.labs.kquran.util.ArabicStyle;
import com.scalosphere.labs.kquran.util.QuranFileUtils;
import com.scalosphere.labs.kquran.util.QuranSettings;
import com.scalosphere.labs.kquran.util.QuranUtils;

public class QuranDisplayHelper {
   private static final String TAG = "QuranDisplayHelper";
   
   public static Response getQuranPage(Context context,
                                     String widthParam, int page){
      Response response;

      String filename = QuranFileUtils.getPageFileName(page);
      response = QuranFileUtils.getImageFromSD(context, widthParam, filename);
      if (!response.isSuccessful()) {
        // let's only try if an sdcard is found... otherwise, let's tell
        // the user to mount their sdcard and try again.
        if (response.getErrorCode() != Response.ERROR_SD_CARD_NOT_FOUND) {
          android.util.Log.i(TAG, "failed to get " + page +
              " with name " + filename + " from sd...");
          response = QuranFileUtils.getImageFromWeb(context, filename);
        }
      }
      return response;
   }


   
   public static long displayMarkerPopup(Context context, int page,
                                         long lastPopupTime) {
      //New code for Custom view of Toast
       PagerActivity activity=null;
       if(context instanceof PagerActivity){
           activity= (PagerActivity)context;
       }
       LayoutInflater inflater = activity.getLayoutInflater();
       View layout = inflater.inflate(R.layout.toast,(ViewGroup) activity.findViewById(R.id.toast_layout_root));

       TextView text = (TextView) layout.findViewById(R.id.text);

       Toast toast = new Toast(context);
       toast.setGravity(Gravity.TOP, 0, 0);
       toast.setDuration(Toast.LENGTH_SHORT);
       toast.setView(layout);
       //New code ends here


      if (System.currentTimeMillis() - lastPopupTime < 3000)
         return lastPopupTime;
      int rub3 = QuranInfo.getRub3FromPage(page);
      if (rub3 == -1)
         return lastPopupTime;
      int hizb = (rub3 / 4) + 1;
      StringBuilder sb = new StringBuilder();

      if (rub3 % 8 == 0) {
         sb.append(context.getString(R.string.quran_juz2)).append(' ')
                 .append(QuranUtils.getLocalizedNumber(context,
                         (hizb / 2) + 1));
      }
      else {
         int remainder = rub3 % 4;
         if (remainder == 1){
            sb.append(context.getString(R.string.quran_rob3)).append(' ');
         }
         else if (remainder == 2){
            sb.append(context.getString(R.string.quran_nos)).append(' ');
         }
         else if (remainder == 3){
            sb.append(context.getString(R.string.quran_talt_arb3)).append(' ');
         }
         sb.append(context.getString(R.string.quran_hizb)).append(' ')
                 .append(QuranUtils.getLocalizedNumber(context, hizb));
      }

      String result = sb.toString();
      if (QuranSettings.isReshapeArabic(context)){
         result = ArabicStyle.reshape(context, result);
      }
       text.setText(result);
       toast.show();
      //Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
      return System.currentTimeMillis();
   }
   
   public static PaintDrawable getPaintDrawable(int startX, int endX){
      PaintDrawable drawable = new PaintDrawable();
      drawable.setShape(new RectShape());
      drawable.setShaderFactory(getShaderFactory(startX, endX));
      return drawable;
   }

   public static ShapeDrawable.ShaderFactory
   getShaderFactory(final int startX, final int endX){
      return new ShapeDrawable.ShaderFactory(){

         @Override
         public Shader resize(int width, int height) {
            return new LinearGradient(startX, 0, endX, 0,
                  new int[]{ 0xFFDCDAD5, 0xFFFDFDF4,
                  0xFFFFFFFF, 0xFFFDFBEF },
                  new float[]{ 0, 0.18f, 0.48f, 1 },
                  Shader.TileMode.REPEAT);
         }
      };
   }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static int getWidthKitKat(Display display){
    Point point = new Point();
    display.getRealSize(point);
    return point.x;
  }
}
