package org.smartregister.giz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import org.smartregister.domain.FetchStatus;
import org.smartregister.giz.R;
import org.smartregister.giz.fragment.FilterReportFragment;
import org.smartregister.giz.model.ReportGroupingModel;

import java.util.ArrayList;

public class ReportTypeListRegisterActivity extends ReportRegisterActivity {
    @Override
    protected void setUpViews() {
        super.setUpViews();
        if (titleTv != null) {
            titleTv.setText(R.string.reports_type);
        }
    }

    @Override
    protected void loadData() {
        final ArrayList<ReportGroupingModel.ReportGrouping> reportGroupings = getReportGroupings();
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.report_types_list_item, reportGroupings));
        listView.setOnItemClickListener((parent, view, position, id) -> {

            switch (position) {
                case 0:
                    Intent intent = new Intent(ReportTypeListRegisterActivity.this, ReportRegisterActivity.class);
                    startActivity(intent);
                    break;
                case 1:
                    Bundle dueVaccines = new Bundle();
                    dueVaccines.putString(FilterReportFragment.REPORT_NAME, getString(R.string.child_due_report_grouping_title));
                    FragmentBaseActivity.startMe(ReportTypeListRegisterActivity.this, FilterReportFragment.TAG, getString(R.string.child_due_report_grouping_title), dueVaccines);
                    break;
                case 2:
                    Bundle dosesNeeded= new Bundle();
                    dosesNeeded.putString(FilterReportFragment.REPORT_NAME, getString(R.string.vaccine_doses_needed));
                    FragmentBaseActivity.startMe(ReportTypeListRegisterActivity.this, FilterReportFragment.TAG, getString(R.string.vaccine_doses_needed), dosesNeeded);
                    break;
                default:
                    break;
            }

        });
    }

    @Override
    protected ArrayList<ReportGroupingModel.ReportGrouping> getReportGroupings() {
        return (new ReportGroupingModel(this)).getReportListGroupings();
    }


    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        // Nothing to happen here

    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        // Nothing to happen here
    }


    @Override
    public void onSyncStart() {
        // Nothing to happen here
    }
}
