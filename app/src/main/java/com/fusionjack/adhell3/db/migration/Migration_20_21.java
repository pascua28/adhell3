package com.fusionjack.adhell3.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_20_21 extends Migration {

    public Migration_20_21(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE BlockUrlProviders ADD COLUMN policyPackageId TEXT DEFAULT 'default-policy' REFERENCES PolicyPackage(id)");
        database.execSQL("ALTER TABLE UserBlockUrl ADD COLUMN policyPackageId TEXT DEFAULT 'default-policy' REFERENCES PolicyPackage(id)");
        database.execSQL("ALTER TABLE WhiteUrl ADD COLUMN policyPackageId TEXT DEFAULT 'default-policy' REFERENCES PolicyPackage(id)");
    }
}
