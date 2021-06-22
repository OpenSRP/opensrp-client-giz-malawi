package org.smartregister.giz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.view.GravityCompat;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.child.activity.BaseActivity;
import org.smartregister.child.toolbar.LocationSwitcherToolbar;
import org.smartregister.domain.FetchStatus;
import org.smartregister.giz.R;
import org.smartregister.giz.model.ReportGroupingModel;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.giz.view.NavigationMenu;

import java.util.ArrayList;
import java.util.Map;

public class ReportRegisterActivity extends BaseActivity {
    protected ListView listView;
    protected TextView titleTv;
    protected ImageView reportSyncBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpViews();
        loadData();
    }

    protected void setUpViews() {
        listView = findViewById(R.id.lv_reportRegister_groupings);
        titleTv = findViewById(R.id.title);

        if (titleTv != null) {
            titleTv.setText(R.string.dhis2_reports);
        }
    }

    protected void loadData() {
        reportSyncBtn = findViewById(R.id.report_sync_btn);
        reportSyncBtn.setOnClickListener(v -> GizUtils.startReportJob(getApplicationContext()));

        final ArrayList<ReportGroupingModel.ReportGrouping> reportGroupings = getReportGroupings();
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.report_grouping_list_item, reportGroupings));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(ReportRegisterActivity.this, HIA2ReportsActivity.class);
            intent.putExtra(GizConstants.IntentKey.REPORT_GROUPING, reportGroupings.get(position).getGrouping());
            startActivity(intent);
        });

        if (GizUtils.getSyncStatus()) {
            reportSyncBtn.setVisibility(View.VISIBLE);
        } else {
            reportSyncBtn.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_report_register;
    }


    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return null;
    }

    protected ArrayList<ReportGroupingModel.ReportGrouping> getReportGroupings() {
        return (new ReportGroupingModel(this)).getReportGroupings();
    }

    public void onClickReport(View view) {
        switch (view.getId()) {
            case R.id.btn_back_to_home:

                NavigationMenu navigationMenu = NavigationMenu.getInstance(this, null, null);
                if (navigationMenu != null) {
                    navigationMenu.getDrawer()
                            .openDrawer(GravityCompat.START);
                }
                break;
            default:
                break;

        }
    }

    @Override
    public void onUniqueIdFetched(Triple<String, Map<String, String>, String> triple, String s) {
        //do nothing
    }

    @Override
    public void onNoUniqueId() {
        // Nothing to happen here
    }

    @Override
    public void onRegistrationSaved(boolean b) {
        // Nothing to happen here
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        super.onSyncInProgress(fetchStatus);
        toggleReportSyncButton();
    }

    protected void toggleReportSyncButton() {
        if (reportSyncBtn != null) {
            reportSyncBtn.setVisibility(View.GONE);
        }
        GizUtils.updateSyncStatus(false);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
        toggleReportSyncButton();
    }


    @Override
    public void onSyncStart() {
        super.onSyncStart();
        if (reportSyncBtn != null) {
            reportSyncBtn.setVisibility(View.GONE);
        }
        GizUtils.updateSyncStatus(false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        createDrawer();

        if (reportSyncBtn == null)
            return;

        if (GizUtils.getSyncStatus()) {
            reportSyncBtn.setVisibility(View.VISIBLE);
        } else {
            reportSyncBtn.setVisibility(View.GONE);
        }
    }

    public void createDrawer() {
        NavigationMenu navigationMenu = NavigationMenu.getInstance(this, null, null);
        if (navigationMenu != null) {
            navigationMenu.getNavigationAdapter().setSelectedView(null);
            navigationMenu.runRegisterCount();
        }
    }

}
