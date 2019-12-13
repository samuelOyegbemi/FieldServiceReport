package com.floromsolutions.fieldservicereport;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Patterns;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class FSUtils {
    private static Context appContext;
    @SuppressWarnings("deprecation")
    public static ContextWrapper changeFSLanguage(Context context, String langCode){
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale myLocale;

        if(!langCode.equals("")){
             myLocale = new Locale(langCode);
            Locale.setDefault(myLocale);
        }
        else {
            myLocale = Locale.getDefault();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            config.setLocale(myLocale);
        }
        else {
            config.locale = myLocale;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            context = context.createConfigurationContext(config);
        }
        else {
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
        appContext = context;
        return new ContextWrapper(context);
    }

    public static String dateInStringFromLong(long d){

        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(d));
        String rDay = padLeadingZero(reportDay.get(Calendar.DAY_OF_MONTH));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        return (rYear + rMonth + rDay);
    }

    public static long dateInLongFromString(String dDate){

        if(dDate.length() == 8) {
            int y = Integer.parseInt(dDate.substring(0, 4));
            int m = Integer.parseInt(dDate.substring(4, 6));
            int d = Integer.parseInt(dDate.substring(6, 8));
            Calendar reportDay = Calendar.getInstance();
            reportDay.set(Calendar.YEAR, y);
            reportDay.set(Calendar.MONTH, m);
            reportDay.set(Calendar.DAY_OF_MONTH, d);

            return reportDay.getTimeInMillis();
        }
        else {
            return 0;
        }
    }

    private static String padLeadingZero(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return Integer.toString(value);
        }
    }

    private static boolean  isYesterday(long dDate){
        Calendar todayDate = Calendar.getInstance();
        todayDate.add(Calendar.DAY_OF_MONTH, -1); //Turning today to a logical yesterday
        Calendar yesterdayDate = Calendar.getInstance();
        yesterdayDate.setTime(new Date(dDate));
        return (todayDate.get(Calendar.YEAR) == yesterdayDate.get(Calendar.YEAR)) &&
                (todayDate.get(Calendar.MONTH) == yesterdayDate.get(Calendar.MONTH)) &&
                (todayDate.get(Calendar.DAY_OF_MONTH) == yesterdayDate.get(Calendar.DAY_OF_MONTH));
    }

    public static double roundDouble(double dValue, int precision){
        int scale = (int) Math.pow(10, precision);
        return ((double)Math.round(dValue * scale) / scale);
    }

    public static String getFormattedVDate(String vDate, Locale dLocale){
        return getFormattedVDate(dateInLongFromString(vDate), dLocale);
    }

    public static String getFormattedVDate(long vDate, Locale dLocale){
        String formattedDate;
        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(vDate));
        String myFormat = "EEE, MMM d, yyyy";
        Locale localeToUse;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            localeToUse = dLocale;
        } else {
            localeToUse = Locale.US;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, localeToUse);

        if(DateUtils.isToday(vDate))
        {
            try {
                formattedDate = appContext.getResources().getString(R.string.today);
            }
            catch (Exception e){
                formattedDate = "today";
            }
        }
        else if(FSUtils.isYesterday(vDate)){
            try {
                formattedDate = appContext.getResources().getString(R.string.yesterday);
            }
            catch (Exception e){
                formattedDate = "yesterday";
            }
        }
        else {
            formattedDate = sdf.format(reportDay.getTime());
        }
        return formattedDate;
    }

    public static int getMonthCount(String fromDate){
        int monthCount = 12;
        if(fromDate.length() == 8) {
            int y = Integer.parseInt(fromDate.substring(0, 4));
            int m = Integer.parseInt(fromDate.substring(4, 6));
            int d = Integer.parseInt(fromDate.substring(6, 8));
            Calendar reportDay = Calendar.getInstance();
            reportDay.set(Calendar.YEAR, y);
            reportDay.set(Calendar.MONTH, m);
            reportDay.set(Calendar.DAY_OF_MONTH, d);

            Calendar now = Calendar.getInstance();
            int currYear = now.get(Calendar.YEAR);

            if(y == currYear){
                monthCount = now.get(Calendar.MONTH) + 1 ;
            }
        }
        return monthCount;
    }

    public static int getCurrentYear(){
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR);
    }

    public static String getMonthName(int monthNum){
        //monthNum -= 1;
        String dResult;
        try {
            dResult = DateFormatSymbols.getInstance(Locale.getDefault()).getMonths()[monthNum];
        }
        catch (Exception e){
            dResult = DateFormatSymbols.getInstance(Locale.getDefault()).getMonths()[0];
        }
        return dResult;
    }

    public static String getFormattedMonthYear(String dDate)
    {
        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(dateInLongFromString(dDate)));
        String myFormat = "MMMM yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        return sdf.format(reportDay.getTime());
    }

    public static String getFormattedDateAndTime(long dTimeStamp, Locale dLocale)
    {
        String formattedDate;
        Calendar dDate = Calendar.getInstance();
        dDate.setTime(new Date(dTimeStamp));
        String dateFormat = "EEE, MMM d, yyyy";
        Locale localeToUse;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            localeToUse = dLocale;
        } else {
            localeToUse = Locale.US;
        }

        String timeFormat = "hh:mm a";
        SimpleDateFormat dateSdf = new SimpleDateFormat(dateFormat, localeToUse);
        SimpleDateFormat timeSdf = new SimpleDateFormat(timeFormat, Locale.US);

        if(DateUtils.isToday(dTimeStamp))
        {
            try {
                formattedDate = appContext.getResources().getString(R.string.today);
            }
            catch (Exception e){
                formattedDate = "Today";
            }
        }
        else if(FSUtils.isYesterday(dTimeStamp)){
            try {
                formattedDate = appContext.getResources().getString(R.string.yesterday);
            }
            catch (Exception e){
                formattedDate = "Yesterday";
            }
        }
        else {
            formattedDate = dateSdf.format(dDate.getTime());
        }

        formattedDate += "  " + timeSdf.format(dDate.getTime());
        return formattedDate;
    }

    public static String getGreeting(){
        String result = "Good ";
        Calendar now = Calendar.getInstance();
        int hh = now.get(Calendar.HOUR_OF_DAY);

        if(hh >=0 && hh < 12){
            result += "Morning Sir.";
        }
        else if(hh >= 12 && hh < 18){
            result += "Afternoon Sir.";
        }
        else {
            result += "Evening Sir.";
        }

        return result;
    }

    public static String getDefaultEmail(){
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        try
        {
            Account [] accounts = AccountManager.get(appContext).getAccountsByType("com.google");
            for (Account account : accounts){
                if(emailPattern.matcher(account.name).matches()){
                    return account.name;
                }
            }
        }catch(Exception ex)
        {
            Log.d("Error", ex.getMessage());
        }
        return "";
    }

    public static String[] getAllEmailAccount(){
        String[] results;
        try
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                results = selfPermissionRequest(MySettings.GET_ACCOUNTS_REQ_CODE);
            }
            else {
                results = getAccountsName();
            }

        }catch(Exception ex)
        {
            Log.d("Error", ex.getMessage());
            results = new String[]{appContext.getResources().getString(R.string.backup_account_info)};
        }
        return results;
    }

    protected static String[] getAccountsName()
    {
        String[] returnString;
        try {
            Account [] accounts = AccountManager.get(appContext).getAccountsByType("com.google");
            ArrayList<String> emailList = new ArrayList<>();
            emailList.add(appContext.getResources().getString(R.string.backup_account_info));
            Pattern emailPattern = Patterns.EMAIL_ADDRESS;
            for (Account account : accounts){
                if(emailPattern.matcher(account.name).matches()){
                    emailList.add(account.name);
                }
            }
            returnString = new String[emailList.size()];
            emailList.toArray(returnString);
        }
        catch (Exception ex)
        {
            returnString = new String[]{appContext.getResources().getString(R.string.backup_account_info)};
        }
        return returnString;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static String[] selfPermissionRequest(int code)
    {
        switch (code) {
            case MySettings.GET_ACCOUNTS_REQ_CODE:
                //do for call here
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    ((Activity)appContext).requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, MySettings.GET_ACCOUNTS_REQ_CODE);
                }
                else {
                    return getAccountsName();
                }
                break;
            case MySettings.READ_WRITE_DRIVE_REQ_CODE:
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(appContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    try {
                        ((Activity) appContext).requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MySettings.READ_WRITE_DRIVE_REQ_CODE);
                    }catch (ClassCastException cE)
                    {
                        Log.d("Error:", cE.getMessage());
                    }catch (Exception ex){
                        Log.d("Error:", ex.getMessage());
                    }
                }
                break;
            default:
                break;

        }
        return new String[]{};
    }
}
