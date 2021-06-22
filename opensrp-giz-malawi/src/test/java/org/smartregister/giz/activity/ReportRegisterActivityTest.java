package org.smartregister.giz.activity;

import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.domain.FetchStatus;
import org.smartregister.giz.BaseUnitTest;
import org.smartregister.giz.R;
import org.smartregister.giz.util.GizUtils;

public class ReportRegisterActivityTest extends BaseUnitTest {

    private final ImageView reportSyncBtn = Mockito.mock(ImageView.class);
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private ReportRegisterActivity activity;
    private ActivityController<ReportRegisterActivity> controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = Context.getInstance();
        CoreLibrary.init(context);

        //Auto login by default
        context.session().start(context.session().lengthInMilliseconds());

        controller = Robolectric.buildActivity(ReportRegisterActivity.class).create().start().resume().restart();
        activity = controller.get();
    }

    @Test
    public void testOnBackActivityReturnsNull() {
        activity.onBackActivity();
        Assert.assertNull(activity.onBackActivity());
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetupViewsInitializesView() {
        TextView titleTv = Mockito.mock(TextView.class);
        ListView listView = Mockito.mock(ListView.class);

        activity.setTheme(org.smartregister.R.style.AppTheme);
        activity.setContentView(R.layout.activity_report_register);
        ReflectionHelpers.setField(activity, "titleTv", titleTv);
        ReflectionHelpers.setField(activity, "listView", listView);

        activity.setUpViews();

        Assert.assertNotNull(ReflectionHelpers.getField(activity, "listView"));
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "titleTv"));
    }

    @Test
    public void testOnSyncUpdatesTheSyncStatus() {
        activity = Mockito.spy(activity);
        activity.setContentView(R.layout.activity_report_register);
        ReflectionHelpers.setField(activity, "reportSyncBtn", reportSyncBtn);
        activity.onSyncStart();
        Assert.assertFalse(GizUtils.getSyncStatus());
    }

    @Test
    public void testOnSyncCompleteWillToggleSyncStatusToComplete() {
        activity = Mockito.spy(activity);
        activity.setContentView(R.layout.activity_report_register);
        ReflectionHelpers.setField(activity, "reportSyncBtn", reportSyncBtn);
        FetchStatus status = FetchStatus.fetched;

        activity.onSyncComplete(status);
        Mockito.verify(activity).toggleReportSyncButton();
    }


    @Test
    public void testOnSyncInProgressWillToggleSyncStatus() {
        activity = Mockito.spy(activity);
        activity.setContentView(R.layout.activity_report_register);
        ReflectionHelpers.setField(activity, "reportSyncBtn", reportSyncBtn);
        FetchStatus status = FetchStatus.fetchStarted;

        activity.onSyncInProgress(status);
        Mockito.verify(activity).toggleReportSyncButton();
    }
}
