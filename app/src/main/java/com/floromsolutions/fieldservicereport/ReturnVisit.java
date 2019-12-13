package com.floromsolutions.fieldservicereport;

import android.os.Parcel;
import android.os.Parcelable;

public class ReturnVisit implements Parcelable {
    public long dId;
    public String dName;
    public String dAddress;
    public String dPhone;
    public String dLDisc;
    public String dNDisc;
    public String dCategory;
    public String dVisitDay;
    public String dVisitTime;
    public String dLastVisit;

    ReturnVisit(String rvName){
        this.dName = rvName;
    }

    ReturnVisit(long rvId, String rvName)
    {
        this.dId = rvId;
        this.dName = rvName;
    }

    private ReturnVisit(Parcel in) {
        dId = in.readLong();
        dName = in.readString();
        dAddress = in.readString();
        dPhone = in.readString();
        dLDisc = in.readString();
        dNDisc = in.readString();
        dCategory = in.readString();
        dVisitDay = in.readString();
        dVisitTime = in.readString();
        dLastVisit = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(dId);
        dest.writeString(dName);
        dest.writeString(dAddress);
        dest.writeString(dPhone);
        dest.writeString(dLDisc);
        dest.writeString(dNDisc);
        dest.writeString(dCategory);
        dest.writeString(dVisitDay);
        dest.writeString(dVisitTime);
        dest.writeString(dLastVisit);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ReturnVisit> CREATOR = new Creator<ReturnVisit>() {
        @Override
        public ReturnVisit createFromParcel(Parcel in) {
            return new ReturnVisit(in);
        }

        @Override
        public ReturnVisit[] newArray(int size) {
            return new ReturnVisit[size];
        }
    };
}
