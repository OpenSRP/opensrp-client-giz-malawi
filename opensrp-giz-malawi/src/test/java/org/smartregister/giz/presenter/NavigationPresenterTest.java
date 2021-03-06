package org.smartregister.giz.presenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.smartregister.giz.contract.NavigationContract;
import org.smartregister.giz.util.GizConstants;

import java.util.HashMap;

@RunWith(PowerMockRunner.class)
public class NavigationPresenterTest {

    private NavigationPresenter navigationPresenter;

    @Mock
    private NavigationContract.View view;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        navigationPresenter = new NavigationPresenter(view);
    }

    @Test
    public void initialize() throws Exception {
        HashMap<String, String> tableMap = new HashMap<>();
        Whitebox.setInternalState(navigationPresenter, "tableMap", tableMap);
        Whitebox.invokeMethod(navigationPresenter, "initialize");
        Assert.assertEquals(6, tableMap.size());
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.ALL_CLIENTS));
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.CHILD_CLIENTS));
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.PNC_CLIENTS));
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.OPD_CLIENTS));
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.MATERNITY_CLIENTS));
        Assert.assertTrue(tableMap.containsKey(GizConstants.DrawerMenu.ANC_CLIENTS));
    }

    @Test
    public void getNavigationView() {
        Assert.assertNotNull(navigationPresenter.getNavigationView());
    }
}