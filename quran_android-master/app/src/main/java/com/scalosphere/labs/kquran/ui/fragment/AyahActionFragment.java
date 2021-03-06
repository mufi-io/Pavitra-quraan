package com.scalosphere.labs.kquran.ui.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.scalosphere.labs.kquran.data.SuraAyah;
import com.scalosphere.labs.kquran.ui.PagerActivity;

public abstract class AyahActionFragment extends Fragment {

  protected SuraAyah mStart;
  protected SuraAyah mEnd;
  private boolean mJustCreated;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mJustCreated = true;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mJustCreated) {
      mJustCreated = false;
      PagerActivity activity = (PagerActivity) getActivity();
      if (activity != null) {
        mStart = activity.getSelectionStart();
        mEnd = activity.getSelectionEnd();
        refreshView();
      }
    }
  }

  public void updateAyahSelection(SuraAyah start, SuraAyah end) {
    mStart = start;
    mEnd = end;
    refreshView();
  }

  protected abstract void refreshView();

}
