package com.ces.cloudstorge.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;

import com.ces.cloudstorge.CloudStorgeProcessor;
import com.ces.cloudstorge.Contract;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;

import org.json.JSONObject;

/**
 * Created by MichaelDai on 13-7-22.
 */
public class CloudStorgeSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String selection_need_sync = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC +
            "=" + Contract.NEED_SYNC + " and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='%s')";

    public CloudStorgeSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        ContentResolver contentResolver = getContext().getContentResolver();
        AccountManager am = AccountManager.get(getContext());
        boolean isSync = bundle.getBoolean("isSync");
        try {
            String delete_file_array = "";
            String delete_folder_array = "";
            String selection = String.format(selection_need_sync, account.name);
            Cursor needSyncCursor = contentResolver.query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
            while (needSyncCursor.moveToNext()) {
                String syncAction = needSyncCursor.getString(Contract.PROJECTION_SYNC_ACTION);
                String[] syncActionArray = syncAction.split(",");
                int fileId = needSyncCursor.getInt(Contract.PROJECTION_FILE_ID);
                int folderId = needSyncCursor.getInt(Contract.PROJECTION_FOLDER_ID);
                int parent_folder_id = needSyncCursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
                String name = needSyncCursor.getString(Contract.PROJECTION_NAME);
                int _id = needSyncCursor.getInt(Contract.PROJECTION_ID);
                int fileType;
                if (fileId == -1)
                    fileType = Contract.TYPE_FOLDER;
                else
                    fileType = Contract.TYPE_FILE;
                // 暂时更新记录为无需再更新
                CloudStorgeProcessor.set_rowDontNeedSync(contentResolver, _id);
                for (int i = 0; i < syncActionArray.length; i++) {
                    switch (Integer.parseInt(syncActionArray[i])) {
                        case Contract.SYNC_ACTION_DELETE:
                            if (fileId == -1) {
                                delete_folder_array = folderId + "";
                            } else {
                                delete_file_array = fileId + "";
                            }
                            JSONObject jsonObjectDelete = CloudStorgeRestUtilities.deleteFile(delete_file_array, delete_folder_array, 0, am.peekAuthToken(account, "all"));
                            if (null == jsonObjectDelete || jsonObjectDelete.getInt("result") != 0)
                                CloudStorgeProcessor.set_rowNeedSync(contentResolver, _id);
                            else
                                CloudStorgeProcessor.set_rowDontNeedSyncForever(contentResolver, _id);
                            break;
                        case Contract.SYNC_ACTION_MOVE:
                            JSONObject jsonObjectMove = CloudStorgeRestUtilities.moveFile(fileId, folderId, parent_folder_id,
                                    fileType, am.peekAuthToken(account, "all"));
                            if (null == jsonObjectMove || jsonObjectMove.getInt("result") != 0)
                                // 还原记录为未同步
                                CloudStorgeProcessor.set_rowNeedSync(contentResolver, _id);
                            else
                                CloudStorgeProcessor.set_rowDontNeedSyncForever(contentResolver, _id);
                            break;
                        case Contract.SYNC_ACTION_RENAME:
                            JSONObject jsonObjectRename = CloudStorgeRestUtilities.renameFile(fileId, folderId, name,
                                    fileType, am.peekAuthToken(account, "all"));
                            if (null == jsonObjectRename || jsonObjectRename.getInt("result") != 0)
                                // 还原记录为未同步
                                CloudStorgeProcessor.set_rowNeedSync(contentResolver, _id);
                            else
                                CloudStorgeProcessor.set_rowDontNeedSyncForever(contentResolver, _id);
                            break;
                    }
                }
            }
            if (isSync) {
                JSONObject jobject = CloudStorgeRestUtilities.syncAllContent(account.name, am.peekAuthToken(account, "all"));
                if (null == jobject) {
                    getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
                    return;
                }
                CloudStorgeProcessor.syncContentData(account, jobject, needSyncCursor, authority, contentResolver, syncResult);
            } else {
                getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
            }

        } catch (Exception e) {
            getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        }
    }
}
