package com.floromsolutions.fieldservicereport;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class VisitRecord {
    public long vId;
    public String vDisc;
    public String vNDisc;
    public String vDate;
    public long vRVId;
    public String vType;
    public String formattedVDate;
    public String vRVName = "Anonymous";

    VisitRecord(ReturnVisit rv){
        this.vId = System.currentTimeMillis();
        this.vRVId = rv.dId;
        this.vType = rv.dCategory;

        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(this.vId));
        String rDay = padLeadingZero(reportDay.get(Calendar.DAY_OF_MONTH));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        this.vDate = rYear + rMonth + rDay;
        setFormattedVDate();
    }

    VisitRecord(ReturnVisit rv, long dateInLong){
        this.vId = System.currentTimeMillis();

        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(dateInLong));
        String rDay = padLeadingZero(reportDay.get(Calendar.DAY_OF_MONTH));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        this.vDate = rYear + rMonth + rDay;
        this.vRVId = rv.dId;
        this.vType = rv.dCategory;
        setFormattedVDate();
    }

    private String padLeadingZero(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return Integer.toString(value);
        }
    }

    public void setFormattedVDate(){
        int mDate;

        try {
            mDate = Integer.parseInt(vDate);
        }catch (Exception e)
        {
            mDate = 0;
        }

        if(mDate == 0)
        {
            vDate = FSUtils.dateInStringFromLong(vRVId);
        }
        this.formattedVDate = FSUtils.getFormattedVDate(vDate, Locale.getDefault());
    }
}
