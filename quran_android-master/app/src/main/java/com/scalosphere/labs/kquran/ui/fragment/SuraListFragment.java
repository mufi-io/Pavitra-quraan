package com.scalosphere.labs.kquran.ui.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.fragment.app.Fragment;


import com.scalosphere.labs.kquran.QuranApplication;
import com.scalosphere.labs.kquran.R;
import com.scalosphere.labs.kquran.data.Constants;
import com.scalosphere.labs.kquran.data.QuranInfo;
import com.scalosphere.labs.kquran.ui.QuranActivity;
import com.scalosphere.labs.kquran.ui.helpers.QuranListAdapter;
import com.scalosphere.labs.kquran.ui.helpers.QuranRow;
import com.scalosphere.labs.kquran.util.QuranSettings;
import com.scalosphere.labs.kquran.util.QuranUtils;

public class  SuraListFragment extends Fragment {

  private ListView mListView;
  private QuranListAdapter mAdapter;

  public static SuraListFragment newInstance() {
    return new SuraListFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
      ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.quran_list, container, false);
    mListView = (ListView) view.findViewById(R.id.list);
    mAdapter = new QuranListAdapter(getActivity(),
        R.layout.index_sura_row, getSuraList(), true);
    mListView.setAdapter(mAdapter);

    mListView.setOnItemClickListener(new OnItemClickListener() {

      public void onItemClick(AdapterView<?> parent, View v,
          int position, long id) {
          //Log.i("SuraFragment", " on OK click id : " + id + " position : " + position);
        QuranRow elem = (QuranRow) mAdapter.getItem((int) id);
          //Log.i("SuraFragment", " (elem) page : " + elem );
          //Log.i("SuraFragment", " elem.page : " + elem.page + " elem.sura : " + elem.sura + " elem.ayah : " + elem.ayah + " elem.metadata "+ elem.metadata );
        if (elem.page > 0) {
            if(elem.metadata != null ){
                ((QuranActivity) getActivity()).jumpToAndUnHighlight(elem.page, elem.sura, 1);
            }
            else{
                ((QuranActivity) getActivity()).jumpTo(elem.page);
            }
        }
      }
    });
    return view;
  }

  @Override
  public void onResume() {
    final Activity activity = getActivity();
    ((QuranApplication) activity.getApplication()).refreshLocale(false);

    int lastPage = QuranSettings.getLastPage(activity);
    if (lastPage != Constants.NO_PAGE_SAVED &&
        lastPage >= Constants.PAGES_FIRST &&
        lastPage <= Constants.PAGES_LAST) {
      int sura = QuranInfo.PAGE_SURA_START[lastPage - 1];
      int juz = QuranInfo.getJuzFromPage(lastPage);
      int position = sura + juz - 1;
      mListView.setSelectionFromTop(position, 20);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
        QuranSettings.isArabicNames(activity)) {
      updateScrollBarPositionHoneycomb();
    }

    super.onResume();
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void updateScrollBarPositionHoneycomb() {
    mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
  }

  private QuranRow[] getSuraList() {
    int pos = 0;
    int sura = 1;
    int next = 1;
    QuranRow[] elements = new QuranRow[Constants.SURAS_COUNT + Constants.JUZ2_COUNT];

    Activity activity = getActivity();
    for (int juz = 1; juz <= Constants.JUZ2_COUNT; juz++) {
      elements[pos++] = new QuranRow(QuranInfo.getJuzTitle(activity) + " " +
          QuranUtils.getLocalizedNumber(activity, juz),
          null, QuranRow.HEADER, juz,
          QuranInfo.JUZ_PAGE_START[juz - 1], null
      );
      next = (juz == Constants.JUZ2_COUNT) ? Constants.PAGES_LAST + 1 :
          QuranInfo.JUZ_PAGE_START[juz];

      while ((sura <= Constants.SURAS_COUNT) &&
          (QuranInfo.SURA_PAGE_START[sura - 1] < next)) {
        String title = QuranInfo.getSuraName(activity, sura,
            activity.getResources().getBoolean(R.bool.show_surat_prefix));
        elements[pos++] = new QuranRow(title,
            QuranInfo.getSuraListMetaString(activity, sura),
            sura, QuranInfo.SURA_PAGE_START[sura - 1], null);
        sura++;
      }
    }

    return elements;
  }
}
