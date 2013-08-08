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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MichaelDai on 13-7-22.
 */
public class CloudStorgeSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String selection_need_sync = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC +
            "=" + Contract.NEED_SYNC + ")";

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
            List<Integer> listId = new ArrayList<Integer>();
            int delete_count = 0;
            Cursor needSyncCursor = contentResolver.query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection_need_sync, null, null);
            while(needSyncCursor.moveToNext()) {
                int sync_action = needSyncCursor.getInt(Contract.PROJECTION_SYNC_ACTION);
                int fileId = needSyncCursor.getInt(Contract.PROJECTION_FILE_ID);
                int folderId = needSyncCursor.getInt(Contract.PROJECTION_FOLDER_ID);
                int parent_folder_id = needSyncCursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
                String name = needSyncCursor.getString(Contract.PROJECTION_NAME);
                int _id = needSyncCursor.getInt(Contract.PROJECTION_ID);
                listId.add(_id);
                int fileType;
                if(fileId == -1)
                    fileType = Contract.TYPE_FOLDER;
                else
                    fileType = Contract.TYPE_FILE;
                System.out.println("fileId == " + fileId + "||--fileType ===== " + fileType);
                // 暂时更新记录为无需再更新
                CloudStorgeProcessor.set_rowDontNeedSync(contentResolver, _id);
                switch (sync_action) {
                    case Contract.SYNC_ACTION_DELETE:
                        if(fileId == -1)
                            delete_folder_array += folderId + ",";
                        else
                            delete_file_array += fileId + ",";
                        delete_count++;
                        break;
                    case Contract.SYNC_ACTION_MOVE:
                        JSONObject jsonObject = CloudStorgeRestUtilities.moveFile(fileId, folderId, parent_folder_id,
                                fileType, am.peekAuthToken(account, "all"));
                        if(null == jsonObject || jsonObject.getInt("result") != 0)
                            // 还原记录为未同步
                            CloudStorgeProcessor.set_rowNeedSync(contentResolver, _id);
                        break;
                    case Contract.SYNC_ACTION_RENAME:
                        JSONObject jsonObject1 = CloudStorgeRestUtilities.renameFile(fileId, folderId, name,
                                fileType, am.peekAuthToken(account, "all"));
                        if(null == jsonObject1 || jsonObject1.getInt("result") != 0)
                            // 还原记录为未同步
                            CloudStorgeProcessor.set_rowNeedSync(contentResolver, _id);
                        break;
                }
            }
            // 如果有删除的，则sync
            if(delete_count > 0) {
                JSONObject jsonObject = CloudStorgeRestUtilities.deleteFile(delete_file_array, delete_folder_array, 0, am.peekAuthToken(account, "all"));
                if(null == jsonObject || jsonObject.getInt("result") != 0) {
                    // 还原记录为未同步
                    for(int i = 0; i < listId.size(); i++)
                        CloudStorgeProcessor.set_rowNeedSync(contentResolver, listId.get(i));
                }
            }
            if(isSync) {
                JSONObject jobject = CloudStorgeRestUtilities.syncAllContent(account.name, am.peekAuthToken(account, "all"));
                if (null == jobject) {
                    getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
                    return;
                }
                CloudStorgeProcessor.syncContentData(jobject, needSyncCursor, authority, contentResolver, syncResult);
            }
            else
            {
                getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
            }

        } catch (Exception e) {
            getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        }
    }
}
