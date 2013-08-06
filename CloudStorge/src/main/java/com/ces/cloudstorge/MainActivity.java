package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AddNewFolderDialog.AddNewFolderDialogListener, DeleteFileDialog.DeleteFileDialogListener,
        RenameFileDialog.RenameFileDialogListener, FolderListDialog.FolderListDialogListener {
    //private SimpleCursorAdapter mAdapter;
    // 文件夹、文件列表adapter
    public static FileListAdapter mAdapter;
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
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER
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

    public static final String selection_file_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    // 需要的字段
    public static final String[] fromColumns = {CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER
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
    public static Account current_account;
    // ContentProvider回调响应对象
    private CloudStorgeObserver mCloudStorgeObserver;
    // 是否为根目录
    public static Boolean isRoot;
    // 是否为回收站
    public static Boolean isTrash;
    // 是否为共享文件夹
    public static Boolean isShare;
    // 是否是空文件夹
    public static Boolean isEmptyFolder;
    // 当前目录编号
    public static int currentFolderId;
    // 父目录编号
    public static int parentFolderId;
    // context
    public static Context mContext;
    // 碎片管理
    public static FragmentManager fragmentManager;
    // loader回调接口
    public static LoaderManager.LoaderCallbacks<Cursor> callbackLoader;
    // loader管理
    public static LoaderManager loadmanager;
    // ProgressDialog对象
    private ProgressDialog progressDialog;

    // 目录等级列表
    public static List<String> listFolder;

    public static AccountManager am;

    // activity onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 从上一个activity中获得当前账户对象
        current_account = (Account) getIntent().getExtras().get("current_account");
        am = AccountManager.get(this);
        // 从上一个activity中获得是否是当前目录
        isRoot = (Boolean) getIntent().getExtras().get("isRoot");
        isTrash = false;
        isShare = false;
        isEmptyFolder = false;
        if (isRoot) {
            currentFolderId = Contract.FOLDER_ROOT;
            parentFolderId = Contract.FOLDER_ROOT;
        }

        if (isTrash) {
            currentFolderId = Contract.FOLDER_TRASH;
            parentFolderId = Contract.FOLDER_ROOT;
        }

        if (isShare) {
            currentFolderId = Contract.FOLDER_SHARE;
            parentFolderId = Contract.FOLDER_ROOT;
        }
        listFolder = new ArrayList<String>();
        LayoutInflater inflater = getLayoutInflater();
        mContext = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        callbackLoader = MainActivity.this;
        loadmanager = getSupportLoaderManager();
        mSortType = SORT_NAME;

        // 查询目录列表
        listFolder.add(getString(R.string.app_activity_title));
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

        // 初始化碎片布局
        initFragment(false);
        if (!ConnectionChangeReceiver.isHasConnect) {
            create_tipDialog(R.string.terrible, R.string.network_error);
        } else {
            call_syncAdapter();
        }
    }

    // 创建选择菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mDrawerLayout.isDrawerOpen(mDrawList);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem actionNew = menu.findItem(R.id.action_new);
        if (isRoot)
            actionNew.setVisible(true);
        if (isTrash || isShare)
            actionNew.setVisible(false);
        return true;
    }

    // 创建数据Loader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        if (isShare)
            specialFolderId = Contract.FOLDER_SHARE;
        String selection = isRoot || isTrash || isShare ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
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
            if (Contract.FOLDER_ROOT == get_assignParentFolder(parentFolderId))
                parentFolderId = Contract.FOLDER_ROOT;
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

    // 获取根目录文件夹编号
    public int get_folderRoot() {
        return get_specialFolder(Contract.FOLDER_ROOT);
    }

    // 获取回收站文件夹编号
    public int get_folderTrash() {
        return get_specialFolder(Contract.FOLDER_TRASH);
    }

    // 获得共享文件夹编号
    public int get_folderShare() {
        return get_specialFolder(Contract.FOLDER_SHARE);
    }

    // 特殊文件夹编号获取
    public int get_specialFolder(int folderId) {
        String selection = String.format(SELECTION_CHILD, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getInt(Contract.PROJECTION_FOLDER_ID);
    }

    // 指定文件夹获取
    public Cursor get_assignFolder(int folderId) {
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // 指定文件获取
    public Cursor get_assignFile(int fileId) {
        String selection = String.format(selection_file_format, fileId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // 指定文件夹父文件夹获取
    public int get_assignParentFolder(int folderId) {
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
    }

    // 创建普通提示对话框
    public void create_tipDialog(int title, int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    // 创建普通ProgressDialog
    public void create_progressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    // 创建文件夹回调接口
    @Override
    public void onFinishAddFolderDialog(String inputText) {
        if (null == inputText || "".equals(inputText)) {
            create_tipDialog(R.string.tip, R.string.null_new_folder_name_tip);
            return;
        }
        // 查询是否有重名文件夹
        boolean flag = true;
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                PROJECTION, selection, null, null);
        while (cursor.moveToNext()) {
            String oldname = cursor.getString(Contract.PROJECTION_NAME);
            if (oldname.equals(inputText)) {
                flag = false;
                break;
            }
        }
        if (!flag) {
            create_tipDialog(R.string.tip, R.string.error_add_folder_exists_tip);
            return;
        }
        if (!ConnectionChangeReceiver.isHasConnect) {
            create_tipDialog(R.string.tip, R.string.error_add_folder_no_network_tip);
            return;
        }
        new AddFolderAsyncTask().execute(inputText);
    }

    // 删除文件回调接口
    @Override
    public void onFinishDeleteFileDialog(String filelist, String folderlist) {
        // 获得删除文件夹编号
        String[] array_folder = folderlist.split(",");
        // 获得删除文件编号
        String[] array_file = filelist.split(",");
        int trashId = get_folderTrash();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        String current_date = CommonUtil.get_currentDateString();
        for (int i = 0; i < array_folder.length; i++) {
            if (null == array_folder[i] || "".equals(array_folder[i]))
                continue;
            int folderId = Integer.parseInt(array_folder[i]);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + folderId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, trashId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .build());
        }
        for (int i = 0; i < array_file.length; i++) {
            if (null == array_file[i] || "".equals(array_file[i]))
                continue;
            int fileId = Integer.parseInt(array_file[i]);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, trashId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .build());
        }
        try {
            getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
            //getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
            Toast.makeText(this, R.string.delete_tip, Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    // 重命名文件回调接口
    @Override
    public void onFinishRename(int id, int type, String name) {
        // 判断是否有重名文件，有则报错
        boolean flag = true;
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        String current_date = CommonUtil.get_currentDateString();
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                PROJECTION, selection, null, null);
        while (cursor.moveToNext()) {
            int folderId = cursor.getInt(Contract.PROJECTION_FOLDER_ID);
            String oldname = cursor.getString(Contract.PROJECTION_NAME);
            if (type == Contract.TYPE_FILE && folderId == -1) {
                int fileId = cursor.getInt(Contract.PROJECTION_FILE_ID);
                if (fileId == id)
                    continue;
                if (oldname.equals(name)) {
                    flag = false;
                    break;
                }
            } else if (type == Contract.TYPE_FOLDER && folderId != -1) {
                if (folderId == id)
                    continue;
                if (oldname.equals(name)) {
                    flag = false;
                    break;
                }
            }
        }
        if (!flag) {
            create_tipDialog(R.string.tip, R.string.rename_error);
        } else {
            //没有重名文件则进行数据库操作
            String selectionUpdate = "";
            if (type == Contract.TYPE_FOLDER)
                selectionUpdate = CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + id;
            if (type == Contract.TYPE_FILE)
                selectionUpdate = CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + id;
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            batch.add(ContentProviderOperation
                    .newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withSelection(selectionUpdate, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, name)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .build()
            );
            try {
                getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
                //getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                Toast.makeText(this, R.string.rename_tip, Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }

    }

    // 选择移动目标文件夹回调接口
    @Override
    public void onFinishSelectFolder(int destFolderId, String fileList, String folderList) {
        // 获得删除文件夹编号
        String[] array_folder = folderList.split(",");
        // 获得删除文件编号
        String[] array_file = fileList.split(",");
        String current_date = CommonUtil.get_currentDateString();
        if (destFolderId == Contract.FOLDER_ROOT)
            destFolderId = get_folderRoot();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < array_folder.length; i++) {
            if (null == array_folder[i] || "".equals(array_folder[i]))
                continue;
            int folderId = Integer.parseInt(array_folder[i]);
            Cursor cursorFolder = get_assignFolder(folderId);
            String newName = get_newFileName(cursorFolder.getString(Contract.PROJECTION_NAME), destFolderId, Contract.TYPE_FOLDER, folderId);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + folderId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, destFolderId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, newName)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .build());
        }
        for (int i = 0; i < array_file.length; i++) {
            if (null == array_file[i] || "".equals(array_file[i]))
                continue;
            int fileId = Integer.parseInt(array_file[i]);
            Cursor cursorFile = get_assignFile(fileId);
            String newName = get_newFileName(cursorFile.getString(Contract.PROJECTION_NAME), destFolderId, Contract.TYPE_FILE, fileId);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, destFolderId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, newName)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .build());
        }
        try {
            getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
            //getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
            Toast.makeText(this, R.string.move_tip, Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    // 判断移动文件时是否有重名，有则在文件后添加-copy
    private String get_newFileName(String oldname, int destFolderId, int type, int id) {
        boolean runflag = true;
        String newName = oldname;
        String selection = destFolderId == Contract.FOLDER_ROOT ? String.format(SELECTION_SPECIAL, destFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, destFolderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                PROJECTION, selection, null, null);
        do {
            runflag = false;
            if (cursor.isFirst())
                cursor.moveToPrevious();
            while (cursor.moveToNext()) {
                int folderId = cursor.getInt(Contract.PROJECTION_FOLDER_ID);
                String nameTmp = cursor.getString(Contract.PROJECTION_NAME);
                if (type == Contract.TYPE_FILE && folderId == -1) {
                    int fileId = cursor.getInt(Contract.PROJECTION_FILE_ID);
                    if (fileId == id)
                        continue;
                    if (nameTmp.equals(newName)) {
                        if (newName.indexOf(".") <= 0)
                            newName = newName + "-Copy";
                        if (newName.indexOf(".") > 0)
                            newName = newName.substring(0, newName.indexOf(".")) +
                                    "-Copy" + newName.substring(newName.indexOf("."), newName.length());
                        runflag = true;
                        break;
                    }
                } else if (type == Contract.TYPE_FOLDER && folderId != -1) {
                    if (folderId == id)
                        continue;
                    if (nameTmp.equals(newName)) {
                        runflag = true;
                        newName = newName + "-Copy";
                        break;
                    }
                }
            }
            cursor.moveToFirst();
        } while (runflag);
        return newName;
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
                    mDrawList.setItemChecked(Contract.DRAWER_SHARE, true);
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
                        isShare = false;
                        listFolder.clear();
                        listFolder.add(getString(R.string.app_activity_title));
                        break;
                    case Contract.DRAWER_SHARE:
                        isRoot = false;
                        isTrash = false;
                        isShare = true;
                        listFolder.clear();
                        listFolder.add(getString(R.string.share_folder));
                        break;
                    case Contract.DRAWER_TRASH:
                        isRoot = false;
                        isTrash = true;
                        isShare = false;
                        listFolder.clear();
                        listFolder.add(getString(R.string.trash_folder));
                        break;
                }
                changeListHeader();
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

    private void call_syncAdapter() {
        if(!ConnectionChangeReceiver.isHasConnect)
        {
            create_tipDialog(R.string.terrible, R.string.network_error);
            return;
        }
        create_progressDialog(getString(R.string.sync_message));
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(current_account, Contract.CONTENT_AUTHORITY, settingsBundle);
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
                call_syncAdapter();
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
            create_exitDialog();
        } else {
            listFolder.remove(listFolder.size() - 1);
            isRoot = parentFolderId == Contract.FOLDER_ROOT ? true : false;
            currentFolderId = parentFolderId;
            changeListHeader();
            // 查询上级目录的上级目录
            parentFolderId = get_assignParentFolder(currentFolderId);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    public static void changeListHeader()
    {
        ListHeaderFragment listFragment = (ListHeaderFragment) fragmentManager.findFragmentByTag("listHeader");
        String folder_trace = "";
        for (int i = 0; i < listFolder.size(); i++) {
            folder_trace += "/" + listFolder.get(i);
        }
        listFragment.set_folderTrace(folder_trace);
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
                    Intent intent = new Intent();
                    intent.setAction(Contract.UPLOAD_ACTION);
                    if (currentFolderId == Contract.FOLDER_ROOT)
                        currentFolderId = get_folderRoot();
                    intent.putExtra("destFolderId", currentFolderId);
                    intent.putExtra("currentAccount", current_account);
                    intent.setClass(MainActivity.this, UploadActivity.class);
                    startActivity(intent);
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
        Fragment fragmentTmp;
        Fragment fragmentTmp1;
        Fragment fragment = new ContentFragment();
        Fragment fragmentHeader = new ListHeaderFragment();
        Bundle args = new Bundle();
        args.putBoolean("isEmpty", isEmpty);
        fragment.setArguments(args);
        FragmentTransaction f = fragmentManager.beginTransaction();
        if (null != (fragmentTmp = fragmentManager.findFragmentByTag("listTag")))
            f.remove(fragmentTmp);
        if (null != (fragmentTmp1 = fragmentManager.findFragmentByTag("listHeader")))
            f.remove(fragmentTmp1);
        FragmentTransaction f1 = f.add(R.id.content_frame, fragment, "listTag").add(R.id.content_frame, fragmentHeader, "listHeader");
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
            if (null != progressDialog && progressDialog.isShowing())
                progressDialog.dismiss();
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    // 添加文件夹异步接口
    private class AddFolderAsyncTask extends AsyncTask<String, Void, Integer> {
        private String folderName;
        private int parentFolderId;

        @Override
        protected void onPreExecute() {
            create_progressDialog(getString(R.string.progress_add_folder));
        }

        @Override
        protected Integer doInBackground(String... folder_name) {
            try {
                folderName = folder_name[0];
                if (currentFolderId == Contract.FOLDER_ROOT)
                    parentFolderId = get_folderRoot();
                else
                    parentFolderId = currentFolderId;
                return CloudStorgeRestUtilities.commitAddFolder(parentFolderId, folder_name[0]);
            } catch (Exception ex) {
                return -1;
            }
        }

        @Override
        protected void onPostExecute(final Integer folderId) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (-1 == folderId)
                Toast.makeText(mContext, R.string.error_add_folder_rest_tip, Toast.LENGTH_SHORT).show();
            else {
                ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
                String currentDate = CommonUtil.get_currentDateString();
                batch.add(ContentProviderOperation.newInsert(CloudStorgeContract.CloudStorge.CONTENT_URI)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID, -1)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID, folderId)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, folderName)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_CREATE_TIME, currentDate)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, currentDate)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME, current_account.name)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_REVISION_INFO, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_DESCRIPTION, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER, -9999)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, parentFolderId)
                        .build());
                try {
                    getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
                    loadmanager.restartLoader(0, null, callbackLoader);
                    Toast.makeText(mContext, R.string.add_folder_tip, Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    Toast.makeText(mContext, R.string.error_add_folder_rest_tip, Toast.LENGTH_SHORT).show();
                } catch (OperationApplicationException e) {
                    Toast.makeText(mContext, R.string.error_add_folder_rest_tip, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}