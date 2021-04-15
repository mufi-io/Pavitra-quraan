package com.scalosphere.labs.kquran.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.common.QuranAyah;
import com.scalosphere.labs.kquran.common.TranslationItem;
import com.scalosphere.labs.kquran.task.TranslationTask;
import com.scalosphere.labs.kquran.ui.PagerActivity;
import com.scalosphere.labs.kquran.util.TranslationUtils;
import com.scalosphere.labs.kquran.widgets.TranslationView;

import java.util.List;

public class AyahTranslationFragment extends AyahActionFragment {
  private static final String TAG = "AyahTranslationFragment";

  private ProgressBar mProgressBar;
  private TranslationView mTranslationView;
  private View mEmptyState;
  private AsyncTask mCurrentTask;
  private TranslationItem mTranslationItem;
  private View mTranslationControls;

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(
        R.layout.translation_panel, container, false);

    mTranslationView =
        (TranslationView) view.findViewById(R.id.translation_view);
    mTranslationView.setIsInAyahActionMode(true);
    mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
    mEmptyState = view.findViewById(R.id.empty_state);
    mTranslationControls = view.findViewById(R.id.controls);
    final View next = mTranslationControls.findViewById(R.id.next_ayah);
    next.setOnClickListener(mOnClickListener);

    final View prev = mTranslationControls.findViewById(R.id.previous_ayah);
    prev.setOnClickListener(mOnClickListener);

    final Button getTranslations =
        (Button) view.findViewById(R.id.get_translations_button);
    getTranslations.setOnClickListener(mOnClickListener);
    return view;
  }

  private View.OnClickListener mOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      final Activity activity = getActivity();
      final PagerActivity pagerActivity;
      if (activity instanceof PagerActivity) {
        pagerActivity = (PagerActivity) activity;
      } else {
        return;
      }

      switch (v.getId()) {
        case R.id.get_translations_button:
           pagerActivity.switchToTranslation();
          //pagerActivity.startTranslationManager();
          break;
        case R.id.next_ayah:
          pagerActivity.nextAyah();
          break;
        case R.id.previous_ayah:
          pagerActivity.previousAyah();
          break;
      }
    }
  };

  public void refreshView() {
    if (mStart == null || mEnd == null) { return; }

    final Activity activity = getActivity();
    if (activity instanceof PagerActivity) {
      /* commenting the original code
      mTranslationItem = TranslationUtils.getDefaultTranslationItem(
          activity, ((PagerActivity) activity).getTranslations());
       */

      //new code added for KQD
        mTranslationItem = TranslationUtils.getKannadaTranslationItem();
      //  TODO comment the below code after testing
      if (mTranslationItem == null) {
        mProgressBar.setVisibility(View.GONE);
        mEmptyState.setVisibility(View.VISIBLE);
        mTranslationControls.setVisibility(View.GONE);
        return;
      }

      if (mStart.equals(mEnd)) {
        mTranslationControls.setVisibility(View.VISIBLE);
      } else {
        mTranslationControls.setVisibility(View.GONE);
      }

      Integer[] bounds = new Integer[]{ mStart.sura,
          mStart.ayah, mEnd.sura, mEnd.ayah };
      if (mCurrentTask != null) {
        mCurrentTask.cancel(true);
      }
      mCurrentTask = new ShowTafsirTask(activity, bounds,
          mTranslationItem.filename).execute();
    }
  }

  private class ShowTafsirTask extends TranslationTask {

    public ShowTafsirTask(Context context, Integer[] bounds, String db) {
      super(context, bounds, db);
      mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected boolean loadArabicAyahText() {
      return false;
    }

    @Override
    protected void onPostExecute(List<QuranAyah> result) {
      mProgressBar.setVisibility(View.GONE);
      if (result != null) {
        mEmptyState.setVisibility(View.GONE);
        String who = null;
        if (mTranslationItem != null) {
          who = mTranslationItem.translator;
          if (TextUtils.isEmpty(who)) {
            who = mTranslationItem.name;
          }
        }
        mTranslationView.setTranslatorName(who);
        mTranslationView.setAyahs(result);
      } else {
        mEmptyState.setVisibility(View.VISIBLE);
      }
      mCurrentTask = null;
    }
  }

}
