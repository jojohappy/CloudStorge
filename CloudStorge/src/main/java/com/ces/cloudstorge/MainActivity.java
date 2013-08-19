package com.ces.cloudstorge;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ces.cloudstorge.Dialog.AboutDialog;
import com.ces.cloudstorge.Dialog.AddNewFolderDialog;
import com.ces.cloudstorge.Dialog.DeleteFileDialog;
import com.ces.cloudstorge.Dialog.FolderListDialog;
import com.ces.cloudstorge.Dialog.RenameFileDialog;
import com.ces.cloudstorge.adapter.AccountListAdapter;
import com.ces.cloudstorge.adapter.DrawerListAdapter;
import com.ces.cloudstorge.adapter.FileListAdapter;
import com.ces.cloudstorge.network.CloudStorgeRestUtilities;
import com.ces.cloudstorge.provider.CloudStorgeContract;
import com.ces.cloudstorge.util.CommonUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AddNewFolderDialog.AddNewFolderDialogListener, DeleteFileDialog.DeleteFileDialogListener,
        RenameFileDialog.RenameFileDialogListener, FolderListDialog.FolderListDialogListener {
    //private SimpleCursorAdapter mAdapter;
    // �ļ��С��ļ��б�adapter
    public static FileListAdapter mAdapter;
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

    // sql��ѯ����(root)
    public static final String SELECTION_SPECIAL = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "= (select " + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID +
            " from " + CloudStorgeContract.CloudStorge.TABLE_NAME + " where " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "=%d and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='%s' limit 1)" +
            " and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s'and " +
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER + " <> -20)";

    public static final String SELECTION_SHARE = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "= (select " + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID +
            " from " + CloudStorgeContract.CloudStorge.TABLE_NAME + " where " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID +
            "=%d and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + "='%s' limit 1)" +
            ")";

    // sql��ѯ����(root)
    public static final String SELECTION_CHILD = "( " + CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID + "=%d " +
            "and " + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME + " = '%s'and " +
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER + " <> -20)";

    public static final String selection_folder_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    public static final String selection_file_format = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=%d and "
            + CloudStorgeContract.CloudStorge.COLUMN_NAME_USERNAME +
            " = '%s')";

    public static final String selection_file_share = "(" + CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=%d)";

    // ��Ҫ���ֶ�
    public static final String[] fromColumns = {CloudStorgeContract.CloudStorge.COLUMN_NAME_MIME_TYPE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE,
            CloudStorgeContract.CloudStorge.COLUMN_NAME_ORIGIN_FOLDER
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
    public static Boolean isRoot;
    // �Ƿ�Ϊ����վ
    public static Boolean isTrash;
    // �Ƿ�Ϊ�����ļ���
    public static Boolean isShare;
    // �Ƿ��ǿ��ļ���
    public static Boolean isEmptyFolder;
    // ��ǰĿ¼���
    public static int currentFolderId;
    // ��Ŀ¼���
    public static int parentFolderId;
    // context
    public static Context mContext;
    // ��Ƭ����
    public static FragmentManager fragmentManager;
    // loader�ص��ӿ�
    public static LoaderManager.LoaderCallbacks<Cursor> callbackLoader;
    // loader����
    public static LoaderManager loadmanager;
    // ProgressDialog����
    private ProgressDialog progressDialog;
    // Ŀ¼�ȼ��б�
    public static ArrayList<String> listFolder;
    // �ʺŹ������
    public static AccountManager am;

    public static Uri changeUri;

    private List<String> listAccount;

    private String currentAccountName;


    // Uriƥ����
    public static final int ROUTE_CLOUDSTORGE = 1;
    public static final int ROUTE_CLOUDSTORGE_DELETE = 3;
    public static final int ROUTE_CLOUDSTORGE_MOVE = 4;
    public static final int ROUTE_CLOUDSTORGE_RENAME = 5;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, "cloudstorge", ROUTE_CLOUDSTORGE);
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, "cloudstorge_delete", ROUTE_CLOUDSTORGE_DELETE);
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, "cloudstorge_move", ROUTE_CLOUDSTORGE_MOVE);
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, "cloudstorge_rename", ROUTE_CLOUDSTORGE_RENAME);
    }

    // activity onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ����һ��activity�л�õ�ǰ�˻�����
        current_account = (Account) getIntent().getExtras().get("current_account");
        am = AccountManager.get(this);
        // ����һ��activity�л���Ƿ��ǵ�ǰĿ¼
        isRoot = (Boolean) getIntent().getExtras().get("isRoot");
        // ��ѯĿ¼�б�
        listFolder = new ArrayList<String>();
        if (null == savedInstanceState) {
            currentAccountName = current_account.name;
            isTrash = false;
            isShare = false;
            isEmptyFolder = false;
            mSortType = SORT_NAME;
            listFolder.add(getString(R.string.app_activity_title));
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
        } else {
            isRoot = savedInstanceState.getBoolean("isRoot");
            isTrash = savedInstanceState.getBoolean("isTrash");
            isShare = savedInstanceState.getBoolean("isShare");
            isEmptyFolder = savedInstanceState.getBoolean("isEmptyFolder");
            mSortType = savedInstanceState.getInt("mSortType");
            currentFolderId = savedInstanceState.getInt("currentFolderId");
            parentFolderId = savedInstanceState.getInt("parentFolderId");
            listFolder = savedInstanceState.getStringArrayList("listFolder");
            currentAccountName = savedInstanceState.getString("currentAccountName");
        }
        changeUri = null;
        LayoutInflater inflater = getLayoutInflater();
        mContext = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        callbackLoader = MainActivity.this;
        loadmanager = getSupportLoaderManager();

        // ��ʼ��ActionBar
        ActionBar actionBar = getActionBar();
        // ����homeͼ����
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        // ����title
        actionBar.setTitle(R.string.app_activity_title);
        actionBar.setIcon(R.drawable.ic_actionbar);

        // ��ʼ�����˵����б�
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawList = (ListView) findViewById(R.id.left_drawer);
        //View drawerHeader = inflater.inflate(R.layout.navigate_drawer_header, mDrawList, false);
        //TextView drawerHeaderText = (TextView) drawerHeader.findViewById(R.id.drawer_header_title);
        //drawerHeaderText.setText(current_account.name);
        //mDrawList.addHeaderView(drawerHeader, null, false);
        int selectFlag = 0;
        Spinner spinner = new Spinner(getBaseContext());
        Account[] accounts = am.getAccountsByType(Contract.ACCOUNT_TYPE);
        listAccount = new ArrayList<String>();
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equals(currentAccountName)) {
                listAccount.add(0, accounts[i].name);
                current_account = accounts[i];
            } else
                listAccount.add(accounts[i].name);
        }
        AccountListAdapter accountListAdapter = new AccountListAdapter(this, R.layout.navigate_drawer_header, listAccount, getLayoutInflater());
        spinner.setAdapter(accountListAdapter);
        //spinner.setSelection(selectFlag);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentAccountName.equals(listAccount.get(i))) {
                    return;
                }
                currentAccountName = listAccount.get(i);
                isRoot = true;
                isTrash = false;
                isShare = false;
                listFolder.clear();
                listFolder.add(getString(R.string.app_activity_title));
                currentFolderId = Contract.FOLDER_ROOT;
                parentFolderId = Contract.FOLDER_ROOT;
                mDrawList.setItemChecked(1, true);
                Account[] accounts = am.getAccountsByType(Contract.ACCOUNT_TYPE);
                for (int index = 0; index < accounts.length; index++) {
                    if (accounts[index].name.equals(currentAccountName)) {
                        current_account = accounts[index];
                        break;
                    }
                }
                if (null != mDrawerLayout && null != mDrawList)
                    mDrawerLayout.closeDrawer(mDrawList);
                ContentValues contentValues = new ContentValues();
                contentValues.put(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, listAccount.get(i));
                getContentResolver().update(CloudStorgeContract.CloudStorge.CONTENT_URI, contentValues,
                        CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=-99999", null);
                getActionBar().setTitle(R.string.app_activity_title);
                changeListHeader();
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                if (ConnectionChangeReceiver.isHasConnect)
                    call_syncAdapter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mDrawList.addHeaderView(spinner, null, false);
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
        getContentResolver().registerContentObserver(CloudStorgeContract.BASE_CONTENT_URI, true, mCloudStorgeObserver);

        // ��ʼ����Ƭ����
        if (null == savedInstanceState) {
            initFragment(false);
            if (!ConnectionChangeReceiver.isHasConnect) {
                create_tipDialog(R.string.terrible, R.string.network_error);
            } else {
                call_syncAdapter();
            }
        }
    }

    // ��������״̬
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isRoot", isRoot);
        outState.putBoolean("isTrash", isTrash);
        outState.putBoolean("isShare", isShare);
        outState.putBoolean("isEmptyFolder", isEmptyFolder);
        outState.putInt("mSortType", mSortType);
        outState.putInt("currentFolderId", currentFolderId);
        outState.putInt("parentFolderId", parentFolderId);
        outState.putStringArrayList("listFolder", listFolder);
        outState.putString("currentAccountName", current_account.name);
        super.onSaveInstanceState(outState);
    }

    // ����ѡ��˵�
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem actionNew = menu.findItem(R.id.action_new);
        if(mDrawerLayout.isDrawerOpen(mDrawList)) {
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.action_new).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
        }
        else {
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_sort).setVisible(true);
            if (isRoot)
                actionNew.setVisible(true);
            if (isTrash || isShare)
                actionNew.setVisible(false);
        }
        return true;
    }

    // ��������Loader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int specialFolderId = Contract.FOLDER_ROOT;
        if (isTrash)
            specialFolderId = Contract.FOLDER_TRASH;
        if (isShare)
            specialFolderId = Contract.FOLDER_SHARE;
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        if (isShare)
            selection = String.format(SELECTION_SHARE, specialFolderId, current_account.name);
        String order = "";
        if (mSortType == SORT_NAME)
            order = "folder_id desc,name asc";
        else if (mSortType == SORT_LAST_MODIFITY)
            order = "folder_id desc, last_modified asc";
        return new CursorLoader(this, CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, order);
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
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        int folderIdSp = cursor.getInt(Contract.PROJECTION_FOLDER_ID);
        cursor.close();
        return folderIdSp;
    }

    // ָ���ļ��л�ȡ
    public Cursor get_assignFolder(int folderId) {
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // ָ���ļ���ȡ
    public Cursor get_assignFile(int fileId) {
        String selection = String.format(selection_file_format, fileId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    // ָ���ļ��и��ļ��л�ȡ
    public int get_assignParentFolder(int folderId) {
        if(Contract.FOLDER_ROOT == folderId)
            return folderId;
        String selection = String.format(selection_folder_format, folderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI, Contract.PROJECTION, selection, null, null);
        cursor.moveToFirst();
        int parentFolderId = cursor.getInt(Contract.PROJECTION_PARENT_FOLDER_ID);
        cursor.close();
        return parentFolderId;
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
                Contract.PROJECTION, selection, null, null);
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
        cursor.close();
        new AddFolderAsyncTask().execute(inputText);
    }

    // ɾ���ļ��ص��ӿ�
    @Override
    public void onFinishDeleteFileDialog(String filelist, String folderlist, boolean isForever) {
        // ���ɾ���ļ��б��
        String[] array_folder = folderlist.split(",");
        // ���ɾ���ļ����
        String[] array_file = filelist.split(",");
        int trashId = get_folderTrash();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        String current_date = CommonUtil.get_currentDateString();
        if (!isForever) {
            for (int i = 0; i < array_folder.length; i++) {
                if (null == array_folder[i] || "".equals(array_folder[i]))
                    continue;
                int folderId = Integer.parseInt(array_folder[i]);
                Cursor cursor = get_assignFolder(folderId);
                String syncAction = cursor.getString(Contract.PROJECTION_SYNC_ACTION);
                batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_DELETE_URI)
                        .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + folderId, null)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, trashId)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION, null == syncAction ? Contract.SYNC_ACTION_DELETE + "" : syncAction + "," + Contract.SYNC_ACTION_DELETE + "")
                        .build());
                cursor.close();
            }
            for (int i = 0; i < array_file.length; i++) {
                if (null == array_file[i] || "".equals(array_file[i]))
                    continue;
                int fileId = Integer.parseInt(array_file[i]);
                Cursor cursor = get_assignFile(fileId);
                String syncAction = cursor.getString(Contract.PROJECTION_SYNC_ACTION);
                batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_DELETE_URI)
                        .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, trashId)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC)
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SHARE, "")
                        .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION, null == syncAction ? Contract.SYNC_ACTION_DELETE + "" : syncAction + "," + Contract.SYNC_ACTION_DELETE + "")
                        .build());
                cursor.close();
            }
            try {
                getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
                changeUri = CloudStorgeContract.CloudStorge.CONTENT_DELETE_URI;
                getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_DELETE_URI, null, false);
                //getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                Toast.makeText(this, R.string.delete_tip, Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        } else {
            if (!ConnectionChangeReceiver.isHasConnect) {
                create_tipDialog(R.string.terrible, R.string.network_error);
            } else {
                new DeleteFileForeverAsyncTask().execute(filelist, folderlist);
            }

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
        String current_date = CommonUtil.get_currentDateString();
        String selection = isRoot || isTrash ? String.format(SELECTION_SPECIAL, specialFolderId, current_account.name, current_account.name)
                : String.format(SELECTION_CHILD, currentFolderId, current_account.name);
        Cursor cursor = getContentResolver().query(CloudStorgeContract.CloudStorge.CONTENT_URI,
                Contract.PROJECTION, selection, null, null);
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
        cursor.close();
        if (!flag) {
            create_tipDialog(R.string.tip, R.string.rename_error);
        } else {
            //û�������ļ���������ݿ����
            String selectionUpdate = "";
            Cursor cursor1 = null;
            String syncAction = "";
            if (type == Contract.TYPE_FOLDER) {
                cursor1 = get_assignFolder(id);
                syncAction = cursor1.getString(Contract.PROJECTION_SYNC_ACTION);
                selectionUpdate = CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + id;
            }
            if (type == Contract.TYPE_FILE) {
                cursor1 = get_assignFile(id);
                syncAction = cursor1.getString(Contract.PROJECTION_SYNC_ACTION);
                selectionUpdate = CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + id;
            }
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            batch.add(ContentProviderOperation
                    .newUpdate(CloudStorgeContract.CloudStorge.CONTENT_RENAME_URI)
                    .withSelection(selectionUpdate, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, name)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION, null == syncAction ? Contract.SYNC_ACTION_RENAME + "" : syncAction + "," + Contract.SYNC_ACTION_RENAME + "")
                    .build()
            );
            try {
                getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
                changeUri = CloudStorgeContract.CloudStorge.CONTENT_RENAME_URI;
                getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_RENAME_URI, null, false);
                //getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                Toast.makeText(this, R.string.rename_tip, Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
            cursor1.close();
        }
    }

    // ѡ���ƶ�Ŀ���ļ��лص��ӿ�
    @Override
    public void onFinishSelectFolder(int destFolderId, String fileList, String folderList) {
        // ����ƶ��ļ��б��
        String[] array_folder = folderList.split(",");
        // ����ƶ��ļ����
        String[] array_file = fileList.split(",");
        String current_date = CommonUtil.get_currentDateString();
        if (destFolderId == Contract.FOLDER_ROOT)
            destFolderId = get_folderRoot();
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        String syncAction = "";
        for (int i = 0; i < array_folder.length; i++) {
            if (null == array_folder[i] || "".equals(array_folder[i]))
                continue;
            int folderId = Integer.parseInt(array_folder[i]);
            Cursor cursorFolder = get_assignFolder(folderId);
            syncAction = cursorFolder.getString(Contract.PROJECTION_SYNC_ACTION);
            String newName = get_newFileName(cursorFolder.getString(Contract.PROJECTION_NAME), destFolderId, Contract.TYPE_FOLDER, folderId);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_MOVE_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + folderId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, destFolderId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, newName)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION, null == syncAction ? Contract.SYNC_ACTION_MOVE + "" : syncAction + "," + Contract.SYNC_ACTION_MOVE + "")
                    .build());
            cursorFolder.close();
        }
        for (int i = 0; i < array_file.length; i++) {
            if (null == array_file[i] || "".equals(array_file[i]))
                continue;
            int fileId = Integer.parseInt(array_file[i]);
            Cursor cursorFile = get_assignFile(fileId);
            syncAction = cursorFile.getString(Contract.PROJECTION_SYNC_ACTION);
            String newName = get_newFileName(cursorFile.getString(Contract.PROJECTION_NAME), destFolderId, Contract.TYPE_FILE, fileId);
            batch.add(ContentProviderOperation.newUpdate(CloudStorgeContract.CloudStorge.CONTENT_MOVE_URI)
                    .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_PARENT_FOLDER_ID, destFolderId)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_NAME, newName)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_LAST_MODIFIED, current_date)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_IS_NEED_SYNC, Contract.NEED_SYNC)
                    .withValue(CloudStorgeContract.CloudStorge.COLUMN_NAME_SYNC_ACTION, null == syncAction ? Contract.SYNC_ACTION_MOVE + "" : syncAction + "," + Contract.SYNC_ACTION_MOVE + "")
                    .build());
            cursorFile.close();
        }
        try {
            getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
            changeUri = CloudStorgeContract.CloudStorge.CONTENT_MOVE_URI;
            getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_MOVE_URI, null, false);
            //getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
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
                Contract.PROJECTION, selection, null, null);
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
        cursor.close();
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
                    mDrawList.setItemChecked(Contract.DRAWER_SHARE, true);
                create_exitDialog();
            } else {
                TextView drawerText = (TextView) view.findViewById(R.id.drawer_title);
                mDrawList.setItemChecked(position, true);
                getActionBar().setTitle(drawerText.getText());
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
                mDrawerLayout.closeDrawer(mDrawList);
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

    private void call_syncAdapter() {
        if (!ConnectionChangeReceiver.isHasConnect) {
            create_tipDialog(R.string.terrible, R.string.network_error);
            return;
        }
        //create_progressDialog(getString(R.string.sync_message));
        changeSyncProgressBarVisibilty(View.VISIBLE);
        request_syncAdapter(true);
    }

    private void request_syncAdapter(boolean isSync) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        settingsBundle.putBoolean("isSync", isSync);
        ContentResolver.requestSync(current_account, Contract.CONTENT_AUTHORITY, settingsBundle);
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
                Toast.makeText(this, R.string.sync_message, Toast.LENGTH_SHORT).show();
                call_syncAdapter();
                return true;
            case R.id.action_new:
                create_addDialog();
                return true;
            case R.id.action_sort:
                create_sortDialog();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(this, SettingsActivity.class);
                startActivity(intent);
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
        if (isRoot || isTrash || isShare) {
            create_exitDialog();
        } else {
            listFolder.remove(listFolder.size() - 1);
            isRoot = parentFolderId == Contract.FOLDER_ROOT ? true : false;
            currentFolderId = parentFolderId;
            changeListHeader();
            // ��ѯ�ϼ�Ŀ¼���ϼ�Ŀ¼
            parentFolderId = get_assignParentFolder(currentFolderId);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    public static void changeListHeader() {
        ListHeaderFragment listFragment = (ListHeaderFragment) fragmentManager.findFragmentByTag("listHeader");
        String folder_trace = "";
        for (int i = 0; i < listFolder.size(); i++) {
            folder_trace += "/" + listFolder.get(i);
        }
        listFragment.set_folderTrace(folder_trace);
    }

    public static void changeSyncProgressBarVisibilty(int v) {
        ListHeaderFragment listFragment = (ListHeaderFragment) fragmentManager.findFragmentByTag("listHeader");
        if (null != listFragment)
            listFragment.set_syncProgressBarVisibilty(v);
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

    // ContentProvider֪ͨ
    public class CloudStorgeObserver extends ContentObserver {
        public CloudStorgeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, changeUri);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (null != progressDialog && progressDialog.isShowing())
                progressDialog.dismiss();
            changeSyncProgressBarVisibilty(View.GONE);
            if (null != uri) {
                int match = sUriMatcher.match(uri);
                switch (match) {
                    case ROUTE_CLOUDSTORGE_DELETE:
                    case ROUTE_CLOUDSTORGE_MOVE:
                    case ROUTE_CLOUDSTORGE_RENAME:
                        changeUri = null;
                        request_syncAdapter(false);
                        break;
                    case ROUTE_CLOUDSTORGE:
                        changeUri = null;
                        getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                        break;
                }
            }
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
    }

    // ����ļ����첽�ӿ�
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

    // ����ɾ���ļ��첽�ӿ�
    private class DeleteFileForeverAsyncTask extends AsyncTask<String, Void, JSONObject> {
        private String fileArray;
        private String folderArray;

        @Override
        protected void onPreExecute() {
            create_progressDialog(getString(R.string.progress_delete_file));
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            fileArray = strings[0];
            folderArray = strings[1];
            return CloudStorgeRestUtilities.deleteFile(fileArray, folderArray, 1, null);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            String[] array_folder = folderArray.split(",");
            // ���ɾ���ļ����
            String[] array_file = fileArray.split(",");
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            for (int i = 0; i < array_folder.length; i++) {
                if (null == array_folder[i] || "".equals(array_folder[i]))
                    continue;
                int folderId = Integer.parseInt(array_folder[i]);
                batch.add(ContentProviderOperation.newDelete(CloudStorgeContract.CloudStorge.CONTENT_URI)
                        .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FOLDER_ID + "=" + folderId, null).build());
            }
            for (int i = 0; i < array_file.length; i++) {
                if (null == array_file[i] || "".equals(array_file[i]))
                    continue;
                int fileId = Integer.parseInt(array_file[i]);
                batch.add(ContentProviderOperation.newDelete(CloudStorgeContract.CloudStorge.CONTENT_URI)
                        .withSelection(CloudStorgeContract.CloudStorge.COLUMN_NAME_FILE_ID + "=" + fileId, null).build());
            }

            try {
                getContentResolver().applyBatch(Contract.CONTENT_AUTHORITY, batch);
                //getContentResolver().notifyChange(CloudStorgeContract.CloudStorge.CONTENT_URI, null, false);
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                Toast.makeText(MainActivity.this, R.string.delete_tip_forever, Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }
}