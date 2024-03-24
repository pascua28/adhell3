package com.fusionjack.adhell3.db.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_25_26 extends Migration {

    public Migration_25_26(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE StaticProxy " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT UNIQUE, " +
                "hostname TEXT, " +
                "port INTEGER NOT NULL, " +
                "exclusionList TEXT, " +
                "user TEXT, " +
                "password TEXT)");
    }
}
