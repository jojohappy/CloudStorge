package com.ces.cloudstorge.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.ces.cloudstorge.CloudStorgeProcessor;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;

import org.json.JSONObject;

/**
 * Created by MichaelDai on 13-7-22.
 */
public class CloudStorgeSyncAdapter extends AbstractThreadedSyncAdapter {
    public CloudStorgeSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        ContentResolver contentResolver = getContext().getContentResolver();
        JSONObject jobject = CloudStorgeRestUtilities.syncAllContent(account.name);
        int result = CloudStorgeProcessor.syncContentData(jobject, authority, contentResolver, syncResult);
    }
}
