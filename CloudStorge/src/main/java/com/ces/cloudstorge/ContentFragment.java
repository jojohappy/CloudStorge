package com.ces.cloudstorge;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ces.cloudstorge.Dialog.DeleteFileDialog;
import com.ces.cloudstorge.Dialog.FolderListDialog;
import com.ces.cloudstorge.Dialog.RenameFileDialog;
import com.ces.cloudstorge.adapter.FileListAdapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by MichaelDai on 13-8-5.
 */
public class ContentFragment extends Fragment {
    private ListView listView;
    private Map<Integer, Integer> mapSelected;
    private FrameLayout mProfileHeaderContainer;
    private View mProfileHeader;
    private TextView mProfileTitle;
    private TextView mCounterHeaderView;

    public ContentFragment() {
    }

    // 创建view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        boolean isEmpty = true;
        if (null != getArguments())
            isEmpty = getArguments().getBoolean("isEmpty");
        //if(null == getArguments().getBoolean("isEmpty"))

        // 当文件夹内容为空时，替换碎片空布局
        if (isEmpty) {
            mapSelected = new HashMap<Integer, Integer>();
            MainActivity.isEmptyFolder = true;
            View currentView = inflater.inflate(R.layout.list_empty, container, false);
            return currentView;
        } else {
            View currentView = inflater.inflate(R.layout.fragment_filelist, container, false);
            mapSelected = new HashMap<Integer, Integer>();

            // 文件夹、文件列表view
            listView = (ListView) currentView.findViewById(R.id.listView);
            mProfileHeaderContainer = new FrameLayout(inflater.getContext());
            mProfileHeader = inflater.inflate(R.layout.list_header, null, false);
            mProfileTitle = (TextView) mProfileHeader.findViewById(R.id.folder_trace);
            //mProfileTitle.setAllCaps(true);
            //changeHeader();
            mProfileHeaderContainer.addView(mProfileHeader);
            listView.addHeaderView(mProfileHeaderContainer, null, false);
            listView.setVerticalScrollBarEnabled(false);
            listView.setScrollbarFadingEnabled(true);
            // 设置为多选模式
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            // 多选模式响应
            listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                private int count = 0;

                // 列选择状态变更
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    // Here you can do something when items are selected/de-selected,
                    Cursor cursor = MainActivity.mAdapter.getCursor();
                    cursor.moveToPosition(position - 1);
                    int folderId = cursor.getInt(Contract.PROJECTION_FOLDER_ID);
                    if (checked) {
                        mapSelected.put(position - 1, folderId);
                        count++;
                    } else {
                        mapSelected.remove(position - 1);
                        count--;
                    }
                    if (count == 0) {
                        mode.finish();
                        return;
                    }
                    mode.setTitle(getString(R.string.cab_selectd, count));
                    {
                        int folderflag = 0;
                        int fileflag = 0;
                        Iterator it = mapSelected.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry selected = (Map.Entry) it.next();
                            if (-1 == selected.getValue())
                                fileflag = 1;
                            if (-1 != selected.getValue())
                                folderflag = 1;
                        }
                        mode.getMenu().findItem(R.id.action_copy).setVisible(false);
                        mode.getMenu().findItem(R.id.action_share).setVisible(false);
                        mode.getMenu().findItem(R.id.action_delete).setVisible(true);
                        if (1 == folderflag && count == 1) {
                            mode.getMenu().findItem(R.id.action_rename).setVisible(true);
                            mode.getMenu().findItem(R.id.action_download).setVisible(false);
                        } else if (1 == fileflag && count == 1) {
                            mode.getMenu().findItem(R.id.action_rename).setVisible(true);
                            mode.getMenu().findItem(R.id.action_download).setVisible(true);
                        } else if (((fileflag == 1 && folderflag == 1) ||
                                (fileflag == 0 && folderflag == 1)) ||
                                (fileflag == 1 && folderflag == 0)) {

                            mode.getMenu().findItem(R.id.action_rename).setVisible(false);
                            mode.getMenu().findItem(R.id.action_download).setVisible(false);
                        }
                    }
                    mode.getMenu().findItem(R.id.action_undo).setVisible(false);
                    if (MainActivity.isTrash) {
                        mode.getMenu().findItem(R.id.action_move).setVisible(false);
                        mode.getMenu().findItem(R.id.action_rename).setVisible(false);
                        mode.getMenu().findItem(R.id.action_download).setVisible(false);
                        mode.getMenu().findItem(R.id.action_delete).setVisible(true);
                        mode.getMenu().findItem(R.id.action_undo).setVisible(true);
                    }
                    if (MainActivity.isShare) {
                        mode.getMenu().findItem(R.id.action_move).setVisible(false);
                        mode.getMenu().findItem(R.id.action_rename).setVisible(false);
                        mode.getMenu().findItem(R.id.action_download).setVisible(true);
                        mode.getMenu().findItem(R.id.action_delete).setVisible(false);
                        //mode.getMenu().findItem(R.id.action_undo).setVisible(false);
                    }

                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    FragmentManager fm = MainActivity.fragmentManager;
                    //FragmentManager fm = getFragmentManager();
                    String filelist = "";
                    String folderlist = "";
                    String oldname = "";
                    Cursor cursor;
                    Iterator it = mapSelected.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry ma = (Map.Entry) it.next();
                        if (-1 == ma.getValue()) {
                            cursor = MainActivity.mAdapter.getCursor();
                            cursor.moveToPosition(Integer.parseInt(ma.getKey() + ""));
                            filelist += cursor.getInt(Contract.PROJECTION_FILE_ID) + "" + ",";
                        } else {
                            folderlist += ma.getValue() + "" + ",";
                        }
                        if (item.getItemId() == R.id.action_rename) {
                            Cursor temp = MainActivity.mAdapter.getCursor();
                            temp.moveToPosition(Integer.parseInt(ma.getKey() + ""));
                            oldname = temp.getString(Contract.PROJECTION_NAME);
                        }
                    }
                    switch (item.getItemId()) {
                        case R.id.action_rename:
                            int type;
                            int id;
                            if ("".equals(filelist)) {
                                type = Contract.TYPE_FOLDER;
                                id = Integer.parseInt(folderlist.substring(0, folderlist.indexOf(",")));
                            } else {
                                type = Contract.TYPE_FILE;
                                id = Integer.parseInt(filelist.substring(0, filelist.indexOf(",")));
                            }
                            RenameFileDialog renameFileDialog = new RenameFileDialog();
                            Bundle rf = new Bundle();
                            rf.putString("oldername", oldname);
                            rf.putInt("type", type);
                            rf.putInt("id", id);
                            renameFileDialog.setArguments(rf);
                            renameFileDialog.show(fm, "renamefile");
                            break;
                        case R.id.action_delete:
                            DeleteFileDialog deleteFileDialog = new DeleteFileDialog();
                            Bundle db = new Bundle();
                            db.putString("filelist", filelist);
                            db.putString("folderlist", folderlist);
                            if(MainActivity.isTrash)
                                db.putBoolean("isForever", true);
                            else
                                db.putBoolean("isForever", false);
                            deleteFileDialog.setArguments(db);
                            deleteFileDialog.show(fm, "deletefile");
                            break;
                        case R.id.action_move:
                        case R.id.action_undo:
                            FolderListDialog folderListDialog = new FolderListDialog();
                            Bundle fld = new Bundle();
                            fld.putString("currentUser", MainActivity.current_account.name);
                            fld.putString("filelist", filelist);
                            fld.putString("folderlist", folderlist);
                            fld.putInt("currentFolderId", Contract.FOLDER_ROOT);
                            fld.putInt("parentFolderId", Contract.FOLDER_ROOT);
                            folderListDialog.setArguments(fld);
                            folderListDialog.show(fm, "movefile");
                            break;
                        case R.id.action_download:
                            Intent intent = new Intent();
                            intent.putExtra("fileId", Integer.parseInt(filelist.substring(0, filelist.indexOf(","))));
                            intent.setClass(MainActivity.mContext, DownloadActivity.class);
                            startActivity(intent);
                            break;
                        case R.id.action_share:
                            break;
                        default:
                            mode.finish();
                            return false;
                    }
                    mode.finish();
                    return true;
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.context_file_menu, menu);
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // Here you can make any necessary updates to the activity when
                    // the CAB is removed. By default, selected items are deselected/unchecked.
                    count = 0;
                    mapSelected.clear();
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // Here you can perform updates to the CAB due to
                    // an invalidate() request
                    return false;
                }
            });

            // 初始化文件夹、文件列表adapter
            MainActivity.mAdapter = new FileListAdapter(MainActivity.mContext,
                    R.layout.list_item, null,
                    //MainActivity.fromColumns, MainActivity.toViews, 0, getFragmentManager());
                    MainActivity.fromColumns, MainActivity.toViews, 0, MainActivity.fragmentManager);
            listView.setAdapter(MainActivity.mAdapter);
            // 初始化load数据
            MainActivity.loadmanager.initLoader(0, null, MainActivity.callbackLoader);
            // 文件夹、文件列表点击响应事件
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    TextView viewFileId = (TextView) view.findViewById(R.id.list_fileId);
                    if (viewFileId.getText().equals("-1")) {
                        TextView viewFolderId = (TextView) view.findViewById(R.id.list_folderId);
                        TextView viewParentFolderId = (TextView) view.findViewById(R.id.list_parentFolderId);
                        TextView viewFileName = (TextView) view.findViewById(R.id.list_file_name);
                        MainActivity.currentFolderId = Integer.parseInt(viewFolderId.getText().toString());
                        if (MainActivity.isRoot)
                            MainActivity.parentFolderId = Contract.FOLDER_ROOT;
                        else
                            MainActivity.parentFolderId = Integer.parseInt(viewParentFolderId.getText().toString());
                        MainActivity.isRoot = false;
                        MainActivity.listFolder.add(viewFileName.getText().toString());
                        //changeHeader();
                        MainActivity.changeListHeader();
                        MainActivity.loadmanager.restartLoader(0, null, MainActivity.callbackLoader);
                    } else {
                        // 下载文件
                        Intent intent = new Intent();
                        intent.putExtra("fileId", Integer.parseInt(viewFileId.getText().toString()));
                        intent.setClass(MainActivity.mContext, DownloadActivity.class);
                        startActivity(intent);
                    }
                }
            });
            return currentView;
        }
    }
}
