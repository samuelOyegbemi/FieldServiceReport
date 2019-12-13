package com.floromsolutions.fieldservicereport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class FSDatabaseHelper extends SQLiteOpenHelper {
    //Database information here
    private static final String DATABASE_NAME = "FSDatabase.db";
    private static final int DATABASE_VERSION = 1;

    //All table names here
    private static final String RV_TABLE_NAME = "all_return_visit";
    private static final String REPORT_TABLE_NAME = "my_report";
    private static final String VISIT_TABLE_NAME = "visit_record";

    //All return visit table columns here
    private static final String RV_ID_COLUMN = "rv_id";
    protected static final String RV_NAME_COLUMN = "rv_name";
    protected static final String RV_ADDRESS_COLUMN = "rv_address";
    private static final String RV_PHONE_COLUMN = "rv_phone";
    protected static final String RV_CATEGORY_COLUMN = "rv_category";
    private static final String RV_VISIT_DAY_COLUMN = "rv_visit_day";
    private static final String RV_VISIT_TIME_COLUMN = "rv_visit_time";

    //All report table columns here
    private  static final String REPORT_ID_COLUMN = "rp_id";
    private  static final String REPORT_H_COLUMN = "rp_h_spent";
    private  static final String REPORT_VS_COLUMN = "rp_video_showing";
    private  static final String REPORT_PLC_COLUMN = "rp_placement";
    private  static final String REPORT_DATE_COLUMN = "rp_date";

    //All visit record columns here
    private static final String VISIT_ID_COLUMN = "visit_id";
    private static final String VISIT_DISC_COLUMN = "visit_disc";
    private static final String VISIT_N_DISC_COLUMN = "visit_n_disc";
    private static final String VISIT_RV_ID_COLUMN = "visit_rv_id"; // foreign key to rv table's rv id column
    private static final String VISIT_REPORT_DATE_COLUMN = "visit_rp_date"; //foreign key to report table's report date column
    private static final String VISIT_TYPE_COLUMN = "visit_type";

    private static FSDatabaseHelper singletonInstance;

    public static synchronized FSDatabaseHelper getInstance(Context context)
    {
        if(singletonInstance == null){
            singletonInstance = new FSDatabaseHelper(context.getApplicationContext());
        }
        return singletonInstance;
    }

    private FSDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase fsDatabase) {
        super.onConfigure(fsDatabase);
    }

    @Override
    public void onCreate(SQLiteDatabase fsDatabase) {
        String CREATE_RV_TABLE = "CREATE TABLE IF NOT EXISTS " + RV_TABLE_NAME + "(" +
                RV_ID_COLUMN + " integer PRIMARY KEY, " +
                RV_NAME_COLUMN + " text, " +
                RV_ADDRESS_COLUMN + " text, " +
                RV_PHONE_COLUMN + " text, " +
                RV_CATEGORY_COLUMN + " text, " +
                RV_VISIT_DAY_COLUMN + " text, " +
                RV_VISIT_TIME_COLUMN + " text " + ")";

        String CREATE_REPORT_TABLE = "CREATE TABLE IF NOT EXISTS " + REPORT_TABLE_NAME + "(" +
                REPORT_ID_COLUMN + " integer PRIMARY KEY, " +
                REPORT_H_COLUMN + " integer, " +
                REPORT_VS_COLUMN + " integer, " +
                REPORT_PLC_COLUMN + " integer, " +
                REPORT_DATE_COLUMN + " text " + ")";

        String CREATE_VISIT_TABLE = "CREATE TABLE IF NOT EXISTS " + VISIT_TABLE_NAME + "(" +
                VISIT_ID_COLUMN + " integer PRIMARY KEY, " +
                VISIT_DISC_COLUMN + " text, " +
                VISIT_N_DISC_COLUMN + " text, " +
                VISIT_RV_ID_COLUMN + " integer, " +
                VISIT_REPORT_DATE_COLUMN + " text, " +
                VISIT_TYPE_COLUMN + " text, " +
                "FOREIGN KEY (" + VISIT_RV_ID_COLUMN + ") REFERENCES " + RV_TABLE_NAME + " (" + RV_ID_COLUMN + "))";

        fsDatabase.execSQL(CREATE_RV_TABLE);
        fsDatabase.execSQL(CREATE_REPORT_TABLE);
        fsDatabase.execSQL(CREATE_VISIT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase fsDatabase, int i, int i1) {
        fsDatabase.execSQL("DROP TABLE IF EXISTS "+ RV_TABLE_NAME);
        fsDatabase.execSQL("DROP TABLE IF EXISTS "+ REPORT_TABLE_NAME);
        fsDatabase.execSQL("DROP TABLE IF EXISTS "+ VISIT_TABLE_NAME);
        onCreate(fsDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    protected static String getDBName()
    {
        return DATABASE_NAME;
    }

    public long insertOrUpdateExistingRV(ReturnVisit rv){
        SQLiteDatabase fsDatabase = getWritableDatabase();
        long rvId = -1;
        boolean tryInsertVisitRecord = false;
        VisitRecord vr = null;

        try {
            fsDatabase.beginTransaction();
        }
        catch (Exception ebend) {
            Log.d("Error", ebend.getMessage());
        }

        try {
            ContentValues values = new ContentValues();
            values.put(RV_NAME_COLUMN, rv.dName);
            values.put(RV_ADDRESS_COLUMN, rv.dAddress);
            values.put(RV_PHONE_COLUMN, rv.dPhone);
            values.put(RV_CATEGORY_COLUMN, rv.dCategory);
            values.put(RV_VISIT_DAY_COLUMN, rv.dVisitDay);
            values.put(RV_VISIT_TIME_COLUMN, rv.dVisitTime);

            int numOfRows = 0;

            if(rv.dId != 0) {
                numOfRows = fsDatabase.update(
                        RV_TABLE_NAME,
                        values,
                        RV_ID_COLUMN + " = ? ",
                        new String[]{String.valueOf(rv.dId)}
                );
            }

            if(numOfRows > 0){
                rvId = rv.dId;
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
            }
            else {
                values.put(RV_ID_COLUMN, rv.dId);
                rvId = fsDatabase.insertOrThrow(RV_TABLE_NAME, null, values);
                rvId = rv.dId;
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }

                //create last discussion and next discussion for initial call
                vr = new VisitRecord(rv);
                vr.vDisc = rv.dLDisc;
                vr.vNDisc = rv.dNDisc;
                vr.vDate = "00000000"; // so that is will not count this as a return visit
                tryInsertVisitRecord = true;
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            try {
                fsDatabase.endTransaction();
            }
            catch (Exception ebend) {
                Log.d("Error", ebend.getMessage());
            }
        }

        if(tryInsertVisitRecord)
        {
            try {
                insertORUpdateVisitRecord(vr);
            }catch (Exception il)
            {
                il.printStackTrace();
            }
        }
        return rvId;
    }

    public ArrayList <ReturnVisit> getAllRV(@Nullable String whereCondition, @Nullable String[] conditionParams){
        String SELECT_RV_QUERY;
        ArrayList<ReturnVisit> returnVisitList = new ArrayList<>();
        SQLiteDatabase fsDatabase = getReadableDatabase();

        if(whereCondition != null && conditionParams != null && conditionParams.length > 0) {
            SELECT_RV_QUERY = String.format(
                    "SELECT * FROM %s WHERE %s ORDER BY %s ASC",
                    RV_TABLE_NAME, whereCondition, RV_NAME_COLUMN
            );
        }
        else {
            SELECT_RV_QUERY = "SELECT * FROM " + RV_TABLE_NAME + " ORDER BY " + RV_NAME_COLUMN + " ASC";
            conditionParams = null;
        }

        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, conditionParams);
        try {
            if(cursor.moveToFirst()){
                do{
                    ReturnVisit rv = new ReturnVisit("New");
                    rv.dId = cursor.getLong(cursor.getColumnIndex(RV_ID_COLUMN));
                    rv.dName = cursor.getString(cursor.getColumnIndex(RV_NAME_COLUMN));
                    rv.dPhone = cursor.getString(cursor.getColumnIndex(RV_PHONE_COLUMN));
                    rv.dAddress = cursor.getString(cursor.getColumnIndex(RV_ADDRESS_COLUMN));
                    rv.dCategory = cursor.getString(cursor.getColumnIndex(RV_CATEGORY_COLUMN));
                    rv.dVisitDay = cursor.getString(cursor.getColumnIndex(RV_VISIT_DAY_COLUMN));
                    rv.dVisitTime = cursor.getString(cursor.getColumnIndex(RV_VISIT_TIME_COLUMN));

                    VisitRecord vRec = getLastVisit(rv);
                    rv.dLDisc = vRec.vDisc;
                    rv.dNDisc = vRec.vNDisc;
                    rv.dLastVisit = vRec.formattedVDate;

                    returnVisitList.add(rv);
                }
                while (cursor.moveToNext());
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return returnVisitList;
    }

    public long deleteRV(ReturnVisit rv){
        SQLiteDatabase fsDatabase = getWritableDatabase();
        long rvId = -1;
        fsDatabase.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(RV_ID_COLUMN, rv.dId);
            values.put(RV_NAME_COLUMN, rv.dName);
            values.put(RV_ADDRESS_COLUMN, rv.dAddress);
            values.put(RV_PHONE_COLUMN, rv.dPhone);
            values.put(RV_CATEGORY_COLUMN, rv.dCategory);
            values.put(RV_VISIT_DAY_COLUMN, rv.dVisitDay);
            values.put(RV_VISIT_TIME_COLUMN, rv.dVisitTime);


            int numOfRows = fsDatabase.update(
                    RV_TABLE_NAME,
                    values,
                    RV_ID_COLUMN + " = ?",
                    new String[]{String.valueOf(rv.dId)}
            );

            if(numOfRows > 0){
                String whereString = RV_ID_COLUMN + " = ? ";
                rvId = fsDatabase.delete(RV_TABLE_NAME, whereString, new String[]{String.valueOf(rv.dId)});
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            fsDatabase.endTransaction();
        }
        return rvId;
    }

    public void deleteReport(MyReport report){
        SQLiteDatabase fsDatabase = getWritableDatabase();
        fsDatabase.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(REPORT_ID_COLUMN, report.dId);
            values.put(REPORT_DATE_COLUMN, report.dDate);
            values.put(REPORT_H_COLUMN, report.dHourSpent);
            values.put(REPORT_PLC_COLUMN, report.dPlacement);
            values.put(REPORT_VS_COLUMN, report.dVideoShowing);


            int numOfRows = fsDatabase.update(
                    REPORT_TABLE_NAME,
                    values,
                    REPORT_DATE_COLUMN + " = ?",
                    new String[]{String.valueOf(report.dDate)}
            );

            if(numOfRows > 0){
                String whereString = REPORT_DATE_COLUMN + " = ? ";
                fsDatabase.delete(REPORT_TABLE_NAME, whereString, new String[]{String.valueOf(report.dDate)});
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            fsDatabase.endTransaction();
        }
    }

    public long insertORUpdateVisitRecord(VisitRecord visitRecord)
    {
        SQLiteDatabase fsDatabase = getWritableDatabase();
        long visitId = -1;
        boolean tryInsertReport = false;
        try {
            fsDatabase.beginTransaction();
        }
        catch (Exception ebend) {
            Log.d("Error", ebend.getMessage());
        }

        try {
            ContentValues visitRecordValues = new ContentValues();
            visitRecordValues.put(VISIT_DISC_COLUMN, visitRecord.vDisc);
            visitRecordValues.put(VISIT_N_DISC_COLUMN, visitRecord.vNDisc);
            visitRecordValues.put(VISIT_TYPE_COLUMN, visitRecord.vType);


            int vNumOfRows = 0;

            if(visitRecord.vId != 0) {
                vNumOfRows = fsDatabase.update(
                        VISIT_TABLE_NAME,
                        visitRecordValues,
                        VISIT_REPORT_DATE_COLUMN + " = ? AND " + VISIT_RV_ID_COLUMN + " = ? ",
                        new String[]{String.valueOf(visitRecord.vDate), String.valueOf(visitRecord.vRVId)}
                );
            }

            if(vNumOfRows > 0){
                visitId = visitRecord.vId;
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
            }
            else {
                visitRecordValues.put(VISIT_ID_COLUMN, visitRecord.vId);
                visitRecordValues.put(VISIT_RV_ID_COLUMN, visitRecord.vRVId);
                visitRecordValues.put(VISIT_REPORT_DATE_COLUMN, visitRecord.vDate);
                visitId = fsDatabase.insertOrThrow(VISIT_TABLE_NAME, null, visitRecordValues);
                visitId = visitRecord.vId;
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
                tryInsertReport = true;
            }

        }catch (Exception ex)
        {
            Log.d("Error", ex.getMessage());
        }
        finally {
            try {
                fsDatabase.endTransaction();
            }
            catch (Exception ebend) {
                Log.d("Error", ebend.getMessage());
            }
        }

        if(tryInsertReport && !isReportExists(fsDatabase, visitRecord.vDate) && !visitRecord.vDate.equals("00000000"))
        {
            try {
                insertOrUpdateExistingReport(((MainActivity) MySettings.getContext()).getReportFromView(visitRecord.vDate));
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return visitId;
    }

    private boolean isReportExists(SQLiteDatabase fsDatabase, String dDate)
    {
        String drSelectString = String.format(
                "SELECT %s FROM %s WHERE %s = ?",
                REPORT_ID_COLUMN, REPORT_TABLE_NAME, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(drSelectString, new String[]{String.valueOf(dDate)});

        try {
            return cursor.moveToFirst();
        }
        catch (Exception ex)
        {
            return false;
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
    }

    private int getRVRecordCount(String dVDate, String type){
        int rcCount;
        SQLiteDatabase fsDatabase = getReadableDatabase();

        String SELECT_RV_QUERY = String.format(
                "SELECT * FROM %s WHERE %s LIKE ? AND %s = ? ",
                VISIT_TABLE_NAME, VISIT_REPORT_DATE_COLUMN, VISIT_TYPE_COLUMN
        );

        dVDate = dVDate + "%";
        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, new String[]{String.valueOf(dVDate), String.valueOf(type)});
        rcCount = cursor.getCount();
        cursor.close();
        return rcCount;
    }

    private int getMonthlyRVCount(String dVDate){
        int rcCount;
        SQLiteDatabase fsDatabase = getReadableDatabase();

        String SELECT_RV_QUERY = String.format(
                "SELECT * FROM %s WHERE %s LIKE ? ",
                VISIT_TABLE_NAME, VISIT_REPORT_DATE_COLUMN
        );

        dVDate = dVDate + "%";
        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, new String[]{String.valueOf(dVDate)});
        rcCount = cursor.getCount();
        cursor.close();
        return rcCount;
    }

    private int getMonthlyBSCount(String dVDate){
        int rcCount;
        SQLiteDatabase fsDatabase = getReadableDatabase();

        String SELECT_RV_QUERY = String.format(
            "SELECT DISTINCT (%s) FROM %s WHERE %s LIKE ? AND %s = ? ",
            VISIT_RV_ID_COLUMN, VISIT_TABLE_NAME, VISIT_REPORT_DATE_COLUMN, VISIT_TYPE_COLUMN
        );

        dVDate = dVDate + "%";
        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, new String[]{String.valueOf(dVDate), String.valueOf("bs")});
        rcCount = cursor.getCount();
        cursor.close();
        return rcCount;
    }

    protected VisitRecord getLastVisit(ReturnVisit rv){

        SQLiteDatabase fsDatabase = getReadableDatabase();
        VisitRecord vR = new VisitRecord(rv);
        String SELECT_RV_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ? ORDER BY %s DESC LIMIT 1",
                VISIT_TABLE_NAME, VISIT_RV_ID_COLUMN, VISIT_REPORT_DATE_COLUMN
        );

        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, new String[]{String.valueOf(rv.dId)});
        try {
            if(cursor.moveToFirst()){
                vR.vDate = cursor.getString(cursor.getColumnIndex(VISIT_REPORT_DATE_COLUMN));
                vR.vDisc = cursor.getString(cursor.getColumnIndex(VISIT_DISC_COLUMN));
                vR.vNDisc = cursor.getString(cursor.getColumnIndex(VISIT_N_DISC_COLUMN));

                if(TextUtils.isEmpty(vR.vDisc)){
                    vR.vDisc = MySettings.getContext().getResources().getString(R.string.not_specified);
                }
                if(TextUtils.isEmpty(vR.vNDisc)){
                    vR.vNDisc = MySettings.getContext().getResources().getString(R.string.not_specified);
                }

                vR.setFormattedVDate();
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return  vR;
    }

    public ArrayList <VisitRecord> getVisitRecord(String dDate, String rvType){
        ArrayList<VisitRecord> myVisitRecord = new ArrayList<>();
        SQLiteDatabase fsDatabase = getReadableDatabase();

        String SELECT_RV_QUERY = "SELECT a.*, b.rv_name FROM " + VISIT_TABLE_NAME + " a" +
                " LEFT OUTER JOIN " + RV_TABLE_NAME + " b" +
                " ON a.visit_rv_id = b.rv_id" +
                " WHERE a.visit_rp_date LIKE ? AND a.visit_type = ?" +
                " ORDER BY b.rv_name ASC ";

        Cursor cursor = fsDatabase.rawQuery(SELECT_RV_QUERY, new String[]{String.valueOf(dDate), String.valueOf(rvType)});
        try {
            if(cursor.moveToFirst()){
                do{
                    VisitRecord vr = new VisitRecord(new ReturnVisit("new"));
                    vr.vId = cursor.getLong(cursor.getColumnIndex(VISIT_ID_COLUMN));
                    vr.vDisc = cursor.getString(cursor.getColumnIndex(VISIT_DISC_COLUMN));
                    vr.vNDisc = cursor.getString(cursor.getColumnIndex(VISIT_N_DISC_COLUMN));
                    vr.vDate = cursor.getString(cursor.getColumnIndex(VISIT_REPORT_DATE_COLUMN));
                    vr.vType = cursor.getString(cursor.getColumnIndex(VISIT_TYPE_COLUMN));
                    vr.setFormattedVDate();
                    vr.vRVName = cursor.getString(cursor.getColumnIndex(RV_NAME_COLUMN));
                    myVisitRecord.add(vr);

                    if(TextUtils.isEmpty(vr.vDisc)){
                        vr.vDisc = MySettings.getContext().getResources().getString(R.string.not_specified);
                    }
                    if(TextUtils.isEmpty(vr.vNDisc)){
                        vr.vNDisc = MySettings.getContext().getResources().getString(R.string.not_specified);
                    }
                }
                while (cursor.moveToNext());
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return myVisitRecord;
    }

    public void deleteVisitRecord(VisitRecord vRecord)
    {
        SQLiteDatabase fsDatabase = getWritableDatabase();
        fsDatabase.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(VISIT_TYPE_COLUMN, vRecord.vType);


            int numOfRows = fsDatabase.update(
                    VISIT_TABLE_NAME,
                    values,
                    VISIT_ID_COLUMN + " = ?",
                    new String[]{String.valueOf(vRecord.vId)}
            );

            if(numOfRows > 0){
                String whereString = VISIT_ID_COLUMN + " = ? ";
                fsDatabase.delete(VISIT_TABLE_NAME, whereString, new String[]{String.valueOf(vRecord.vId)});
                try {
                    fsDatabase.setTransactionSuccessful();
                }catch (Exception e)
                {
                    Log.d("Error:", e.getMessage());
                }
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            fsDatabase.endTransaction();
        }
    }

    public String insertOrUpdateExistingReport(MyReport dr){
        SQLiteDatabase fsDatabase = getWritableDatabase();
        String actionTaken = "none";
        try {
            fsDatabase.beginTransaction();
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        try {
            ContentValues values = new ContentValues();
            values.put(REPORT_H_COLUMN, dr.dHourSpent);
            values.put(REPORT_VS_COLUMN, dr.dVideoShowing);
            values.put(REPORT_PLC_COLUMN, dr.dPlacement);
            values.put(REPORT_DATE_COLUMN, dr.dDate);


            int numOfRows = fsDatabase.update(
                    REPORT_TABLE_NAME,
                    values,
                    REPORT_DATE_COLUMN + " = ?",
                    new String[]{String.valueOf(dr.dDate)}
            );

            if(numOfRows == 1){
                String drSelectString = String.format(
                        "SELECT %s FROM %s WHERE %s = ?",
                        REPORT_ID_COLUMN, REPORT_TABLE_NAME, REPORT_DATE_COLUMN);
                Cursor cursor = fsDatabase.rawQuery(drSelectString, new String[]{String.valueOf(dr.dDate)});

                try {
                    if(cursor.moveToFirst()){
                        fsDatabase.setTransactionSuccessful();
                        actionTaken = MySettings.getContext().getResources().getString(R.string.updated);
                    }
                }
                finally {
                    if(cursor != null && !cursor.isClosed()){
                        cursor.close();
                    }
                }
            }
            else {
                values.put(REPORT_ID_COLUMN, dr.dId);
                fsDatabase.insertOrThrow(REPORT_TABLE_NAME, null, values);
                try {
                    fsDatabase.setTransactionSuccessful();
                    actionTaken = MySettings.getContext().getResources().getString(R.string.saved);
                }catch (Exception ex)
                {
                    Log.d("Error:", ex.getMessage());
                    actionTaken = "Report not saved!";
                }
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            fsDatabase.endTransaction();
        }
        return actionTaken;
    }

    public MyReport getDailyReport(long vDate){

        String SELECT_DR_QUERY;
        MyReport myReport = new MyReport(vDate);
        SQLiteDatabase fsDatabase = getReadableDatabase();

        SELECT_DR_QUERY = String.format(
                    "SELECT * FROM %s WHERE %s = ?",
                    REPORT_TABLE_NAME, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(SELECT_DR_QUERY, new String[]{myReport.dDate});
        try {
            if(cursor.moveToFirst()){
                myReport.dDate = cursor.getString(cursor.getColumnIndex(REPORT_DATE_COLUMN));
                myReport.dHourSpent = cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                myReport.dVideoShowing = cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                myReport.dPlacement = cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }

        myReport.dNumOfRV = getRVRecordCount(myReport.dDate, "rv");
        myReport.dNumOfBS = getRVRecordCount(myReport.dDate, "bs");

        return myReport;
    }

    public MyReport getMonthlyReport(String year, String month)
    {
        int hSpent = 0, vShowings = 0, placements = 0;
        String filter = year + month;
        String countFilter = filter;
        String dDate = filter + "01";
        filter = filter + "%";
        MyReport myReport = new MyReport(dDate);
        SQLiteDatabase fsDatabase = getReadableDatabase();

        String SELECT_DR_QUERY = String.format(
                "SELECT * FROM %s WHERE %s LIKE ?",
                REPORT_TABLE_NAME, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(SELECT_DR_QUERY, new String[]{filter});
        try {
            if(cursor.moveToFirst()){
                do {
                    hSpent += cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                    vShowings += cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                    placements += cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                }
                while (cursor.moveToNext());
            }
            myReport.dHourSpent = hSpent;
            myReport.dVideoShowing = vShowings;
            myReport.dPlacement = placements;
            myReport.dNumOfRV = getMonthlyRVCount(countFilter);
            myReport.dNumOfBS = getMonthlyBSCount(countFilter);
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return myReport;
    }

    public ArrayList<MyReport> getAllYearlyAverage(){
        ArrayList<MyReport> myYearlyAverageReport = new ArrayList<>();
        String rDate = "00000000";
        double hSpent = 0, vShowings = 0, placements = 0;
        MyReport myReport = new MyReport(System.currentTimeMillis());
        SQLiteDatabase fsDatabase = getReadableDatabase();
        int monthCount;
        double rvCount, bsCount;

        String SELECT_DR_QUERY = String.format(
                "SELECT * FROM %s  ORDER BY %s DESC",
                REPORT_TABLE_NAME, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(SELECT_DR_QUERY, null);

        try {
            if(cursor.moveToFirst()){
                do {
                    String curDate = cursor.getString(cursor.getColumnIndex(REPORT_DATE_COLUMN));
                    if(curDate.substring(0, 4).equals(rDate.substring(0, 4)) || rDate.equals("00000000") ) {
                        hSpent += cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                        vShowings += cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                        placements += cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                        rDate = curDate;
                    }else {
                        //add begins here
                        monthCount = FSUtils.getMonthCount(rDate);
                        hSpent /= monthCount;
                        vShowings /= monthCount;
                        placements /= monthCount;
                        rvCount = (double) getMonthlyRVCount(rDate.substring(0, 4)) / monthCount;
                        bsCount = (double) getMonthlyBSCount(rDate.substring(0, 4)) / monthCount;

                        myReport.dHourSpent = FSUtils.roundDouble(hSpent, 1);
                        myReport.dVideoShowing = FSUtils.roundDouble(vShowings, 1);
                        myReport.dPlacement = FSUtils.roundDouble(placements, 1);
                        myReport.dNumOfRV = FSUtils.roundDouble(rvCount, 1);
                        myReport.dNumOfBS = FSUtils.roundDouble(bsCount, 1);
                        myReport.dDate = rDate;
                        myYearlyAverageReport.add(myReport);
                        //add end here

                        //initiate for another year
                        myReport = new MyReport(System.currentTimeMillis());
                        hSpent = cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                        vShowings = cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                        placements = cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                        rDate = curDate;
                    }
                }
                while (cursor.moveToNext());

                //add begins here for the last year
                monthCount = FSUtils.getMonthCount(rDate);
                hSpent /= monthCount;
                vShowings /= monthCount;
                placements /= monthCount;
                rvCount = (double) getMonthlyRVCount(rDate.substring(0, 4)) / monthCount;
                bsCount = (double) getMonthlyBSCount(rDate.substring(0, 4)) / monthCount;

                myReport.dHourSpent = FSUtils.roundDouble(hSpent, 1);
                myReport.dVideoShowing = FSUtils.roundDouble(vShowings, 1);
                myReport.dPlacement = FSUtils.roundDouble(placements, 1);
                myReport.dNumOfRV = FSUtils.roundDouble(rvCount, 1);
                myReport.dNumOfBS = FSUtils.roundDouble(bsCount, 1);
                myReport.dDate = rDate;
                myYearlyAverageReport.add(myReport);
                //add end here
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return myYearlyAverageReport;
    }

    public ArrayList<MyReport> getAnnualMonthlyReports(String forDYear){
        ArrayList<MyReport> myAnnualMonthlyReport = new ArrayList<>();
        String rDate = "00000000";
        int hSpent = 0, vShowings = 0, placements = 0;
        MyReport myReport = new MyReport(System.currentTimeMillis());
        SQLiteDatabase fsDatabase = getReadableDatabase();
        forDYear += "%";

        String SELECT_DR_QUERY = String.format(
                "SELECT * FROM %s WHERE %s LIKE ? ORDER BY %s DESC",
                REPORT_TABLE_NAME, REPORT_DATE_COLUMN, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(SELECT_DR_QUERY, new String[]{String.valueOf(forDYear)});

        try {
            if(cursor.moveToFirst()){
                do {
                    String curDate = cursor.getString(cursor.getColumnIndex(REPORT_DATE_COLUMN));
                    if(curDate.substring(0, 6).equals(rDate.substring(0, 6)) || rDate.equals("00000000") ) {
                        hSpent += cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                        vShowings += cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                        placements += cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                        rDate = curDate;
                    }else {
                        //add begins here
                        myReport.dHourSpent = hSpent;
                        myReport.dVideoShowing = vShowings;
                        myReport.dPlacement = placements;
                        myReport.dNumOfRV = getMonthlyRVCount(rDate.substring(0, 6));
                        myReport.dNumOfBS = getMonthlyBSCount(rDate.substring(0, 6));
                        myReport.dDate = rDate;
                        myAnnualMonthlyReport.add(myReport);
                        //add end here

                        //initiate for another year
                        myReport = new MyReport(System.currentTimeMillis());
                        hSpent = cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                        vShowings = cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                        placements = cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                        rDate = curDate;
                    }
                }
                while (cursor.moveToNext());

                //add begins here for the last year
                myReport.dHourSpent = hSpent;
                myReport.dVideoShowing = vShowings;
                myReport.dPlacement = placements;
                myReport.dNumOfRV = getMonthlyRVCount(rDate.substring(0, 6));
                myReport.dNumOfBS = getMonthlyBSCount(rDate.substring(0, 6));
                myReport.dDate = rDate;
                myAnnualMonthlyReport.add(myReport);
                //add end here
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return myAnnualMonthlyReport;
    }

    public ArrayList<MyReport> getMonthlyReports(String forDMonth){
        ArrayList<MyReport> myMonthlyDailyReport = new ArrayList<>();
        SQLiteDatabase fsDatabase = getReadableDatabase();
        forDMonth += "%";
        String SELECT_DR_QUERY = String.format(
                "SELECT * FROM %s WHERE %s LIKE ? ORDER BY %s DESC",
                REPORT_TABLE_NAME, REPORT_DATE_COLUMN, REPORT_DATE_COLUMN);
        Cursor cursor = fsDatabase.rawQuery(SELECT_DR_QUERY, new String[]{String.valueOf(forDMonth)});

        try {
            if(cursor.moveToFirst()){
                do {
                    MyReport myReport = new MyReport(System.currentTimeMillis());
                    String dDate = cursor.getString(cursor.getColumnIndex(REPORT_DATE_COLUMN));
                    myReport.dDate = dDate;
                    myReport.dHourSpent = cursor.getInt(cursor.getColumnIndex(REPORT_H_COLUMN));
                    myReport.dVideoShowing = cursor.getInt(cursor.getColumnIndex(REPORT_VS_COLUMN));
                    myReport.dPlacement = cursor.getInt(cursor.getColumnIndex(REPORT_PLC_COLUMN));
                    myReport.dNumOfRV = getRVRecordCount(dDate, "rv");
                    myReport.dNumOfBS = getRVRecordCount(dDate, "bs");
                    myMonthlyDailyReport.add(myReport);
                }
                while (cursor.moveToNext());
            }
        }
        catch (Exception e){
            Log.d("Error", e.getMessage());
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return myMonthlyDailyReport;
    }

}
