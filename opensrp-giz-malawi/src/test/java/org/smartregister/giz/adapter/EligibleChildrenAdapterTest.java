package org.smartregister.giz.adapter;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smartregister.giz.domain.EligibleChild;
import org.smartregister.view.ListContract;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EligibleChildrenAdapterTest {

    @Mock
    private ListContract.View<EligibleChild> view;

    @Mock
    private Context context;

    private EligibleChildrenAdapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        EligibleChild eligibleChild1 = new EligibleChild();
        EligibleChild eligibleChild = new EligibleChild();
        List<EligibleChild> eligibleChildList = Arrays.asList(eligibleChild1, eligibleChild);
        adapter = new EligibleChildrenAdapter(eligibleChildList, view, context);
    }

    @Test
    public void testAdapterInstantiatesCorrectly() {
        assertNotNull(adapter);
    }

    @Test
    public void testGetItemIdReturnsCorrectId() {
        assertEquals(-1, adapter.getItemId(1));
    }

    @Test
    public void testGetItemCountReturnsTotalItemsInList() {
        assertEquals(2, adapter.getItemCount());
    }

}