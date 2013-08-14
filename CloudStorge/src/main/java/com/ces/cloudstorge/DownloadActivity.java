package com.ces.cloudstorge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DownloadActivity extends Activity {
    private int fileId;
    private Cursor cursor;
    private DownloadAsyncTask downloadAsyncTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ConnectionChangeReceiver.isHasConnect) {
            create_DownloadDialog(getString(R.string.terrible), getString(R.string.download_file_network_error));
            return;
        }

        File dir = new File(Environment.getDataDirectory() + "/data/com.ces.cloudstorge/cache/");
        dir.mkdirs();
        dir.setReadable(true, false);
        dir.setWritable(true, false);
        dir.setExecutable(true, false);

        fileId = getIntent().getExtras().getInt("fileId");
        cursor = get_fileInDatabase(fileId);
        downloadAsyncTask = new DownloadAsyncTask();
        downloadAsyncTask.execute(fileId);
        setContentView(R.layout.activity_download);
    }


    public void create_DownloadDialog(String title, String message) {
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

    public class DownloadAsyncTask extends AsyncTask<Integer, String, Integer> {
        private ProgressDialog progressDialog;
        private Uri fileUri;
        private int DOWNLOAD_BUFFER_SIZE = 1024 * 1024;
        private String mimeType;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(DownloadActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(getString(R.string.download_file_tip) + cursor.getString(Contract.PROJECTION_NAME));
            progressDialog.setCancelable(true);
            progressDialog.setIndeterminate(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    downloadAsyncTask.cancel(true);
                    finish();
                }
            });
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Integer... fileId) {
            return downloadfile(fileId[0]);
        }

        @Override
        protected void onPostExecute(final Integer result) {
            if(downloadAsyncTask.isCancelled())
                return;
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (result == 0) {
                Intent downloadIntent = new Intent();
                downloadIntent.setAction(Intent.ACTION_VIEW);
                downloadIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                downloadIntent.setDataAndType(fileUri, mimeType);
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(downloadIntent, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (!isIntentSafe)
                    create_DownloadDialog(getString(R.string.terrible), getString(R.string.download_file_cannot_open));
                else {
                    startActivityForResult(downloadIntent, RESULT_CANCELED);
                    finish();
                }
            } else {
                create_DownloadDialog(getString(R.string.terrible), getString(R.string.download_file_error));
            }

        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }

        private int downloadfile(int fileId) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpURLConnection conn = null;
            Integer fileSize;
            BufferedInputStream inStream;
            BufferedOutputStream outStream;
            File outFile;
            FileOutputStream fileStream;
            try {
                URL url = null;
                url = new URL(CloudStorgeRestUtilities.DOWNLOAD_URL + "?file_id=" + fileId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("User-Agent", Contract.USER_AGENT);
                conn.setRequestProperty("Authorization", CloudStorgeRestUtilities.getAuthToken());
                conn.setUseCaches(false);
                conn.connect();
                conn.getResponseCode();
                fileSize = cursor.getInt(Contract.PROJECTION_SIZE);
                inStream = new BufferedInputStream(conn.getInputStream());
                outFile = new File(Environment.getDataDirectory() + "/data/com.ces.cloudstorge/cache/" + cursor.getString(Contract.PROJECTION_NAME));
                fileStream = new FileOutputStream(outFile);
                outStream = new BufferedOutputStream(fileStream, DOWNLOAD_BUFFER_SIZE);
                byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
                int bytesRead = 0, totalRead = 0;
                while ((bytesRead = inStream.read(data, 0, data.length)) >= 0) {
                    if (Thread.interrupted()) {
                        outFile.delete();
                        return 0;
                    }
                    outStream.write(data, 0, bytesRead);
                    totalRead += bytesRead;
                    publishProgress((int) ((totalRead / fileSize.doubleValue()) * 100) + "");
                }

                outStream.close();
                fileStream.close();
                inStream.close();
                fileUri = Uri.fromFile(outFile);
                String fileType = cursor.getString(Contract.PROJECTION_MIME_TYPE);
                mimeType = CommonUtil.mime_type.get(fileType);
                if (null == mimeType)
                    mimeType = "application/octet-stream";
                outFile.setReadable(true, false);
                outFile.setWritable(true, false);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return -1;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
            return 0;
        }
    }

    private Cursor get_fileInDatabase(int fileId) {
        String selection = String.format(MainActivity.selection_file_share, fileId);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        finish();
    }
}
