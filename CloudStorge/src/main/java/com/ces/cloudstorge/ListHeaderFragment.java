package com.ces.cloudstorge;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by MichaelDai on 13-8-5.
 */
public class ListHeaderFragment extends Fragment {
    private TextView folderTrace;

    public ListHeaderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View currentView = inflater.inflate(R.layout.list_header, container, false);
        folderTrace = (TextView) currentView.findViewById(R.id.folder_trace);
        set_folderTrace(changeHeader());
        return currentView;
    }

    public String changeHeader() {
        String folder_trace = "";
        for (int i = 0; i < MainActivity.listFolder.size(); i++) {
            folder_trace += "/" + MainActivity.listFolder.get(i);
        }
        return folder_trace;
    }

    public void set_folderTrace(String trace) {
        folderTrace.setText(trace);
    }

}
