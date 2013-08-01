package com.ces.cloudstorge;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AddNewFolderDialog.AddNewFolderDialogListener, DeleteFileDialog.DeleteFileDialogListener,
        RenameFileDialog.RenameFileDialogListener, FolderListDialog.FolderListDialogListener {
    //private SimpleCursorAdapter mAdapter;
    // �ļ��С��ļ��б�adapter
    private static FileListAdapter mAdapter;
    // ���˵���
    private DrawerLayout mDrawerLayout;
    // actionBar����
    private ActionBarDrawerToggle mDrawerToggle;
    // ���˵����б�View
    private ListView mDrawList;
    // ����ʽ
    private static final int SORT_NAME = 0;
    private static final int SORT_LAST_MODIFITY = 1;
    private int mSortType;

    // sql��ѯ��(root)
    public static final String[] PROJECTION = new String[]{CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge._ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SIZE
    };

    // sql��ѯ����(root)
    public static final String SELECTION_SPECIAL = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "= (select " + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID +
            " from " + CloudStorgeContract.CloudStorge.TABLE_NAME + " where " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "=%d and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='%s' limit 1)" +
            " and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s')";

    // sql��ѯ����(root)
    public static final String SELECTION_CHILD = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID + "=%d " +
            "and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s')";

    public static final String selection_folder_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    public static final String selection_file_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    // ��Ҫ���ֶ�
    public static final String[] fromColumns = {CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID
    };

    // view�еĶ���
    public static final int[] toViews = {R.id.fileImage,
            R.id.list_file_name,
            R.id.list_last_modified,
            R.id.list_parentFolderId,
            R.id.list_fileId,
            R.id.list_folderId
    };

    // ��ǰ�˻�
    public static Account current_account;
    // ContentProvider�ص���Ӧ����
    private CloudStorgeObserver mCloudStorgeObserver;
    // �Ƿ�Ϊ��Ŀ¼
    private static Boolean isRoot;
    // �Ƿ�Ϊ����վ
    private static Boolean isTrash;
    // �Ƿ��ǿ��ļ���
    private static Boolean isEmptyFolder;
    // ��ǰĿ¼���
    private static int currentFolderId;
    // ��Ŀ¼���
    private static int parentFolderId;
    // context
    private static Context mContext;
    // ��Ƭ����
    private static FragmentManager fragmentManager;
    // loader�ص��ӿ�
    private static LoaderManager.LoaderCallbacks<Cursor> callbackLoader;
    // loader����
    private static LoaderManager loadmanager;
    // ProgressDialog����
    private ProgressDialog progressDialog;

    // activity onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ����һ��activity�л�õ�ǰ�˻�����
        current_account = (Account) getIntent().getExtras().get("current_account");
        // ����һ��activity�л���Ƿ��ǵ�ǰĿ¼
        isRoot = (Boolean) getIntent().getExtras().get("isRoot");
        isTrash = false;
        isEmptyFolder = false;
        if (isRoot) {
            currentFolderId = Contract.FOLDER_ROOT;
            parentFolderId = Contract.FOLDER_ROOT;
        }

        if (isTrash) {
            currentFolderId = Contract.FOLDER_TRASH;
            parentFolderId = Contract.FOLDER_ROOT;
        }
        LayoutInflater inflater = getLayoutInflater();
        mContext = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        callbackLoader = MainActivity.this;
        loadmanager = getSupportLoaderManager();
        mSortType = SORT_NAME;

        // ��ʼ��ActionBar
        ActionBar actionBar = getActionBar();
        // ����homeͼ����
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        // ����title
        actionBar.setTitle(R.string.app_activity_title);

        // ��ʼ�����˵����б�
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

        // ������Ӧ�¼�
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

        // ע��ContentProvider�ص��¼�
        mCloudStorgeObserver = new CloudStorgeObserver(new Handler());
        getContentResolver().registerContentObserver(CloudStorgeContract.CloudStorge.CONTENT_URI, true, mCloudStorgeObserver);
        // �鿴��������״̬
        ConnectionChangeReceiver.isHasConnect = ConnectionChangeReceiver.check_networkStatus(this);
        // ��ʼ����Ƭ����
        initFragment(false);
    }

    // ����ѡ��˵�
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mDrawerLayout.isDrawerOpen(mDrawList);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // ��������Loader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        String order = "";
        if (mSortType == SORT_NAME)
            order = "folder_id desc,name asc";
        else if (mSortType == SORT_LAST_MODIFITY)
            order = "folder_id desc, last_modified asc";
        return new CursorLoader(this, CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, order);
    }

    // Load�����ص�����
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

    // Load���ú���
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    // ��ȡ��Ŀ¼�ļ��б��
    public int get_folderRoot() {
        return get_specialFolder(Contract.FOLDER_ROOT);
    }

    // ��ȡ����վ�ļ��б��
    public int get_folderTrash() {
        return get_specialFolder(Contract.FOLDER_TRASH);
    }

    // ��ù����ļ��б��
    public int get_folderShare() {
        return get_specialFolder(Contract.FOLDER_SHARE);
    }

    // �����ļ��б�Ż�ȡ
    public int get_specialFolder(int folderId) {
        String selection = String.format(SELECTION_CHILD, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getInt(Contract.PROJECTION_FOLDER_ID);
    }

    // ָ���ļ��л�ȡ
    public Cursor get_assignFolder(int folderId) {
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // ָ���ļ���ȡ
    public Cursor get_assignFile(int fileId) {
        String selection = String.format(selection_file_format, fileId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // ָ���ļ��и��ļ��л�ȡ
    public int get_assignParentFolder(int folderId) {
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
    }

    // ��ȡ��ǰʱ���ַ���(yyyy-MM-dd HH:mm:ss)
    public String get_currentDateString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }

    // ������ͨ��ʾ�Ի���
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

    // ������ͨProgressDialog
    public void create_progressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    // �����ļ��лص��ӿ�
    @Override
    public void onFinishAddFolderDialog(String inputText) {
        if (null == inputText || "".equals(inputText)) {
            create_tipDialog(R.string.tip, R.string.null_new_folder_name_tip);
            return;
        }
        // ��ѯ�Ƿ��������ļ���
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

    // ɾ���ļ��ص��ӿ�
    @Override
    public void onFinishDeleteFileDialog(String filelist, String folderlist) {
        // ���ɾ���ļ��б��
        String[] array_folder = folderlist.split(",");
        // ���ɾ���ļ����
        String[] array_file = filelist.split(",");
        int trashId = get_folderTrash();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        String current_date = get_currentDateString();
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

    // �������ļ��ص��ӿ�
    @Override
    public void onFinishRename(int id, int type, String name) {
        // �ж��Ƿ��������ļ������򱨴�
        boolean flag = true;
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        String current_date = get_currentDateString();
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
            //û�������ļ���������ݿ����
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

    // ѡ���ƶ�Ŀ���ļ��лص��ӿ�
    @Override
    public void onFinishSelectFolder(int destFolderId, String fileList, String folderList) {
        // ���ɾ���ļ��б��
        String[] array_folder = folderList.split(",");
        // ���ɾ���ļ����
        String[] array_file = fileList.split(",");
        String current_date = get_currentDateString();
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

    // �ж��ƶ��ļ�ʱ�Ƿ����������������ļ������-copy
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

    // ���˵��������Ӧ
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

    // ���˵�������
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

    // ѡ��˵���Ӧ�¼�
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

    // ���˼���Ӧ
    @Override
    public void onBackPressed() {
        if (isRoot || isTrash) {
            create_exitDialog();
        } else {
            isRoot = parentFolderId == Contract.FOLDER_ROOT ? true : false;
            currentFolderId = parentFolderId;
            // ��ѯ�ϼ�Ŀ¼���ϼ�Ŀ¼
            parentFolderId = get_assignParentFolder(currentFolderId);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    // �������ڶԻ���
    public void create_aboutDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AboutDialog aboutDialog = new AboutDialog();
        aboutDialog.show(fm, "aboutdialog");
    }

    // ���������Ի���
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

    // ��������Ի���
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

    // �����½��ļ��жԻ���
    private void create_newFolderDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AddNewFolderDialog addFolderNameDialog = new AddNewFolderDialog();
        addFolderNameDialog.show(fm, "add folder");
    }

    // �����˳��Ի���
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

    // ��Ƭ���ֳ�ʼ��
    private void initFragment(boolean isEmpty) {
        Fragment fragment = new ContentFragment();
        Bundle args = new Bundle();
        args.putBoolean("isEmpty", isEmpty);
        fragment.setArguments(args);
        FragmentTransaction f = fragmentManager.beginTransaction();
        FragmentTransaction f1 = f.replace(R.id.content_frame, fragment);
        f1.commitAllowingStateLoss();

    }

    // ContentProvider֪ͨ
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

    // ��Ƭ������
    public static class ContentFragment extends Fragment {
        private ListView listView;
        private Map<Integer, Integer> mapSelected;

        public ContentFragment() {
        }

        // ����view
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            boolean isEmpty = getArguments().getBoolean("isEmpty");
            // ���ļ�������Ϊ��ʱ���滻��Ƭ�ղ���
            if (isEmpty) {
                mapSelected = new HashMap<Integer, Integer>();
                isEmptyFolder = true;
                View currentView = inflater.inflate(R.layout.list_empty, container, false);
                return currentView;
            } else {
                View currentView = inflater.inflate(R.layout.fragment_filelist, container, false);
                mapSelected = new HashMap<Integer, Integer>();
                // �ļ��С��ļ��б�view
                listView = (ListView) currentView.findViewById(R.id.listView);
                // ����Ϊ��ѡģʽ
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                // ��ѡģʽ��Ӧ
                listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                    private int count = 0;

                    // ��ѡ��״̬���
                    @Override
                    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                        // Here you can do something when items are selected/de-selected,
                        Cursor cursor = mAdapter.getCursor();
                        cursor.moveToPosition(position);
                        int folderId = cursor.getInt(Contract.PROJECTION_FOLDER_ID);
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
                        String oldname = "";
                        Cursor cursor;
                        Iterator it = mapSelected.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry ma = (Map.Entry) it.next();
                            if (-1 == ma.getValue()) {
                                cursor = mAdapter.getCursor();
                                cursor.moveToPosition(Integer.parseInt(ma.getKey() + ""));
                                filelist += cursor.getInt(Contract.PROJECTION_FILE_ID) + "" + ",";
                            } else {
                                folderlist += ma.getValue() + "" + ",";
                            }
                            if (item.getItemId() == R.id.action_rename) {
                                Cursor temp = mAdapter.getCursor();
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
                                deleteFileDialog.setArguments(db);
                                deleteFileDialog.show(fm, "deletefile");
                                break;
                            case R.id.action_move:
                                FolderListDialog folderListDialog = new FolderListDialog();
                                Bundle fld = new Bundle();
                                fld.putString("currentUser", current_account.name);
                                fld.putString("filelist", filelist);
                                fld.putString("folderlist", folderlist);
                                fld.putInt("currentFolderId", Contract.FOLDER_ROOT);
                                fld.putInt("parentFolderId", Contract.FOLDER_ROOT);
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

                // ��ʼ���ļ��С��ļ��б�adapter
                mAdapter = new FileListAdapter(mContext,
                        R.layout.list_item, null,
                        fromColumns, toViews, 0, fragmentManager);
                listView.setAdapter(mAdapter);
                // ��ʼ��load����
                loadmanager.initLoader(0, null, callbackLoader);
                // �ļ��С��ļ��б�����Ӧ�¼�
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        TextView viewFileId = (TextView) view.findViewById(R.id.list_fileId);
                        if (viewFileId.getText().equals("-1")) {
                            TextView viewFolderId = (TextView) view.findViewById(R.id.list_folderId);
                            TextView viewParentFolderId = (TextView) view.findViewById(R.id.list_parentFolderId);
                            currentFolderId = Integer.parseInt(viewFolderId.getText().toString());
                            if (isRoot)
                                parentFolderId = Contract.FOLDER_ROOT;
                            else
                                parentFolderId = Integer.parseInt(viewParentFolderId.getText().toString());
                            isRoot = false;
                            loadmanager.restartLoader(0, null, callbackLoader);
                        } else {
                        }
                    }
                });
                return currentView;
            }
        }
    }

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
                String currentDate = get_currentDateString();
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