package com.fusionjack.adhell3.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_23_24 extends Migration {

    public Migration_23_24(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE AppInfo ADD COLUMN wifiRestricted INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE RestrictedPackage ADD COLUMN type TEXT DEFAULT 'mobile'");
    }
}
