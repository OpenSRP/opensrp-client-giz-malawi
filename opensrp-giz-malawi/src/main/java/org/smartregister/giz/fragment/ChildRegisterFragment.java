package org.smartregister.giz.fragment;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import org.smartregister.child.domain.RegisterClickables;
import org.smartregister.child.fragment.BaseChildRegisterFragment;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.giz.R;
import org.smartregister.giz.activity.ChildImmunizationActivity;
import org.smartregister.giz.activity.ChildRegisterActivity;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.model.ChildRegisterFragmentModel;
import org.smartregister.giz.presenter.ChildRegisterFragmentPresenter;
import org.smartregister.giz.util.AppExecutors;
import org.smartregister.giz.util.DBQueryHelper;
import org.smartregister.immunization.job.VaccineSchedulesUpdateJob;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ChildRegisterFragment extends BaseChildRegisterFragment implements CompoundButton.OnCheckedChangeListener {
    private View view;
    private SwitchCompat filterSection;

    private AppExecutors appExecutors;

    public ChildRegisterFragment() {
        super();
        appExecutors = GizMalawiApplication.getInstance().getAppExecutors();
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }

        String viewConfigurationIdentifier = ((BaseRegisterActivity) getActivity()).getViewIdentifiers().get(0);
        presenter = new ChildRegisterFragmentPresenter(this, new ChildRegisterFragmentModel(), viewConfigurationIdentifier);
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
        //do nothing
    }


    protected void toggleFilterSelection() {
        if (filterSection != null) {
            String tagString = "PRESSED";

            // The due-only toggle is on
            if (filterSection.getTag() == null) {
                filter("", "", filterSelectionCondition(false), false);
                filterSection.setTag(tagString);
                filterSection.setBackgroundResource(R.drawable.transparent_clicked_background);

                initiateReportJob();

            } else if (filterSection.getTag().toString().equals(tagString)) {
                filter("", "", " ( " + Constants.KEY.DOD + " is NULL OR " + Constants.KEY.DOD + " = '' ) ", false);
                filterSection.setTag(null);
                filterSection.setBackgroundResource(R.drawable.transparent_gray_background);
            }
        }
    }

    protected void initiateReportJob() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) != 0 && calendar.get(Calendar.HOUR_OF_DAY) != 1) {
            calendar.set(Calendar.HOUR_OF_DAY, 1);
            long hoursSince1AM = (System.currentTimeMillis() - calendar.getTimeInMillis()) / TimeUnit.HOURS.toMillis(1);

            if (VaccineSchedulesUpdateJob.isLastTimeRunLongerThan(hoursSince1AM)) {
                Toast.makeText(getContext(), R.string.vaccine_schedule_update_wait_message, Toast.LENGTH_LONG)
                        .show();
                VaccineSchedulesUpdateJob.scheduleJobImmediately();
            }
        }
    }

    @Override
    public void setupViews(View view) {
        this.view = view;
        filterSection = view.findViewById(R.id.switch_selection);
        filterSection.setOnCheckedChangeListener(this);
        super.setupViews(view);
    }

    @Override
    public String getMainCondition() {
        return presenter().getMainCondition();
    }

    @Override
    protected String getDefaultSortQuery() {
        return presenter().getDefaultSortQuery();
    }

    @Override
    protected void onViewClicked(View view) {
        super.onViewClicked(view);
        RegisterClickables registerClickables = new RegisterClickables();
        if (view.getTag(R.id.record_action) != null) {
            registerClickables.setRecordWeight(
                    Constants.RECORD_ACTION.GROWTH.equals(view.getTag(R.id.record_action)));
            registerClickables.setRecordAll(
                    Constants.RECORD_ACTION.VACCINATION.equals(view.getTag(R.id.record_action)));
        }

        CommonPersonObjectClient client = null;
        if (view.getTag() != null && view.getTag() instanceof CommonPersonObjectClient) {
            client = (CommonPersonObjectClient) view.getTag();
        }

        switch (view.getId()) {
            case R.id.child_profile_info_layout:
                ChildImmunizationActivity.launchActivity(getActivity(), client, registerClickables);
                break;
            case R.id.record_growth:
                registerClickables.setRecordWeight(true);
                ChildImmunizationActivity.launchActivity(getActivity(), client, registerClickables);
                break;
            case R.id.record_vaccination:
                registerClickables.setRecordAll(true);
                ChildImmunizationActivity.launchActivity(getActivity(), client, registerClickables);
                break;
            case R.id.filter_selection:
                toggleFilterSelection();
                break;
            case R.id.global_search:
                ((ChildRegisterActivity) getActivity()).startAdvancedSearch();
                break;
            case R.id.scan_qr_code:
                ((ChildRegisterActivity) getActivity()).startQrCodeScanner();
                break;
            case R.id.back_button:
                ((ChildRegisterActivity) getActivity()).openDrawer();
                break;
            default:
                break;
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        ImageView hamburgerMenu = view.findViewById(R.id.left_menu);
        if (hamburgerMenu != null) {
            hamburgerMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ChildRegisterActivity) {
                        ((ChildRegisterActivity) getActivity()).openDrawer();
                    }
                }
            });
        }
    }


    @Override
    protected String filterSelectionCondition(boolean urgentOnly) {
        return DBQueryHelper.getFilterSelectionCondition(urgentOnly);
    }

    @Override
    public void onClick(View view) {

        onViewClicked(view);
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        toggleFilterSelection();
    }

    @Override
    public void countExecute() {
        try {
            appExecutors.diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    String sql = Utils.metadata().getRegisterQueryProvider().getCountExecuteQuery(mainCondition, filters);
                    Timber.i(sql);
                    int totalCount = commonRepository().countSearchIds(sql);
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            clientAdapter.setTotalcount(totalCount);
                            Timber.i("Total Register Count %d", clientAdapter.getTotalcount());
                            clientAdapter.setCurrentlimit(20);
                            clientAdapter.setCurrentoffset(0);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
