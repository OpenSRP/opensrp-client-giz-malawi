package org.smartregister.giz.interactor;

import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.contract.FindReportContract;
import org.smartregister.giz.dao.ReportDao;
import org.smartregister.giz.util.AppExecutors;

import java.util.Map;

import timber.log.Timber;

public class FindReportInteractor implements FindReportContract.Interactor {

    protected AppExecutors appExecutors;

    public FindReportInteractor() {
        appExecutors = GizMalawiApplication.getInstance().getAppExecutors()
        ;
    }

    @Override
    public void processAvailableLocations(Map<String, String> locations, FindReportContract.Presenter presenter) {
        Runnable runnable = () -> {
            try {
                Map<String, String> hierarchy = ReportDao.extractRecordedLocations();

                appExecutors.mainThread().execute(() -> presenter.onReportHierarchyLoaded(hierarchy));
            } catch (Exception e) {
                Timber.e("No locations Found");
            }
        };
        appExecutors.diskIO().execute(runnable);
    }
}
