package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.ces.cloudstorge.provider.CloudStorgeContract;

public class LaunchActivity extends Activity {

    private AccountManager mAccountManager;
    private final String[] PROJECTION = new String[]{
            CloudStorgeContract.CloudStorge._ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME
    };
    private final String selection = CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + " = -99999";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_launch);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // 判断是否存在帐户
                mAccountManager = AccountManager.get(LaunchActivity.this);
                Account[] accounts = mAccountManager.getAccountsByType(Contract.ACCOUNT_TYPE);
                Intent intent = new Intent();
                if (accounts.length == 0) {
                    // 不存在则调用login activity
                    intent.setClass(LaunchActivity.this, LoginActivity.class);
                } else {
                    Account account = accounts[0];
                    // 查询已选择的用户
                    Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
                    if(null == cursor || !cursor.moveToFirst())
                        account = accounts[0];
                    else {
                        for(int i = 0; i < accounts.length; i++) {
                            if(accounts[i].name.equals(cursor.getString(1)))
                            {
                                account = accounts[i];
                                break;
                            }
                        }
                    }
                    // 存在则调用MainActivity
                    intent.putExtra("current_account", account);
                    intent.putExtra("isRoot", true);
                    intent.setClass(LaunchActivity.this, MainActivity.class);
                }
                // 查看网络连接状态
                ConnectionChangeReceiver.isHasConnect = ConnectionChangeReceiver.check_networkStatus(LaunchActivity.this);
                startActivity(intent);
                finish();
            }
        }, 2000);


    }
}
