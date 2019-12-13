package com.floromsolutions.fieldservicereport;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class FSBackupAgent extends BackupAgentHelper {
    @Override
    public void onCreate() {
        super.onCreate();
        //for shared pref
        SharedPreferencesBackupHelper spBackupHelper = new SharedPreferencesBackupHelper(this, MySettings.SETTINGS_PREF_TAG);
        addHelper(MySettings.SETTINGS_PREF_BACKUP_KEY, spBackupHelper);

        //for database
        FileBackupHelper fileBackupHelper = new FileBackupHelper(this, getDatabasePath(FSDatabaseHelper.getDBName()).getAbsolutePath());
        addHelper(MySettings.SETTINGS_DB_BACKUP_KEY, fileBackupHelper);
    }
}
