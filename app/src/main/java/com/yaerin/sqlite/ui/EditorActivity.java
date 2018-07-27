package com.yaerin.sqlite.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.yaerin.sqlite.C;
import com.yaerin.sqlite.R;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private String mPath;
    private SQLiteDatabase mDatabase;
    private List<String> mTableNames;
    private String mTableName;

    private ListView mTables;
    private ArrayAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sql_editor_main);
        mTables = findViewById(R.id.table);

        String p = getIntent().getStringExtra("path");
        if (p != null) {
            mPath = p;
        } else {

            Uri uri = getIntent().getData();
            if (uri == null || uri.getScheme() == null || uri.getPath() == null) {
                Toast.makeText(this, getIntent().toString(), Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mPath = getFileForUri(uri).getPath();
            } else {
                mPath = uri.getPath();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            main();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sql_editor_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_exec) {
            View view = View.inflate(EditorActivity.this, R.layout.sql_editor_dialog_edit, null);
            final EditText editText = view.findViewById(R.id.edit_text);
            editText.setHint(R.string.sql_editor_hint_sql);
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.sql_editor_action_exec_sql)
                    .setView(view)
                    .setNegativeButton(R.string.sql_editor_action_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.sql_editor_action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mDatabase.execSQL(editText.getText().toString());
                                Toast.makeText(EditorActivity.this, R.string.sql_editor_message_success, Toast.LENGTH_SHORT).show();
                                refresh();
                            } catch (Exception e) {
                                Toast.makeText(EditorActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .create()
                    .show();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        main();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.sql_editor_title_confirm)
                    .setMessage(R.string.sql_editor_message_irreversible)
                    .setPositiveButton(R.string.sql_editor_action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            mDatabase.execSQL("DROP TABLE \"" + mTableName + "\"");
                            refresh();
                        }
                    })
                    .create()
                    .show();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        mDatabase.close();
        super.onDestroy();
    }

    private void main() {
        try {
            openDatabase();
            mTableNames = getTableNames();
            initView();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void refresh() {
        mTableNames.clear();
        mTableNames.addAll(getTableNames());
        mAdapter.notifyDataSetChanged();
    }

    private void initView() {
        mTables.setAdapter(mAdapter = new ArrayAdapter<>(
                this, R.layout.sql_editor_table_item, R.id.table_name, mTableNames));
        mTables.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                           @Override
                                           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                               startActivity(new Intent(EditorActivity.this, TableActivity.class)
                                                       .putExtra(C.EXTRA_DATABASE_PATH, mPath)
                                                       .putExtra(C.EXTRA_TABLE_NAME, mTableNames.get(position)));
                                           }
                                       }
        );
        mTables.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mTableName = mTableNames.get(position);
                PopupMenu popup = new PopupMenu(EditorActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.sql_editor_main_popup, popup.getMenu());
                popup.setOnMenuItemClickListener(EditorActivity.this);
                popup.show();
                return true;
            }

        });
    }

    private void openDatabase() {
        String[] arr = mPath.split("/");
        setTitle("`" + arr[arr.length - 1].replaceAll("\\.db$", "") + "`");
        mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private List<String> getTableNames() {
        List<String> list = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
                null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        Collections.sort(list, Collator.getInstance(Locale.getDefault()));
        return list;
    }

    private File getFileForUri(Uri uri) {
        String path = uri.getEncodedPath();

        assert path != null;
        final int splitIndex = path.indexOf('/', 1);
        path = Uri.decode(path.substring(splitIndex + 1));
        File file = new File("/storage", path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
        }

        return file;
    }

}
