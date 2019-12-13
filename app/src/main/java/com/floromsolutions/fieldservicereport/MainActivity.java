package com.floromsolutions.fieldservicereport;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends FSActivity implements FSFragment.OnCompleteListener,
        SelectRVDialog.OnDialogFinishListener, SmsResultReceiver.OnSMSSentListener,
        VisitDetailsDialog.OnVisitDialogFinishListener, ShowVisitRecordDialog.OnDialogFinishListener {
    Calendar myCalendar;
    DatePickerDialog.OnDateSetListener dateSetListener;

    @Override
    public void onFragmentReady(int layoutId) {
        prepareContentByLayoutId(layoutId);
    }

    @Override
    public void onFragmentPaused(int layoutId) {
        switch (layoutId)
        {
            case R.layout.fragment_settings:
                saveMySettings();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDialogItemSelected(SelectRVDialog dDialog, ReturnVisit dSelectedRV, int refererLayoutId) {
        dDialog.dismiss(); //close the dialog
        switch (refererLayoutId)
        {
            case R.layout.fragment_view_bs:
                confirmUpdateRVToBS(MainActivity.this, dSelectedRV);
                break;
            case R.layout.fragment_home:
                VisitRecord newVisit = new VisitRecord(dSelectedRV, selectedDate);
                //create new visit record dialog here
                VisitDetailsDialog vDialog = VisitDetailsDialog.getInstance(MainActivity.this, dSelectedRV, newVisit, refererLayoutId);
                vDialog.show();
                break;
            case R.layout.fragment_edit_report:
                VisitRecord editedVisit = new VisitRecord(dSelectedRV, FSUtils.dateInLongFromString(selectedDailyReport.dDate));
                //create new visit record dialog here
                VisitDetailsDialog editedVDialog = VisitDetailsDialog.getInstance(MainActivity.this, dSelectedRV, editedVisit, refererLayoutId);
                editedVDialog.show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRecordDetailsSubmission(VisitDetailsDialog d, VisitRecord vr, int refererId) {
        d.dismiss(); //close dialog
        //save the visit record here
        long id = databaseHelper.insertORUpdateVisitRecord(vr);
        if(id > -1){
            switch (refererId)
            {
                case R.layout.fragment_home:
                    refreshHomeView(selectedDate);
                    break;
                case R.layout.fragment_edit_report:
                    refreshHomeView(FSUtils.dateInLongFromString(selectedDailyReport.dDate));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onVisitRecordDialogClose(long dSelectedDate) {
        refreshHomeView(dSelectedDate);
    }

    @Override
    public void onSMSSent() {
        // do something when report is sent automatically
        Toast.makeText(MainActivity.this, "Report Sent Successfully", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSMSDecline(String errorMessage) {
        // do something when report can not be sent due to a specific error message provided in the errorMessage argument
        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //assign context to settings
        MySettings.setContext(MainActivity.this);
        setContentView(R.layout.activity_main);

        databaseHelper = FSDatabaseHelper.getInstance(this);

        configureToolBar();
        configureNavigationDrawer();
        configureDateSelector();
        prepareMenuToLayoutMap();
        prepareLayoutToTitleMap();
        prepareBroadCastReceiver(MainActivity.this);

        if (savedInstanceState == null) {
            selectFSMenuItem(navView.getMenu().getItem(0));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveMyInstance(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        getMyInstance(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(messageSent != null) {
            unregisterReceiver(messageSent);
        }
    }

    private void configureDateSelector(){
        selectedDate = System.currentTimeMillis();
        selectedDateInString = getResources().getString(R.string.today);
        dateSelector = findViewById(R.id.select_date);
        myCalendar = Calendar.getInstance();

        dateSetListener = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                myCalendar.set(Calendar.YEAR, y);
                myCalendar.set(Calendar.MONTH, m);
                myCalendar.set(Calendar.DAY_OF_MONTH, d);
                updateDateSelector();
                refreshHomeView(selectedDate);
            }
        };

        dateSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog d = new DatePickerDialog(MainActivity.this, R.style.MyDatePickerDialogTheme, dateSetListener, myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));
                d.getDatePicker().setMaxDate(new Date().getTime());
                d.show();
            }
        });
    }

    private void updateDateSelector(){
        selectedDate = myCalendar.getTimeInMillis();
        selectedDateInString = FSUtils.getFormattedVDate(selectedDate, Locale.getDefault());

        dateSelector.setText(selectedDateInString);

        TextView reportHeader = findViewById(R.id.enter_report_header);
        if(reportHeader != null && selectedDateInString != null){
            String text = getResources().getString(R.string.home_info_label) + " " + selectedDateInString;
            reportHeader.setText(text);
        }
    }

    private void  configureToolBar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    private void configureNavigationDrawer(){
        navView = findViewById(R.id.navigation_drawer);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        return selectFSMenuItem(menuItem);
                    }
                });
    }

    private boolean selectFSMenuItem(MenuItem menuItem)
    {
        int itemId = menuItem.getItemId();
        int frag_layout_id;
        frag_layout_id = menuItemToLayoutMap.get(itemId);
        titleResId = layoutToTitleMap.get(frag_layout_id);
        Fragment f = FSFragment.newInstance(frag_layout_id, titleResId, itemId);
        drawerLayout.closeDrawers();
        replaceFragment(f);
        return true;
    }

    public void prepareContentByLayoutId(int layoutId)
    {
        prepareGeneralFragmentSettings(layoutId);
        switch (layoutId){
            case R.layout.fragment_home:
                prepareHomeView();
                break;
            case R.layout.fragment_view_rv:
                prepareRVListView();
                break;
            case R.layout.fragment_view_rv_details:
                prepareRVDetailsView(selectedReturnVisit);
                break;
            case R.layout.fragment_edit_rv:
                prepareRVEditView(selectedReturnVisit);
                break;
            case R.layout.fragment_view_bs:
                prepareBSListView();
                break;
            case R.layout.fragment_view_bs_details:
                prepareBSDetailsView(selectedBibleStudy);
                break;
            case R.layout.fragment_edit_bs:
                prepareBSEditView(selectedBibleStudy);
                break;
            case R.layout.fragment_view_my_report:
                prepareAllMyYearlyReport();
                break;
            case R.layout.fragment_view_all_month_report:
                prepareAllMyMonthlyReport();
                break;
            case R.layout.fragment_view_month_report_details:
                prepareMonthlyDetailsReport();
                break;
            case R.layout.fragment_edit_report:
                prepareEditReport();
                break;
            case R.layout.fragment_settings:
                prepareSettingsView();
                break;
            case R.layout.fragment_select_language:
                prepareLanguageView(MainActivity.this);
                break;
            case R.layout.fragment_backup_restore:
                prepareBackupRestore();
                break;
            case R.layout.fragment_about:
                prepareAboutView();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        switch (itemId)
        {
            case android.R.id.home:
                //open navigation drawer
                drawerLayout.openDrawer(Gravity.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareMenuToLayoutMap(){
        menuItemToLayoutMap = new SparseIntArray();
        menuItemToLayoutMap.put(R.id.home_drawer, R.layout.fragment_home);
        menuItemToLayoutMap.put(R.id.rv_drawer, R.layout.fragment_view_rv);
        menuItemToLayoutMap.put(R.id.bs_drawer, R.layout.fragment_view_bs);
        menuItemToLayoutMap.put(R.id.my_report_drawer, R.layout.fragment_view_my_report);
        menuItemToLayoutMap.put(R.id.settings_drawer, R.layout.fragment_settings);
    }

    private void prepareLayoutToTitleMap(){
        layoutToTitleMap = new SparseIntArray();
        layoutToTitleMap.put(R.layout.fragment_home, R.string.home_label);
        layoutToTitleMap.put(R.layout.fragment_view_rv, R.string.rv_label);
        layoutToTitleMap.put(R.layout.fragment_view_bs, R.string.bs_label);
        layoutToTitleMap.put(R.layout.fragment_view_my_report, R.string.my_report_label);
        layoutToTitleMap.put(R.layout.fragment_settings, R.string.settings_label);
    }

    public void showNewRVBtn(View v)
    {
        int frag_layout_id = R.layout.fragment_add_rv;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.add_rv_label, R.id.rv_drawer);
        replaceFragment(f);
    }

    public void popRVListForBSUpdate(View v)
    {
        //show rv list as dialog
        SelectRVDialog rvDialog = SelectRVDialog.getInstance(MainActivity.this, R.layout.fragment_view_bs, "rv");
        rvDialog.show();
    }

    public void showEditRvBtn(View v)
    {
        int frag_layout_id = R.layout.fragment_edit_rv;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.edit_rv_label, R.id.rv_drawer);
        replaceFragment(f);
    }

    public void showEditBsBtn(View v)
    {
        int frag_layout_id = R.layout.fragment_edit_bs;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.edit_bs_label, R.id.bs_drawer);
        replaceFragment(f);
    }

    public void saveRVBtn(View v)// do this when user click save for rv
    {
        EditText edtName = findViewById(R.id.ent_rv_name);
        String newRVName = edtName.getText().toString();
        if (!TextUtils.isEmpty(newRVName)) {
            try {
                ReturnVisit newRv = new ReturnVisit(generateId(), newRVName);

                EditText edtAddress = findViewById(R.id.ent_rv_address);
                EditText edtPhone = findViewById(R.id.ent_rv_phone);
                Spinner edtDays = findViewById(R.id.visit_days);
                TextView edtTime = findViewById(R.id.visit_time);
                EditText edtLDisc = findViewById(R.id.ent_rv_l_disc);
                EditText edtNDisc = findViewById(R.id.ent_rv_n_disc);

                newRv.dCategory = "rv";
                newRv.dAddress = edtAddress.getText().toString();
                newRv.dPhone = edtPhone.getText().toString();
                newRv.dVisitDay = String.valueOf(edtDays.getSelectedItemPosition());
                newRv.dVisitTime = edtTime.getText().toString();
                newRv.dLDisc = edtLDisc.getText().toString();
                newRv.dNDisc = edtNDisc.getText().toString();
                long id = databaseHelper.insertOrUpdateExistingRV(newRv);

                if (id > -1) {//insert has been done
                    onBackPressed(); // go back
                }
            }catch (Exception ex){
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Please type the Return visit name", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveRVEditBtn(View v)// do this when user click save for rv
    {
        EditText edtName = findViewById(R.id.edit_rv_name);
        String rvName = edtName.getText().toString();
        if (!TextUtils.isEmpty(rvName)) {
            try {
                if(selectedReturnVisit == null) {
                    selectedReturnVisit = new ReturnVisit(generateId(), rvName);
                }

                EditText edtAddress = findViewById(R.id.edit_rv_address);
                EditText edtPhone = findViewById(R.id.edit_rv_phone);
                Spinner edtDays = findViewById(R.id.visit_days);
                TextView edtTime = findViewById(R.id.visit_time);

                selectedReturnVisit.dCategory = "rv";
                selectedReturnVisit.dName = rvName;
                selectedReturnVisit.dAddress = edtAddress.getText().toString();
                selectedReturnVisit.dPhone = edtPhone.getText().toString();
                selectedReturnVisit.dVisitDay = String.valueOf(edtDays.getSelectedItemPosition());
                selectedReturnVisit.dVisitTime = edtTime.getText().toString();

                long id = databaseHelper.insertOrUpdateExistingRV(selectedReturnVisit);

                if(id > -1){//insert has been done
                    onBackPressed(); // go back
                }
            }catch (Exception ex){
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Please type the Return visit name", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveBSEditBtn(View v)// do this when user click save for rv
    {
        EditText edtName = findViewById(R.id.edit_bs_name);
        String bsName = edtName.getText().toString();
        if (!TextUtils.isEmpty(bsName)) {
            try {
                if(selectedBibleStudy == null) {
                    selectedBibleStudy = new ReturnVisit(generateId(), bsName);
                }

                EditText edtAddress = findViewById(R.id.edit_bs_address);
                EditText edtPhone = findViewById(R.id.edit_bs_phone);
                Spinner edtDays = findViewById(R.id.visit_days);
                TextView edtTime = findViewById(R.id.visit_time);

                selectedBibleStudy.dCategory = "bs";
                selectedBibleStudy.dName = bsName;
                selectedBibleStudy.dAddress = edtAddress.getText().toString();
                selectedBibleStudy.dPhone = edtPhone.getText().toString();
                selectedBibleStudy.dVisitDay = String.valueOf(edtDays.getSelectedItemPosition());
                selectedBibleStudy.dVisitTime = edtTime.getText().toString();

                long id = databaseHelper.insertOrUpdateExistingRV(selectedBibleStudy);

                if(id > -1){//insert has been done
                    onBackPressed(); // go back
                }
            }catch (Exception ex){
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Please type the Return visit name", Toast.LENGTH_SHORT).show();
        }
    }

    public void addRVVisitRecord(View v){
        //to indicate a visit to a particular return visit This happens by clicking the add rv on adding new report
        //show rv list as dialog
        SelectRVDialog rvDialog = SelectRVDialog.getInstance(MainActivity.this, R.layout.fragment_home, "rv");
        rvDialog.show();
    }

    public void addEditRVVisitRecord(View v){
        //to indicate a visit to a particular return visit This happens by clicking the add rv on adding new report
        //show rv list as dialog
        SelectRVDialog rvDialog = SelectRVDialog.getInstance(MainActivity.this, R.layout.fragment_edit_report, "rv");
        rvDialog.show();
    }

    public void addBSVisitRecord(View v){
        //to indicate a visit to a particular bible student This happens by clicking the add bs on adding new report
        //show bs list as dialog
        SelectRVDialog rvDialog = SelectRVDialog.getInstance(MainActivity.this, R.layout.fragment_home, "bs");
        rvDialog.show();
    }

    public void addEditBSVisitRecord(View v){
        //to indicate a visit to a particular bible student This happens by clicking the add bs on adding new report
        //show bs list as dialog
        SelectRVDialog rvDialog = SelectRVDialog.getInstance(MainActivity.this, R.layout.fragment_edit_report, "bs");
        rvDialog.show();
    }

    public void saveReportBtn(View v)
    {
        // do this when user click save for report
        try {
            MyReport newReport = new MyReport(selectedDate);

            try {
                newReport.dHourSpent = Integer.valueOf(((EditText) findViewById(R.id.input_my_h)).getText().toString());
            }catch (Exception e0)
            {
                newReport.dHourSpent = 0;
            }

            try {
                newReport.dVideoShowing = Integer.valueOf(((EditText)findViewById(R.id.input_my_vs)).getText().toString());
            }catch (Exception e1)
            {
                newReport.dVideoShowing = 0;
            }

            try {
                newReport.dPlacement = Integer.valueOf(((EditText)findViewById(R.id.input_my_plc)).getText().toString());
            }catch (Exception e2)
            {
                newReport.dPlacement = 0;
            }
            if(!(newReport.dHourSpent == 0 && newReport.dPlacement == 0 && newReport.dVideoShowing == 0)) {
                String action = databaseHelper.insertOrUpdateExistingReport(newReport);
                //use android toast to show that the saving is successful
                if (!action.trim().equals("none")) {
                    Toast.makeText(this, action, Toast.LENGTH_LONG).show();
                }
                //refresh the home view so as to reflect changes in the monthly overview
                refreshHomeView(selectedDate);
            }
            else {
                Toast.makeText(this, "Report not saved! please enter at least one of the inputs", Toast.LENGTH_SHORT).show();
            }

        }catch (Exception ex)
        {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void editReportBtn(View v)
    {
        // do this when user click save for report
        try {
            MyReport newReport = new MyReport(FSUtils.dateInLongFromString(selectedDailyReport.dDate));

            try {
                newReport.dHourSpent = Integer.valueOf(((EditText) findViewById(R.id.input_my_h)).getText().toString());
            } catch (Exception e0) {
                newReport.dHourSpent = 0;
            }

            try {
                newReport.dVideoShowing = Integer.valueOf(((EditText) findViewById(R.id.input_my_vs)).getText().toString());
            } catch (Exception e1) {
                newReport.dVideoShowing = 0;
            }

            try {
                newReport.dPlacement = Integer.valueOf(((EditText) findViewById(R.id.input_my_plc)).getText().toString());
            } catch (Exception e2) {
                newReport.dPlacement = 0;
            }

            String action = databaseHelper.insertOrUpdateExistingReport(newReport);
            //use android toast to show that the saving is successful
            if (!action.trim().equals("none")) {
                Toast.makeText(this, action, Toast.LENGTH_LONG).show();
            }
            //refresh the home view so as to reflect changes in the monthly overview
            refreshHomeView(FSUtils.dateInLongFromString(selectedDailyReport.dDate));
        }catch (Exception ex)
        {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteRVBtn(View v)
    {
        confirmDeleteRVOrBS(MainActivity.this, selectedReturnVisit);
    }

    public void deleteBSBtn(View v)
    {
        confirmDeleteRVOrBS(MainActivity.this, selectedBibleStudy);
    }

    public void showRVVisitRecord(View v){
        ShowVisitRecordDialog showVisitRecordDialog = ShowVisitRecordDialog.getInstance(this, "rv", selectedDate);
        showVisitRecordDialog.show();
    }

    public void showEditRVVisitRecord(View v){
        ShowVisitRecordDialog showVisitRecordDialog = ShowVisitRecordDialog.getInstance(this, "rv", FSUtils.dateInLongFromString(selectedDailyReport.dDate));
        showVisitRecordDialog.show();
    }

    public void showBSVisitRecord(View v){
        ShowVisitRecordDialog showVisitRecordDialog = ShowVisitRecordDialog.getInstance(this, "bs", selectedDate);
        showVisitRecordDialog.show();
    }

    public void showEditBSVisitRecord(View v){
        ShowVisitRecordDialog showVisitRecordDialog = ShowVisitRecordDialog.getInstance(this, "bs",  FSUtils.dateInLongFromString(selectedDailyReport.dDate));
        showVisitRecordDialog.show();
    }

    public void callRVBtn(View v)
    {
        //place call here to the current rv
        callSomeone(MainActivity.this, selectedReturnVisit);
    }

    public void callBSBtn(View v){
        //place call here to the current bs
        callSomeone(MainActivity.this, selectedBibleStudy);
    }

    public void messageRVBtn(View v)
    {
        //send message here to the current r
        messageSomeone(MainActivity.this, selectedReturnVisit.dPhone, messageToSend);
    }

    public void messageBSBtn(View v)
    {
        //send message here to the current bs
        messageSomeone(MainActivity.this, selectedBibleStudy.dPhone, messageToSend);
    }

    public void setTimeForRV(View v){ // do this when user click the time for visit
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        final TextView timeLabel = findViewById(R.id.visit_time);
        TimePickerDialog timePickerDialog =
                new TimePickerDialog(MainActivity.this, R.style.MyDatePickerDialogTheme, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selHour, int selMin) {
                        timeLabel.setText(formattedTime(selHour, selMin));
                    }
                }, hour, minute, false);
        timePickerDialog.setTitle(getResources().getString(R.string.time_for_visit));
        timePickerDialog.show();
    }

    public void sendReportBtn(View v){
        sendReport(this, reportToSend);
    }

    public void emailReportBtn(View v){
        emailReport(this, reportToSend);
    }

    public void shareReportBtn(View v){
        shareReport(reportToSend);
    }

    public void openLanguageSettings(View v){
        int frag_layout_id = R.layout.fragment_select_language;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.lang_label, R.id.settings_drawer);
        replaceFragment(f);
    }

    public void openBackupSettings(View v){
        int frag_layout_id = R.layout.fragment_backup_restore;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.backup_restore_label, R.id.settings_drawer);
        replaceFragment(f);
    }

    public void openAboutPage(View v){
        int frag_layout_id = R.layout.fragment_about;
        Fragment f = FSFragment.newInstance(frag_layout_id, R.string.about_label, R.id.settings_drawer);
        replaceFragment(f);
    }

    public void selectEmailBtn(View v)
    {
        Spinner s = findViewById(R.id.acc_info);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            s.callOnClick();
        }
        else {
            s.performClick();
        }
    }

    protected MyReport getReportFromView(String dDate)
    {
        MyReport newReport = new MyReport(dDate);
        try
        {
            try {
                newReport.dHourSpent = Integer.valueOf(((EditText) findViewById(R.id.input_my_h)).getText().toString());
            }catch (Exception e0)
            {
                newReport.dHourSpent = 0;
            }

            try {
                newReport.dVideoShowing = Integer.valueOf(((EditText)findViewById(R.id.input_my_vs)).getText().toString());
            }catch (Exception e1)
            {
                newReport.dVideoShowing = 0;
            }

            try {
                newReport.dPlacement = Integer.valueOf(((EditText)findViewById(R.id.input_my_plc)).getText().toString());
            }catch (Exception e2)
            {
                newReport.dPlacement = 0;
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return newReport;
    }

    public void backupBtn(View v)
    {
        backupRestoreDialog = BackupRestoreDialog.getInstance(this, BackupRestoreDialog.DialogType.BACKUP);
        backupRestoreDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //refresh backup dates here
                //get last local backup here
                try
                {
                    long lastLocalBackup = MySettings.getLastLocalBackup();
                    if (lastLocalBackup != 0) {
                        TextView lastLocalDate = findViewById(R.id.last_local_backup);
                        lastLocalDate.setText(FSUtils.getFormattedDateAndTime(lastLocalBackup, Locale.getDefault()));
                    }
                    //get last google backup here
                    long lastGoogleBackup = MySettings.getLastGoogleBackup();
                    if (lastGoogleBackup != 0) {
                        TextView lastGoogleDate = findViewById(R.id.last_gg_backup);
                        lastGoogleDate.setText(FSUtils.getFormattedDateAndTime(lastGoogleBackup, Locale.getDefault()));
                    }
                }catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        backupRestoreDialog.show();
    }

    public void restoreBtn(View v)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Restore");
            String mess = "Please note that restore is irreversible. Are you sure to restore your backup ?";
            builder.setMessage(mess);
            final Context context = this;
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //update rv here
                    backupRestoreDialog = BackupRestoreDialog.getInstance(context, BackupRestoreDialog.DialogType.RESTORE);
                    backupRestoreDialog.show();
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
