package com.ces.cloudstorge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;
import com.ces.cloudstorge.util.FileStruct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileDetailActivity extends Activity {
    private ViewGroup mContainerView;
    private FileStruct fileStruct;
    private int hasTenant;
    private Map<Integer, String> tenantsData;
    private Switch shareSwitch;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);
        mContainerView = (ViewGroup) findViewById(R.id.detail_tenant_list);
        int fileId = Integer.parseInt(getIntent().getExtras().getString("file_id"));
        fileStruct = get_fileStruct(fileId);
        setTitle(fileStruct.getName());

        hasTenant = 0;
        tenantsData = new HashMap<Integer, String>();
        TextView detailSize = (TextView) findViewById(R.id.detail_size);
        TextView detailLastModified = (TextView) findViewById(R.id.detail_lastmodified);
        String newSize = CommonUtil.format_fileSize(fileStruct.getSize());
        detailSize.setText(newSize);
        detailLastModified.setText(fileStruct.getLast_modified());

        Switch downloadSwitch = (Switch) findViewById(R.id.detail_download_switch);
        shareSwitch = (Switch) findViewById(R.id.detail_share_switch);
        if (null == fileStruct.getShare() || "".equals(fileStruct.getShare())) {
            mContainerView.setVisibility(View.GONE);
            shareSwitch.setChecked(false);
        } else {
            mContainerView.setVisibility(View.VISIBLE);
            shareSwitch.setChecked(true);
            new GetTenantsAsyncTask().execute(fileStruct.getShare());
        }

        shareSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mContainerView.setVisibility(View.VISIBLE);
                    if (hasTenant == 0)
                        new GetTenantsAsyncTask().execute(fileStruct.getShare());
                } else {
                    int childCount = mContainerView.getChildCount();
                    int flag = 0;
                    for (int i = 0; i < childCount; i++) {
                        View view = mContainerView.getChildAt(i);
                        CheckBox c;
                        if (null == (c = (CheckBox) view.findViewById(R.id.tenant_checkbox))) {
                            break;
                        } else {
                            if (c.isChecked())
                                flag++;
                        }
                    }
                    if (flag != 0)
                        create_closeShareDialog(R.string.tip, R.string.detail_confirm_close_share_string);
                    else
                        mContainerView.setVisibility(View.GONE);
                }
            }
        });
    }

    // 创建普通ProgressDialog
    public void create_progressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    // 创建普通提示对话框
    public void create_closeShareDialog(int title, int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                shareSwitch.setChecked(true);
                dialog.cancel();
            }
        }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mContainerView.setVisibility(View.GONE);
                new CloseShareFileAsyncTask().execute(fileStruct.getFileId(), -1, 1);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private FileStruct get_fileStruct(int fileId) {
        String selection = String.format(MainActivity.selection_file_format, fileId, MainActivity.current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, MainActivity.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        FileStruct fileStruct = new FileStruct(cursor.getInt(Contract.PROJECTION_FILE_ID), cursor.getInt(Contract.PROJECTION_FOLDER_ID),
                cursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID), cursor.getString(Contract.PROJECTION_NAME),
                cursor.getString(Contract.PROJECTION_MIME_TYPE), cursor.getString(Contract.PROJECTION_SHARE),
                cursor.getInt(Contract.PROJECTION_SIZE), cursor.getString(Contract.PROJECTION_LAST_MODIFIED));
        return fileStruct;
    }

    public void addItem(String tenantName, int tenantId, boolean isChecked) {
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.list_tenant_checked, mContainerView, false);
        final int selected_tenantId = tenantId;

        ((TextView) newView.findViewById(R.id.tenant_id_text)).setText(tenantName);
        CheckBox checkBox = (CheckBox) newView.findViewById(R.id.tenant_checkbox);
        checkBox.setChecked(isChecked);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    new ShareFileAsyncTask().execute(fileStruct.getFileId(), selected_tenantId);
                } else {
                    new CloseShareFileAsyncTask().execute(fileStruct.getFileId(), selected_tenantId, 0);
                }
            }
        });
        mContainerView.addView(newView, 0);
    }

    public void addProgressItem() {
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.tenants_progress_bar, mContainerView, false);

        mContainerView.addView(newView, 0);
    }

    public void addShareFailureItem() {
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                android.R.layout.activity_list_item, mContainerView, false);
        ((TextView) newView.findViewById(android.R.id.text1)).setText(R.string.detail_share_failure_string);
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

    private class GetTenantsAsyncTask extends AsyncTask<String, Void, JSONObject> {
        private String shares;

        @Override
        protected void onPreExecute() {
            addProgressItem();
        }

        @Override
        protected JSONObject doInBackground(String... shares) {
            this.shares = shares[0];
            if (!ConnectionChangeReceiver.isHasConnect)
                return null;
            JSONObject result = CloudStorgeRestUtilities.getTenants();
            return result;
        }

        @Override
        protected void onPostExecute(final JSONObject tenants) {
            if (null == tenants) {
                mContainerView.removeAllViews();
                addShareFailureItem();
                return;
            } else {
                tenantsData.clear();
                try {
                    int result = tenants.getInt("result");
                    if (result != 0) {
                        mContainerView.removeAllViews();
                        addShareFailureItem();
                        return;
                    }
                    JSONArray array_tenants = tenants.getJSONArray("tenants");
                    mContainerView.removeAllViews();
                    String[] array_shares = this.shares.split(",");
                    for (int i = 0; i < array_tenants.length(); i++) {
                        boolean isChecked = false;
                        JSONObject data = array_tenants.getJSONObject(i);
                        int id = data.getInt("id");
                        String name = data.getString("name");
                        tenantsData.put(id, name);
                        for (int j = 0; j < array_shares.length; j++) {
                            if (null == array_shares[j] || "".equals(array_shares[j]))
                                continue;
                            if (Integer.parseInt(array_shares[j]) == id) {
                                isChecked = true;
                                break;
                            }
                        }
                        addItem(name, id, isChecked);
                    }
                    hasTenant = 1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ShareFileAsyncTask extends AsyncTask<Integer, Void, JSONObject> {
        private int tenant;

        @Override
        protected void onPreExecute() {
            create_progressDialog(getString(R.string.share_file_progress_message));
        }

        @Override
        protected JSONObject doInBackground(Integer... file_tenant) {
            if (!ConnectionChangeReceiver.isHasConnect)
                return null;
            tenant = file_tenant[1];

            JSONObject result = CloudStorgeRestUtilities.shareFile(file_tenant[0], file_tenant[1]);
            return result;
        }

        @Override
        protected void onPostExecute(final JSONObject result) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (null == result) {
                Toast.makeText(FileDetailActivity.this, R.string.share_file_error, Toast.LENGTH_SHORT).show();
                return;
            }
            // 更新文件的share字段
            String share = fileStruct.getShare() + "," + tenant;
            fileStruct.setShare(share);
            updateFileShare(fileStruct.getFileId(), share);
            Toast.makeText(FileDetailActivity.this, R.string.share_file_success, Toast.LENGTH_SHORT).show();
        }
    }

    private class CloseShareFileAsyncTask extends AsyncTask<Integer, Void, JSONObject> {
        private int tenant;
        private boolean isAll;

        @Override
        protected void onPreExecute() {
            create_progressDialog(getString(R.string.close_share_file_progress_message));
        }

        @Override
        protected JSONObject doInBackground(Integer... file_tenant) {
            if (!ConnectionChangeReceiver.isHasConnect)
                return null;
            isAll = false;
            if (1 == file_tenant[2])
                isAll = true;
            tenant = file_tenant[1];
            JSONObject result = CloudStorgeRestUtilities.closeShareFile(file_tenant[0], file_tenant[1], isAll);
            return result;
        }

        @Override
        protected void onPostExecute(final JSONObject result) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (null == result) {
                Toast.makeText(FileDetailActivity.this, R.string.share_file_error, Toast.LENGTH_SHORT).show();
                return;
            }
            // 更新文件的share字段
            String share = fileStruct.getShare();
            if (isAll)
                share = "";
            else {
                String tmp = tenant + "";
                String[] array = share.split(",");
                String newShare = "";
                for (int i = 0; i < array.length; i++) {
                    if (null == array[i] || "".equals(array[i]))
                        continue;
                    if (array[i].equals(tmp)) {
                        continue;
                    } else
                        newShare += array[i] + ",";
                }
                share = newShare;
            }
            updateFileShare(fileStruct.getFileId(), share);
            fileStruct.setShare(share);
            if (isAll) {
                hasTenant = 0;
                mContainerView.removeAllViews();
            }
            Toast.makeText(FileDetailActivity.this, R.string.close_share_file_success, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFileShare(int fileId, String share) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null)
                .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, share)
                .build());
        try {
            getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }
}
