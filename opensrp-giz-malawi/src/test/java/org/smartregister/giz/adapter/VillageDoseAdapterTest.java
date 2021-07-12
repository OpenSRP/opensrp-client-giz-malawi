package org.smartregister.giz.adapter;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smartregister.giz.domain.VillageDose;
import org.smartregister.view.ListContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VillageDoseAdapterTest {
    private VillageDoseAdapter adapter;

    @Mock
    private Context context;

    @Mock
    private ListContract.View<VillageDose> view;

    @Before
    public void setUp() {
        Map<String, Integer> recurringServices = new LinkedHashMap<>();

        ArrayList<VillageDose> villageDoses = new ArrayList<>();
        MockitoAnnotations.initMocks(this);
        VillageDose villageDose = new VillageDose();
        villageDose.setID("278949-4894-400-422-90");
        recurringServices.put("BCG", 1);
        recurringServices.put("OPV", 4);
        villageDose.setVillageName("Nakuru");
        villageDose.setRecurringServices(recurringServices);
        villageDoses.add(villageDose);
        adapter = new VillageDoseAdapter(villageDoses, view, context);
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
        assertEquals(1, adapter.getItemCount());
    }
}
