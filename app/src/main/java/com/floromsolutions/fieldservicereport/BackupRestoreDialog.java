package com.floromsolutions.fieldservicereport;

import android.app.Activity;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.Map;

public class BackupRestoreDialog extends Dialog {
    private static DialogType myDialogType;
    private View myDialogView;
    private Context appContext;
    private String header;
    private BackupRestoreState backupRestoreState;
    private ProgressBar progressBar;
    private TextView statusMonitor;
    private Button closeBtn;
    private Activity appActivity;

    private static boolean lDriveSuccess = false, gDriveSuccess = false;
    private static boolean backupToLocal, backupToGoogle;
    private String lFailureReason = "", gFailureReason = "";
    private static final int SLEEP_TIME = 750;
    private BackupManager backupManager;
    private boolean isRestoredFromLocal, isRestoredFromOnline;

    public enum DialogType
    {
        BACKUP,
        RESTORE
    }

    private enum BackupRestoreState
    {
        START(0),
        FINISH_LOCAL(1),
        FINISH_GOOGLE(2);

        private int dValue;
        BackupRestoreState(int value)
        {
            this.dValue = value;
        }

        protected int getDValue()
        {
            return dValue;
        }
    }

    protected static BackupRestoreDialog getInstance(Context c, DialogType dialogType)
    {
        myDialogType = dialogType;

        return new BackupRestoreDialog(c);
    }

    private BackupRestoreDialog(@NonNull Context context) {
        super(context);
        appContext = context;
        appActivity = (Activity)context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getContentView(R.layout.dialog_backup_restore));

        switch (myDialogType) {
            case BACKUP:
                header = appContext.getResources().getString(R.string.backup);
                break;
            case RESTORE:
                header = appContext.getResources().getString(R.string.restore);
                break;
            default:
                break;
        }

        TextView headerView = myDialogView.findViewById(R.id.br_header);
        headerView.setText(header);

