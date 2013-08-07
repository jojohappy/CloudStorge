package com.ces.cloudstorge.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.ces.cloudstorge.CloudStorgeProcessor;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;

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
        AccountManager am = AccountManager.get(getContext());
        try {
            JSONObject jobject = CloudStorgeRestUtilities.syncAllContent(account.name, am.peekAuthToken(account, "all"));
            if (null == jobject) {
                getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
                return;
            }
            int result = CloudStorgeProcessor.syncContentData(jobject, authority, contentResolver, syncResult);
        } catch (Exception e) {
            getContext().getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
        }
    }
}
