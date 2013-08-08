package com.ces.cloudstorge;

import android.os.Build;

import com.ces.cloudstorge.provider.CloudStorgeContract;

/**
 * Created by MichaelDai on 13-7-24.
 */
public class Contract {
    public static final String ACCOUNT_TYPE = "com.ces.cloudstorge";
    public static final String CONTENT_AUTHORITY = "com.ces.cloudstorge.cloudstorgeprovider";
    public static final int DRAWER_ROOT = 1;
    public static final int DRAWER_SHARE = 2;
    public static final int DRAWER_TRASH = 3;
    public static final int DRAWER_LOGOUT = 4;
    public static final int FOLDER_ROOT = -1;
    public static final int FOLDER_TRASH = -10;
    public static final int FOLDER_SHARE = -20;
    public static final int PROJECTION_MIME_TYPE = 0;
    public static final int PROJECTION_LAST_MODIFIED = 1;
    public static final int PROJECTION_ID = 2;
    public static final int PROJECTION_NAME = 3;
    public static final int PROJECTION_FILE_ID = 4;
    public static final int PROJECTION_FOLDER_ID = 5;
    public static final int PROJECTION_PARENT_FOLDER_ID = 6;
    public static final int PROJECTION_SHARE = 7;
    public static final int PROJECTION_SIZE = 8;
    public static final int PROJECTION_ORIGIN_FOLDER = 9;
    public static final int PROJECTION_SYNC_ACTION = 10;
    public static final int TYPE_FILE = 0;
    public static final int TYPE_FOLDER = 1;
    public static final String UPLOAD_ACTION = "com.ces.cloudstorge.UploadActivity.UPLOAD_FILE";
    public static final String USER_AGENT = "CloudStorge/1.0 Android-Agent/" + Build.VERSION.RELEASE;
    public static final String CLIENT_ID = "9032657667.app.boc-service.com";
    public static final String CLIENT_SECRET = "r6wOV2HUxWypntSSn4UPbQ";
    public static final int SYNC_ACTION_MOVE = 1;
    public static final int SYNC_ACTION_DELETE = 2;
    public static final int SYNC_ACTION_RENAME = 3;
    public static final int NOT_OFFLINE = 0;
    public static final int IS_OFFLINE = 1;
    public static final int DONT_NEED_SYNC = 0;
    public static final int NEED_SYNC = 1;
    public static final String[] PROJECTION = new String[]{CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge._ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION
    };
}
