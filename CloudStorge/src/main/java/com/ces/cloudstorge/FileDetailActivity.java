package com.ces.cloudstorge;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileDetailActivity extends Activity {
    private ViewGroup mContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);
        mContainerView = (ViewGroup) findViewById(R.id.detail_tenant_list);

        setTitle("detail");
        List<Map<String, String>> listData = new ArrayList<Map<String, String>>();
        for (int i = 0; i < 10; i++) {
            addItem("text" + i);
        }

    }

    public void addItem(String text) {
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.list_tenant_checked, mContainerView, false);

        ((TextView) newView.findViewById(R.id.tenant_id_text)).setText(text);
        mContainerView.addView(newView, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