        closeBtn = myDialogView.findViewById(R.id.ok_btn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        statusMonitor = findViewById(R.id.status_monitor);
        this.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                String checking = "Checking Preferences...";
                statusMonitor.setText(checking);
                onVisibleToUser();
            }
        });
    }

    private void onVisibleToUser()
    {
        statusMonitor = findViewById(R.id.status_monitor);
        backupRestoreState = BackupRestoreState.START;
        //do this when the dialog is already shown
        Thread progressThread, backupRestoreThread;
        progressBar = findViewById(R.id.progress_bar);
        if(progressBar.getVisibility() == View.GONE) {
            progressBar.setVisibility(View.VISIBLE);
        }

        progressThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                backupToLocal = MySettings.getSaveBackupToLocalDrive();
                backupToGoogle = MySettings.getSaveBackupToGoogleDrive();

                // process has started
                switch (myDialogType)
                {
                    case BACKUP:
                        startBackupProgressThread();
                        break;
                    case RESTORE:
                        startRestoreProgressThread();
                        break;
                    default:
                        break;
                }
                appActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(progressBar.getVisibility() == View.VISIBLE)
                        {
                            progressBar.setVisibility(View.GONE);
                        }
                        closeBtn.setText(android.R.string.ok);
                    }
                });
            }
        };

        backupRestoreThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                switch (myDialogType)
                {
                    case BACKUP:
                        backupNow(System.currentTimeMillis());
                        break;
                    case RESTORE:
                        restoreNow();
                        break;
                    default:
                        break;
                }
            }
        };

        progressThread.start();
        backupRestoreThread.start();
    }

    private void backupNow(long backupId)
    {
        String databasePath = appContext.getDatabasePath(FSDatabaseHelper.getDBName()).getAbsolutePath();
        File currentDB = new File(databasePath);
        backupToLocal = MySettings.getSaveBackupToLocalDrive();
        backupToGoogle = MySettings.getSaveBackupToGoogleDrive();

        if(backupToLocal)
        {
            lDriveSuccess = backupToLocalDrive(backupId, currentDB);
        }

        if(backupToGoogle) {
            backupToOnline(backupId);
            gDriveSuccess = true;
            backupRestoreState = BackupRestoreState.FINISH_GOOGLE;
            gFailureReason = "current app version does not support online backup";
        }
    }

    private boolean backupToLocalDrive(long backupId, File currDB){
        boolean isBackupSuccessful;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //FSUtils.selfPermissionRequest(MySettings.READ_WRITE_DRIVE_REQ_CODE);
            ((FSActivity)appContext).selfPermissionRequest(MySettings.READ_WRITE_DRIVE_REQ_CODE);
        }

        String backupFileName = MySettings.DB_BACKUP_PRE_NAME + backupId + MySettings.BACKUP_FILE_EXTENSION;
        String pathName = Environment.getExternalStorageDirectory() + File.separator + MySettings.FS_REPORT_LABEL + File.separator + MySettings.BACKUP_DIR  + File.separator;
        File backupDir = new File(pathName);
        FileChannel dSource, dDestination;
        long prevBackupId = MySettings.getLastLocalBackup();
        boolean exist = true;
        if(!backupDir.exists()){
            exist = backupDir.mkdirs();
        }

        if(exist)
        {
            File backupDB = new File(backupDir, backupFileName);
            boolean destExist = true;
            if(!backupDB.exists())
            {
                try {
                    destExist = backupDB.createNewFile();
                }catch (IOException io)
                {
                    io.printStackTrace();
                }
            }

            if(destExist)
            {
                try {
                    dSource = new FileInputStream(currDB).getChannel();
                    dDestination = new FileOutputStream(backupDB).getChannel();
                    dDestination.transferFrom(dSource, 0, dSource.size());
                    dSource.close();
                    dDestination.close();
                    //delete previous backup
                    String prevBackupName = MySettings.DB_BACKUP_PRE_NAME + prevBackupId + MySettings.BACKUP_FILE_EXTENSION;
                    File prevBackupFile = new File(backupDir, prevBackupName);
                    if (prevBackupFile.exists()) {
                        boolean deleted = prevBackupFile.getCanonicalFile().delete();
                        if (deleted) {
                            appContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(prevBackupFile)));
                        }
                    }
                    MySettings.setLastLocalBackup(backupId);
                    isBackupSuccessful = true;
                } catch (IOException ex) {
                    Log.d("Backup Error", ex.getMessage());
                    lFailureReason = ex.getMessage();
                    isBackupSuccessful = false;
                }
            }
            else {
                lFailureReason = "Read and Write Permission not yet granted, please grant permission first and try again";
                isBackupSuccessful = false;
            }
        }
        else {
            lFailureReason = "Read and Write Permission not yet granted, please grant permission first and try again";
            isBackupSuccessful = false;
        }

        if(MySettings.getBackupMySettings())
        {
            backupFileName = MySettings.SP_BACKUP_PRE_NAME + backupId + MySettings.BACKUP_FILE_EXTENSION;
            File backupSP = new File(backupDir, backupFileName);
            isBackupSuccessful = isBackupSuccessful && backupSettingsToLocalDrive(prevBackupId, backupDir, backupSP);
        }

        backupRestoreState = BackupRestoreState.FINISH_LOCAL;
        return isBackupSuccessful;
    }

    private boolean restoreFromLocalDrive()
    {
        boolean result = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //FSUtils.selfPermissionRequest(MySettings.READ_WRITE_DRIVE_REQ_CODE);
            ((FSActivity)appContext).selfPermissionRequest(MySettings.READ_WRITE_DRIVE_REQ_CODE);
        }
        // get list of files in backup folder
        String databasePath = appContext.getDatabasePath(FSDatabaseHelper.getDBName()).getAbsolutePath();
        File currentDB = new File(databasePath);
        long backupId = MySettings.getLastLocalBackup();
        String backupFileName = MySettings.DB_BACKUP_PRE_NAME + backupId + MySettings.BACKUP_FILE_EXTENSION;
        String pathName = Environment.getExternalStorageDirectory() + File.separator + MySettings.FS_REPORT_LABEL + File.separator + MySettings.BACKUP_DIR  + File.separator;
        File backupsDir = new File(pathName);
        File backupFileList[] = backupsDir.listFiles();
        File myDbBackup = new File(backupsDir, backupFileName);
        if(myDbBackup.exists())
        {
            result = doRestoreFromLocal(myDbBackup, currentDB);
        }
        else if(backupFileList != null && backupFileList.length > 0)
        {
            myDbBackup = null;
            for(File f: backupFileList)
            {
                if(f.getName().substring(0, 2).equals("db")){
                    myDbBackup = f;
                }
            }

            if(myDbBackup != null)
            {
                result = doRestoreFromLocal(myDbBackup, currentDB);
            }
        }

        backupFileName = MySettings.SP_BACKUP_PRE_NAME + backupId + MySettings.BACKUP_FILE_EXTENSION;
        File mySpBackup = new File(backupsDir, backupFileName);
        if(mySpBackup.exists())
        {
            result = result && restoreSettingsFromLocalDrive(mySpBackup);
        }
        else if(backupFileList != null && backupFileList.length > 0)
        {
            mySpBackup = null;
            for(File f: backupFileList)
            {
                if(f.getName().substring(0, 2).equals("sp")){
                    mySpBackup = f;
                }
            }

            if(mySpBackup != null)
            {
                result = result && restoreSettingsFromLocalDrive(mySpBackup);
            }
        }
        backupRestoreState = BackupRestoreState.FINISH_LOCAL;
        return result;
    }

    private boolean doRestoreFromLocal(File backup, File myDB)
    {
        try {
            FileChannel dSource = new FileInputStream(backup).getChannel();
            FileChannel dDestination = new FileOutputStream(myDB).getChannel();
            dDestination.transferFrom(dSource, 0, dSource.size());
            dSource.close();
            dDestination.close();
            return true;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }
    }

    private void startBackupProgressThread()
    {
        boolean gottenLocalState = false, gottenOnlineState = false;
        if(backupToLocal)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            }catch (InterruptedException ie)
            {
                Log.d("Interrupt:", ie.getMessage());
            }

            // display initial text for starting local backup
            appActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusMonitor.setText(R.string.starting_l_backup);
                }
            });

            while (!gottenLocalState && backupRestoreState.getDValue() > BackupRestoreState.START.getDValue())
            {
                try {
                    Thread.sleep(SLEEP_TIME);
                }catch (InterruptedException ie)
                {
                    Log.d("Interrupt:", ie.getMessage());
                }
                if(lDriveSuccess)
                {
                    // display text for successful backup to local drive
                    appActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusMonitor.setText(R.string.l_backup_successful);
                        }
                    });
                }
                else {
                    appActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String failedText = appContext.getResources().getString(R.string.l_backup_failed) + ", " + lFailureReason;
                            statusMonitor.setText(failedText);
                        }
                    });
                }
                gottenLocalState = true;
            }
        }

        if(backupToGoogle)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            }catch (InterruptedException ie)
            {
                Log.d("Interrupt:", ie.getMessage());
            }
            // display initial text to backup to google
            appActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusMonitor.setText(R.string.starting_g_backup);
                }
            });

            while (!gottenOnlineState && backupRestoreState.getDValue() > BackupRestoreState.FINISH_LOCAL.getDValue())
            {
                try {
                    Thread.sleep(SLEEP_TIME);
                }catch (InterruptedException ie)
                {
                    Log.d("Interrupt:", ie.getMessage());
                }
                if(gDriveSuccess)
                {
                    // display text for successful backup to google drive
                    appActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusMonitor.setText(R.string.g_backup_successful);
                        }
                    });
                }
                else {
                    appActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String failedText = appContext.getResources().getString(R.string.g_backup_failed) + ", " + gFailureReason;
                            statusMonitor.setText(failedText);
                        }
                    });
                }
                gottenOnlineState = true;
            }
        }

        if(!backupToLocal && !backupToGoogle) {
            try {
                Thread.sleep(SLEEP_TIME);
            }catch (InterruptedException ie)
            {
                Log.d("Interrupt:", ie.getMessage());
            }
            //display initial text for no backup method found
            appActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    statusMonitor.setText(R.string.no_backup_method_found);
                }
            });
        }
    }

    private void startRestoreProgressThread()
    {
        boolean backupRestored = false, gottenLocalState = false, gottenOnlineState = false;
        try {
            Thread.sleep(SLEEP_TIME);
        }catch (InterruptedException ie)
        {
            Log.d("Interrupt:", ie.getMessage());
        }

        // display initial text for starting local backup
        appActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusMonitor.setText(R.string.starting_restore);
            }
        });

        while (!gottenLocalState && backupRestoreState.getDValue() >= BackupRestoreState.FINISH_LOCAL.getDValue())
        {
            try {
                Thread.sleep(SLEEP_TIME);
            }catch (InterruptedException ie)
            {
                Log.d("Interrupt:", ie.getMessage());
            }
            backupRestored = isRestoredFromLocal;
            gottenLocalState = true;
        }

        if(!isRestoredFromLocal)
        {
            //start online restore
            while (!gottenOnlineState && backupRestoreState.getDValue() == BackupRestoreState.FINISH_GOOGLE.getDValue())
            {
                backupRestored = isRestoredFromOnline;
                gottenOnlineState = true;
            }
        }

        if(backupRestored) {
            appActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusMonitor.setText(R.string.restore_successful);
                }
            });
        }
        else {
            appActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusMonitor.setText(R.string.restore_failed);
                }
            });
        }
    }

    private void restoreNow()
    {
        //try local restore
        isRestoredFromLocal = restoreFromLocalDrive();
        if(!isRestoredFromLocal) {
            //for online
            try {
                int result = backupManager.requestRestore(new RestoreObserver() {
                    @Override
                    public void restoreFinished(int error) {
                        super.restoreFinished(error);
                    }

                    @Override
                    public void restoreStarting(int numPackages) {
                        super.restoreStarting(numPackages);
                    }

                    @Override
                    public void onUpdate(int nowBeingRestored, String currentPackage) {
                        super.onUpdate(nowBeingRestored, currentPackage);
                    }
                });
                gDriveSuccess = (result == 0);
            }catch (Exception ex){
                gDriveSuccess = false;
            }finally {
                backupRestoreState = BackupRestoreState.FINISH_GOOGLE;
                isRestoredFromOnline = gDriveSuccess;
            }
        }
    }

    private void backupToOnline(long backupId)
    {
        backupManager = new BackupManager(appContext);
        backupManager.dataChanged();
        MySettings.setLastOnlineBackup(backupId);
    }

    private boolean backupSettingsToLocalDrive(long prevBackupId, File destinationFolder, File destinationFile)
    {
        boolean dirExist = true, destinationFileExist = true;
        if(!destinationFolder.exists())
        {
            dirExist = destinationFolder.mkdirs();
        }

        if(dirExist && !destinationFile.exists())
        {
            try {
                destinationFileExist = destinationFile.createNewFile();
            }
            catch (IOException io)
            {
                destinationFileExist = false;
                io.printStackTrace();
            }
        }
        boolean result = false;
        if(destinationFileExist) {
            ObjectOutputStream outputStream = null;
            try {
                outputStream = new ObjectOutputStream(new FileOutputStream(destinationFile));
                SharedPreferences preferences = appContext.getSharedPreferences(MySettings.SETTINGS_PREF_TAG, Context.MODE_PRIVATE);
                outputStream.writeObject(preferences.getAll());
                String prevBackupName = MySettings.SP_BACKUP_PRE_NAME + prevBackupId + MySettings.BACKUP_FILE_EXTENSION;
                File prevBackupFile = new File(destinationFolder, prevBackupName);
                if (prevBackupFile.exists()) {
                    boolean deleted = prevBackupFile.getCanonicalFile().delete();
                    if (deleted) {
                        appContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(prevBackupFile)));
                    }
                }
                result = true;
            } catch (FileNotFoundException fEx) {
                fEx.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    private boolean restoreSettingsFromLocalDrive(File source)
    {
        boolean result = false;
        ObjectInputStream inputStream = null;
        if(source.exists()) {
            try {
                inputStream = new ObjectInputStream(new FileInputStream(source));
                SharedPreferences.Editor prefEditor = appContext.getSharedPreferences(MySettings.SETTINGS_PREF_TAG, Context.MODE_PRIVATE).edit();
                prefEditor.clear();
                Map<String, ?> entries = (Map<String, ?>) inputStream.readObject();
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    Object object = entry.getValue();
                    String key = entry.getKey();

                    if (object instanceof Boolean) {
                        prefEditor.putBoolean(key, ((Boolean) object));
                    } else if (object instanceof String) {
                        prefEditor.putString(key, ((String) object));
                    } else if (object instanceof Integer) {
                        prefEditor.putInt(key, ((Integer) object));
                    } else if (object instanceof Long) {
                        prefEditor.putLong(key, ((Long) object));
                    } else if (object instanceof Float) {
                        prefEditor.putFloat(key, ((Float) object));
                    }
                }
                prefEditor.apply();
                result = true;
            } catch (FileNotFoundException fEx) {
                fEx.printStackTrace();
            } catch (IOException io) {
                io.printStackTrace();
            } catch (ClassNotFoundException cEx) {
                cEx.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException iox) {
                    iox.printStackTrace();
                }
            }
        }
        return result;
    }

    private View getContentView(int resLayoutId){
        myDialogView = getLayoutInflater().inflate(resLayoutId, null);
        return myDialogView;
    }
}
