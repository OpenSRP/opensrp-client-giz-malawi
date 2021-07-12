package org.smartregister.giz.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.giz.R;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.contract.FindReportContract;
import org.smartregister.giz.util.GizConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@RunWith(RobolectricTestRunner.class)
@Config(application = GizMalawiApplication.class, sdk = 22)
public class FilterReportFragmentTest {
    @Mock
    private LayoutInflater layoutInflater;
    @Mock
    private ViewGroup container;
    @Mock
    private Bundle bundle;

    private FilterReportFragment filterReportFragment;

    @Mock
    private FindReportContract.Presenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        filterReportFragment = Mockito.mock(FilterReportFragment.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testRunReportStartsTheRightView() {
        filterReportFragment = Mockito.spy(FilterReportFragment.class);
        ReflectionHelpers.setField(filterReportFragment, "presenter", presenter);

        LinkedHashMap<String, String> communityIdList = new LinkedHashMap<>();
        communityIdList.put("community 1", "456");
        List<String> communityList = new ArrayList<>();
        communityList.add("community 1");
        ReflectionHelpers.setField(filterReportFragment, "communityList", communityList);
        ReflectionHelpers.setField(filterReportFragment, "communityIdList", communityIdList);
        ReflectionHelpers.setField(filterReportFragment, "selectedItem", "community 1");
        ReflectionHelpers.setField(filterReportFragment, "selectedItemPosition", 0);


        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        Calendar myCalendar = Calendar.getInstance();
        Map<String, String> map = new HashMap<>();
        map.put(GizConstants.ReportParametersHelper.COMMUNITY, "community 1");
        map.put(GizConstants.ReportParametersHelper.COMMUNITY_ID, "community 1");
        map.put(GizConstants.ReportParametersHelper.REPORT_DATE, dateFormat.format(myCalendar.getTime()));
        filterReportFragment.runReport();

        Mockito.verify(presenter).runReport(map);
    }

    @Test
    public void testLoadPresenterAssignsPresenter() {
        FilterReportFragment spyFragment = Mockito.spy(FilterReportFragment.class);

        assertNull(Whitebox.getInternalState(spyFragment, "presenter"));

        spyFragment.loadPresenter();

        assertNotNull(Whitebox.getInternalState(spyFragment, "presenter"));
    }

    @Test
    public void testOnCreateViewsInitializesViews() {
        filterReportFragment = Mockito.spy(FilterReportFragment.class);
        View parentLayout = LayoutInflater.from(RuntimeEnvironment.application.getApplicationContext()).inflate(R.layout.filter_report_fragment, null, false);
        doReturn(parentLayout).when(layoutInflater).inflate(R.layout.filter_report_fragment, container, false);


        ProgressBar progressBar = Mockito.mock(ProgressBar.class);
        EditText editTextDate = Mockito.mock(EditText.class);
        AutoCompleteTextView autoCompleteTextView = Mockito.mock(AutoCompleteTextView.class);
        Button buttonSave = Mockito.mock(Button.class);
        ImageView imageView = Mockito.mock(ImageView.class);
        View view = Mockito.mock(View.class);

        ReflectionHelpers.setField(filterReportFragment, "buttonSave", buttonSave);
        ReflectionHelpers.setField(filterReportFragment, "progressBar", progressBar);
        ReflectionHelpers.setField(filterReportFragment, "autoCompleteTextView", autoCompleteTextView);
        ReflectionHelpers.setField(filterReportFragment, "view", view);
        ReflectionHelpers.setField(filterReportFragment, "imageView", imageView);
        ReflectionHelpers.setField(filterReportFragment, "editTextDate", editTextDate);

        doNothing().when(filterReportFragment).bindAutoCompleteText();

        Whitebox.setInternalState(filterReportFragment, "presenter", presenter);

        filterReportFragment.loadPresenter();
        filterReportFragment.onCreateView(layoutInflater, container, bundle);
        Mockito.verify(filterReportFragment, Mockito.times(1)).bindLayout();
    }
}

