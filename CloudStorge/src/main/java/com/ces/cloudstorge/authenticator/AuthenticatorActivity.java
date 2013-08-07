package com.ces.cloudstorge.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ces.cloudstorge.Contract;
import com.ces.cloudstorge.R;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;

/**
 * Created by MichaelDai on 13-7-22.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

    public static final String PARAM_USERNAME = "username";

    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    private static final String TAG = "AuthenticatorActivity";
    private AccountManager mAccountManager;

    private UserLoginTask mAuthTask = null;

    private Boolean mConfirmCredentials = false;

    private String mPassword;

    private EditText mPasswordEdit;

    protected boolean mRequestNewAccount = false;

    private String mUsername;

    private EditText mUsernameEdit;

    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        mAccountManager = AccountManager.get(this);
        Log.i(TAG, "loading data from Intent");
        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);
        Log.i(TAG, "    request new: " + mRequestNewAccount);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.login_linearlayout);
        linearLayout.getBackground().setAlpha(45);
        mUsernameEdit = (EditText) findViewById(R.id.email);
        mPasswordEdit = (EditText) findViewById(R.id.password);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleLogin(view);
            }
        });
    }

    public void handleLogin(View view) {
        mUsernameEdit.setError(null);
        mPasswordEdit.setError(null);
        View focusView = null;
        boolean cancel = false;
        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
            if (TextUtils.isEmpty(mUsername)) {
                mUsernameEdit.setError(getString(R.string.error_field_required));
                focusView = mUsernameEdit;
                focusView.requestFocus();
                return;
            }
        }
        mPassword = mPasswordEdit.getText().toString();
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordEdit.setError(getString(R.string.error_field_required));
            focusView = mUsernameEdit;
            focusView.requestFocus();
            return;
        } else {
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void finishConfirmCredentials(boolean result) {
        Log.i(TAG, "finishConfirmCredentials()");
        final Account account = new Account(mUsername, Contract.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishLogin(String authToken) {

        Log.i(TAG, "finishLogin()");
        final Account account = new Account(mUsername, Contract.ACCOUNT_TYPE);
        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, mPassword, null);
            mAccountManager.setAuthToken(account, "all", authToken);
            //ContentResolver.setSyncAutomatically(account, CloudStorgeContract.AUTHORITY, true);
        } else {
            mAccountManager.setPassword(account, mPassword);
        }
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Contract.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onAuthenticationResult(String authToken) {

        boolean success = ((authToken != null) && (authToken.length() > 0));
        Log.i(TAG, "onAuthenticationResult(" + success + ")");
        mAuthTask = null;
        //hideProgress();

        if (success) {
            if (!mConfirmCredentials) {
                finishLogin(authToken);
            } else {
                finishConfirmCredentials(success);
            }
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate");
            if (mRequestNewAccount) {
                //mMessage.setText(getText(R.string.login_activity_loginfail_text_both));
            } else {
                //mMessage.setText(getText(R.string.login_activity_loginfail_text_pwonly));
            }
        }
    }

    public void onAuthenticationCancel() {
        Log.i(TAG, "onAuthenticationCancel()");
        mAuthTask = null;
        //hideProgress();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                return CloudStorgeRestUtilities.authenticate(mUsername, mPassword);
            } catch (Exception ex) {
                Log.e(TAG, "UserLoginTask.doInBackground: failed to authenticate");
                Log.i(TAG, ex.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String authToken) {
            onAuthenticationResult(authToken);
        }

        @Override
        protected void onCancelled() {
            onAuthenticationCancel();
        }
    }
}
