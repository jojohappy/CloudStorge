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
import android.widget.TextView;

import com.ces.cloudstorge.R;

/**
 * Created by MichaelDai on 13-7-30.
 */
public class RenameFileDialog extends DialogFragment {
    public RenameFileDialog() {
    }

    public interface RenameFileDialogListener {
        void onFinishRename(int id, int type, String name);
    }

    RenameFileDialogListener mListener;
    private EditText mEditText;
    private int id;
    private int type;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (RenameFileDialogListener) activity;
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
        TextView meeage = (TextView) view.findViewById(R.id.lbl_folder_name);
        meeage.setText(R.string.input_new_name);
        String oldername = getArguments().getString("oldername");
        id = getArguments().getInt("id");
        type = getArguments().getInt("type");
        mEditText = (EditText) view.findViewById(R.id.txt_add_folder);
        mEditText.setText(oldername);
        builder.setTitle(R.string.menu_action_rename).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        mListener.onFinishRename(id, type, mEditText.getText().toString());
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
}
