package com.scalosphere.labs.kquran.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.scalosphere.labs.kquran.AboutUsActivity;
import com.scalosphere.labs.kquran.QuranApplication;
import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.database.BookmarksDBAdapter;
import com.scalosphere.labs.kquran.task.TranslationListTask;
import com.scalosphere.labs.kquran.ui.fragment.AbsMarkersFragment;
import com.scalosphere.labs.kquran.ui.fragment.AddTagDialog;
import com.scalosphere.labs.kquran.ui.fragment.BookmarksFragment;
import com.scalosphere.labs.kquran.ui.fragment.JumpFragment;
import com.scalosphere.labs.kquran.ui.fragment.SuraListFragment;
import com.scalosphere.labs.kquran.ui.fragment.TagBookmarkDialog;
import com.scalosphere.labs.kquran.ui.helpers.BookmarkHandler;
import com.scalosphere.labs.kquran.util.QuranSettings;

//import com.scalosphere.labs.kquran.service.AudioService;

public class QuranActivity extends SherlockFragmentActivity
        implements ActionBar.TabListener, BookmarkHandler,
        AddTagDialog.OnTagChangedListener,
                   TagBookmarkDialog.OnBookmarkTagsUpdateListener {
   public static final String TAG = "QuranActivity";
   public static final String EXTRA_SHOW_TRANSLATION_UPGRADE = "transUp";
   public static final String SI_SHOWED_UPGRADE_DIALOG = "si_showed_dialog";
   private SharedPreferences mSharedPreferences = null;
   private static Context sharedContext = null;
   private TranslationListTask mTask = null;

   private static final int SURA_LIST = 0;

   // Orginal
   // private static final int JUZ2_LIST = 1;
   // private static final int BOOKMARKS_LIST = 2;

   private static final int BOOKMARKS_LIST = 1;
   private static final int JUZ2_LIST = 2;
   private static final int TAGS_LIST = 3;

   private static final int REFRESH_BOOKMARKS = 1;
   private static final int REFRESH_TAGS = 2;
   
  /* //Original code
  private int[] mTabs = new int[]{ R.string.quran_sura,
                                    R.string.quran_juz2,
                                    R.string.menu_bookmarks,
                                    R.string.menu_tags };
   private int[] mArabicTabs = new int[]{ R.string.menu_tags,
                                          R.string.menu_bookmarks,
                                          R.string.quran_juz2,
                                          R.string.quran_sura };*/
    //Modified for K Qur'an
    private int[] mTabs = new int[]{ R.string.quran_sura,
            R.string.menu_bookmarks,
            };
    private int[] mArabicTabs = new int[]{
            R.string.menu_bookmarks,
            R.string.quran_sura };
   
   private ViewPager mPager = null;
   private AlertDialog mUpgradeDialog = null;
   private PagerAdapter mPagerAdapter = null;
   private BookmarksDBAdapter mBookmarksDBAdapter = null;
   private boolean mShowedTranslationUpgradeDialog = false;
   private boolean mIsArabic;

   @Override
   public void onCreate(Bundle savedInstanceState){
      ((QuranApplication)getApplication()).refreshLocale(false);
      sharedContext = getApplicationContext();

      setTheme(R.style.QuranAndroid);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.quran_index);

      ActionBar actionbar = getSupportActionBar();
      actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

      mIsArabic = QuranSettings.isArabicNames(this);
      final int[] tabs = mIsArabic ? mArabicTabs : mTabs;
      for (int i=0; i<tabs.length; i++){
        ActionBar.Tab tab = actionbar.newTab();
        tab.setText(tabs[i]);
        tab.setTag(i);
        tab.setTabListener(this);
        actionbar.addTab(tab);
      }

      mPager = (ViewPager)findViewById(R.id.index_pager);

      mPager.setOffscreenPageLimit(3);
      mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), mIsArabic);
      mPager.setAdapter(mPagerAdapter);
      mPager.setOnPageChangeListener(mOnPageChangeListener);

      if (mIsArabic) {
        mPager.setCurrentItem(mTabs.length - 1);
      }

      mBookmarksDBAdapter = new BookmarksDBAdapter(this);

      if (savedInstanceState != null){
         mShowedTranslationUpgradeDialog = savedInstanceState.getBoolean(
                 SI_SHOWED_UPGRADE_DIALOG, false);
         //Log.i(TAG, " value of SI_SHOWED_UPGRADE_DIALOG " +mShowedTranslationUpgradeDialog);

      }

      Intent intent = getIntent();
      if (intent != null){
         Bundle extras = intent.getExtras();
         if (extras != null){
            if (extras.getBoolean(EXTRA_SHOW_TRANSLATION_UPGRADE, false)){
               if (!mShowedTranslationUpgradeDialog){
                  //TODO commenting the original code to block the dialog, handle Kannada upgrade
                  // showTranslationsUpgradeDialog();
                   mTask = new TranslationListTask(this);
                   mTask.execute();
               }
            }
         }
      }

   }

    //To be used only in QuranSettings for setting the font text
    public static Context getSharedContext() {
        return sharedContext;
    }

   @Override
   public void onResume(){
      super.onResume();
      final boolean isArabic = QuranSettings.isArabicNames(this);
      if (isArabic != mIsArabic) {
        final Intent i = getIntent();
        finish();
        startActivity(i);
      } else {
        //TODO Uncomment AudioService for Audio Playback features
        //startService(new Intent(AudioService.ACTION_STOP));
      }
   }

   @Override
   protected void onDestroy() {
      mBookmarksDBAdapter.close();
      super.onDestroy();
   }

   @Override
   public BookmarksDBAdapter getBookmarksAdapter(){
      return mBookmarksDBAdapter;
   }

   private Handler mHandler = new Handler(){
      @Override
      public void handleMessage(Message msg) {
         if (msg.what == REFRESH_BOOKMARKS){
            final int pos = getPosition(BOOKMARKS_LIST);
            String bookmarksTag = mPagerAdapter.getFragmentTag(
                    R.id.index_pager, pos);
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = fm.findFragmentByTag(bookmarksTag);
            if (f != null && f instanceof AbsMarkersFragment){
               ((AbsMarkersFragment)f).refreshData();
            }
         } else if (msg.what == REFRESH_TAGS){
            final int pos = getPosition(TAGS_LIST);
            String tagsTag = mPagerAdapter.getFragmentTag(
                  R.id.index_pager, pos);
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = fm.findFragmentByTag(tagsTag);
            if (f != null && f instanceof AbsMarkersFragment){
               ((AbsMarkersFragment)f).refreshData();
            }
         }
      }
   };

   @Override
   public void onTabSelected(Tab tab, FragmentTransaction transaction){
      Integer tag = (Integer)tab.getTag();
      if (mPager != null && tag != mPager.getCurrentItem()) {
        mPager.setCurrentItem(tag);
      }
   }
   
   @Override
   public void onTabReselected(Tab tab, FragmentTransaction transaction){
   }

   @Override
   public void onTabUnselected(Tab tab, FragmentTransaction transaction){
   }
   
   OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener(){
      @Override
      public void onPageScrollStateChanged(int state) {
      }

      @Override
      public void onPageScrolled(int position,
            float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageSelected(int position) {
         ActionBar actionbar = getSherlock().getActionBar();
         Tab tab = actionbar.getTabAt(position);
         if (actionbar.getSelectedTab() != tab) {
           actionbar.selectTab(tab);
         }
      }
   };
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu){
      super.onCreateOptionsMenu(menu);
      MenuInflater inflater = getSupportMenuInflater();
      inflater.inflate(R.menu.home_menu, menu);
      return true;
   }
   
   @Override
	public boolean onOptionsItemSelected(MenuItem item) {
       if (item.getItemId() == R.id.jump) {
           gotoPageDialog();
           return true;
       }else if (item.getItemId() == R.id.search){
         return onSearchRequested();
      }else if (item.getItemId() == R.id.about_us){
         Intent i = new Intent(this, AboutUsActivity.class);
         startActivity(i);
         return true;
      }
//      else if (item.getItemId() == R.id.settings){
//         Intent i = new Intent(this, QuranPreferenceActivity.class);
//         startActivity(i);
//         return true;
//      }
      // Original Code to enable settings
//      else if (item.getItemId() == R.id.last_page){
//         int page = QuranSettings.getLastPage(this);
//         jumpTo(page);
//         return true;
//      }
      // Original Code to enable help and about
//      else if (item.getItemId() == R.id.help) {
//         Intent i = new Intent(this, HelpActivity.class);
//         startActivity(i);
//         return true;
//      }
//      else if (item.getItemId() == R.id.about) {
//    	  Intent i = new Intent(this, AboutUsActivity.class);
//    	  startActivity(i);
//    	  return true;
//      }
      return super.onOptionsItemSelected(item);
	}

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putBoolean(SI_SHOWED_UPGRADE_DIALOG,
              mShowedTranslationUpgradeDialog);
      super.onSaveInstanceState(outState);
   }

   private void showTranslationsUpgradeDialog(){
      mShowedTranslationUpgradeDialog = true;
      AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CustomTheme_Dialog));
      builder.setMessage(R.string.translation_updates_available);
      builder.setCancelable(false);
      builder.setPositiveButton(R.string.translation_dialog_yes,
              new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    mUpgradeDialog = null;
                    //launchTranslationActivity();
                 }
              });

      builder.setNegativeButton(R.string.translation_dialog_later,
              new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mUpgradeDialog = null;

                    // pretend we don't have updated translations.  we'll
                    // check again after 10 days.
                    PreferenceManager.getDefaultSharedPreferences(
                            getApplicationContext()).edit()
                            .putBoolean(
                                    Constants.PREF_HAVE_UPDATED_TRANSLATIONS,
                                    false).commit();
                 }
              });

      mUpgradeDialog = builder.create();
      mUpgradeDialog.show();
   }

   public void launchTranslationActivity(){
      Intent i = new Intent(this, TranslationManagerActivity.class);
      startActivity(i);
   }
   
   public void jumpTo(int page) {
      Intent i = new Intent(this, PagerActivity.class);
      i.putExtra("page", page);
      startActivity(i);
   }

   public void jumpToAndHighlight(int page, int sura, int ayah) {
      Intent i = new Intent(this, PagerActivity.class);
      i.putExtra("page", page);
      i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_SURA, sura);
      i.putExtra(PagerActivity.EXTRA_HIGHLIGHT_AYAH, ayah);
      startActivity(i);
   }

    public void jumpToAndUnHighlight(int page, int sura, int ayah) {
        Intent i = new Intent(this, PagerActivity.class);
        i.putExtra("page", page);
        i.putExtra(PagerActivity.EXTRA_UNHIGHLIGHT_SURA, sura);
        i.putExtra(PagerActivity.EXTRA_UNHIGHLIGHT_AYAH, ayah);
        startActivity(i);
    }

   public void gotoPageDialog() {
      FragmentManager fm = getSupportFragmentManager();
      JumpFragment jumpDialog = new JumpFragment();
      jumpDialog.show(fm, JumpFragment.TAG);
   }

    /*public void gotoSeekBarDialog() {
        FragmentManager fm = getSupportFragmentManager();
        SeekbarFragment seekbarDialog = new SeekbarFragment();
        seekbarDialog.show(fm, SeekbarFragment.TAG);
    }*/

   public void addTag(){
      FragmentManager fm = getSupportFragmentManager();
      AddTagDialog addTagDialog = new AddTagDialog();
      addTagDialog.show(fm, AddTagDialog.TAG);
   }

   public void editTag(long id, String name){
      FragmentManager fm = getSupportFragmentManager();
      AddTagDialog addTagDialog = AddTagDialog.newInstance(id, name);
      addTagDialog.show(fm, AddTagDialog.TAG);
   }

   public void tagBookmarks(long[] ids){
      if (ids != null && ids.length == 1) {
         tagBookmark(ids[0]);
         return;
      }
      FragmentManager fm = getSupportFragmentManager();
      TagBookmarkDialog tagBookmarkDialog = TagBookmarkDialog.newInstance(ids);
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG);
   }

   public void tagBookmark(long id){
      FragmentManager fm = getSupportFragmentManager();
      TagBookmarkDialog tagBookmarkDialog = TagBookmarkDialog.newInstance(id);
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG);
   }
   
   public void onBookmarkDeleted() {
      mHandler.sendEmptyMessage(REFRESH_TAGS);
   }
   
   @Override
   public void onBookmarkTagsUpdated() {
      mHandler.sendEmptyMessage(REFRESH_BOOKMARKS);
      mHandler.sendEmptyMessage(REFRESH_TAGS);
   }
   
   @Override
   public void onTagAdded(final String name) {
      if (TextUtils.isEmpty(name))
         return;
      FragmentManager fm = getSupportFragmentManager();
      Fragment f = fm.findFragmentByTag(TagBookmarkDialog.TAG);
      if (f != null && f instanceof TagBookmarkDialog){
         ((TagBookmarkDialog)f).handleTagAdded(name);
         mHandler.sendEmptyMessage(REFRESH_TAGS);
      } else {
	     new Thread(new Runnable() {
	        @Override
	        public void run() {
	           mBookmarksDBAdapter.addTag(name);
	           mHandler.sendEmptyMessage(REFRESH_TAGS);
	        }
	     }).start();
      }
   }

   @Override
   public void onTagUpdated(final long id, final String name) {
      new Thread(new Runnable() {
         @Override
         public void run() {
            mBookmarksDBAdapter.updateTag(id, name);
            mHandler.sendEmptyMessage(REFRESH_TAGS);
         }
      }).start();
   }

   @Override
   public void onAddTagSelected() {
      FragmentManager fm = getSupportFragmentManager();
      AddTagDialog dialog = new AddTagDialog();
      dialog.show(fm, AddTagDialog.TAG);
   }

   public int getPosition(int position) {
     if (mIsArabic) {
       return Math.abs(position - 3);
     } else {
       return position;
     }
   }
   
   public static class PagerAdapter extends FragmentPagerAdapter {
      private boolean mIsArabic;
      public PagerAdapter(FragmentManager fm, boolean isArabic){
         super(fm);
         mIsArabic = isArabic;
      }
      
      @Override
      public int getCount(){
         return 2;
      }
      
      @Override
      public Fragment getItem(int position){
         int pos = position;
         if (mIsArabic) {
           pos = Math.abs(position - 1);
         }

        switch (pos) {
          case QuranActivity.SURA_LIST:
            return SuraListFragment.newInstance();
          case QuranActivity.BOOKMARKS_LIST:
             return BookmarksFragment.newInstance();
          // Orginal
//          case QuranActivity.JUZ2_LIST:
//            return JuzListFragment.newInstance();
//          case QuranActivity.TAGS_LIST:
          default:
            return SuraListFragment.newInstance();
        }
      }

      /**
       * this is a private method in FragmentPagerAdapter that
       * allows getting the tag that it uses to store the fragment
       * in (for use by getFragmentByTag).  in the future, this could
       * change and cause us issues...
       * @param viewId the view id of the viewpager
       * @param index the index of the fragment to get
       * @return the tag in which it would be stored under
       */
      public static String getFragmentTag(int viewId, int index){
         return "android:switcher:" + viewId + ":" + index;
      }
   }
}
