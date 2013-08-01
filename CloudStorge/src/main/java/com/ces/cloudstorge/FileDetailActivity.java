package com.ces.cloudstorge;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;
import com.ces.cloudstorge.util.FileStruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileDetailActivity extends Activity {
    private ViewGroup mContainerView;
    private FileStruct fileStruct;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);
        mContainerView = (ViewGroup) findViewById(R.id.detail_tenant_list);
        int fileId = Integer.parseInt(getIntent().getExtras().getString("file_id"));
        fileStruct = get_fileStruct(fileId);
        setTitle(fileStruct.getName());
        List<Map<String, String>> listData = new ArrayList<Map<String, String>>();
        for (int i = 0; i < 10; i++) {
            addItem("text" + i);
        }

        TextView detailSize = (TextView) findViewById(R.id.detail_size);
        TextView detailLastModified = (TextView) findViewById(R.id.detail_lastmodified);
        String newSize = CommonUtil.format_fileSize(fileStruct.getSize());
        detailSize.setText(newSize);
        detailLastModified.setText(fileStruct.getLast_modified());

        Switch downloadSwitch = (Switch) findViewById(R.id.detail_download_switch);
        Switch shareSwitch = (Switch) findViewById(R.id.detail_share_switch);
        if(null == fileStruct.getShare() || "".equals(fileStruct.getShare()))
        {
            mContainerView.setVisibility(View.GONE);
            shareSwitch.setChecked(false);
        }
        else
        {
            mContainerView.setVisibility(View.VISIBLE);
            shareSwitch.setChecked(true);
        }
        shareSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked)
                    mContainerView.setVisibility(View.VISIBLE);
                else
                    mContainerView.setVisibility(View.GONE);
            }
        });
    }

    private FileStruct get_fileStruct(int fileId)
    {
        String selection = String.format(MainActivity.selection_file_format, fileId, MainActivity.current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, MainActivity.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        FileStruct fileStruct = new FileStruct(cursor.getInt(Contract.PROJECTION_FILE_ID), cursor.getInt(Contract.PROJECTION_FOLDER_ID),
                cursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID), cursor.getString(Contract.PROJECTION_NAME),
                cursor.getString(Contract.PROJECTION_MIME_TYPE), cursor.getString(Contract.PROJECTION_SHARE),
                cursor.getInt(Contract.PROJECTION_SIZE), cursor.getString(Contract.PROJECTION_LAST_MODIFIED));
        return fileStruct;
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
