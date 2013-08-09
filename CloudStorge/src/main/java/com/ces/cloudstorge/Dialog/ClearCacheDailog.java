package com.ces.cloudstorge.Dialog;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.ces.cloudstorge.R;

import java.io.File;

/**
 * Created by MichaelDai on 13-8-9.
 */
public class ClearCacheDailog extends DialogPreference {
    public ClearCacheDailog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogMessage(R.string.clear_cache_confirm);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(android.R.drawable.ic_dialog_alert);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            trimCache(this.getContext());
        }
    }

    private void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                if(deleteDir(dir)) {
                    Toast.makeText(getContext(), R.string.clear_cache_success, Toast.LENGTH_SHORT).show();
                    getPreferenceManager().findPreference("clearCache").setEnabled(false);
                }
                else
                    Toast.makeText(getContext(), R.string.clear_cache_failure, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
