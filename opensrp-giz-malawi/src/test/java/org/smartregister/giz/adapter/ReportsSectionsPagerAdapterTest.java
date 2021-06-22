package org.smartregister.giz.adapter;

import android.os.Build;

import androidx.fragment.app.FragmentManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.giz.activity.HIA2ReportsActivity;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.fragment.DailyTalliesFragment;
import org.smartregister.giz.fragment.DraftMonthlyFragment;
import org.smartregister.giz.fragment.SentMonthlyFragment;

@RunWith(RobolectricTestRunner.class)
@Config(application = GizMalawiApplication.class, sdk = Build.VERSION_CODES.P)
public class ReportsSectionsPagerAdapterTest {
    @Mock
    private FragmentManager fragmentManager;

    private HIA2ReportsActivity activity = new HIA2ReportsActivity();


    private ReportsSectionsPagerAdapter sectionsPagerAdapter = new ReportsSectionsPagerAdapter(activity, fragmentManager);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetItemReturnsCorrectInstance() {
        Assert.assertTrue(sectionsPagerAdapter.getItem(0) instanceof DailyTalliesFragment);
        Assert.assertTrue(sectionsPagerAdapter.getItem(1) instanceof DraftMonthlyFragment);
        Assert.assertTrue(sectionsPagerAdapter.getItem(2) instanceof SentMonthlyFragment);
    }

    @Test
    public void testGetCountReturnsCount() {
        Assert.assertEquals(3, sectionsPagerAdapter.getCount());
    }
}
