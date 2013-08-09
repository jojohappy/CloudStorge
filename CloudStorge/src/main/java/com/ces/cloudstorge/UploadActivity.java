package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ces.cloudstorge.Dialog.FolderListDialog;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UploadActivity extends FragmentActivity implements FolderListDialog.FolderListDialogListener {
    private TextView uploadFileName;
    private TextView uploadFileLocationText;
    private LinearLayout uploadFileLocation;
    private File uploadFile;
    private Account currentAccount;
    private int destFolderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_upload);
        if (getIntent().getAction().equals(Contract.UPLOAD_ACTION)) {
            destFolderId = getIntent().getExtras().getInt("destFolderId");
            currentAccount = (Account) getIntent().getExtras().get("currentAccount");
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_GET_CONTENT);
            shareIntent.addCategory(Intent.CATEGORY_OPENABLE);
            shareIntent.setType("*/*");
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(shareIntent, 0);
            //boolean isIntentSafe = activities.size() > 0;
            startActivityForResult(shareIntent, RESULT_CANCELED);
            return;
        }
        setContentView(R.layout.activity_upload);
        setTitle(R.string.upload_title);
        uploadFileName = (TextView) findViewById(R.id.upload_file_name);
        uploadFileLocationText = (TextView) findViewById(R.id.upload_file_location_text);
        uploadFileLocation = (LinearLayout) findViewById(R.id.upload_location);
        uploadFileLocationText.setText(R.string.location_text);
        Intent intent = getIntent();
        String type = intent.getType();
        Uri uploadFileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uploadFileUri != null) {
            String realPath = getRealPathFromURI(uploadFileUri);
            if (null == realPath)
                realPath = uploadFileUri.getPath();
            uploadFile = new File(realPath);
            uploadFileName.setText(uploadFile.getName());
        } else {
            CommonUtil.create_tipDialog(this, getString(R.string.terrible), getString(R.string.read_file_error));
            finish();
        }

        AccountManager mAccountManager = AccountManager.get(this);
        Account[] accounts = mAccountManager.getAccountsByType(Contract.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            CommonUtil.create_tipDialog(UploadActivity.this, getString(R.string.terrible),
                    getString(R.string.no_account));
            finish();
            return;
        }
        currentAccount = accounts[0];

        uploadFileLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FolderListDialog folderListDialog = new FolderListDialog();
                Bundle fld = new Bundle();
                fld.putString("currentUser", currentAccount.name);
                fld.putString("filelist", "");
                fld.putString("folderlist", "");
                fld.putInt("currentFolderId", Contract.FOLDER_ROOT);
                fld.putInt("parentFolderId", Contract.FOLDER_ROOT);
                folderListDialog.setArguments(fld);
                folderListDialog.show(getSupportFragmentManager(), "uploadfile");
            }
        });

        Button cancelButton = (Button) findViewById(R.id.upload_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        Button confirmButton = (Button) findViewById(R.id.upload_ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new UploadAsyncTask().execute(destFolderId + "", uploadFile.getPath());
            }
        });
        destFolderId = getRootFolderId();
    }

    @Override
    public void onFinishSelectFolder(int destFolderId, String fileList, String folderList) {
        if (destFolderId == Contract.FOLDER_ROOT) {
            destFolderId = getRootFolderId();
        }
        this.destFolderId = destFolderId;
        uploadFileLocationText.setText(getDestFolderName(destFolderId));
    }

    public int getRootFolderId() {
        String selection = String.format(MainActivity.SELECTION_CHILD, Contract.FOLDER_ROOT, currentAccount.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getInt(Contract.PROJECTION_FOLDER_ID);
    }

    private String getDestFolderName(int destFolderId) {
        String selection = String.format(MainActivity.selection_folder_format, destFolderId, currentAccount.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getString(Contract.PROJECTION_NAME);
    }

    public String getRealPathFromURI(Uri contentUri) {
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            //Cursor cursor = managedQuery(contentUri, proj, null, null, null);
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
            int column_index;
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    private void insert_newFile(int newFileId, String mime_type) {
        // 插入新数据
        try {
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            batch.add(ContentProviderOperation.newInsert(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, newFileId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, destFolderId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, uploadFile.getName())
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, mime_type)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, uploadFile.length())
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, CommonUtil.get_currentDateString())
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, CommonUtil.get_currentDateString())
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, currentAccount.name)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, "")
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, "")
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, "")
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, -1)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, destFolderId)
                    .build());

            getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
            getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public void create_uploadDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public String getAuthToken() {
        AccountManager mAccountManager = AccountManager.get(this);
        String auth = mAccountManager.peekAuthToken(currentAccount, "all");
        return "OAuth2 " + auth;
    }

    public class UploadAsyncTask extends AsyncTask<String, String, Integer> {
        //private String UPLOAD_URL = "http://rd.114.chinaetek.com:18081/file/upload";
        private ProgressDialog progressDialog;
        private int newFileId;
        private String mime_type;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(UploadActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(getString(R.string.upload_file_wait));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(String... strings) {
            return uploadFile2Server(Integer.parseInt(strings[0]), strings[1]);
        }

        @Override
        protected void onPostExecute(final Integer result) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (result == 0) {
                insert_newFile(newFileId, mime_type);
                create_uploadDialog(getString(R.string.upload_title), getString(R.string.upload_success));
            } else if (result == -2) {
                create_uploadDialog(getString(R.string.terrible), getString(R.string.upload_max_size));
            } else {
                create_uploadDialog(getString(R.string.terrible), getString(R.string.upload_error));
            }
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }

        private int uploadFile2Server(int destFolderId, String path) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize, result;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            try {
                File file = new File(path);
                if (file.length() > 20 * 1024 * 1024)
                    return -2;
                FileInputStream fileInputStream = new FileInputStream(file);
                Long fileSize = file.length();
                URL url = new URL(CloudStorgeRestUtilities.UPLOAD_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("User-Agent", Contract.USER_AGENT);
                conn.setRequestProperty("Charsert", "UTF-8");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("Authorization", getAuthToken());
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"current_folder_id\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(destFolderId + "" + lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_device\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("android" + lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"attachment\"; filename=\"");
                byte[] filenamebuf = file.getName().getBytes("UTF-8");
                //dos.writeUTF(file.getName());
                dos.write(filenamebuf, 0, filenamebuf.length);
                dos.writeBytes("\"" + lineEnd);
                dos.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    publishProgress((int) (((fileSize - bytesAvailable) / fileSize.doubleValue()) * 100) + "");
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                int serverResponseCode = conn.getResponseCode();
                if (serverResponseCode == HttpStatus.SC_OK) {
                    InputStream in = conn.getInputStream();
                    int ch;
                    StringBuilder sb2 = new StringBuilder();
                    while ((ch = in.read()) != -1) {
                        sb2.append((char) ch);
                    }

                    JSONObject jsonObject = new JSONObject(sb2.toString());
                    result = jsonObject.getInt("result");
                    newFileId = jsonObject.getInt("file_id");
                    mime_type = jsonObject.getString("mime_type");
                } else
                    result = -1;
                fileInputStream.close();
                dos.flush();
                dos.close();
                return result;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return -1;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            } catch (JSONException e) {
                e.printStackTrace();
                return -1;
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) {
            finish();
            return;
        }
        Uri fileUri = data.getData();
        String realPath = getRealPathFromURI(fileUri);
        if (null == realPath)
            realPath = fileUri.getPath();
        uploadFile = new File(realPath);
        new UploadAsyncTask().execute(destFolderId + "", realPath);
    }

}
