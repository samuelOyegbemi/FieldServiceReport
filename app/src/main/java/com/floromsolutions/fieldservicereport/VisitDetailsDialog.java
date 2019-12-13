package com.floromsolutions.fieldservicereport;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class VisitDetailsDialog extends Dialog {

    private String header;
    private View mainDialogView;
    private static String rvName;
    private static VisitRecord currVRec;
    private VisitDetailsDialog myDialog;
    private static int refererId;
    private static ReturnVisit returnVisit;

    public interface OnVisitDialogFinishListener{
        void onRecordDetailsSubmission(VisitDetailsDialog d, VisitRecord vr, int refererId);
    }

    private OnVisitDialogFinishListener dialogFinish;

    public static VisitDetailsDialog getInstance(Context context, ReturnVisit rv, VisitRecord vRecord, int referer){
        rvName = rv.dName;
        refererId = referer;
        returnVisit = rv;
        VisitDetailsDialog vDetailsDialog = new VisitDetailsDialog(context);
        if(context instanceof  OnVisitDialogFinishListener) {
            vDetailsDialog.setListener(context);
        }
        currVRec = vRecord;
        return vDetailsDialog;
    }

    private VisitDetailsDialog(@NonNull Context context) {
        super(context);
        header = context.getString(R.string.visit_detail_header) + " " + rvName;
        myDialog = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getContentView());

        TextView headerView = mainDialogView.findViewById(R.id.dialog_title);
        headerView.setText(header);

        Button close = mainDialogView.findViewById(R.id.cancel_btn);
        close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        Button save = mainDialogView.findViewById(R.id.save_btn);
        save.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //check before doing the submission
                currVRec.vDisc = ((EditText)mainDialogView.findViewById(R.id.ent_l_disc)).getText().toString();
                currVRec.vNDisc = ((EditText)mainDialogView.findViewById(R.id.ent_n_disc)).getText().toString();

                dialogFinish.onRecordDetailsSubmission(myDialog, currVRec, refererId);
            }
        });

        //check if it exist and fill
        try {
            FSDatabaseHelper d = FSDatabaseHelper.getInstance(MySettings.getContext());
            VisitRecord mLast = d.getLastVisit(returnVisit);
            if (mLast != null && mLast.vDate.equals(currVRec.vDate)) {
                if (!mLast.vDisc.equals(MySettings.getContext().getResources().getString(R.string.not_specified))) {
                    currVRec.vDisc = mLast.vDisc;
                    ((EditText)mainDialogView.findViewById(R.id.ent_l_disc)).setText(mLast.vDisc);
                }
                if (!mLast.vNDisc.equals(MySettings.getContext().getResources().getString(R.string.not_specified))) {
                    currVRec.vNDisc = mLast.vNDisc;
                    ((EditText)mainDialogView.findViewById(R.id.ent_n_disc)).setText(mLast.vNDisc);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void setListener(Context context){
        try {
            this.dialogFinish = (OnVisitDialogFinishListener) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString());
        }
    }

    private View getContentView(){
        mainDialogView = getLayoutInflater().inflate(R.layout.dialog_insert_visit_details, null);
        return mainDialogView;
    }
}
