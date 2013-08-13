package com.ces.cloudstorge;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;

import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.FileStruct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by MichaelDai on 13-7-24.
 */
public class CloudStorgeProcessor {

    public static int syncContentData(Account account, JSONObject jobject, Cursor needSyncCursor, String authority, ContentResolver mContentResolver, SyncResult syncResult) {
        try {
            if (null == jobject)
                return -1;
            JSONArray jarray = jobject.getJSONArray("file_list");
            if (null == jarray || 0 == jarray.length())
                return -1;
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            //batch.add(ContentProviderOperation.newDelete(CloudStorgeContract.CloudStorge.CONTENT_URI).build());
            ArrayList<FileStruct> listServerData = new ArrayList<FileStruct>();
            for (int i = 0; i < jarray.length(); i++) {
                JSONObject data = jarray.getJSONObject(i);
                FileStruct fileStruct = new FileStruct();
                fileStruct.setFileId(data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID));
                fileStruct.setFolderId(data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID));
                fileStruct.setName(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME));
                fileStruct.setMimeType(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE));
                fileStruct.setSize(data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE));
                fileStruct.setLast_modified(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED));
                fileStruct.setShare(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE));
                fileStruct.setParentFolderId(data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID));
                fileStruct.setCreateTime(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME));
                fileStruct.setUsername(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME));
                fileStruct.setRevisionInfo(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO));
                fileStruct.setDescription(data.getString(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION));
                fileStruct.setOriginFolder(data.getInt(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER));
                listServerData.add(fileStruct);
            }
            Cursor allContentData = mContentResolver.query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION,
                    CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='" + account.name + "'", null, null);
            while (allContentData.moveToNext()) {
                int _id = allContentData.getInt(Contract.PROJECTION_ID);
                int fileId = allContentData.getInt(Contract.PROJECTION_FILE_ID);
                int folderId = allContentData.getInt(Contract.PROJECTION_FOLDER_ID);
                int originFolder = allContentData.getInt(Contract.PROJECTION_ORIGIN_FOLDER);
                int parentFolderId = allContentData.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
                boolean isDelete = true;
                if (fileId == -99999)
                    continue;
                for (int i = 0; i < listServerData.size(); i++) {
                    FileStruct fileStruct = listServerData.get(i);
                    if (fileId == -1) {
                        if (folderId == fileStruct.getFolderId()) {
                            isDelete = false;
                            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                                    .withSelection(CloudStorgeContract.CloudStorge._ID + "=" + _id, null)
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, fileStruct.getFileId())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, fileStruct.getFolderId())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, fileStruct.getName())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, fileStruct.getMimeType())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, fileStruct.getSize())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, fileStruct.getCreateTime())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, fileStruct.getLast_modified())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, fileStruct.getUsername())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, fileStruct.getRevisionInfo())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, fileStruct.getShare())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, fileStruct.getDescription())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, fileStruct.getOriginFolder())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, fileStruct.getParentFolderId())
                                    .build());
                            syncResult.stats.numUpdates++;
                            break;
                        }
                    } else {
                        if (fileId == fileStruct.getFileId() &&
                                fileStruct.getOriginFolder() == originFolder &&
                                fileStruct.getParentFolderId() == parentFolderId) {
                            isDelete = false;
                            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                                    .withSelection(CloudStorgeContract.CloudStorge._ID + "=" + _id, null)
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, fileStruct.getFileId())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, fileStruct.getFolderId())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, fileStruct.getName())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, fileStruct.getMimeType())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, fileStruct.getSize())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, fileStruct.getCreateTime())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, fileStruct.getLast_modified())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, fileStruct.getUsername())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, fileStruct.getRevisionInfo())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, fileStruct.getShare())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, fileStruct.getDescription())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, fileStruct.getOriginFolder())
                                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, fileStruct.getParentFolderId())
                                    .build());
                            syncResult.stats.numUpdates++;
                            break;
                        }
                    }
                }
                if (isDelete) {
                    batch.add(ContentProviderOperation.newDelete(CloudStorgeContract.CloudStorge.CONTENT_URI)
                            .withSelection(CloudStorgeContract.CloudStorge._ID + "=" + _id, null)
                            .build());
                    syncResult.stats.numDeletes++;
                }
            }

            for (int i = 0; i < listServerData.size(); i++) {
                FileStruct fileStruct = listServerData.get(i);
                boolean newData = true;
                if (allContentData.moveToFirst()) {
                    int fileId = fileStruct.getFileId();
                    int folderId = fileStruct.getFolderId();
                    int originFolder = fileStruct.getOriginFolder();
                    int parentFolderId = fileStruct.getParentFolderId();
                    do {
                        int fileContentId = allContentData.getInt(Contract.PROJECTION_FILE_ID);
                        int folderContentId = allContentData.getInt(Contract.PROJECTION_FOLDER_ID);
                        int originContentFolder = allContentData.getInt(Contract.PROJECTION_ORIGIN_FOLDER);
                        int parentFolderContentId = allContentData.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
                        if (fileId == -1) {
                            if (folderContentId == folderId) {
                                newData = false;
                                break;
                            }
                        } else {
                            if (fileContentId == fileId && originContentFolder == originFolder && parentFolderContentId == parentFolderId) {
                                newData = false;
                                break;
                            }
                        }
                    } while (allContentData.moveToNext());
                }
                if (newData) {
                    batch.add(ContentProviderOperation.newInsert(CloudStorgeContract.CloudStorge.CONTENT_URI)
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, fileStruct.getFileId())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, fileStruct.getFolderId())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, fileStruct.getName())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, fileStruct.getMimeType())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, fileStruct.getSize())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, fileStruct.getCreateTime())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, fileStruct.getLast_modified())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, fileStruct.getUsername())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, fileStruct.getRevisionInfo())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, fileStruct.getShare())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, fileStruct.getDescription())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, fileStruct.getOriginFolder())
                            .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, fileStruct.getParentFolderId())
                            .build());
                    syncResult.stats.numInserts++;
                }
            }
            allContentData.close();
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

    public static void set_rowDontNeedSync(ContentResolver mContentResolver, int _id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.DONT_NEED_SYNC);
        mContentResolver.update(CloudStorgeContract.CloudStorge.CONTENT_URI, contentValues, CloudStorgeContract.CloudStorge._ID + "=" + _id, null);
    }

    public static void set_rowNeedSync(ContentResolver mContentResolver, int _id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC);
        mContentResolver.update(CloudStorgeContract.CloudStorge.CONTENT_URI, contentValues, CloudStorgeContract.CloudStorge._ID + "=" + _id, null);
    }

    public static void set_rowDontNeedSyncForever(ContentResolver mContentResolver, int _id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.DONT_NEED_SYNC);
        contentValues.putNull(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION);
        mContentResolver.update(CloudStorgeContract.CloudStorge.CONTENT_URI, contentValues, CloudStorgeContract.CloudStorge._ID + "=" + _id, null);
    }
}
