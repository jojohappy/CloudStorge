package com.ces.cloudstorge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by MichaelDai on 13-7-31.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {
    public static boolean isHasConnect;

    @Override
    public void onReceive(Context context, Intent intent) {
        isHasConnect = check_networkStatus(context);
    }

    public static boolean check_networkStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (null != activeNetInfo && activeNetInfo.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        if (null != mobNetInfo && mobNetInfo.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

}
