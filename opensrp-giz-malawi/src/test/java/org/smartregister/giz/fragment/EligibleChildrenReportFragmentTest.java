package org.smartregister.giz.fragment;

import android.view.View;
import android.widget.ProgressBar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.smartregister.view.ListContract;

public class EligibleChildrenReportFragmentTest {
    private EligibleChildrenReportFragment fragment;
    @Mock
    private ListContract.Presenter presenter;

    @Mock
    private ListContract.View view;

    @Mock
    private ListContract.Model model;

    @Mock
    private ListContract.Interactor interactor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fragment = Mockito.mock(EligibleChildrenReportFragment.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(presenter.getInteractor()).thenReturn(interactor);
        Mockito.when(presenter.getModel()).thenReturn(model);
        Mockito.when(presenter.getView()).thenReturn(view);
    }

    @Test
    public void testLoadPresenterAssignsPresenter() {
        fragment.loadPresenter();
        Assert.assertNotNull(Whitebox.getInternalState(fragment, "presenter"));

    }

    @Test
    public void testSetLoadingStateCalledOnceHidesViewAfterBeingCalled() {
        ProgressBar progressBar = Mockito.mock(ProgressBar.class);
        Whitebox.setInternalState(fragment, "progressBar", progressBar);
        fragment.setLoadingState(true);
        Mockito.verify(progressBar, Mockito.times(1)).setVisibility(View.VISIBLE);

        fragment.setLoadingState(false);
        Mockito.verify(progressBar, Mockito.times(1)).setVisibility(View.INVISIBLE);
    }
}

