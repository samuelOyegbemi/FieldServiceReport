package com.floromsolutions.fieldservicereport;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class ShowVisitRecordDialog extends Dialog {
    private FSDatabaseHelper databaseHelper;
    private List<VisitRecord> myVisitRecord;
    private Context appContext;
    private int selectedVisitPosition;
    private String header;
    private View mainDialogView;
    private String emptyListText;
    private static String intention;
    private long selectedDate;
    private static String selectedDateInString;

    public interface OnDialogFinishListener{
        void onVisitRecordDialogClose(long dDate);
    }

    private OnDialogFinishListener dialogFinish;

    public static ShowVisitRecordDialog getInstance(Context context, String searchFilter, long dateInLong){
        selectedDateInString = FSUtils.dateInStringFromLong(dateInLong);
        ShowVisitRecordDialog selectRVDialog = new ShowVisitRecordDialog(context, dateInLong);
        if(context instanceof  OnDialogFinishListener) {
            selectRVDialog.setListener(context);
        }
        intention = searchFilter;
        return selectRVDialog;
    }

    private ShowVisitRecordDialog(@NonNull Context context, long sDate) {
        super(context);
        selectedDate = sDate;
        this.appContext = context;
        databaseHelper = FSDatabaseHelper.getInstance(context);
        header = appContext.getResources().getString(R.string.select_rv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getContentView());

        switch (intention) {
            case "rv":
                header = appContext.getResources().getString(R.string.vr_rv_header) + " " + FSUtils.getFormattedVDate(selectedDate, Locale.getDefault());
                emptyListText = appContext.getResources().getString(R.string.empty_rv_list);
                break;
            case "bs":
                header = appContext.getResources().getString(R.string.vr_bs_header) + " " + FSUtils.getFormattedVDate(selectedDate, Locale.getDefault());
                emptyListText = appContext.getResources().getString(R.string.empty_bs_list);
                break;
            default:
                break;
        }

        TextView headerView = mainDialogView.findViewById(R.id.dialog_title);
        headerView.setText(header);

        mainDialogView.findViewById(R.id.search_my_rv).setVisibility(View.GONE); // we don't want to show search bar.

        Button close = mainDialogView.findViewById(R.id.close_btn);
        close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dialogFinish.onVisitRecordDialogClose(selectedDate);
                dismiss();
            }
        });

        TextView empty_rv_view = mainDialogView.findViewById(R.id.empty_rv_list);

        if (empty_rv_view != null) {
            ListView list = mainDialogView.findViewById(R.id.rv_list);
            if (list != null) {
                empty_rv_view.setText(emptyListText);
                list.setEmptyView(empty_rv_view);
            }
        }

        loadVisitRecordView(mainDialogView, intention);
    }

    /*protected String getIntention(){
        return intention;
    }*/

    private void setListener(Context context){
        try {
            this.dialogFinish = (OnDialogFinishListener) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString());
        }
    }

    private void loadVisitRecordView(final View mView, String searchIntention) {

        myVisitRecord = databaseHelper.getVisitRecord(selectedDateInString, searchIntention);

        ListView rvListView = mView.findViewById(R.id.rv_list);
        if (rvListView != null) {
            ArrayAdapter<VisitRecord> rvListAdapter =
                    new ArrayAdapter<VisitRecord>(appContext, R.layout.dialog_visit_record_item, myVisitRecord) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.dialog_visit_record_item, null);
                            }

                            final VisitRecord currVR = myVisitRecord.get(position);
                            final int pos = position;

                            TextView rvName = convertView.findViewById(R.id.item_rv_name);
                            rvName.setText(currVR.vRVName);

                            TextView rvLDisc = convertView.findViewById(R.id.item_rv_disc);
                            rvLDisc.setText(currVR.vDisc);

                            ImageButton removeBtn = convertView.findViewById(R.id.remove_visit);
                            removeBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    selectedVisitPosition = pos;
                                    confirmVisitRecordDelete(selectedVisitPosition);
                                }
                            });
                            return convertView;
                        }
                    };
            rvListView.setAdapter(rvListAdapter);
        }
    }

    private void confirmVisitRecordDelete(final int dPosition){
        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
            builder.setTitle("Confirm Delete");
            String mess = "Are you sure to delete " + FSUtils.getFormattedVDate(selectedDate, Locale.US) + " visit record of " + myVisitRecord.get(dPosition).vRVName + " permanently?";
            builder.setMessage(mess);

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //update rv here
                    databaseHelper.deleteVisitRecord(myVisitRecord.get(dPosition));
                    loadVisitRecordView(mainDialogView, intention);
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

    private View getContentView(){
        mainDialogView = getLayoutInflater().inflate(R.layout.dialog_select_rv, null);
        return mainDialogView;
    }
}
