package com.ces.cloudstorge;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;

import com.ces.cloudstorge.provider.CloudStorgeContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by MichaelDai on 13-7-24.
 */
public class CloudStorgeProcessor {

    public static int syncContentData(JSONObject jobject, String authority, ContentResolver mContentResolver, SyncResult syncResult) {
        try {
            if (null == jobject)
                return -1;
            JSONArray jarray = jobject.getJSONArray("file_list");
            if (0 == jarray.length())
                return -1;
            //mContentResolver.delete(CloudStorgeContract.CloudStorge.CONTENT_URI, "", null);
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            batch.add(ContentProviderOperation.newDelete(CloudStorgeContract.CloudStorge.CONTENT_URI).build());
            for (int i = 0; i < jarray.length(); i++) {
                JSONObject data = jarray.getJSONObject(i);
                batch.add(ContentProviderOperation.newInsert(CloudStorgeContract.CloudStorge.CONTENT_URI)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER))
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID))
                        .build());
                syncResult.stats.numInserts++;
            }
            mContentResolver.applyBatch(Contract.CONTENT_AUTHORITY, batch);
            //mContentResolver.notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        mContentResolver.notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        return 0;
    }
}
