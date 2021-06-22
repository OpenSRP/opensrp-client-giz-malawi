package org.smartregister.giz.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.child.domain.RegisterClickables;
import org.smartregister.child.util.Constants;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.giz.R;
import org.smartregister.giz.activity.ChildImmunizationActivity;
import org.smartregister.giz.activity.FragmentBaseActivity;
import org.smartregister.giz.adapter.EligibleChildrenAdapter;
import org.smartregister.giz.dao.ReportDao;
import org.smartregister.giz.domain.EligibleChild;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.util.AppExecutors;
import org.smartregister.view.ListContract;
import org.smartregister.view.adapter.ListableAdapter;
import org.smartregister.view.viewholder.ListableViewHolder;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class EligibleChildrenReportFragment extends ReportResultFragment<EligibleChild> {
    public static final String TAG = "EligibleChildrenReportFragment";

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
        presenter.fetchList(() -> ReportDao.fetchLiveEligibleChildrenReport(communityId, reportDate), fetchRequestType());
    }

    private AppExecutors.Request fetchRequestType() {
        return AppExecutors.Request.DISK_THREAD;
    }

    @NonNull
    @Override
    public ListableAdapter<EligibleChild, ListableViewHolder<EligibleChild>> adapter() {
        return new EligibleChildrenAdapter(list, (ListContract.View<EligibleChild>) this, this.getContext());
    }

    @Override
    public boolean hasDivider() {
        return false;
    }

    @Override
    public void onListItemClicked(EligibleChild eligibleChild, int layoutID) {

        Observable<CommonPersonObjectClient> observable = Observable.create(observableEmitter -> {
            CommonPersonObject personObject = GizUtils.getCommonRepository(GizConstants.TABLE_NAME.CHILD).findByBaseEntityId(eligibleChild.getID());
            CommonPersonObjectClient client = new CommonPersonObjectClient(personObject.getCaseId(),
                    personObject.getDetails(), "");
            client.setColumnmaps(personObject.getColumnmaps());

            observableEmitter.onNext(client);
            observableEmitter.onComplete();
        });

        final Disposable[] disposable = new Disposable[1];
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<CommonPersonObjectClient>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
                setLoadingState(true);
            }

            @Override
            public void onNext(CommonPersonObjectClient client) {
                setLoadingState(false);
                Activity activity = getActivity();
                if (activity != null) {
                    RegisterClickables registerClickables = new RegisterClickables();
                    if (view.getTag(R.id.record_action) != null) {
                        registerClickables.setRecordWeight(
                                Constants.RECORD_ACTION.GROWTH.equals(view.getTag(R.id.record_action)));
                        registerClickables.setRecordAll(
                                Constants.RECORD_ACTION.VACCINATION.equals(view.getTag(R.id.record_action)));
                    }

                    ChildImmunizationActivity.launchActivity(getActivity(), client, registerClickables);

                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                setLoadingState(false);
                disposable[0].dispose();
                disposable[0] = null;
            }
        });
    }

    @Override
    public void onFetchError(Exception e) {
        // Do Nothing
    }

    @Override
    public void refreshView() {
        super.refreshView();
        if (getActivity() instanceof FragmentBaseActivity) {
            ((FragmentBaseActivity) getActivity()).setTitle(list.size() + " " + getString(R.string.child_due_report_grouping_title));
        }
    }


}
