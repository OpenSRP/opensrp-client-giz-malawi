package org.smartregister.giz.activity;

import android.content.Intent;
import android.widget.LinearLayout;

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
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.giz.BaseUnitTest;
import org.smartregister.giz.R;
import org.smartregister.giz.domain.Hia2Indicator;
import org.smartregister.giz.domain.MonthlyTally;
import org.smartregister.giz.domain.Tally;
import org.smartregister.giz.util.GizConstants;

import java.util.ArrayList;
import java.util.Date;

public class ReportSummaryActivityTest extends BaseUnitTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ReportSummaryActivity activity;
    private ActivityController<ReportSummaryActivity> controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = Context.getInstance();
        CoreLibrary.init(context);

        //Auto login by default
        context.session().start(context.session().lengthInMilliseconds());

        controller = Robolectric.buildActivity(ReportSummaryActivity.class).create().start().resume();
        activity = controller.get();
    }

    @Test
    public void testOnBackActivityReturnsNull() {
        activity.onBackActivity();
        Assert.assertNull(activity.onBackActivity());
    }

    @Test
    public void testFetchIndicatorTalliesForDayRefreshesIndicatorViews() {
        ReportSummaryActivity spyActivity = Mockito.spy(activity);

        LinearLayout view = Mockito.mock(LinearLayout.class);

        Intent intent = new Intent();
        String baseEntityId = "2323-006282-323";
        intent.putExtra(GizConstants.Columns.RegisterType.BASE_ENTITY_ID, baseEntityId);
        Date date = new Date();
        String reportGrouping = "reports";
        ArrayList<MonthlyTally> tallies = new ArrayList<>();
        MonthlyTally monthlyTally = new MonthlyTally();
        Tally tally = new Tally();
        tally.setId(10929 - 283883);
        tally.setIndicator("BCG");
        tally.setValue("3" +
                "0");

        Hia2Indicator indicator = new Hia2Indicator();
        monthlyTally.setMonth(new Date());
        monthlyTally.setGrouping("Reports");
        monthlyTally.setHia2Indicator(indicator);
        monthlyTally.setIndicatorTally(tally);
        tallies.add(0, monthlyTally);

        Mockito.doReturn(view)
                .when(spyActivity).findViewById(R.id.indicator_canvas);
        activity.setTallies(tallies);

        activity.fetchIndicatorTalliesForDay(date, reportGrouping);

        spyActivity.setTallies(tallies);
        Mockito.verify(view, Mockito.times(1)).removeAllViews();
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

}
