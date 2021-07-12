package org.smartregister.giz.fragment;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.giz.R;
import org.smartregister.giz.activity.FragmentBaseActivity;
import org.smartregister.giz.adapter.VillageDoseAdapter;
import org.smartregister.giz.dao.ReportDao;
import org.smartregister.giz.domain.VillageDose;
import org.smartregister.giz.model.FilterReportFragmentModel;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.util.AppExecutors;
import org.smartregister.view.adapter.ListableAdapter;
import org.smartregister.view.viewholder.ListableViewHolder;

import java.util.ArrayList;
import java.util.List;

public class VillageDoseReportFragment extends ReportResultFragment<VillageDose> {
    public static final String TAG = "VillageDoseReportFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_send_report, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_send_report) {
            ((FragmentBaseActivity) getActivity()).sendEmail();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void executeFetch() {
        presenter.fetchList(() -> {
            boolean includeAll = communityName.equalsIgnoreCase(GizConstants.KEY.ALL_COMMUNITIES);
            FilterReportFragmentModel model = new FilterReportFragmentModel();
            List<VillageDose> result = new ArrayList<>(ReportDao.fetchLiveVillageDosesReport(communityId, reportDate, includeAll,
                    includeAll ? communityName : null, model.getAllLocations()));

            return result;
        }, fetchRequestType());
    }

    private AppExecutors.Request fetchRequestType() {
        return AppExecutors.Request.DISK_THREAD;
    }

    @Override
    public void onFetchError(Exception e) {
        // Do nothing
    }

    @NonNull
    @Override
    public ListableAdapter<VillageDose, ListableViewHolder<VillageDose>> adapter() {
        return new VillageDoseAdapter(list, this, this.getContext());
    }

    @Override
    public boolean hasDivider() {
        return false;
    }
}

