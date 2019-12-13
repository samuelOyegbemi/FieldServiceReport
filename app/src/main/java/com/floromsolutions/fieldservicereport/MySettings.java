package com.floromsolutions.fieldservicereport;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class MySettings {
    protected static final String ACTION_SMS_SENT = "com.floromsolutions.fieldservicereport.SMS_SENT";
    protected static final int CALL_REQ_CODE = 49;
    protected static final int MESSAGE_REQ_CODE = 51;
    protected static final int GET_ACCOUNTS_REQ_CODE = 53;
    protected static final int READ_WRITE_DRIVE_REQ_CODE = 55;
    private static final String ALLOW_AUTO_CALL = "allowAutoCall";
    private static final String ALLOW_AUTO_MESSAGE = "allowAutoMessage";
    private static final String APPLICATION_LANGUAGE = "application_language";
    private static final String INCLUDE_SALUTATION = "include_salutation";
    private static final String USER_NAME = "user_name";
    private static final String SECRETARY_NUMBER = "secretary_num";
    private static final String SECRETARY_NAME = "secretary_name";
    private static final String SECRETARY_EMAIL = "secretary_e_mail";
    private static final String GROUP_OVERSEER_NUMBER = "group_overseer_num";
    private static final String GROUP_OVERSEER_NAME = "group_overseer_name";
    private static final String GROUP_OVERSEER_EMAIL = "group_overseer_e_mail";
    private static final String SEND_TO_GROUP_OVERSEER = "send_to_group_overseer";
    protected static final String SETTINGS_PREF_TAG = "com.floromsolutions.fieldservicereport.user.settings";
    protected static final String SETTINGS_PREF_BACKUP_KEY = "floromprefkey";
    protected static final String SETTINGS_DB_BACKUP_KEY = "floromdbkey";
    protected static final String YEARLY_REPORT_INSTANCE = "yearly_report_instance";
    protected static final String MONTHLY_REPORT_INSTANCE = "monthly_report_instance";
    protected static final String DAILY_REPORT_INSTANCE = "daily_report_instance";
    protected static final String SENDING_REPORT_INSTANCE = "sending_report_instance";
    protected static final String MY_RV_LIST = "my_rv_list";
    protected static final String MY_BS_LIST = "my_bs_list";
    protected static final String MY_REPORT_LIST = "my_report_list";
    protected static final String SELECTED_RV = "selected_rv";
    protected static final String SELECTED_BS = "selected_bs";
    protected static final String SELECTED_DATE = "selected_date";
    protected static final String ACTION_BAR_TITLE = "action_bar_title";
    protected static final String FS_REPORT_LABEL = "FSReport";
    protected static final String BACKUP_DIR = "backup";
    protected static final String DB_BACKUP_PRE_NAME = "db_";
    protected static final String SP_BACKUP_PRE_NAME = "sp_";
    protected static final String BACKUP_FILE_EXTENSION = ".fsb";
    private static final String SAVE_BACKUP_TO_GOOGLE_DRIVE = "save_backup_to_google_drive";
    private static final String SAVE_BACKUP_TO_LOCAL_DRIVE = "save_backup_to_local_drive";
    private static final String BACKUP_MY_SETTINGS = "backup_my_settings";
    private static final String SELECTED_EMAIL_FOR_BACKUP = "selected_email_for_backup";
    private static final String LAST_LOCAL_BACKUP = "last_local_backup";
    private static final String LAST_GOOGLE_BACKUP = "last_google_backup";
    private static Context appContext;
    private static SharedPreferences fsSharedPref;
    private static SharedPreferences.Editor sEditor;

    public static void setContext(Context c){
        appContext = c;
        fsSharedPref = appContext.getSharedPreferences(SETTINGS_PREF_TAG, Context.MODE_PRIVATE);
    }

    protected static Context getContext()
    {
        return appContext;
    }

    protected static String getUserName(){
        return fsSharedPref.getString(USER_NAME, FSUtils.getDefaultEmail());
    }

    protected static boolean getAutoCallValue(){
        return fsSharedPref.getBoolean(ALLOW_AUTO_CALL, false);
    }

    protected static boolean getAutoMessageValue(){
        return fsSharedPref.getBoolean(ALLOW_AUTO_MESSAGE, false);
    }

    protected static boolean getSendReportToGroupOverseer(){
        return fsSharedPref.getBoolean(SEND_TO_GROUP_OVERSEER, false);
    }

    protected static boolean getIncludeSalutation(){
        return fsSharedPref.getBoolean(INCLUDE_SALUTATION, false);
    }

    protected static String getGroupOverseerName(){
        return fsSharedPref.getString(GROUP_OVERSEER_NAME, "");
    }

    protected static String getGroupOverseerNumber(){
        return fsSharedPref.getString(GROUP_OVERSEER_NUMBER, "");
    }

    protected static String getGroupOverseerEmail(){
        return fsSharedPref.getString(GROUP_OVERSEER_EMAIL, "");
    }

    protected static String getSecretaryName(){
        return fsSharedPref.getString(SECRETARY_NAME, "");
    }

    protected static String getSecretaryNumber(){
        return fsSharedPref.getString(SECRETARY_NUMBER, "");
    }

    protected static String getSecretaryEmail(){
        return fsSharedPref.getString(SECRETARY_EMAIL, "");
    }

    protected static String getPreferredLanguage(){
        return fsSharedPref.getString(APPLICATION_LANGUAGE, "en");
    }

    protected static boolean getSaveBackupToGoogleDrive()
    {
        return fsSharedPref.getBoolean(SAVE_BACKUP_TO_GOOGLE_DRIVE, false);
    }

    protected static boolean getSaveBackupToLocalDrive()
    {
        return fsSharedPref.getBoolean(SAVE_BACKUP_TO_LOCAL_DRIVE, true);
    }

    protected static boolean getBackupMySettings()
    {
        return fsSharedPref.getBoolean(BACKUP_MY_SETTINGS, true);
    }

    protected static String getSelectedEmailForBackup(){
        return fsSharedPref.getString(SELECTED_EMAIL_FOR_BACKUP, appContext.getResources().getString(R.string.backup_account_info));
    }

    protected static long getLastLocalBackup()
    {
        return fsSharedPref.getLong(LAST_LOCAL_BACKUP, getAltLastLocalBackup());
    }

    protected static long getLastGoogleBackup()
    {
        return fsSharedPref.getLong(LAST_GOOGLE_BACKUP, 0);
    }

    private static long getAltLastLocalBackup()
    {
        long backupDate = 0;
        try {
            String pathName = Environment.getExternalStorageDirectory() + File.separator + FS_REPORT_LABEL + File.separator + BACKUP_DIR + File.separator;
            File backupDir = new File(pathName);
            if (backupDir.exists()) {
                File[] files = backupDir.listFiles();
                if (files.length > 0) {
                    String fileName = files[0].getName();
                    String backupId = fileName.replaceAll("[^0-9]", "");
                    backupDate = Long.parseLong(backupId);
                }
            }
        }
        catch (Exception e){
            Log.d("Backup Error", e.getMessage());
        }
        return backupDate;
    }

    protected static void setUserName(String dUserName)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putString(USER_NAME, dUserName);
        sEditor.apply();
    }

    protected static void setAllowAutoCall (boolean checked){
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(ALLOW_AUTO_CALL, checked);
        sEditor.apply();
    }

    protected static void setAllowAutoMessage (boolean checked){
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(ALLOW_AUTO_MESSAGE, checked);
        sEditor.apply();
    }

    protected static void setIncludeSalutation (boolean checked){
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(INCLUDE_SALUTATION, checked);
        sEditor.apply();
    }

    protected static void setSecretaryName (String secretaryName){
        sEditor = fsSharedPref.edit();
        sEditor.putString(SECRETARY_NAME, secretaryName);
        sEditor.apply();
    }

    protected static void setSecretaryNumber (String secretaryNumber){
        sEditor = fsSharedPref.edit();
        sEditor.putString(SECRETARY_NUMBER, secretaryNumber);
        sEditor.apply();
    }

    protected static void setSecretaryEmail (String secretaryEmail){
        sEditor = fsSharedPref.edit();
        sEditor.putString(SECRETARY_EMAIL, secretaryEmail);
        sEditor.apply();
    }

    protected static void setSendToGroupOverseer (boolean checked){
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(SEND_TO_GROUP_OVERSEER, checked);
        sEditor.apply();
    }

    protected static void setGroupOverseerName (String groupOverseerName){
        sEditor = fsSharedPref.edit();
        sEditor.putString(GROUP_OVERSEER_NAME, groupOverseerName);
        sEditor.apply();
    }

    protected static void setGroupOverseerNumber (String groupOverseerNumber){
        sEditor = fsSharedPref.edit();
        sEditor.putString(GROUP_OVERSEER_NUMBER, groupOverseerNumber);
        sEditor.apply();
    }

    protected static void setGroupOverseerEmail (String groupOverseerEmail){
        sEditor = fsSharedPref.edit();
        sEditor.putString(GROUP_OVERSEER_EMAIL, groupOverseerEmail);
        sEditor.apply();
    }

    protected static void setApplicationLanguage (String applicationLanguage){
        sEditor = fsSharedPref.edit();
        sEditor.putString(APPLICATION_LANGUAGE, applicationLanguage);
        sEditor.apply();
    }

    protected static void setSaveBackupToGoogleDrive(boolean checked)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(SAVE_BACKUP_TO_GOOGLE_DRIVE, checked);
        sEditor.apply();
    }

    protected static void setSaveBackupToLocalDrive(boolean checked)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(SAVE_BACKUP_TO_LOCAL_DRIVE, checked);
        sEditor.apply();
    }

    protected static void setBackupMySettings(boolean checked)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putBoolean(BACKUP_MY_SETTINGS, checked);
        sEditor.apply();
    }

    protected static void setSelectedEmailForBackup(String selectedEmailForBackup)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putString(SELECTED_EMAIL_FOR_BACKUP, selectedEmailForBackup);
        sEditor.apply();
    }

    protected static void setLastLocalBackup(long lastLocalBackup)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putLong(LAST_LOCAL_BACKUP, lastLocalBackup);
        sEditor.apply();
    }

    protected static void setLastOnlineBackup(long lastOnlineBackup)
    {
        sEditor = fsSharedPref.edit();
        sEditor.putLong(LAST_GOOGLE_BACKUP, lastOnlineBackup);
        sEditor.apply();
    }
}
