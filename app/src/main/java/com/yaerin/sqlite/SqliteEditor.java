package com.yaerin.sqlite;

import android.content.Context;
import android.content.Intent;

import com.yaerin.sqlite.ui.EditorActivity;

public class SqliteEditor {

    public static void edit(Context c, String path) {
        Intent i = new Intent(c, EditorActivity.class);
        i.putExtra("path", path);
        c.startActivity(i);
    }
}
