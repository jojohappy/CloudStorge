package com.ces.cloudstorge.Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.ces.cloudstorge.R;

/**
 * Created by MichaelDai on 13-7-30.
 */
public class DeleteFileDialog extends DialogFragment {
    public DeleteFileDialog() {

    }

    public interface DeleteFileDialogListener {
        void onFinishDeleteFileDialog(String filelist, String folderlist, boolean isForever);
    }

    DeleteFileDialogListener mListener;
    private String filelist;
    private String folderlist;
    private Boolean isForever;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeleteFileDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        filelist = getArguments().getString("filelist");
        folderlist = getArguments().getString("folderlist");
        isForever = getArguments().getBoolean("isForever");
        int dialogMessageId = isForever ? R.string.delete_confirm_forever : R.string.delete_confirm;
        builder.setTitle(R.string.menu_action_delete).
                setMessage(dialogMessageId).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onFinishDeleteFileDialog(filelist, folderlist, isForever);
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
}
