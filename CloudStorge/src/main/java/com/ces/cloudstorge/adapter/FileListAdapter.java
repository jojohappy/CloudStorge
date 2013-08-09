package com.ces.cloudstorge.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ces.cloudstorge.Contract;
import com.ces.cloudstorge.FileDetailActivity;
import com.ces.cloudstorge.MainActivity;
import com.ces.cloudstorge.R;
import com.ces.cloudstorge.util.CommonUtil;

/**
 * Created by MichaelDai on 13-7-25.
 */
public class FileListAdapter extends SimpleCursorAdapter {
    private ImageView fileImage;
    /*private static final int COLUMN_MIME_TYPE = 0;
    private static final int COLUMN_LAST_MODIFIED = 1;
    private static final int COLUMN_FILE_NAME = 3;
    private static final int COLUMN_FILE_ID = 4;
    private static final int COLUMN_FOLDER_ID = 5;
    private static final int COLUMN_PARENT_FOLDER_ID = 6;
    private static final int COLUMN_SHARE = 7;
    private static final int COLUMN_ORIGIN_FOLDER = 9;*/
    private LayoutInflater mInflater;
    private FragmentManager mFragmentManager;
    private Cursor mCursor;
    private Context mContext;

    public FileListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, FragmentManager fragmentManager) {
        super(context, layout, c, from, to, flags);
        mInflater = LayoutInflater.from(context);
        mFragmentManager = fragmentManager;
        mContext = context;
    }

    /*@Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        String mime_type = "";
        fileImage = (ImageView) view.findViewById(R.id.fileImage);
        if(cursor.getString(3).equals("BasicSyncAdapter.zip"))
        {

            view = mInflater.inflate(R.layout.list_item_header, null);
            TextView text = (TextView) view.findViewById(R.id.list_header_title);
            text.setText(cursor.getString(3));
            return;
        }
        if(null != (mime_type = cursor.getString(COLUMN_MIME_TYPE)))
        {
            if("".equals(mime_type))
            {
                fileImage.setImageResource(R.drawable.icon_folder);
            }
            else if("default".equals(mime_type))
            {
                fileImage.setImageResource(R.drawable.icon_default);
            }
            else
            {
                fileImage.setImageResource(R.drawable.icon_default);
            }
        }
        else
        {
            fileImage.setImageResource(R.drawable.icon_default);
        }
    }*/

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mCursor.moveToPosition(position);
        Integer mimeTypeIconResId;
        /*if(position == 1)
        {
            if(null == convertView)
            {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.list_item_header, parent, false);
                convertView.setTag(0);
            }
            TextView headerTextView = (TextView)convertView.findViewById(R.id.list_header_title);
            headerTextView.setText("folders");
            return convertView;
        }
        else*/
        {
            String mime_type = "";
            if (null == convertView) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.list_item, parent, false);
                convertView.setTag(1);
            }
            TextView fileName = (TextView) convertView.findViewById(R.id.list_file_name);
            TextView lastm = (TextView) convertView.findViewById(R.id.list_last_modified);
            TextView lastmtmp = (TextView) convertView.findViewById(R.id.list_lastmodified_tmp);
            TextView parentFolderId = (TextView) convertView.findViewById(R.id.list_parentFolderId);
            TextView folderId = (TextView) convertView.findViewById(R.id.list_folderId);
            TextView fileId = (TextView) convertView.findViewById(R.id.list_fileId);
            TextView imagefileId = (TextView) convertView.findViewById(R.id.list_image_fileId);
            fileImage = (ImageView) convertView.findViewById(R.id.fileImage);
            ImageView shareImage = (ImageView) convertView.findViewById(R.id.list_file_share);
            ImageView sharedImage = (ImageView) convertView.findViewById(R.id.list_shared);
            ImageView offlineImage = (ImageView) convertView.findViewById(R.id.list_offline);
            offlineImage.setVisibility(View.GONE);
            sharedImage.setVisibility(View.GONE);
            shareImage.setVisibility(View.VISIBLE);
            LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.list_share_line);
            if (mCursor.getInt(Contract.PROJECTION_ORIGIN_FOLDER) == -20 || MainActivity.isTrash) {
                linearLayout.setVisibility(View.GONE);
                shareImage.setVisibility(View.GONE);
                if (!MainActivity.isTrash) {
                    lastmtmp.setText(R.string.share_from);
                    lastm.setText(mCursor.getString(Contract.PROJECTION_SHARE).substring(9, mCursor.getString(Contract.PROJECTION_SHARE).length()));
                } else {
                    lastmtmp.setText(R.string.lastmodified);
                    lastm.setText(mCursor.getString(Contract.PROJECTION_LAST_MODIFIED));
                }
            } else {
                lastmtmp.setText(R.string.lastmodified);
                linearLayout.setVisibility(View.VISIBLE);
                linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView imagefileId = (TextView) view.findViewById(R.id.list_image_fileId);
                        Intent intent = new Intent();
                        intent.putExtra("file_id", imagefileId.getText());
                        intent.setClass(mContext, FileDetailActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                });
                lastm.setText(mCursor.getString(Contract.PROJECTION_LAST_MODIFIED));
            }
            if (null != (mime_type = mCursor.getString(Contract.PROJECTION_MIME_TYPE))) {
                if ("".equals(mime_type)) {
                    fileImage.setImageResource(R.drawable.icon_folder);
                    shareImage.setVisibility(View.GONE);
                    linearLayout.setVisibility(View.GONE);
                } else if ("default".equals(mime_type)) {
                    fileImage.setImageResource(R.drawable.icon_default);
                } else {
                    if (null == (mimeTypeIconResId = CommonUtil.mime_type_icon.get(mime_type)))
                        mimeTypeIconResId = R.drawable.icon_default;
                    fileImage.setImageResource(mimeTypeIconResId);
                }
            } else {
                fileImage.setImageResource(R.drawable.icon_default);
            }
            if(null != mCursor.getString(Contract.PROJECTION_SHARE) && !"".equals(mCursor.getString(Contract.PROJECTION_SHARE)) && !MainActivity.isTrash)
            {
                sharedImage.setVisibility(View.VISIBLE);
            }
            imagefileId.setText(mCursor.getInt(Contract.PROJECTION_FILE_ID) + "");
            fileName.setText(mCursor.getString(Contract.PROJECTION_NAME));
            parentFolderId.setText(mCursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID) + "");
            folderId.setText(mCursor.getInt(Contract.PROJECTION_FOLDER_ID) + "");
            fileId.setText(mCursor.getInt(Contract.PROJECTION_FILE_ID) + "");
            parentFolderId.setVisibility(View.GONE);
            folderId.setVisibility(View.GONE);
            fileId.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        super.swapCursor(c);
        mCursor = c;
        return mCursor;
    }

    public class ViewHolder {
        public ImageView imageView;
        public TextView fileName;
        public TextView lastModified;
    }
}
