package com.floromsolutions.fieldservicereport;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.Date;

public class MyReport implements Parcelable {
    public long dId;
    public String dDate;
    public double dHourSpent;
    public double dVideoShowing;
    public double dPlacement;
    public double dNumOfRV;
    public double dNumOfBS;

    MyReport(String dateInString){
        this.dDate = dateInString;

        if(dateInString.length() == 8) {
            int y = Integer.parseInt(dateInString.substring(0, 4));
            int m = Integer.parseInt(dateInString.substring(4, 6));
            int d = Integer.parseInt(dateInString.substring(6, 8));
            Calendar reportDay = Calendar.getInstance();
            reportDay.set(Calendar.YEAR, y);
            reportDay.set(Calendar.MONTH, m);
            reportDay.set(Calendar.DAY_OF_MONTH, d);

            this.dId = reportDay.getTimeInMillis();
        }
    }

    MyReport(long dateInLong){
        this.dId = dateInLong;

        Calendar reportDay = Calendar.getInstance();
        reportDay.setTime(new Date(dateInLong));
        String rDay = padLeadingZero(reportDay.get(Calendar.DAY_OF_MONTH));
        String rMonth = padLeadingZero(reportDay.get(Calendar.MONTH));
        String rYear = padLeadingZero(reportDay.get(Calendar.YEAR));

        this.dDate = rYear + rMonth + rDay;
    }

    private MyReport(Parcel in) {
        dId = in.readLong();
        dDate = in.readString();
        dHourSpent = in.readDouble();
        dVideoShowing = in.readDouble();
        dPlacement = in.readDouble();
        dNumOfRV = in.readDouble();
        dNumOfBS = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(dId);
        dest.writeString(dDate);
        dest.writeDouble(dHourSpent);
        dest.writeDouble(dVideoShowing);
        dest.writeDouble(dPlacement);
        dest.writeDouble(dNumOfRV);
        dest.writeDouble(dNumOfBS);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MyReport> CREATOR = new Creator<MyReport>() {
        @Override
        public MyReport createFromParcel(Parcel in) {
            return new MyReport(in);
        }

        @Override
        public MyReport[] newArray(int size) {
            return new MyReport[size];
        }
    };

    private String padLeadingZero(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return Integer.toString(value);
        }
    }
}
