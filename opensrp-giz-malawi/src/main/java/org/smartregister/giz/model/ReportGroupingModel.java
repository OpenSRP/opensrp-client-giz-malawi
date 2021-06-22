package org.smartregister.giz.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.giz.R;

import java.util.ArrayList;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 16-03-2020.
 */
public class ReportGroupingModel {

    private ArrayList<ReportGrouping> groupings = new ArrayList<>();
    private Context context;

    public ReportGroupingModel(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public ArrayList<ReportGrouping> getReportGroupings() {
        if (groupings.isEmpty()) {
            groupings.add(new ReportGrouping(context.getString(R.string.child_report_grouping_title), "child"));
            groupings.add(new ReportGrouping(context.getString(R.string.anc_report_grouping_title), "anc"));
            groupings.add(new ReportGrouping(context.getString(R.string.maternity_report_grouping_title), "maternity"));
            groupings.add(new ReportGrouping(context.getString(R.string.opd_report_grouping_title), "opd"));
            groupings.add(new ReportGrouping(context.getString(R.string.pnc_report_grouping_title), "pnc"));
        }
        return groupings;
    }

    @NonNull
    public ArrayList<ReportGrouping> getReportListGroupings() {
        if (groupings.isEmpty()) {
            groupings.add(new ReportGrouping(context.getString(R.string.dhis2_report_grouping_title), null));
            groupings.add(new ReportGrouping(context.getString(R.string.child_due_report_grouping_title), null));
            groupings.add(new ReportGrouping(context.getString(R.string.vaccine_needed_report_grouping_title), null));
        }
        return groupings;
    }


    public static class ReportGrouping {

        private String displayName;
        private String grouping;

        public ReportGrouping(@Nullable String displayName, @Nullable String grouping) {
            this.displayName = displayName;
            this.grouping = grouping;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getGrouping() {
            return grouping;
        }

        public void setGrouping(String grouping) {
            this.grouping = grouping;
        }
    }
}
