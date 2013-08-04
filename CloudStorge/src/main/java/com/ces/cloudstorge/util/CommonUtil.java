package com.ces.cloudstorge.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ces.cloudstorge.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by MichaelDai on 13-8-1.
 */
public class CommonUtil {

    static int[] pow = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    static HashMap<Integer, String> map = new HashMap<Integer, String>();

    static {
        map.put(0, "B");
        map.put(1, "KB");
        map.put(2, "MB");
        map.put(3, "GB");
        map.put(4, "TB");
        map.put(5, "PB");
        map.put(6, "EB");
        map.put(7, "ZB");
        map.put(8, "YB");
    }

    public static String format_fileSize(int size) {
        double valueTmp = size / 1.0;
        int step = 1024;
        double newData = 0;
        String unit = "";
        for (int i = 0; i < pow.length; i++) {
            if ((newData = (valueTmp * (Math.pow(step, pow[i] * -1)))) <= 1024) {
                unit = map.get(i);
                break;
            }
        }

        return String.format("%.2f%s", newData, unit);
    }

    public static void create_tipDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 获取当前时间字符串(yyyy-MM-dd HH:mm:ss)
    public static String get_currentDateString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }
}
