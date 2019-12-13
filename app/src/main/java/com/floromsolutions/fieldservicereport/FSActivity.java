package com.floromsolutions.fieldservicereport;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FSActivity extends AppCompatActivity {
    protected FSDatabaseHelper databaseHelper;
    protected ArrayList<ReturnVisit> myReturnVisits;
    protected ArrayList<ReturnVisit> myBibleStudies;
    protected ArrayList<MyReport> myReportList;
    protected NavigationView navView;
    protected DrawerLayout drawerLayout;
    protected CharSequence actionBarTitle = "Home";
    protected int acbTitleResId;
    private static final int EXIT_INTERVAL = 2000;
    private long fBackPressed;
    protected FragmentManager fm;
    protected int titleResId;
    protected long selectedDate;
    protected String selectedDateInString;
    protected SparseIntArray menuItemToLayoutMap;
    protected SparseIntArray layoutToTitleMap;
    protected ActionBar actionBar;
    protected TextView dateSelector;
    protected ReturnVisit selectedReturnVisit;
    protected ReturnVisit selectedBibleStudy;
    protected MyReport selectedYearlyReport;
    protected MyReport selectedMonthlyReport;
    protected MyReport selectedDailyReport;
    protected MyReport reportToSend;
    protected boolean replaceFromBackPressed = false;
    protected Intent callIntent;
    protected String phoneNumberToCall;
    protected BroadcastReceiver messageSent;
    protected String messageToSend;
    private SmsManager smsManager;
    protected String phoneNoToSendMessage;
    protected ArrayList<String> messageParts;
    protected ArrayList<PendingIntent> sentPis;
    protected String myMessage;
    protected static boolean activityRecreatedFromSettings = false;
    BackupRestoreDialog backupRestoreDialog;

    @Override
    public void onBackPressed() {
        if (fm == null) {
            fm = getSupportFragmentManager();
        }
        /*
        else if(actionBar != null && actionBar.getTitle() == getResources().getString(R.string.home_label)){
            ExitApplication();
        }*/
        int managerSize = fm.getBackStackEntryCount();
        if (drawerLayout.isDrawerOpen(navView)) {
            drawerLayout.closeDrawer(navView);
        } else if (managerSize > 1) {
            int prevIndex = managerSize - 2;
            String lastFragTag = fm.getBackStackEntryAt(prevIndex).getName();
            Fragment frag = fm.findFragmentByTag(lastFragTag);
            if (frag == null) {
                ExitApplication();
            } else {
                replaceFromBackPressed = true;
                boolean hasPopped = replaceFragment(frag);
                if (!hasPopped) {
                    super.onBackPressed();
                    ExitApplication();
                }
            }
        } else {
            ExitApplication();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if(MySettings.getContext() == null)
        {
            MySettings.setContext(newBase);
        }
        String prefLanguage = MySettings.getPreferredLanguage();
        Context newContext = FSUtils.changeFSLanguage(newBase, prefLanguage);
        MySettings.setContext(newContext);
        super.attachBaseContext(newContext);
    }

    private void ExitApplication() {
        if (fBackPressed + EXIT_INTERVAL > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_LONG).show();
        }
        fBackPressed = System.currentTimeMillis();
    }

    protected void setMyActionBarTitle(int titleResId) {
        actionBarTitle = getResources().getString(titleResId);
        if (actionBar != null) {
            actionBar.setTitle(actionBarTitle);
        } else {
            if (getSupportActionBar() != null) {
                actionBar = getSupportActionBar();
                actionBar.setTitle(actionBarTitle);
            }
        }
    }

    protected void setMyActionBarTitle(CharSequence title) {
        actionBarTitle = title;
        if (actionBar != null) {
            actionBar.setTitle(actionBarTitle);
        } else {
            if (getSupportActionBar() != null) {
                actionBar = getSupportActionBar();
                actionBar.setTitle(actionBarTitle);
            }
        }
    }

    protected void showView(View myView, boolean value) {
        if (value) {
            if (!isViewVisible(myView)) {
                myView.setVisibility(View.VISIBLE);
            }
        } else {
            if (isViewVisible(myView)) {
                myView.setVisibility(View.GONE);
            }
        }
    }

    protected boolean isViewVisible(View v) {
        return (v.getVisibility() == View.VISIBLE);
    }

    private void refreshSpinner(Spinner s, final String[] myEmailList)
    {
        if(myEmailList.length > 0 && !TextUtils.isEmpty(myEmailList[0])) {
            try
            {
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MySettings.getContext(), R.layout.string_item, myEmailList);

                s.setAdapter(spinnerAdapter);

                if(s.getId() == R.id.acc_info){
                    int selIndex = getIndexOf(myEmailList, MySettings.getSelectedEmailForBackup());
                    s.setSelection(selIndex);

                    s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            MySettings.setSelectedEmailForBackup(myEmailList[i]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
            } catch (Exception ex) {
                Log.d("Error", ex.getMessage());
            }
        }
        else {
            Toast.makeText(MySettings.getContext(), "No content for email", Toast.LENGTH_LONG).show();
        }
    }

    public static int getIndexOf(String[] strings, String string) {
        for (int i = 0; i < strings.length; i++) {
            if (string.equals(strings[i])) {
                return i;
            }
        }
        return 0;
    }

    protected void loadRvListView(String searchString) {
        String whereCondition;
        String[] args;

        if (searchString == null) {
            whereCondition = String.format("%s = ? ", FSDatabaseHelper.RV_CATEGORY_COLUMN);
            args = new String[]{"rv"};
        } else {
            searchString = "%" + searchString + "%";
            whereCondition = String.format("%s = ? AND ( %s LIKE ? OR %s LIKE ? )",
                    FSDatabaseHelper.RV_CATEGORY_COLUMN, FSDatabaseHelper.RV_NAME_COLUMN, FSDatabaseHelper.RV_ADDRESS_COLUMN);
            args = new String[]{"rv", searchString, searchString};
        }

        myReturnVisits = databaseHelper.getAllRV(whereCondition, args);
        final ListView rvListView = findViewById(R.id.rv_list);
        if (rvListView != null) {
            ArrayAdapter<ReturnVisit> rvListAdapter =
                    new ArrayAdapter<ReturnVisit>(this, R.layout.rv_list_item, myReturnVisits) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.rv_list_item, parent, false);
                            }
                            TextView rvName = convertView.findViewById(R.id.item_rv_name);
                            rvName.setText(myReturnVisits.get(position).dName);

                            TextView rvAddress = convertView.findViewById(R.id.item_rv_address);
                            rvAddress.setText(myReturnVisits.get(position).dAddress);

                            CircularTextView rvLabel = (CircularTextView) convertView.findViewById(R.id.item_rv_label);
                            rvLabel.setText(myReturnVisits.get(position).dName.substring(0, 1).toUpperCase());
                            return convertView;
                        }
                    };
            rvListView.setAdapter(rvListAdapter);

            rvListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //open fragment for rv detail here
                    selectedReturnVisit = myReturnVisits.get(i);

                    int frag_layout_id = R.layout.fragment_view_rv_details;
                    Fragment f = FSFragment.newInstance(frag_layout_id, R.string.view_rv_label, R.id.rv_drawer);
                    replaceFragment(f);
                }
            });
        }
    }

    protected void loadYearlyReportList() {
        myReportList = databaseHelper.getAllYearlyAverage();
        ListView reportListView = findViewById(R.id.report_list);
        if (reportListView != null) {
            ArrayAdapter<MyReport> reportListAdapter =
                    new ArrayAdapter<MyReport>(this, R.layout.report_list_item, myReportList) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.report_list_item, null);
                            }
                            TextView itemHeader = convertView.findViewById(R.id.report_item_header);
                            String header = myReportList.get(position).dDate.substring(0, 4) + " - " + getResources().getString(R.string.yearly_item_header);
                            itemHeader.setText(header);

                            TextView hourV = convertView.findViewById(R.id.txt_hour);
                            hourV.setText(String.valueOf(myReportList.get(position).dHourSpent));

                            TextView plcV = convertView.findViewById(R.id.txt_plc);
                            plcV.setText(String.valueOf(myReportList.get(position).dPlacement));

                            TextView videoV = convertView.findViewById(R.id.txt_video);
                            videoV.setText(String.valueOf(myReportList.get(position).dVideoShowing));

                            TextView rvV = convertView.findViewById(R.id.txt_rv);
                            rvV.setText(String.valueOf(myReportList.get(position).dNumOfRV));

                            TextView bsV = convertView.findViewById(R.id.txt_bs);
                            bsV.setText(String.valueOf(myReportList.get(position).dNumOfBS));

                            return convertView;
                        }
                    };
            reportListView.setAdapter(reportListAdapter);

            reportListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //open fragment for rv detail here

                    selectedYearlyReport = myReportList.get(i);

                    int frag_layout_id = R.layout.fragment_view_all_month_report;
                    Fragment f = FSFragment.newInstance(frag_layout_id, R.string.monthly_r_title, R.id.my_report_drawer);
                    replaceFragment(f);
                }
            });
        }
    }

    protected void loadAllMonthlyReportList() {
        String dYear;
        try {
            dYear = selectedYearlyReport.dDate.substring(0, 4);
        }
        catch (Exception e)
        {
            dYear = String.valueOf(FSUtils.getCurrentYear());
        }

        try {
            String acTitle = getResources().getString(R.string.monthly_r_title) + " " + dYear;
            setMyActionBarTitle(acTitle);
        }catch (Exception ex)
        {
            Log.d("Error", ex.getMessage());
        }

        myReportList = databaseHelper.getAnnualMonthlyReports(dYear);
        ListView reportListView = findViewById(R.id.report_list);
        if (reportListView != null) {
            ArrayAdapter<MyReport> reportListAdapter =
                    new ArrayAdapter<MyReport>(this, R.layout.report_list_item, myReportList) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.report_list_item, null);
                            }
                            TextView itemHeader = convertView.findViewById(R.id.report_item_header);
                            int monthNum = Integer.parseInt(myReportList.get(position).dDate.substring(4, 6));
                            String month = FSUtils.getMonthName(monthNum);
                            String header = getResources().getString(R.string.monthly_item_header) + " " + month + " " + myReportList.get(position).dDate.substring(0, 4);
                            itemHeader.setText(header);

                            TextView hourV = convertView.findViewById(R.id.txt_hour);
                            hourV.setText(String.valueOf((int)myReportList.get(position).dHourSpent));

                            TextView plcV = convertView.findViewById(R.id.txt_plc);
                            plcV.setText(String.valueOf((int)myReportList.get(position).dPlacement));

                            TextView videoV = convertView.findViewById(R.id.txt_video);
                            videoV.setText(String.valueOf((int)myReportList.get(position).dVideoShowing));

                            TextView rvV = convertView.findViewById(R.id.txt_rv);
                            rvV.setText(String.valueOf((int)myReportList.get(position).dNumOfRV));

                            TextView bsV = convertView.findViewById(R.id.txt_bs);
                            bsV.setText(String.valueOf((int)myReportList.get(position).dNumOfBS));

                            return convertView;
                        }
                    };
            reportListView.setAdapter(reportListAdapter);

            reportListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    selectedMonthlyReport = myReportList.get(i);

                    int frag_layout_id = R.layout.fragment_view_month_report_details;
                    Fragment f = FSFragment.newInstance(frag_layout_id, R.string.monthly_details_r_title, R.id.my_report_drawer);
                    replaceFragment(f);
                }
            });
        }
    }

    protected void loadMonthlyDetailsReportList() {
        String dYear;

        try {
            dYear = selectedMonthlyReport.dDate.substring(0, 4);
        }
        catch (Exception e)
        {
            dYear = String.valueOf(FSUtils.getCurrentYear());
        }

        try {
            String acTitle = getResources().getString(R.string.monthly_details_r_title) + " " + FSUtils.getFormattedMonthYear(selectedMonthlyReport.dDate);
            setMyActionBarTitle(acTitle);
        }catch (Exception ex)
        {
            Log.d("Error", ex.getMessage());
        }

        myReportList = databaseHelper.getMonthlyReports(selectedMonthlyReport.dDate.substring(0, 6));
        MyReport overView = databaseHelper.getMonthlyReport(dYear, selectedMonthlyReport.dDate.substring(4,6));
        reportToSend = overView;

        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(FSUtils.dateInLongFromString(selectedMonthlyReport.dDate)));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        Calendar currDay = Calendar.getInstance();
        String currMonth = padLeadingZero(currDay.get(Calendar.MONTH));
        String currYear = padLeadingZero(currDay.get(Calendar.YEAR));

        String headerText;
        if (!(currYear.equals(rYear) && currMonth.equals(rMonth))) {
            String myFormat = "MMMM, yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
            headerText = getResources().getString(R.string.overview_label) + " " + sdf.format(reportDay.getTime());
        } else {
            headerText = getResources().getString(R.string.overview_label_this_month);
        }

        ((TextView) findViewById(R.id.m_rp_header)).setText(headerText);

        ((TextView) findViewById(R.id.m_h_text)).setText(String.valueOf((int)overView.dHourSpent));
        ((TextView) findViewById(R.id.m_vs_text)).setText(String.valueOf((int)overView.dVideoShowing));
        ((TextView) findViewById(R.id.m_p_text)).setText(String.valueOf((int)overView.dPlacement));
        ((TextView) findViewById(R.id.m_rv_text)).setText(String.valueOf((int)overView.dNumOfRV));
        ((TextView) findViewById(R.id.m_bs_text)).setText(String.valueOf((int)overView.dNumOfBS));

        ListView reportListView = findViewById(R.id.report_list);
        if (reportListView != null) {
            ArrayAdapter<MyReport> reportListAdapter =
                    new ArrayAdapter<MyReport>(this, R.layout.report_list_item, myReportList) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.report_list_item, null);
                            }
                            TextView itemHeader = convertView.findViewById(R.id.report_item_header);
                            String header = FSUtils.getFormattedVDate(myReportList.get(position).dDate, Locale.getDefault());
                            itemHeader.setText(header);

                            TextView hourV = convertView.findViewById(R.id.txt_hour);
                            hourV.setText(String.valueOf((int)myReportList.get(position).dHourSpent));

                            TextView plcV = convertView.findViewById(R.id.txt_plc);
                            plcV.setText(String.valueOf((int)myReportList.get(position).dPlacement));

                            TextView videoV = convertView.findViewById(R.id.txt_video);
                            videoV.setText(String.valueOf((int)myReportList.get(position).dVideoShowing));

                            TextView rvV = convertView.findViewById(R.id.txt_rv);
                            rvV.setText(String.valueOf((int)myReportList.get(position).dNumOfRV));

                            TextView bsV = convertView.findViewById(R.id.txt_bs);
                            bsV.setText(String.valueOf((int)myReportList.get(position).dNumOfBS));

                            return convertView;
                        }
                    };
            reportListView.setAdapter(reportListAdapter);

            reportListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //open fragment for rv detail here

                    selectedDailyReport = myReportList.get(i);

                    int frag_layout_id = R.layout.fragment_edit_report;
                    Fragment f = FSFragment.newInstance(frag_layout_id, R.string.edit_report, R.id.my_report_drawer);
                    replaceFragment(f);
                }
            });
        }
    }

    protected void loadBsListView(String searchString) {
        String whereCondition;
        String[] args;

        if (searchString == null) {
            whereCondition = String.format("%s = ? ", FSDatabaseHelper.RV_CATEGORY_COLUMN);
            args = new String[]{"bs"};
        } else {
            searchString = "%" + searchString + "%";
            whereCondition = String.format("%s = ? AND ( %s LIKE ? OR %s LIKE ? )",
                    FSDatabaseHelper.RV_CATEGORY_COLUMN, FSDatabaseHelper.RV_NAME_COLUMN, FSDatabaseHelper.RV_ADDRESS_COLUMN);
            args = new String[]{"bs", searchString, searchString};
        }

        myBibleStudies = databaseHelper.getAllRV(whereCondition, args);
        ListView bsListView = findViewById(R.id.bs_list);
        if (bsListView != null) {
            ArrayAdapter<ReturnVisit> bsListAdapter =
                    new ArrayAdapter<ReturnVisit>(this, R.layout.rv_list_item, myBibleStudies) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.rv_list_item, null);
                            }
                            TextView bsName = convertView.findViewById(R.id.item_rv_name);
                            bsName.setText(myBibleStudies.get(position).dName);

                            TextView bsAddress = convertView.findViewById(R.id.item_rv_address);
                            bsAddress.setText(myBibleStudies.get(position).dAddress);

                            CircularTextView bsLabel = convertView.findViewById(R.id.item_rv_label);
                            bsLabel.setText(myBibleStudies.get(position).dName.substring(0, 1).toUpperCase());
                            return convertView;
                        }
                    };
            bsListView.setAdapter(bsListAdapter);

            bsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //open fragment for rv detail here
                    selectedBibleStudy = myBibleStudies.get(i);

                    int frag_layout_id = R.layout.fragment_view_bs_details;
                    Fragment f = FSFragment.newInstance(frag_layout_id, R.string.view_bs_label, R.id.bs_drawer);
                    replaceFragment(f);
                }
            });
        }
    }

    protected String padLeadingZero(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return Integer.toString(value);
        }
    }

    protected String formattedTime(int h, int m) {
        String amPm = " AM";
        if (h > 12) {
            h -= 12;
            amPm = " PM";
        } else if (h == 12) {
            amPm = " PM";
        }
        return padLeadingZero(h) + ":" + padLeadingZero(m) + amPm;
    }

    protected long generateId() {
        return System.currentTimeMillis();
    }

    protected void refreshHomeView(long dSelectedDate){
        MyReport dailyReport = databaseHelper.getDailyReport(dSelectedDate);
        if(dailyReport.dHourSpent != 0){
            ((EditText)findViewById(R.id.input_my_h)).setText(String.valueOf((int)dailyReport.dHourSpent));
        }
        else {
            ((EditText)findViewById(R.id.input_my_h)).setText(String.valueOf(""));
        }

        if(dailyReport.dVideoShowing != 0){
            ((EditText)findViewById(R.id.input_my_vs)).setText(String.valueOf((int)dailyReport.dVideoShowing));
        }
        else {
            ((EditText)findViewById(R.id.input_my_vs)).setText(String.valueOf(""));
        }

        if(dailyReport.dPlacement != 0){
            ((EditText)findViewById(R.id.input_my_plc)).setText(String.valueOf((int)dailyReport.dPlacement));
        }
        else {
            ((EditText)findViewById(R.id.input_my_plc)).setText(String.valueOf(""));
        }

        if(dailyReport.dHourSpent == 0 && dailyReport.dVideoShowing == 0 && dailyReport.dPlacement == 0 &&
                dailyReport.dNumOfBS == 0 && dailyReport.dNumOfRV == 0)
        {
            // delete empty report
            databaseHelper.deleteReport(dailyReport);
        }

        ((TextView)findViewById(R.id.input_my_rv)).setText(String.valueOf((int)dailyReport.dNumOfRV));
        ((TextView)findViewById(R.id.input_my_bs)).setText(String.valueOf((int)dailyReport.dNumOfBS));

        //still do for monthly overview here
        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(dSelectedDate));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        MyReport monthlyReport = databaseHelper.getMonthlyReport(rYear, rMonth);
        reportToSend = monthlyReport;
        Calendar currDay = Calendar.getInstance();
        String currMonth = padLeadingZero(currDay.get(Calendar.MONTH));
        String currYear = padLeadingZero(currDay.get(Calendar.YEAR));

        String headerText;
        if (!(currYear.equals(rYear) && currMonth.equals(rMonth))) {
            String myFormat = "MMMM, yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
            headerText = getResources().getString(R.string.overview_label) + " " + sdf.format(reportDay.getTime());
        } else {
            headerText = getResources().getString(R.string.overview_label_this_month);
        }

        ((TextView) findViewById(R.id.m_rp_header)).setText(headerText);

        ((TextView) findViewById(R.id.m_h_text)).setText(String.valueOf((int)monthlyReport.dHourSpent));
        ((TextView) findViewById(R.id.m_vs_text)).setText(String.valueOf((int)monthlyReport.dVideoShowing));
        ((TextView) findViewById(R.id.m_p_text)).setText(String.valueOf((int)monthlyReport.dPlacement));
        ((TextView) findViewById(R.id.m_rv_text)).setText(String.valueOf((int)monthlyReport.dNumOfRV));
        ((TextView) findViewById(R.id.m_bs_text)).setText(String.valueOf((int)monthlyReport.dNumOfBS));
    }

    protected void confirmUpdateRVToBS(Context context, final ReturnVisit rVisit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (rVisit != null) {
            builder.setTitle("Confirm Update");
            String mess = "Are you sure to update " + rVisit.dName + " as a Bible Student?";
            builder.setMessage(mess);

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //update rv here
                    rVisit.dCategory = "bs";
                    long result = databaseHelper.insertOrUpdateExistingRV(rVisit);
                    if (result > -1) { //update has been done
                        prepareBSListView(); //refresh the list view
                    }
                    dialogInterface.dismiss();
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    protected void confirmDeleteRVOrBS(Context context, final ReturnVisit dRV) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (dRV != null) {
            builder.setTitle("Confirm Delete");
            String mess = "Are you sure to delete " + dRV.dName + " permanently?";
            builder.setMessage(mess);

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //update rv here
                    long result = databaseHelper.deleteRV(dRV);
                    if (result > -1) { //delete has been done
                        onBackPressed();
                    }
                    dialogInterface.dismiss();
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    private void setRVSearchQueryListener() {
        SearchView rvSearch = findViewById(R.id.search_my_rv);
        if (rvSearch != null) {
            rvSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadRvListView(s);
                    } else {
                        loadRvListView(null);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadRvListView(s);
                    } else {
                        loadRvListView(null);
                    }
                    return true;
                }
            });
        }
    }

    private void setBSSearchQueryListener() {
        SearchView bsSearch = findViewById(R.id.search_my_bs);
        if (bsSearch != null) {
            bsSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadBsListView(s);
                    } else {
                        loadBsListView(null);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadBsListView(s);
                    } else {
                        loadBsListView(null);
                    }
                    return true;
                }
            });
        }
    }

    protected boolean replaceFragment(Fragment fragment) {
        int titleResourceId = fragment.getArguments().getInt(FSFragment.ARG_ACB_TITLE_RES_ID);
        int fsFragMenuId = fragment.getArguments().getInt(FSFragment.ARG_MENU_ID);
        int fsFragLayoutId = fragment.getArguments().getInt(FSFragment.ARG_LAYOUT);
        String dFragId = "frag_" + String.valueOf(fsFragLayoutId);

        if (fm == null) {
            fm = getSupportFragmentManager();
        }

        setMyActionBarTitle(titleResourceId);

        if (drawerLayout.isDrawerOpen(navView)) {
            drawerLayout.closeDrawer(navView);
        }

        boolean fragmentPopped = fm.popBackStackImmediate(dFragId, 0);

        if (!fragmentPopped) {
            if (replaceFromBackPressed) {
                replaceFromBackPressed = false;//return to default
                return false;
            } else {
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.content_frame, fragment, dFragId).addToBackStack(dFragId).commit();
            }
        }
        replaceFromBackPressed = false; //return to default
        navView.getMenu().findItem(fsFragMenuId).setChecked(true);
        return true;
    }

    protected void callSomeone(Context context, ReturnVisit rv) {
        boolean allowAutoCall = MySettings.getAutoCallValue();
        phoneNumberToCall = "tel:" + rv.dPhone;

        if (allowAutoCall) {
            Intent newCallIntent = new Intent(Intent.ACTION_CALL);
            newCallIntent.setData(Uri.parse(phoneNumberToCall));
            this.callIntent = newCallIntent;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                selfPermissionRequest(MySettings.CALL_REQ_CODE);
            }
            else {
                try {
                    context.startActivity(newCallIntent);
                }
                catch (SecurityException ex) {
                    //fall back to manual call
                    manualCall(phoneNumberToCall);
                }
            }
        }
        else {
            // fall back to manual call
            manualCall(phoneNumberToCall);
        }
    }

    protected void messageSomeone(final Context context, final String dPhoneNumber, final String dMessage){
        boolean allowAutoMessage = MySettings.getAutoMessageValue();
        phoneNoToSendMessage = dPhoneNumber;
        myMessage = dMessage;

        if (allowAutoMessage && (!(dMessage == null || dMessage.trim().equals("")))) {
            //if message is not empty and autoMessage is set, this can only happen for report sending
            sendSMS(context, dPhoneNumber, dMessage);
        }
        else {
            // fall back to manual message
            sendManualSMS(dPhoneNumber, dMessage);
        }
    }

    protected void mailSomeone(Context c, String dEmail, String dSubject, String dBody)
    {
        if(dEmail.startsWith("mailto:"))
        {
            dEmail = dEmail.split(":")[1];
        }

        Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
        mailIntent.setType("message/rfc822");
        mailIntent.setData(Uri.parse("mailto:"));
        mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{dEmail});
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, dSubject);
        mailIntent.putExtra(Intent.EXTRA_TEXT, dBody);

        try{
            startActivity(Intent.createChooser(mailIntent, c.getResources().getString(R.string.send_using)));
        }
        catch (ActivityNotFoundException e){
            Toast.makeText(c, "E-mail application not found", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void selfPermissionRequest(int code)
    {
        switch (code) {
            case MySettings.CALL_REQ_CODE:
                //do for call here
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, MySettings.CALL_REQ_CODE);
                }
                else {
                    startActivity(callIntent);
                }
                break;
            case MySettings.MESSAGE_REQ_CODE:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, MySettings.MESSAGE_REQ_CODE);
                }
                else {
                    smsManager.sendMultipartTextMessage(phoneNoToSendMessage, null, messageParts, sentPis, null);
                }
                break;
            case MySettings.READ_WRITE_DRIVE_REQ_CODE:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    try {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MySettings.READ_WRITE_DRIVE_REQ_CODE);
                    }catch (Exception ex){
                        Log.d("Error:", ex.getMessage());
                    }
                }
                break;
            default:
                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MySettings.CALL_REQ_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    try {
                        startActivity(callIntent);
                    }catch (SecurityException ex){
                        manualCall(phoneNumberToCall);
                    }
                }
                else {
                    // fall back to manual call
                    manualCall(phoneNumberToCall);
                }
                break;
            case MySettings.MESSAGE_REQ_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    try {
                        smsManager.sendMultipartTextMessage(phoneNoToSendMessage, null, messageParts, sentPis, null);
                    }catch (SecurityException ex){
                        sendManualSMS(phoneNoToSendMessage, myMessage);
                    }
                }
                else {
                    // fall back to manual message
                    sendManualSMS(phoneNoToSendMessage, myMessage);
                }
                break;
            case MySettings.GET_ACCOUNTS_REQ_CODE:
                String[] myEmails;
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    myEmails = FSUtils.getAccountsName();
                }
                else {
                    // fall back to manual message;
                    myEmails = new String[]{getResources().getString(R.string.backup_account_info)};
                }
                Spinner dSpinner = findViewById(R.id.acc_info);
                try {
                    refreshSpinner(dSpinner, myEmails);
                }catch (Exception ex){
                    Log.d("Unknown Error", ex.getMessage());
                }

                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void manualCall(String phoneN){
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse(phoneN));
        startActivity(callIntent);
    }

    private void sendSMS(Context context, String pNumber, String dMess){
        smsManager = SmsManager.getDefault();
        messageParts = smsManager.divideMessage((dMess));
        int count = messageParts.size();

        sentPis = new ArrayList<>(count);

        for(int i = 0; i < count; i++){
            PendingIntent piSent = PendingIntent.getBroadcast(context, i, new Intent(MySettings.ACTION_SMS_SENT), 0);
            sentPis.add(piSent);
        }

        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                selfPermissionRequest(MySettings.MESSAGE_REQ_CODE);
            }
            else {
                smsManager.sendMultipartTextMessage(pNumber, null, messageParts, sentPis, null);
            }
        }
        catch (Exception ex){
            sendManualSMS(pNumber, dMess);
        }
    }

    private void sendManualSMS(String pNumber, String messageBody){
        //set data here
        String urL = "smsto:" + pNumber;
        Uri url = Uri.parse(urL);
        Intent messageIntent = new Intent(Intent.ACTION_SENDTO, url);

        if(messageBody != null && (!messageBody.trim().equals("")))
        {
            messageIntent.putExtra("sms_body", messageBody);
        }

        startActivity(messageIntent);
    }

    private String prepareReportMessage(MyReport dReport)
    {
        boolean includeSalutation = MySettings.getIncludeSalutation();
        String myName = MySettings.getUserName();

        String message = FSUtils.getFormattedMonthYear(dReport.dDate) + " Field Service Report";
        message += "\n Hours: " + (int)dReport.dHourSpent;
        message += "\n Placement: " + (int)dReport.dPlacement;
        message += "\n Video: " + (int)dReport.dVideoShowing;
        message += "\n RV: " + (int)dReport.dNumOfRV;
        message += "\n BS: " + (int)dReport.dNumOfBS;
        message += "\n ";

        if(!TextUtils.isEmpty(myName)){
            message += "\nFrom " + myName;
        }

        if(includeSalutation){
            message += "\n" + FSUtils.getGreeting();
        }
        return message;
    }

    protected void sendReport(final Context c, MyReport report){
        final String rMessage = prepareReportMessage(report);
        final String rNumber;
        boolean sendToGroupOverseer = MySettings.getSendReportToGroupOverseer();
        boolean allowAutoSend = MySettings.getAutoMessageValue();
        if(sendToGroupOverseer){
            rNumber = MySettings.getGroupOverseerNumber();
        }else {
            rNumber = MySettings.getSecretaryNumber();
        }
        if(report.dHourSpent > 0) {
            if (!rNumber.equals("") && allowAutoSend) {
                AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle("Confirm Send");
                String mess = "Are you sure you want to send your field service report for "+ FSUtils.getFormattedMonthYear(report.dDate) + " ?";
                builder.setMessage(mess);

                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        messageSomeone(c, rNumber, rMessage);
                        dialogInterface.dismiss();
                    }
                });

                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                sendManualSMS(rNumber, rMessage);
            }
        }else {
            Toast.makeText(MySettings.getContext(), getResources().getString(R.string.empty_report_text), Toast.LENGTH_LONG).show();
        }
    }

    protected void emailReport(Context c, MyReport report)
    {
        if(report.dHourSpent > 0) {
            String rBody = prepareReportMessage(report);
            String rEmail;
            boolean sendToGroupOverseer = MySettings.getSendReportToGroupOverseer();
            if (sendToGroupOverseer) {
                rEmail = MySettings.getGroupOverseerEmail();
            } else {
                rEmail = MySettings.getSecretaryEmail();
            }
            String rSubject = FSUtils.getFormattedMonthYear(report.dDate) + " Field Service Report";
            mailSomeone(c, rEmail, rSubject, rBody);
        }else {
            Toast.makeText(MySettings.getContext(), getResources().getString(R.string.empty_report_text), Toast.LENGTH_LONG).show();
        }
    }

    protected void shareReport(MyReport report){
        if(report.dHourSpent > 0) {
            String rMessage = prepareReportMessage(report);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Field Service Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, rMessage);
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_using)));
        }else {
            Toast.makeText(MySettings.getContext(), getResources().getString(R.string.empty_report_text), Toast.LENGTH_LONG).show();
        }
    }

    protected void prepareBroadCastReceiver(Context context){
        messageSent = SmsResultReceiver.getInstance(context);
        registerReceiver(messageSent, new IntentFilter(MySettings.ACTION_SMS_SENT));
    }

    protected void prepareGeneralFragmentSettings(int layoutId){
        showView(dateSelector, (layoutId == R.layout.fragment_home));
    }

    protected void prepareHomeView(){
        TextView reportHeader = findViewById(R.id.enter_report_header);
        if(reportHeader != null && selectedDateInString != null){
            String text = getResources().getString(R.string.home_info_label) + " " + selectedDateInString;
            reportHeader.setText(text);
        }
        refreshHomeView(selectedDate);
    }

    protected void prepareRVListView(){
        View empty_rv_view = findViewById(R.id.empty_rv_list);

        if (empty_rv_view != null) {
            ListView list = findViewById(R.id.rv_list);
            if (list != null) {
                list.setEmptyView(empty_rv_view);
            }
        }

        loadRvListView(null);
        setRVSearchQueryListener();
    }

    protected void prepareBSListView(){
        View empty_bs_view = findViewById(R.id.empty_bs_list);

        if (empty_bs_view != null) {
            ListView list = findViewById(R.id.bs_list);
            if (list != null) {
                list.setEmptyView(empty_bs_view);
            }
        }

        loadBsListView(null);
        setBSSearchQueryListener();
    }

    protected void prepareRVDetailsView(ReturnVisit rV){
        int selDay;
        try {
            selDay = Integer.valueOf(rV.dVisitDay);
        }
        catch (Exception e)
        {
            selDay = getIndexOf(getResources().getStringArray(R.array.days_of_week), rV.dVisitDay);
        }

        ((TextView) findViewById(R.id.text_name)).setText(rV.dName);
        ((TextView) findViewById(R.id.text_address)).setText(rV.dAddress);
        ((TextView) findViewById(R.id.text_phone)).setText(rV.dPhone);
        ((TextView) findViewById(R.id.text_v_day)).setText(getResources().getStringArray(R.array.days_of_week)[selDay]);
        ((TextView) findViewById(R.id.text_v_time)).setText(rV.dVisitTime);
        ((TextView) findViewById(R.id.text_l_disc)).setText(rV.dLDisc);
        ((TextView) findViewById(R.id.text_n_disc)).setText(rV.dNDisc);
        ((TextView) findViewById(R.id.text_d_l_v)).setText(rV.dLastVisit);

        Button callBtn = findViewById(R.id.call_btn);
        Button messageBtn = findViewById(R.id.message_btn);

        boolean show = !(String.valueOf(rV.dPhone) == null || String.valueOf(rV.dPhone).trim().equals(""));

        showView(callBtn, show);
        showView(messageBtn, show);
    }

    protected void prepareBSDetailsView(ReturnVisit bS){
        prepareRVDetailsView(bS);
    }

    protected void prepareRVEditView(ReturnVisit rv){
        int selDay;
        try {
               selDay = Integer.valueOf(rv.dVisitDay);
        }
        catch (Exception e)
        {
            selDay =getIndexOf(getResources().getStringArray(R.array.days_of_week), rv.dVisitDay);
        }
        ((EditText)findViewById(R.id.edit_rv_name)).setText(rv.dName);
        ((EditText)findViewById(R.id.edit_rv_address)).setText(rv.dAddress);
        ((EditText)findViewById(R.id.edit_rv_phone)).setText(rv.dPhone);
        ((Spinner)findViewById(R.id.visit_days)).setSelection(selDay);
        ((TextView)findViewById(R.id.visit_time)).setText(rv.dVisitTime);
    }

    protected void prepareBSEditView(ReturnVisit bs){
        int selDay;
        try {
            selDay = Integer.valueOf(bs.dVisitDay);
        }
        catch (Exception e)
        {
            selDay =getIndexOf(getResources().getStringArray(R.array.days_of_week), bs.dVisitDay);
        }
        ((EditText)findViewById(R.id.edit_bs_name)).setText(bs.dName);
        ((EditText)findViewById(R.id.edit_bs_address)).setText(bs.dAddress);
        ((EditText)findViewById(R.id.edit_bs_phone)).setText(bs.dPhone);
        ((Spinner)findViewById(R.id.visit_days)).setSelection(selDay);
        ((TextView)findViewById(R.id.visit_time)).setText(bs.dVisitTime);
    }

    protected void prepareAllMyYearlyReport()
    {
        View empty_rv_view = findViewById(R.id.empty_report_list);

        if (empty_rv_view != null) {
            ListView list = findViewById(R.id.report_list);
            if (list != null) {
                list.setEmptyView(empty_rv_view);
            }
        }
        loadYearlyReportList();
    }

    protected void prepareAllMyMonthlyReport()
    {
        View empty_rv_view = findViewById(R.id.empty_report_list);

        if (empty_rv_view != null) {
            ListView list = findViewById(R.id.report_list);
            if (list != null) {
                list.setEmptyView(empty_rv_view);
            }
        }
        loadAllMonthlyReportList();
    }

    protected void prepareMonthlyDetailsReport()
    {
        View empty_rv_view = findViewById(R.id.empty_report_list);

        if (empty_rv_view != null) {
            ListView list = findViewById(R.id.report_list);
            if (list != null) {
                list.setEmptyView(empty_rv_view);
            }
        }
        loadMonthlyDetailsReportList();
    }

    protected void prepareEditReport(){
        long selectedDateToEdit = FSUtils.dateInLongFromString(selectedDailyReport.dDate);
        TextView reportHeader = findViewById(R.id.enter_report_header);
        if(reportHeader != null && selectedDateInString != null){
            String text = getResources().getString(R.string.home_info_label) + " " + FSUtils.getFormattedVDate(selectedDateToEdit, Locale.getDefault());
            reportHeader.setText(text);
        }
        String acTitle = getResources().getString(R.string.edit_report) + " " + FSUtils.getFormattedVDate(selectedDateToEdit, Locale.getDefault()); //

        try {
            setMyActionBarTitle(acTitle);
        }catch (Exception ex)
        {
            Log.d("Error", ex.getMessage());
        }

        try {
            refreshHomeView(selectedDateToEdit);
        }catch (Exception e){
            Log.d("Error", e.getMessage());
        }
    }

    protected void prepareSettingsView(){
        EditText usnTxb = findViewById(R.id.ent_user_name);
        usnTxb.setText(MySettings.getUserName());

        final CheckBox allowAutoCall = findViewById(R.id.auto_call);
        allowAutoCall.setChecked(MySettings.getAutoCallValue());

        TextView allowAutoCallLabel = findViewById(R.id.auto_call_l);
        allowAutoCallLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allowAutoCall.performClick();
            }
        });

        final CheckBox allowAutoMessage = findViewById(R.id.auto_message);
        allowAutoMessage.setChecked(MySettings.getAutoMessageValue());

        TextView allowAutoMessageLabel = findViewById(R.id.auto_message_l);
        allowAutoMessageLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allowAutoMessage.performClick();
            }
        });

        final CheckBox includeSalutation = findViewById(R.id.add_salutation);
        includeSalutation.setChecked(MySettings.getIncludeSalutation());

        TextView includeSalutationLabel = findViewById(R.id.add_salutation_l);
        includeSalutationLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                includeSalutation.performClick();
            }
        });

        final CheckBox sendReportToSec = findViewById(R.id.report_to_sec);
        sendReportToSec.setChecked(!MySettings.getSendReportToGroupOverseer());

        TextView sendReportToSecLabel = findViewById(R.id.report_to_sec_l);
        sendReportToSecLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendReportToSec.performClick();
            }
        });

        final CheckBox sendReportToGO = findViewById(R.id.report_to_g_o);
        sendReportToGO.setChecked(MySettings.getSendReportToGroupOverseer());

        TextView sendReportToGOLabel = findViewById(R.id.report_to_g_o_l);
        sendReportToGOLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendReportToGO.performClick();
            }
        });

        sendReportToSec.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(sendReportToGO.isChecked() == b){
                    sendReportToGO.setChecked(!b);
                }
            }
        });

        sendReportToGO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(sendReportToSec.isChecked() == b){
                    sendReportToSec.setChecked(!b);
                }
            }
        });

        EditText entSecName = findViewById(R.id.ent_sec_name);
        entSecName.setText(MySettings.getSecretaryName());

        EditText entSecNumber = findViewById(R.id.ent_sec_phone);
        entSecNumber.setText(MySettings.getSecretaryNumber());

        EditText entSecEmail = findViewById(R.id.ent_sec_email);
        entSecEmail.setText(MySettings.getSecretaryEmail());

        EditText entGOName = findViewById(R.id.ent_g_o_name);
        entGOName.setText(MySettings.getGroupOverseerName());

        EditText entGONumber = findViewById(R.id.ent_g_o_phone);
        entGONumber.setText(MySettings.getGroupOverseerNumber());

        EditText entGOEmail = findViewById(R.id.ent_g_o_email);
        entGOEmail.setText(MySettings.getGroupOverseerEmail());
    }

    protected void saveMySettings(){
        String userName = ((EditText)findViewById(R.id.ent_user_name)).getText().toString();
        MySettings.setUserName(userName);

        boolean allowAutoCall = ((CheckBox)findViewById(R.id.auto_call)).isChecked();
        MySettings.setAllowAutoCall(allowAutoCall);

        boolean allowAutoMessage = ((CheckBox)findViewById(R.id.auto_message)).isChecked();
        MySettings.setAllowAutoMessage(allowAutoMessage);

        boolean includeSalutation = ((CheckBox)findViewById(R.id.add_salutation)).isChecked();
        MySettings.setIncludeSalutation(includeSalutation);

        String secName = ((EditText)findViewById(R.id.ent_sec_name)).getText().toString();
        MySettings.setSecretaryName(secName);

        String secPhone = ((EditText)findViewById(R.id.ent_sec_phone)).getText().toString();
        MySettings.setSecretaryNumber(secPhone);

        String secEmail = ((EditText)findViewById(R.id.ent_sec_email)).getText().toString();
        MySettings.setSecretaryEmail(secEmail);

        boolean sendToGO = ((CheckBox)findViewById(R.id.report_to_g_o)).isChecked();
        MySettings.setSendToGroupOverseer(sendToGO);

        String gOName = ((EditText)findViewById(R.id.ent_g_o_name)).getText().toString();
        MySettings.setGroupOverseerName(gOName);

        String gOPhone = ((EditText)findViewById(R.id.ent_g_o_phone)).getText().toString();
        MySettings.setGroupOverseerNumber(gOPhone);

        String gOEmail = ((EditText)findViewById(R.id.ent_g_o_email)).getText().toString();
        MySettings.setGroupOverseerEmail(gOEmail);
    }

    protected void prepareLanguageView(final Activity a)
    {
        final String[] availableLanguages = a.getResources().getStringArray(R.array.available_languages_text);
        final ListView langListView = findViewById(R.id.language_list);
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<String>(a, R.layout.language_list_item , availableLanguages){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.language_list_item, null);
                }
                TextView lName = convertView.findViewById(R.id.lang_name);
                lName.setText(availableLanguages[position]);

                CheckBox lItem = convertView.findViewById(R.id.lang_item);
                String currLang = MySettings.getPreferredLanguage();
                if((a.getResources().getStringArray(R.array.available_languages_value)[position]).equals(currLang))
                {
                    //check the language
                    lItem.setChecked(true);
                }
                return convertView;
            }
        };

        langListView.setAdapter(languageAdapter);

        langListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String dSelectedLang = (a.getResources().getStringArray(R.array.available_languages_value))[i];
                for (int c = 0; c < langListView.getChildCount(); c ++)
                {
                    if(c != i) {
                        ((CheckBox) langListView.getChildAt(c).findViewById(R.id.lang_item)).setChecked(false);
                    }
                    else {
                        ((CheckBox) langListView.getChildAt(c).findViewById(R.id.lang_item)).setChecked(true);
                    }
                }
                MySettings.setApplicationLanguage(dSelectedLang);
                activityRecreatedFromSettings = true;
                acbTitleResId = R.string.lang_label;
                a.recreate();
            }
        });
    }

    protected void prepareBackupRestore(){
        CheckBox saveToLocal = findViewById(R.id.to_local);
        boolean localTrue = MySettings.getSaveBackupToLocalDrive();
        saveToLocal.setChecked(localTrue);
        saveToLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MySettings.setSaveBackupToLocalDrive(b);
            }
        });

        CheckBox saveToGoogle = findViewById(R.id.to_online);
        boolean googleTrue = MySettings.getSaveBackupToGoogleDrive();
        saveToGoogle.setChecked(googleTrue);
        saveToGoogle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MySettings.setSaveBackupToGoogleDrive(b);
            }
        });

        CheckBox backupMySettings = findViewById(R.id.backup_settings_too);
        boolean settingsTrue = MySettings.getBackupMySettings();
        backupMySettings.setChecked(settingsTrue);
        backupMySettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MySettings.setBackupMySettings(b);
            }
        });

        Spinner accountInfo = findViewById(R.id.acc_info);
        //get Emails and load it to spinner here
        try {
            refreshSpinner(accountInfo, FSUtils.getAllEmailAccount());
        }catch (Exception ex){
            Log.d("Unknown Error", ex.getMessage());
        }

        //get last local backup here
        long lastLocalBackup = MySettings.getLastLocalBackup();
        if(lastLocalBackup != 0)
        {
            TextView lastLocalDate = findViewById(R.id.last_local_backup);
            lastLocalDate.setText(FSUtils.getFormattedDateAndTime(lastLocalBackup, Locale.getDefault()));
        }
        //get last google backup here
        long lastGoogleBackup = MySettings.getLastGoogleBackup();
        if(lastGoogleBackup != 0)
        {
            TextView lastGoogleDate = findViewById(R.id.last_gg_backup);
            lastGoogleDate.setText(FSUtils.getFormattedDateAndTime(lastGoogleBackup, Locale.getDefault()));
        }
    }

    protected void prepareAboutView()
    {
        try {
            WebView webView = findViewById(R.id.app_info);
            webView.loadDataWithBaseURL(null, getResources().getString(R.string.application_info),"text/html", "UTF-8", null);
            webView.setWebViewClient(new FSWebViewClient());
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    protected void saveMyInstance(Bundle dInstance){
        dInstance.putParcelable(MySettings.YEARLY_REPORT_INSTANCE, selectedYearlyReport);
        dInstance.putParcelable(MySettings.MONTHLY_REPORT_INSTANCE, selectedMonthlyReport);
        dInstance.putParcelable(MySettings.DAILY_REPORT_INSTANCE, selectedDailyReport);
        dInstance.putParcelable(MySettings.SENDING_REPORT_INSTANCE, reportToSend);

        dInstance.putParcelable(MySettings.SELECTED_RV, selectedReturnVisit);
        dInstance.putParcelable(MySettings.SELECTED_BS, selectedBibleStudy);

        dInstance.putParcelableArrayList(MySettings.MY_RV_LIST, myReturnVisits);
        dInstance.putParcelableArrayList(MySettings.MY_BS_LIST, myBibleStudies);
        dInstance.putParcelableArrayList(MySettings.MY_REPORT_LIST, myReportList);

        dInstance.putInt(MySettings.ACTION_BAR_TITLE, acbTitleResId);
        dInstance.putLong(MySettings.SELECTED_DATE, selectedDate);
    }

    protected void getMyInstance(Bundle savedInstance){
        selectedYearlyReport = savedInstance.getParcelable(MySettings.YEARLY_REPORT_INSTANCE);
        selectedMonthlyReport = savedInstance.getParcelable(MySettings.MONTHLY_REPORT_INSTANCE);
        selectedDailyReport = savedInstance.getParcelable(MySettings.DAILY_REPORT_INSTANCE);
        reportToSend = savedInstance.getParcelable(MySettings.SENDING_REPORT_INSTANCE);

        selectedReturnVisit = savedInstance.getParcelable(MySettings.SELECTED_RV);
        selectedBibleStudy = savedInstance.getParcelable(MySettings.SELECTED_BS);

        myReturnVisits = savedInstance.getParcelableArrayList(MySettings.MY_RV_LIST);
        myBibleStudies = savedInstance.getParcelableArrayList(MySettings.MY_BS_LIST);
        myReportList = savedInstance.getParcelableArrayList(MySettings.MY_REPORT_LIST);

        acbTitleResId = savedInstance.getInt(MySettings.ACTION_BAR_TITLE);
        try {
            setMyActionBarTitle(acbTitleResId);
        }catch (Exception ex){
            Log.d("Error", ex.getMessage());
        }

        selectedDate = savedInstance.getLong(MySettings.SELECTED_DATE);
        selectedDateInString = FSUtils.getFormattedVDate(selectedDate, Locale.getDefault());
    }

    public class FSWebViewClient extends WebViewClient
    {
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            mailSomeone(MySettings.getContext(), url, "", "");
            return true;
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            mailSomeone(MySettings.getContext(), url, "", "");
            return true;
        }
    }
}
