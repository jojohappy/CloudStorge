package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

public class LaunchActivity extends Activity {

    private AccountManager mAccountManager;

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
                    // 存在则调用MainActivity
                    intent.putExtra("current_account", accounts[0]);
                    intent.putExtra("isRoot", true);
                    intent.setClass(LaunchActivity.this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, 2000);


    }
}
