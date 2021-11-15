package com.scalosphere.labs.kquran.ui.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.util.ArabicStyle;
import com.scalosphere.labs.kquran.util.QuranSettings;
import com.scalosphere.labs.kquran.util.QuranUtils;

public class QuranListAdapter extends BaseAdapter {

   private Context mContext;
   private LayoutInflater mInflater;
   private QuranRow[] mElements;
   private int mLayout;
   private boolean mReshapeArabic;
   private boolean mUseArabicFont;
   private boolean mSelectableHeaders;

   public QuranListAdapter(Context context, int layout,
       QuranRow[] items, boolean selectableHeaders){
      mInflater = LayoutInflater.from(context);
      mElements = items;
      mLayout = layout;
      mContext = context;
      mSelectableHeaders = selectableHeaders;

      // should we reshape if we have arabic?
      mUseArabicFont = QuranSettings.needArabicFont(context);
      mReshapeArabic = QuranSettings.isReshapeArabic(context);
   }

   public int getCount() {
      return mElements.length;
   }

   public Object getItem(int position) {
      return mElements[position];
   }

   public long getItemId(int position) {
      return position;
   }

   public void setElements(QuranRow[] elements){
      mElements = elements;
   }

   public View getView(final int position, View convertView, ViewGroup parent) {
      ViewHolder holder;

      if (convertView == null) {
         convertView = mInflater.inflate(mLayout, null);
         holder = new ViewHolder();
         holder.text = (TextView)convertView.findViewById(R.id.suraName);
         holder.metadata = (TextView)convertView.findViewById(R.id.suraDetails);
         holder.page = (TextView)convertView.findViewById(R.id.pageNumber);
         holder.number = (TextView)convertView.findViewById(R.id.suraNumber);
         holder.header = (TextView)convertView.findViewById(R.id.headerName);
         holder.image = (TextView)convertView.findViewById(R.id.rowIcon);

         if (mUseArabicFont){
            //Typeface typeface = ArabicStyle.getTypeface(mContext);
            ArabicStyle.applyFont(mContext,holder.text);
            //holder.text.setTypeface(typeface);
            ArabicStyle.applyFont(mContext,holder.metadata);
            //holder.metadata.setTypeface(typeface);
            ArabicStyle.applyFont(mContext,holder.header);
            //holder.header.setTypeface(typeface);
         }
         convertView.setTag(holder);
      }
      else { holder = (ViewHolder) convertView.getTag(); }

      QuranRow item = mElements[position];
      String text = item.text;
      if (mReshapeArabic){
         text = ArabicStyle.reshape(mContext, text);
      }

     // commenting  below line to hide the page number
     // holder.page.setText(QuranUtils.getLocalizedNumber(mContext, item.page));
      holder.text.setText(text);
      holder.header.setText(text);
      holder.number.setText(
              QuranUtils.getLocalizedNumber(mContext, item.sura));


      int color = R.color.sura_details_color;
      if (mElements[position].isHeader()){
         holder.text.setVisibility(View.GONE);
         holder.header.setVisibility(View.VISIBLE);
         holder.metadata.setVisibility(View.GONE);
         holder.number.setVisibility(View.GONE);
         holder.image.setVisibility(View.GONE);
         color = R.color.header_text_color;
      }
      else {
         String info = item.metadata;
         holder.metadata.setVisibility(View.VISIBLE);
         holder.text.setVisibility(View.VISIBLE);
         holder.header.setVisibility(View.GONE);
         holder.metadata.setText(ArabicStyle.reshape(mContext, info));

         if (item.imageResource == null){
            holder.number.setVisibility(View.VISIBLE);
            holder.image.setVisibility(View.GONE);
         }
         else {
            holder.image.setBackgroundResource(item.imageResource);
            holder.image.setText(item.imageText);
            holder.image.setVisibility(View.VISIBLE);
            holder.number.setVisibility(View.GONE);
         }
      }
      holder.page.setTextColor(mContext.getResources().getColor(color));
      int pageVisibility = item.page == 0? View.GONE : View.VISIBLE;
      holder.page.setVisibility(pageVisibility);
      
      // If the row is checked (for CAB mode), theme its bg appropriately
      if (parent != null && parent instanceof ListView){
         int background = ((ListView)parent).isItemChecked(position)?
                 R.drawable.ic_ab_search : 0;
         convertView.setBackgroundResource(background);
      }

      return convertView;
   }

  @Override
  public boolean isEnabled(int position) {
    final QuranRow selected = mElements[position];
    return mSelectableHeaders || selected.isBookmark() ||
        selected.rowType == QuranRow.NONE ||
        (selected.isBookmarkHeader() && selected.tagId >= 0);
  }

  class ViewHolder {
      TextView text;
      TextView page;
      TextView number;
      TextView metadata;
      TextView header;
      TextView image;
   }
}
