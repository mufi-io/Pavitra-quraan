package com.scalosphere.labs.kquran.task;

import android.app.Activity;

import com.scalosphere.labs.kquran.data.SuraAyah;
import com.scalosphere.labs.kquran.database.BookmarksDBAdapter;
import com.scalosphere.labs.kquran.ui.PagerActivity;
import com.scalosphere.labs.kquran.ui.helpers.BookmarkHandler;

public class RefreshBookmarkIconTask extends PagerActivityTask<Void, Void, Boolean> {
  private SuraAyah mSuraAyah;
  private boolean mRefreshHighlight;

  public RefreshBookmarkIconTask(
      PagerActivity activity, SuraAyah suraAyah, boolean refreshHighlight) {
    super(activity);
    mSuraAyah = suraAyah;
    mRefreshHighlight = refreshHighlight;
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    if (mSuraAyah == null) return null;
    BookmarksDBAdapter adapter = null;
    Activity activity = getActivity();
    if (activity != null && activity instanceof BookmarkHandler){
      adapter = ((BookmarkHandler) activity).getBookmarksAdapter();
    }

    if (adapter == null){ return null; }

    boolean bookmarked = adapter.getBookmarkId(
        mSuraAyah.sura, mSuraAyah.ayah, mSuraAyah.getPage()) >= 0;
    return bookmarked;
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
    PagerActivity activity = getActivity();
    if (result != null && activity != null){
      activity.updateAyahBookmark(mSuraAyah, result, mRefreshHighlight);
    }
  }

}
