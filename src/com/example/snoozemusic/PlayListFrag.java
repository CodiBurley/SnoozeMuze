package com.example.snoozemusic;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Owner on 12/28/2014.
 */
public class PlayListFrag extends ListFragment {

    private ArrayList<String> ListTitles;
    private MainActivity parent;
    private ArrayAdapter<String> listAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new ArrayAdapter<String>(inflater.getContext(), R.layout.playlist_list, ListTitles);
        setListAdapter(listAdapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        this.initVars();
        super.onResume();
    }

    public void initVars() {
        parent = (MainActivity) getActivity();
        ListTitles = parent.ListTitles;
    }
}
