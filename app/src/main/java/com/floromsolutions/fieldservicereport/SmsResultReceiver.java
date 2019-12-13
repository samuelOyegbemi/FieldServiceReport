package com.floromsolutions.fieldservicereport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class SmsResultReceiver extends BroadcastReceiver {
    static final int RESULT_OK = -1;
    static String dErrorMessage = "Error";

    //define the interfaces here

    public interface OnSMSSentListener{
        void onSMSSent();
        void onSMSDecline(String errorMessage);
    }

    private static OnSMSSentListener onSMSSentListener;

    public static SmsResultReceiver getInstance(Context context){
        try {
            onSMSSentListener = (OnSMSSentListener) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString());
        }
       return new SmsResultReceiver();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode()){
            case RESULT_OK:
                // on success interface here
                onSMSSentListener.onSMSSent();
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                // on error interface her
                dErrorMessage = "Report not Sent, No Network";
                onSMSSentListener.onSMSDecline(dErrorMessage);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                //on error interface here
                dErrorMessage = "Report not Sent, Turn off Airplane Mode";
                onSMSSentListener.onSMSDecline(dErrorMessage);
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                //on error interface here
                dErrorMessage = "Report not Sent";
                onSMSSentListener.onSMSDecline(dErrorMessage);
                break;
            default:
                break;
        }
    }
}
