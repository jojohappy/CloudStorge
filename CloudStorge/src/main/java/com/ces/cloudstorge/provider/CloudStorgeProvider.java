package com.ces.cloudstorge.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.ces.cloudstorge.Contract;
import com.ces.cloudstorge.util.SelectionBuilder;

/**
 * Created by MichaelDai on 13-7-18.
 */
public class CloudStorgeProvider extends ContentProvider {
    CloudStorgeDatabase mDatabaseHelper;

    private static final String AUTHORITY = Contract.CONTENT_AUTHORITY;
    public static final int ROUTE_CLOUDSTORGES = 1;

    /**
     * URI ID for route: /entries/{ID}
     */
    public static final int ROUTE_CLOUDSTORGE_ID = 2;
    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "cloudstorge", ROUTE_CLOUDSTORGES);
        sUriMatcher.addURI(AUTHORITY, "cloudstorge/*", ROUTE_CLOUDSTORGE_ID);
    }

    /**
     * Determine the mime type for entries returned by a given URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_CLOUDSTORGES:
                return CloudStorgeContract.CloudStorge.CONTENT_TYPE;
            case ROUTE_CLOUDSTORGE_ID:
                return CloudStorgeContract.CloudStorge.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new CloudStorgeDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_CLOUDSTORGE_ID:
                String id = uri.getLastPathSegment();
                builder.where(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=?", id);
            case ROUTE_CLOUDSTORGES:
                //builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME + " a, " + CloudStorgeContract.CloudStorge.TABLE_NAME + " b ")
                builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                /*Context ctx = getContext();
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);*/
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ROUTE_CLOUDSTORGES:
                long id = db.insertOrThrow(CloudStorgeContract.CloudStorge.TABLE_NAME, null, contentValues);
                result = Uri.parse(CloudStorgeContract.CloudStorge.CONTENT_URI + "/" + id);
                break;
            case ROUTE_CLOUDSTORGE_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        /*Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);*/
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_CLOUDSTORGES:
                count = builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_CLOUDSTORGE_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME)
                        .where(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        //ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_CLOUDSTORGES:
                count = builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, contentValues);
                break;
            case ROUTE_CLOUDSTORGE_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(CloudStorgeContract.CloudStorge.TABLE_NAME)
                        .where(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, contentValues);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    static class CloudStorgeDatabase extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;
        /**
         * Filename for SQLite file.
         */
        public static final String DATABASE_NAME = "cloudstorge.db";

        private static final String TYPE_TEXT = " TEXT";
        private static final String TYPE_INTEGER = " INTEGER";
        private static final String COMMA_SEP = ",";
        /**
         * SQL statement to create "entry" table.
         */
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + CloudStorgeContract.CloudStorge.TABLE_NAME + " (" +
                        CloudStorgeContract.CloudStorge._ID + TYPE_INTEGER + " PRIMARY KEY," +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + TYPE_INTEGER + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + TYPE_INTEGER + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE + TYPE_INTEGER + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE + TYPE_INTEGER + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION + TYPE_TEXT + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER + TYPE_INTEGER + COMMA_SEP +
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID + TYPE_INTEGER +
                        ")";

        /**
         * SQL statement to drop "cloudstorge" table.
         */
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + CloudStorgeContract.CloudStorge.TABLE_NAME;

        public CloudStorgeDatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i2) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }
}
