package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.util.CommonUtil;

public class LoginActivity extends Activity {
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private UserLoginTask mAuthTask = null;
    private static final String EXTRA_EMAIL = "email";
    private String mEmail;
    private String mPassword;

    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    private static final String TAG = "LoginActivity";
    private AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountManager = AccountManager.get(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.login_linearlayout);
        linearLayout.getBackground().setAlpha(45);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        mEmailView.setError(null);
        mPasswordView.setError(null);

        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!mEmail.contains("@")) {
            //mEmailView.setError(getString(R.string.error_invalid_email));
            //focusView = mEmailView;
            //cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
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

    private void finishLogin(String authToken) {
        Log.i(TAG, "finishLogin()");
        final Account account = new Account(mEmail, Contract.ACCOUNT_TYPE);
        mAccountManager.addAccountExplicitly(account, mPassword, null);
        mAccountManager.setAuthToken(account, "all", authToken);
        // 跳转到MainActivity
        final Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.putExtra("current_account", account);
        intent.putExtra("isRoot", true);
        startActivity(intent);
        //setResult(RESULT_OK, intent);
        finish();
    }

    public void onAuthenticationResult(String authToken) {
        boolean success = ((authToken != null) && (authToken.length() > 0));
        Log.i(TAG, "onAuthenticationResult(" + success + ")");
        mAuthTask = null;

        if (success) {
            finishLogin(authToken);
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate");
            showProgress(false);
            CommonUtil.create_tipDialog(this, getString(R.string.terrible), getString(R.string.login_error));
        }
    }

    public void onAuthenticationCancel() {
        Log.i(TAG, "onAuthenticationCancel()");
        mAuthTask = null;
    }

    public class UserLoginTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {
                return CloudStorgeRestUtilities.authenticate(mEmail, mPassword);
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
