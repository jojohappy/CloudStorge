package com.ces.cloudstorge.Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ces.cloudstorge.R;

/**
 * Created by Jojohappy on 13-7-29.
 */
public class AddNewFolderDialog extends DialogFragment {
    public interface AddNewFolderDialogListener {
        void onFinishAddFolderDialog(String inputText);
    }

    private EditText mEditText;
    AddNewFolderDialogListener mListener;

    public AddNewFolderDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (AddNewFolderDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_add_folder_dialog, null);
        builder.setView(view);
        mEditText = (EditText) view.findViewById(R.id.txt_add_folder);
        builder.setTitle(R.string.action_new_folder).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mListener.onFinishAddFolderDialog(mEditText.getText().toString());
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
}
