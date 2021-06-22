package org.smartregister.giz.fragment;

import android.view.View;
import android.widget.EditText;

import androidx.appcompat.widget.SwitchCompat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.contract.ChildRegisterFragmentContract;
import org.smartregister.child.cursor.AdvancedMatrixCursor;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.child.presenter.BaseChildRegisterFragmentPresenter;
import org.smartregister.child.provider.RegisterQueryProvider;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.giz.BaseUnitTest;
import org.smartregister.giz.R;
import org.smartregister.giz.presenter.ChildRegisterFragmentPresenter;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.IMConstants;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.service.AlertService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@PrepareForTest({ImmunizationLibrary.class, Utils.class})
public class ChildRegisterFragmentTest extends BaseUnitTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Mock
    protected SwitchCompat filterSection;
    @Mock
    private ImmunizationLibrary immunizationLibrary;
    @Mock
    private CommonRepository commonRepository;
    @Mock
    private VaccineRepository vaccineRepository;
    @Mock
    private Context context;
    @Mock
    private AlertService alertService;
    @Mock
    private View view;
    @Mock
    private EditText editText;
    @Mock
    private RecyclerViewPaginatedAdapter clientAdapter;
    @Mock
    private ChildMetadata childMetadata;
    @Mock
    private RegisterQueryProvider registerQueryProvider;
    @Mock
    private AdvancedMatrixCursor advancedMatrixCursor;
    private BaseChildRegisterFragmentPresenter presenter;

    private ChildRegisterFragment childRegisterFragment;

    @Mock
    private ChildRegisterFragmentContract.Model model;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        childRegisterFragment = Mockito.mock(ChildRegisterFragment.class, Mockito.CALLS_REAL_METHODS);
        mockImmunizationLibrary(immunizationLibrary, context, vaccineRepository, alertService);
        Mockito.doReturn(VaccineRepo.Vaccine.values()).when(immunizationLibrary).getVaccines(IMConstants.VACCINE_TYPE.CHILD);

        presenter = Mockito.mock(ChildRegisterFragmentPresenter.class, Mockito.CALLS_REAL_METHODS);
        ReflectionHelpers.setField(presenter, "viewReference", new WeakReference<>(view));
        ReflectionHelpers.setField(presenter, "model", model);
        ReflectionHelpers.setField(presenter, "viewConfigurationIdentifier", "12345");

        Whitebox.setInternalState(childRegisterFragment, "mainCondition", "is_closed IS NOT 1");
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", null);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
        ReflectionHelpers.setStaticField(ChildLibrary.class, "instance", null);
    }

    @Test
    public void testInitializePresenterWillInitializePresenter() {
        childRegisterFragment.initializePresenter();
        assertNotNull(presenter);
    }


    @Test
    public void testSetUniqueIdReturnsUniqueId() {
        Mockito.doReturn(editText).when(childRegisterFragment).getSearchView();

        String TEST_ID = "unique-identifier";
        childRegisterFragment.setUniqueID(TEST_ID);

        ArgumentCaptor<String> integerArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(editText).setText(integerArgumentCaptor.capture());

        String capturedParam = integerArgumentCaptor.getValue();
        Assert.assertEquals(TEST_ID, capturedParam);
    }

    @Test
    public void testRecalculatePaginationWillRecalculatePagination() {

        assertNotNull(childRegisterFragment);

        Whitebox.setInternalState(childRegisterFragment, "clientAdapter", clientAdapter);

        int TEST_TOTAL_COUNT = 24;
        Mockito.doReturn(TEST_TOTAL_COUNT).when(advancedMatrixCursor).getCount();
        Mockito.doReturn(TEST_TOTAL_COUNT).when(clientAdapter).getTotalcount();

        childRegisterFragment.recalculatePagination(advancedMatrixCursor);

        Mockito.verify(clientAdapter).setTotalcount(TEST_TOTAL_COUNT);
        Mockito.verify(clientAdapter).setCurrentlimit(20);
        Mockito.verify(clientAdapter).setCurrentlimit(24);
        Mockito.verify(clientAdapter).setCurrentoffset(0);
    }

    @Test
    public void testCountExecuteWillReturnCorrectCount() {

        assertNotNull(childRegisterFragment);

        Whitebox.setInternalState(childRegisterFragment, "clientAdapter", clientAdapter);
        Whitebox.setInternalState(childRegisterFragment, "mainCondition", "");
        Whitebox.setInternalState(childRegisterFragment, "filters", "");

        PowerMockito.mockStatic(Utils.class);
        PowerMockito.when(Utils.metadata()).thenReturn(childMetadata);
        Mockito.doReturn(commonRepository).when(childRegisterFragment).commonRepository();
        String TEST_SQL = "Select count(*) from Table where id = 3";
        Mockito.doReturn(TEST_SQL).when(registerQueryProvider).getCountExecuteQuery(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        Mockito.doReturn(5).when(commonRepository).countSearchIds(TEST_SQL);

        childRegisterFragment.countExecute();

        Mockito.verify(childRegisterFragment).countExecute();
    }

    @Test
    public void testToggleFilterSelectionWithNullTagSetsBackgroundToTransparent() {
        assertNotNull(childRegisterFragment);

        Whitebox.setInternalState(childRegisterFragment, "filterSection", filterSection);

        Mockito.doReturn("ID = 8").when(childRegisterFragment).filterSelectionCondition(false);
        Mockito.doNothing().when(childRegisterFragment).filter(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());

        ArgumentCaptor<String> tagCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> bgResourceCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> filterStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> joinTableStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> filterSelectConditionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> qrCodeCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.doNothing().when(childRegisterFragment).initiateReportJob();

        childRegisterFragment.toggleFilterSelection();

        Mockito.verify(childRegisterFragment).filter(filterStringCaptor.capture(), joinTableStringCaptor.capture(), filterSelectConditionCaptor.capture(), qrCodeCaptor.capture());
        Mockito.verify(filterSection).setTag(tagCaptor.capture());
        Mockito.verify(filterSection).setBackgroundResource(bgResourceCaptor.capture());

        String capturedTag = tagCaptor.getValue();
        Integer resIdTag = bgResourceCaptor.getValue();

        Assert.assertEquals("PRESSED", capturedTag);
        Assert.assertEquals(new Integer(org.smartregister.child.R.drawable.transparent_clicked_background), resIdTag);

        String filterString = filterStringCaptor.getValue();
        String joinTable = joinTableStringCaptor.getValue();
        String filterSelect = filterSelectConditionCaptor.getValue();
        boolean qrCodeCaptorValue = qrCodeCaptor.getValue();

        Assert.assertEquals("", filterString);
        Assert.assertEquals("", joinTable);
        Assert.assertEquals("ID = 8", filterSelect);
        Assert.assertEquals(false, qrCodeCaptorValue);

    }

    @Test
    public void testOnViewClickedWillCallToggleFilterSelectionAndToggleSelection() {

        Context.bindtypes = new ArrayList<>();
        when(view.getTag(R.id.record_action)).thenReturn(Constants.RECORD_ACTION.GROWTH);
        CommonPersonObjectClient client = new CommonPersonObjectClient("12-2873-272676-20", null, "");
        client.setColumnmaps(new HashMap<String, String>());
        when(view.getTag()).thenReturn(R.id.record_action);

        when(view.getId()).thenReturn(R.id.filter_selection);

        assertNotNull(childRegisterFragment);

        Whitebox.setInternalState(childRegisterFragment, "filterSection", filterSection);

        Mockito.doReturn("is_closed IS NOT 1").when(childRegisterFragment).getMainCondition();
        Mockito.doReturn("PRESSED").when(filterSection).getTag();
        Mockito.doNothing().when(childRegisterFragment).filter(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());

        ArgumentCaptor<String> tagCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> bgResourceCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> filterStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> joinTableStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mainConditionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> qrCodeCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(childRegisterFragment).initiateReportJob();

        childRegisterFragment.onViewClicked(view);

        Mockito.verify(childRegisterFragment).filter(filterStringCaptor.capture(), joinTableStringCaptor.capture(), mainConditionCaptor.capture(), qrCodeCaptor.capture());
        Mockito.verify(filterSection).setTag(tagCaptor.capture());
        Mockito.verify(filterSection).setBackgroundResource(bgResourceCaptor.capture());

        String capturedTag = tagCaptor.getValue();
        Integer resIdTag = bgResourceCaptor.getValue();

        Assert.assertNull(capturedTag);
        Assert.assertEquals(new Integer(org.smartregister.child.R.drawable.transparent_gray_background), resIdTag);

        String filterString = filterStringCaptor.getValue();
        String joinTable = joinTableStringCaptor.getValue();
        String filterSelect = mainConditionCaptor.getValue();
        boolean qrCodeCaptorValue = qrCodeCaptor.getValue();

        Assert.assertEquals("", filterString);
        Assert.assertEquals("", joinTable);
        Assert.assertEquals(" ( dod is NULL OR dod = '' ) ", filterSelect);
        Assert.assertFalse(qrCodeCaptorValue);
    }
}
