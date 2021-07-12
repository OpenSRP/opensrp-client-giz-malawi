package org.smartregister.giz.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.giz.BaseUnitTest;
import org.smartregister.giz.adapter.NavigationAdapter;
import org.smartregister.giz.presenter.GizChildRegisterPresenter;
import org.smartregister.giz.view.NavigationMenu;

public class ChildRegisterActivityTest extends BaseUnitTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private ChildRegisterActivity activity;
    private ActivityController<ChildRegisterActivity> controller;
    @Captor
    private ArgumentCaptor<Intent> intentArgumentCaptor;
    @Captor
    private ArgumentCaptor<Integer> integerArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = Context.getInstance();
        CoreLibrary.init(context);

        //Auto login by default
        context.session().start(context.session().lengthInMilliseconds());

        controller = Robolectric.buildActivity(ChildRegisterActivity.class).create().start().resume().restart();
        activity = controller.get();
    }

    @Test
    public void testInitializePresenter() {
        activity.initializePresenter();
        Assert.assertTrue(ReflectionHelpers.getField(activity, "presenter") instanceof GizChildRegisterPresenter);
    }

    @Test
    public void testStartFormActivityInvokesChildForm() throws Exception {
        activity = Mockito.spy(activity);

        Mockito.doNothing().when(activity).startActivityForResult(ArgumentMatchers.any(Intent.class), ArgumentMatchers.anyInt());

        activity.startFormActivity(new JSONObject());

        Mockito.verify(activity).startActivityForResult(intentArgumentCaptor.capture(), integerArgumentCaptor.capture());

        Intent capturedIntent = intentArgumentCaptor.getValue();
        Assert.assertNotNull(capturedIntent);
        Assert.assertEquals("org.smartregister.giz.activity.ChildFormActivity", capturedIntent.getComponent().getClassName());

        Integer capturedInteger = integerArgumentCaptor.getValue();

        Assert.assertNotNull(capturedInteger);
        Assert.assertEquals(ChildJsonFormUtils.REQUEST_CODE_GET_JSON, capturedInteger.intValue());

    }

    @Test
    public void testOnResumption() {
        activity = Mockito.spy(activity);
        NavigationMenu navigationMenu = Mockito.mock(NavigationMenu.class);
        NavigationAdapter navigationAdapter = Mockito.mock(NavigationAdapter.class);
        Mockito.when(navigationMenu.getNavigationAdapter()).thenReturn(navigationAdapter);
        activity.onResumption();
        Mockito.verify(activity,Mockito.times(1)).createDrawer();
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
