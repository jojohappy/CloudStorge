package com.ces.cloudstorge;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ces.cloudstorge.Dialog.AboutDialog;
import com.ces.cloudstorge.Dialog.AddNewFolderDialog;
import com.ces.cloudstorge.Dialog.DeleteFileDialog;
import com.ces.cloudstorge.Dialog.FolderListDialog;
import com.ces.cloudstorge.Dialog.RenameFileDialog;
import com.ces.cloudstorge.adapter.DrawerListAdapter;
import com.ces.cloudstorge.adapter.FileListAdapter;
import com.ces.cloudstorge.provider.CloudStorgeContract;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AddNewFolderDialog.AddNewFolderDialogListener, DeleteFileDialog.DeleteFileDialogListener,
        RenameFileDialog.RenameFileDialogListener, FolderListDialog.FolderListDialogListener {
    //private SimpleCursorAdapter mAdapter;
    // 文件夹、文件列表adapter
    private static FileListAdapter mAdapter;
    // 左侧菜单栏
    private DrawerLayout mDrawerLayout;
    // actionBar对象
    private ActionBarDrawerToggle mDrawerToggle;
    // 左侧菜单栏列表View
    private ListView mDrawList;
    // 排序方式
    private static final int SORT_NAME = 0;
    private static final int SORT_LAST_MODIFITY = 1;
    private int mSortType;

    // sql查询列(root)
    public static final String[] PROJECTION = new String[]{CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge._ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID
    };

    // sql查询条件(root)
    public static final String SELECTION_SPECIAL = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "= (select " + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID +
            " from " + CloudStorgeContract.CloudStorge.TABLE_NAME + " where " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "=%d and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='%s' limit 1)" +
            " and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s')";

    // sql查询条件(root)
    public static final String SELECTION_CHILD = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID + "=%d " +
            "and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s')";

    public static final String selection_folder_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    // 需要的字段
    public static final String[] fromColumns = {CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID
    };

    // view中的对象
    public static final int[] toViews = {R.id.fileImage,
            R.id.list_file_name,
            R.id.list_last_modified,
            R.id.list_parentFolderId,
            R.id.list_fileId,
            R.id.list_folderId
    };

    // 当前账户
    private static Account current_account;
    // ContentProvider回调响应对象
    private CloudStorgeObserver mCloudStorgeObserver;
    // 是否为根目录
    private static Boolean isRoot;
    // 是否为回收站
    private Boolean isTrash;
    // 是否是空文件夹
    private static Boolean isEmptyFolder;
    // 当前目录编号
    private static int currentFolderId;
    // 父目录编号
    private static int parentFolderId;
    // context
    private static Context mContext;

    private static FragmentManager fragmentManager;

    static LoaderManager.LoaderCallbacks<Cursor> callbackLoader;

    static LoaderManager loadm;

    static Intent mIntent;

    // activity onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 从上一个activity中获得当前账户对象
        current_account = (Account) getIntent().getExtras().get("current_account");
        // 从上一个activity中获得是否是当前目录
        isRoot = (Boolean) getIntent().getExtras().get("isRoot");
        mIntent = getIntent();
        isTrash = false;
        isEmptyFolder = false;
        if (isRoot) {
            currentFolderId = -1;
            parentFolderId = -1;
        }

        if (isTrash) {
            currentFolderId = -10;
            parentFolderId = -1;
        }
        LayoutInflater inflater = getLayoutInflater();
        mContext = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        callbackLoader = MainActivity.this;
        loadm = getSupportLoaderManager();
        mSortType = SORT_NAME;

        // 初始化ActionBar
        ActionBar actionBar = getActionBar();
        // 设置home图标点击
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        // 设置title
        actionBar.setTitle(R.string.app_activity_title);

        // 初始化左侧菜单栏列表
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawList = (ListView) findViewById(R.id.left_drawer);
        View drawerHeader = inflater.inflate(R.layout.navigate_drawer_list, mDrawList, false);
        ImageView drawerHeaderImage = (ImageView) drawerHeader.findViewById(R.id.drawer_icon);
        TextView drawerHeaderText = (TextView) drawerHeader.findViewById(R.id.drawer_title);
        drawerHeaderImage.setImageResource(R.drawable.ic_action_account);
        drawerHeaderText.setText(current_account.name);
        mDrawList.addHeaderView(drawerHeader, null, false);
        mDrawList.setAdapter(new DrawerListAdapter(this, getLayoutInflater()));
        mDrawList.setItemChecked(1, true);

        // 设置响应事件
        mDrawList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // 注册ContentProvider回调事件
        mCloudStorgeObserver = new CloudStorgeObserver(new Handler());
        getContentResolver().registerContentObserver(CloudStorgeContract.CloudStorge.CONTENT_URI, true, mCloudStorgeObserver);
        // 查看网络连接状态
        ConnectionChangeReceiver.isHasConnect = ConnectionChangeReceiver.check_networkStatus(this);
        // 初始化碎片布局
        initFragment(false);
    }

    // 创建选择菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mDrawerLayout.isDrawerOpen(mDrawList);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 创建数据Loader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int specialFolderId = -1;
        if (isTrash)
            specialFolderId = -10;
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        String order = "";
        if (mSortType == SORT_NAME)
            order = "folder_id desc,name asc";
        else if (mSortType == SORT_LAST_MODIFITY)
            order = "folder_id desc, last_modified asc";
        return new CursorLoader(this, CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, order);
    }

    // Load结束回调函数
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (isEmptyFolder) {
            isEmptyFolder = false;
            initFragment(false);
            return;
        }
        if (!isRoot) {
            String selection = String.format(selection_folder_format, parentFolderId, current_account.name);
            Cursor mCursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
            mCursor.moveToFirst();
            if (-1 == mCursor.getInt(6))
                parentFolderId = -1;
        }
        mAdapter.swapCursor(cursor);
        if (null == cursor || 0 == cursor.getCount()) {
            isEmptyFolder = true;
            initFragment(true);
        }
    }

    // Load重置函数
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    // 创建文件夹回调接口
    @Override
    public void onFinishAddFolderDialog(String inputText) {
        Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();
    }

    // 删除文件回调接口
    @Override
    public void onFinishDeleteFileDialog(String filelist, String folderlist) {
        String[] array_folder = folderlist.split(",");
        String[] array_file = filelist.split(",");

    }

    // 重命名文件回调接口
    @Override
    public void onFinishRename(int id, int type, String name) {

    }

    // 选择移动目标文件夹回调接口
    @Override
    public void onFinishSelectFolder(int folderId, String arraylist) {

    }

    // 左侧菜单栏点击响应
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position == Contract.DRAWER_LOGOUT) {
                mDrawerLayout.closeDrawer(mDrawList);
                if (isRoot)
                    mDrawList.setItemChecked(Contract.DRAWER_ROOT, true);
                if (isTrash)
                    mDrawList.setItemChecked(Contract.DRAWER_TRASH, true);
                if (!isRoot && !isTrash)
                    mDrawList.setItemChecked(Contract.DRAWER_ROOT, true);
                create_exitDialog();
            } else {
                TextView drawerText = (TextView) view.findViewById(R.id.drawer_title);
                mDrawList.setItemChecked(position, true);
                getActionBar().setTitle(drawerText.getText());
                mDrawerLayout.closeDrawer(mDrawList);

                switch (position) {
                    case Contract.DRAWER_ROOT:
                        isRoot = true;
                        isTrash = false;
                        break;
                    case Contract.DRAWER_SHARE:
                        isRoot = false;
                        isTrash = false;
                        break;
                    case Contract.DRAWER_TRASH:
                        isRoot = false;
                        isTrash = true;
                        break;
                }
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
            }
        }
    }

    // 左侧菜单栏设置
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // 选择菜单响应事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Bundle settingsBundle = new Bundle();
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_MANUAL, true);
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(current_account, Contract.CONTENT_AUTHORITY, settingsBundle);
                return true;
            case R.id.action_new:
                create_addDialog();
                return true;
            case R.id.action_sort:
                create_sortDialog();
                return true;
            case R.id.action_about:
                create_aboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // 回退键响应
    @Override
    public void onBackPressed() {
        if (isRoot || isTrash) {
            // Confirm exit
            create_exitDialog();
        } else {
            isRoot = parentFolderId == -1 ? true : false;
            currentFolderId = parentFolderId;
            // 查询上级目录的上级目录
            String selection = String.format(selection_folder_format, currentFolderId, current_account.name);
            Cursor mCursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
            mCursor.moveToFirst();
            parentFolderId = mCursor.getInt(6);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    // 创建关于对话框
    public void create_aboutDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AboutDialog aboutDialog = new AboutDialog();
        aboutDialog.show(fm, "aboutdialog");
    }

    // 创建新增对话框
    private void create_addDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_new);
        builder.setItems(R.array.array_action_add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position) {
                if (0 == position) {
                    create_newFolderDialog();
                } else {

                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 创建排序对话框
    private void create_sortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_sort).setSingleChoiceItems(R.array.sort_array, mSortType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position) {
                mSortType = position;
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 创建新建文件夹对话框
    private void create_newFolderDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AddNewFolderDialog addFolderNameDialog = new AddNewFolderDialog();
        addFolderNameDialog.show(fm, "add folder");
    }

    // 创建退出对话框
    private void create_exitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.exit_dialog_message).setTitle(R.string.exit_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 碎片布局初始化
    private void initFragment(boolean isEmpty) {
        Fragment fragment = new ContentFragment();
        Bundle args = new Bundle();
        args.putBoolean("isEmpty", isEmpty);
        fragment.setArguments(args);
        FragmentTransaction f = fragmentManager.beginTransaction();
        FragmentTransaction f1 = f.replace(R.id.content_frame, fragment);
        f1.commitAllowingStateLoss();

    }

    // ContentProvider通知
    public class CloudStorgeObserver extends ContentObserver {
        public CloudStorgeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    // 碎片布局类
    public static class ContentFragment extends Fragment {
        private ListView listView;
        private Map<Integer, Integer> mapSelected;

        public ContentFragment() {
            // Empty constructor required for fragment subclasses
        }

        // 创建view
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            boolean isEmpty = getArguments().getBoolean("isEmpty");
            // 当文件夹内容为空时，替换碎片空布局
            if (isEmpty) {
                mapSelected = new HashMap<Integer, Integer>();
                isEmptyFolder = true;
                View currentView = inflater.inflate(R.layout.list_empty, container, false);
                return currentView;
            } else {
                View currentView = inflater.inflate(R.layout.fragment_filelist, container, false);
                mapSelected = new HashMap<Integer, Integer>();
                // 文件夹、文件列表view
                listView = (ListView) currentView.findViewById(R.id.listView);
                // 设置为多选模式
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                // 多选模式响应
                listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                    private int count = 0;
                    // 列选择状态变更
                    @Override
                    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                        // Here you can do something when items are selected/de-selected,
                        Cursor cursor = mAdapter.getCursor();
                        cursor.moveToPosition(position);
                        int folderId = cursor.getInt(5);
                        if (checked) {
                            mapSelected.put(position, folderId);
                            count++;
                        } else {
                            mapSelected.remove(position);
                            count--;
                        }
                        if (count == 0) {
                            mode.finish();
                            return;
                        }
                        mode.setTitle(mContext.getString(R.string.cab_selectd, count));
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
                            if (1 == folderflag && count == 1) {
                                mode.getMenu().findItem(R.id.action_rename).setVisible(true);
                                mode.getMenu().findItem(R.id.action_download).setVisible(false);
                                return;
                            }
                            if (1 == fileflag && count == 1) {
                                mode.getMenu().findItem(R.id.action_rename).setVisible(true);
                                mode.getMenu().findItem(R.id.action_download).setVisible(true);
                                return;
                            }
                            if (((fileflag == 1 && folderflag == 1) ||
                                    (fileflag == 0 && folderflag == 1)) ||
                                    (fileflag == 1 && folderflag == 0)) {

                                mode.getMenu().findItem(R.id.action_rename).setVisible(false);
                                mode.getMenu().findItem(R.id.action_download).setVisible(false);
                                return;
                            }
                        }
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        FragmentManager fm = fragmentManager;
                        String filelist = "";
                        String folderlist = "";
                        Cursor cursor;
                        Iterator it = mapSelected.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry ma = (Map.Entry) it.next();
                            if(-1 == ma.getValue())
                            {
                                ma.getKey();
                                cursor = mAdapter.getCursor();
                                cursor.moveToPosition(Integer.parseInt(ma.getKey()+""));
                                filelist += cursor.getInt(4) + "" + ",";
                            }
                            else
                            {
                                folderlist += ma.getValue() + "" + ",";
                            }
                        }
                        switch (item.getItemId()) {
                            case R.id.action_rename:
                                RenameFileDialog renameFileDialog = new RenameFileDialog();
                                Bundle rf = new Bundle();
                                rf.putString("oldername", "dgsg");
                                rf.putInt("type", 1);
                                rf.putInt("id", 1);
                                renameFileDialog.setArguments(rf);
                                renameFileDialog.show(fm, "renamefile");
                                break;
                            case R.id.action_delete:
                                DeleteFileDialog deleteFileDialog = new DeleteFileDialog();
                                Bundle db = new Bundle();
                                db.putString("filelist", filelist);
                                db.putString("folderlist", folderlist);
                                deleteFileDialog.setArguments(db);
                                deleteFileDialog.show(fm, "deletefile");
                                break;
                            case R.id.action_move:
                                FolderListDialog folderListDialog = new FolderListDialog();
                                Bundle fld = new Bundle();
                                fld.putString("currentUser", current_account.name);
                                fld.putString("arraylist", filelist);
                                fld.putInt("currentFolderId", -1);
                                fld.putInt("parentFolderId", -1);
                                folderListDialog.setArguments(fld);
                                folderListDialog.show(fm, "movefile");
                                break;
                            case R.id.action_download:
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
                mAdapter = new FileListAdapter(mContext,
                        R.layout.list_item, null,
                        fromColumns, toViews, 0, fragmentManager);
                listView.setAdapter(mAdapter);
                // 初始化load数据
                loadm.initLoader(0, null, callbackLoader);
                // 文件夹、文件列表点击响应事件
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        TextView viewFileId = (TextView) view.findViewById(R.id.list_fileId);
                        if (viewFileId.getText().equals("-1")) {
                            //Toast.makeText(getBaseContext(), "teste", Toast.LENGTH_LONG).show();
                            TextView viewFolderId = (TextView) view.findViewById(R.id.list_folderId);
                            TextView viewParentFolderId = (TextView) view.findViewById(R.id.list_parentFolderId);
                            currentFolderId = Integer.parseInt(viewFolderId.getText().toString());
                            if (isRoot)
                                parentFolderId = -1;
                            else
                                parentFolderId = Integer.parseInt(viewParentFolderId.getText().toString());
                            isRoot = false;
                            loadm.restartLoader(0, null, callbackLoader);
                        } else {
                        }
                    }
                });
                return currentView;
            }
        }
    }
}