package com.ces.cloudstorge.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.ces.cloudstorge.Contract;

/**
 * Created by MichaelDai on 13-7-23.
 */
public class CloudStorgeContract {
    private CloudStorgeContract() {
    }

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + Contract.CONTENT_AUTHORITY);

    private static final String PATH_ENTRIES = "cloudstorge";

    public static class CloudStorge implements BaseColumns {

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.cloudstorgeadapter.cloudstorge";

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.cloudstorgeadapter.cloudstorges";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ENTRIES).build();

        public static final String TABLE_NAME = "cloudstorge";

        public static final String COLUMN_NAME_FILE_ID = "file_id";

        public static final String COLUMN_NAME_FOLDER_ID = "folder_id";

        public static final String COLUMN_NAME_NAME = "name";

        public static final String COLUMN_NAME_MIME_TYPE = "mime_type";

        public static final String COLUMN_NAME_SIZE = "size";

        public static final String COLUMN_NAME_CREATE_TIME = "create_time";

        public static final String COLUMN_NAME_LAST_MODIFIED = "last_modified";

        public static final String COLUMN_NAME_USERNAME = "username";

        public static final String COLUMN_NAME_REVISION_INFO = "revision_info";

        public static final String COLUMN_NAME_SHARE = "share";

        public static final String COLUMN_NAME_DESCRIPTION = "description";

        public static final String COLUMN_NAME_ORIGIN_FOLDER = "origin_folder";

        public static final String COLUMN_NAME_PARENT_FOLDER_ID = "parent_folder_id";
    }
}
