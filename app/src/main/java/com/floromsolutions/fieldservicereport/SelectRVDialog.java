package com.floromsolutions.fieldservicereport;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class SelectRVDialog extends Dialog {
    private FSDatabaseHelper databaseHelper;
    private List<ReturnVisit> myReturnVisits;
    private SelectRVDialog myDialog;
    private Context appContext;
    private ReturnVisit selectedRv;
    private static int refererLayoutId;
    private String header;
    private View mainDialogView;
    private String emptyListText;
    private static String intention;

    public interface OnDialogFinishListener{
        void onDialogItemSelected(SelectRVDialog d, ReturnVisit returnVisit, int fromLayoutId);
    }

    private OnDialogFinishListener dialogFinish;

    public static SelectRVDialog getInstance(Context context, int fromLayoutId, String searchFilter){
        SelectRVDialog selectRVDialog = new SelectRVDialog(context);
        if(context instanceof  OnDialogFinishListener) {
            selectRVDialog.setListener(context);
        }
        refererLayoutId = fromLayoutId;
        intention = searchFilter;
        return selectRVDialog;
    }

    private SelectRVDialog(@NonNull Context context) {
        super(context);
        this.appContext = context;
        databaseHelper = FSDatabaseHelper.getInstance(context);
        header = appContext.getResources().getString(R.string.select_rv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getContentView());

        switch (refererLayoutId) {
            case R.layout.fragment_view_bs:
                header = appContext.getResources().getString(R.string.select_bs_from_rv);
                emptyListText = appContext.getResources().getString(R.string.empty_rv_list) +
                        ", " + appContext.getResources().getString(R.string.add_rv_before_bs);
                break;
            case R.layout.fragment_home:
                if(intention.equals("rv")){
                    header = appContext.getResources().getString(R.string.select_rv);
                    emptyListText = appContext.getResources().getString(R.string.empty_rv_list);
                }
                else {
                    header = appContext.getResources().getString(R.string.select_bs);
                    emptyListText = appContext.getResources().getString(R.string.empty_bs_list);
                }
                break;
            default:
                break;
        }

        TextView headerView = mainDialogView.findViewById(R.id.dialog_title);
        headerView.setText(header);

        Button close = mainDialogView.findViewById(R.id.close_btn);
        close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        myDialog = this;

        TextView empty_rv_view = mainDialogView.findViewById(R.id.empty_rv_list);

        if (empty_rv_view != null) {
            ListView list = mainDialogView.findViewById(R.id.rv_list);
            if (list != null) {
                empty_rv_view.setText(emptyListText);
                list.setEmptyView(empty_rv_view);
            }
        }

        loadRvListView(mainDialogView, intention,null);
        setRVSearchQueryListener();
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

    private void loadRvListView(View mView, String searchIntention, String searchString) {
        String whereCondition;
        String[] args;

        if (searchString == null) {
            whereCondition = String.format("%s = ? ", FSDatabaseHelper.RV_CATEGORY_COLUMN);
            args = new String[]{searchIntention};
        } else {
            searchString = "%" + searchString + "%";
            whereCondition = String.format("%s = ? AND ( %s LIKE ? OR %s LIKE ? )",
                    FSDatabaseHelper.RV_CATEGORY_COLUMN, FSDatabaseHelper.RV_NAME_COLUMN, FSDatabaseHelper.RV_ADDRESS_COLUMN);
            args = new String[]{searchIntention, searchString, searchString};
        }

        myReturnVisits = databaseHelper.getAllRV(whereCondition, args);
        ListView rvListView = mView.findViewById(R.id.rv_list);
        if (rvListView != null) {
            ArrayAdapter<ReturnVisit> rvListAdapter =
                    new ArrayAdapter<ReturnVisit>(appContext, R.layout.dialog_rv_list_item, myReturnVisits) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.dialog_rv_list_item, null);
                            }
                            TextView rvName = convertView.findViewById(R.id.item_rv_name);
                            rvName.setText(myReturnVisits.get(position).dName);

                            TextView rvAddress = convertView.findViewById(R.id.item_rv_address);
                            rvAddress.setText(myReturnVisits.get(position).dAddress);
                            return convertView;
                        }
                    };
            rvListView.setAdapter(rvListAdapter);

            rvListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //open fragment for rv detail here
                    selectedRv = myReturnVisits.get(i);
                    dialogFinish.onDialogItemSelected(myDialog, selectedRv, refererLayoutId);
                }
            });
        }
    }

    private void setRVSearchQueryListener() {
        SearchView rvSearch = mainDialogView.findViewById(R.id.search_my_rv);
        if (rvSearch != null) {
            rvSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadRvListView(mainDialogView, intention, s);
                    } else {
                        loadRvListView(mainDialogView, intention,null);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    if (s != null && (!s.trim().equals(""))) {
                        loadRvListView(mainDialogView, intention, s);
                    } else {
                        loadRvListView(mainDialogView, intention,null);
                    }
                    return true;
                }
            });
        }
    }

    private View getContentView(){
        mainDialogView = getLayoutInflater().inflate(R.layout.dialog_select_rv, null);
        return mainDialogView;
    }
}
