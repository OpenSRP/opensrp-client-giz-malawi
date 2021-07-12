package org.smartregister.giz.util;

import android.content.ContentValues;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.child.interactor.ChildRegisterInteractor;
import org.smartregister.child.util.Constants;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.commonregistry.CommonRepositoryInformationHolder;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.giz.BaseRobolectricTest;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.view.activity.DrishtiApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class GizUtilsTest extends BaseRobolectricTest {

    @Mock
    private GizMalawiApplication gizMalawiApplication;

    @Mock
    private Context context;

    @Captor
    private ArgumentCaptor argumentCaptorSaveCurrentLocality;

    @Mock
    private CoreLibrary coreLibrary;

    @Mock
    private AllCommonsRepository allCommonsRepository;

    @Mock
    private AllSharedPreferences allSharedPreferences;

    @Mock
    private LocationHelper locationHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateClientDeathShouldPassCorrectArgs() {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", coreLibrary);
        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", locationHelper);

        PowerMockito.when(gizMalawiApplication.context()).thenReturn(context);
        PowerMockito.doReturn(allCommonsRepository).when(context).allCommonsRepositoryobjects(Mockito.eq(GizConstants.TABLE_NAME.ALL_CLIENTS));
        Client client = new Client("123");
        client.setDeathdate(new DateTime());
        EventClient eventClient = new EventClient(new Event(), client);

        GizUtils.updateClientDeath(eventClient);

        Mockito.verify(allCommonsRepository).update(Mockito.eq(GizConstants.TABLE_NAME.ALL_CLIENTS), Mockito.any(ContentValues.class), Mockito.eq(client.getBaseEntityId()));
        Mockito.verify(allCommonsRepository).updateSearch(Mockito.eq(client.getBaseEntityId()));
    }

    @Test
    public void testGetCurrentLocalityShouldReturnCorrectValueIfPresent() {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        PowerMockito.when(gizMalawiApplication.context()).thenReturn(context);
        PowerMockito.when(context.allSharedPreferences()).thenReturn(allSharedPreferences);
        PowerMockito.when(allSharedPreferences.fetchCurrentLocality()).thenReturn("child location 1");
        Assert.assertEquals("child location 1", GizUtils.getCurrentLocality());
    }

    @Test
    public void testGetCurrentLocalityShouldReturnCorrectValueIfAbsent() {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", locationHelper);
        PowerMockito.when(locationHelper.getDefaultLocation()).thenReturn("Default Location");
        PowerMockito.when(gizMalawiApplication.context()).thenReturn(context);
        PowerMockito.when(context.allSharedPreferences()).thenReturn(allSharedPreferences);
        PowerMockito.when(allSharedPreferences.fetchCurrentLocality()).thenReturn(null);
        Assert.assertEquals("Default Location", GizUtils.getCurrentLocality());
        Mockito.verify(allSharedPreferences).saveCurrentLocality(String.valueOf(argumentCaptorSaveCurrentLocality.capture()));
        Assert.assertEquals("Default Location", argumentCaptorSaveCurrentLocality.getValue());
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", null);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
        ReflectionHelpers.setStaticField(LocationHelper.class, "instance", null);
    }

    @Test
    public void testCreateChildGrowthEventFromRepeatingGroupShouldInvokeGrowthProcessing() throws JSONException {
        String baseEntityId = "32ewsd-424";
        org.smartregister.clientandeventmodel.Client client = new org.smartregister.clientandeventmodel.Client(baseEntityId);
        JSONObject clientJson = new JSONObject();
        ChildRegisterInteractor childRegisterInteractor = Mockito.spy(new ChildRegisterInteractor());
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("birth_height_entered", "2344");
        hashMap.put("birth_weight_entered", "5344");
        hashMap.put("base_entity_id", baseEntityId);
        HashMap<String, HashMap<String, String>> buildRepeatingGroupBorn = new HashMap<>();
        buildRepeatingGroupBorn.put("3234-aew", hashMap);
        Mockito.doNothing().when(childRegisterInteractor).processHeight(Mockito.anyMap(), Mockito.anyString(), Mockito.any(UpdateRegisterParams.class), Mockito.eq(clientJson));
        Mockito.doNothing().when(childRegisterInteractor).processWeight(Mockito.anyMap(), Mockito.anyString(), Mockito.any(UpdateRegisterParams.class), Mockito.eq(clientJson));
        GizUtils.createChildGrowthEventFromRepeatingGroup(clientJson, client, childRegisterInteractor, buildRepeatingGroupBorn);

        ArgumentCaptor<String> tempFormArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(childRegisterInteractor, Mockito.times(1)).processHeight(Mockito.anyMap(), tempFormArgumentCaptor.capture(), Mockito.any(UpdateRegisterParams.class), Mockito.eq(clientJson));
        Mockito.verify(childRegisterInteractor, Mockito.times(1)).processWeight(Mockito.anyMap(), Mockito.contains(Constants.KEY.BIRTH_WEIGHT), Mockito.any(UpdateRegisterParams.class), Mockito.eq(clientJson));

        JSONObject jsonObject = new JSONObject(tempFormArgumentCaptor.getValue());
        JSONArray fieldsJsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).optJSONArray(JsonFormConstants.FIELDS);
        Assert.assertNotNull(fieldsJsonArray);
        Assert.assertEquals(2, fieldsJsonArray.length());

        for (int i = 0; i < fieldsJsonArray.length(); i++) {
            JSONObject field = fieldsJsonArray.optJSONObject(i);
            String key = field.optString(JsonFormConstants.KEY);
            if (Constants.KEY.BIRTH_HEIGHT.equals(key)) {
                String value = field.optString(JsonFormConstants.VALUE);
                Assert.assertEquals(hashMap.get("birth_height_entered"), value);
            } else if ((Constants.KEY.BIRTH_WEIGHT.equals(key))) {
                String value = field.optString(JsonFormConstants.VALUE);
                Assert.assertEquals(GizUtils.convertWeightToKgs(hashMap.get("birth_weight_entered")), value);
            }
        }
    }

    @Test
    public void testGetDurationShouldReturnDuration() {
        GizUtils.getDuration("2021-10-09T18:17:07.830+05:00");

        Locale locale = RuntimeEnvironment.application.getApplicationContext().getResources().getConfiguration().locale;
        DateTime todayDateTime = new DateTime("2021-10-09T18:17:07.830+05:00");

        Assert.assertEquals("1d", GizUtils.getDuration(Long.parseLong("100000000"),
                new DateTime("2021-10-10T18:17:07.830+05:00"),
                todayDateTime,
                locale));

        Assert.assertEquals("2w 5d", GizUtils.getDuration(Long.parseLong("1641600000"),
                new DateTime("2021-11-12T18:17:07.830+05:00"),
                todayDateTime,
                locale));


        Assert.assertEquals("4m 3w", GizUtils.getDuration(Long.parseLong("12700800000"),
                new DateTime("2021-11-12T18:17:07.830+05:00"),
                todayDateTime,
                locale));

        Assert.assertEquals("4y 11m", GizUtils.getDuration(Long.parseLong("157334400000"),
                new DateTime("2016-10-10T05:00:00.000+05:00"),
                todayDateTime,
                locale));

        Assert.assertEquals("5y", GizUtils.getDuration(Long.parseLong("157334400000"),
                new DateTime("2016-10-09T05:00:00.000+05:00"),
                todayDateTime,
                locale));
    }

    @Test
    public void testShouldUpdateSyncStatus() throws Exception {
        Assert.assertEquals(false, GizUtils.getSyncStatus());
        GizUtils.updateSyncStatus(true);
        Assert.assertEquals(true, GizUtils.getSyncStatus());
    }

    @Test
    public void testGetStringResourceByName() {
        android.content.Context androidContext = RuntimeEnvironment.application.getApplicationContext();
        Assert.assertEquals("string", GizUtils.getStringResourceByName("string", androidContext));
    }

    @Test
    public void testGetCommonRepositoryReturnsTableName() {
        String tableName = "ec_child";
        Context context = Mockito.spy(Context.getInstance());

        // Mock ec_client_fields.json file
        String ecClientFields = "{\"bindobjects\":[{\"name\":\"ec_child\",\"columns\":[{\"column_name\":\"base_entity_id\",\"json_mapping\":{\"field\":\"identifiers.opensrp_id\"}},{\"column_name\":\"first_name\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"firstName\"}},{\"column_name\":\"last_name\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"lastName\"}},{\"column_name\":\"village_town\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"addresses.cityVillage\"}},{\"column_name\":\"quarter_clan\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"addresses.commune\"}},{\"column_name\":\"street\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"addresses.street\"}},{\"column_name\":\"landmark\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"addresses.landmark\"}},{\"column_name\":\"gps\",\"type\":\"Event\",\"json_mapping\":{\"field\":\"obs.fieldCode\",\"concept\":\"163277AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}},{\"column_name\":\"fam_source_income\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"attributes.fam_source_income\"}},{\"column_name\":\"family_head\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"relationships.family_head\"}},{\"column_name\":\"primary_caregiver\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"relationships.primary_caregiver\"}},{\"column_name\":\"last_interacted_with\",\"type\":\"Event\",\"json_mapping\":{\"field\":\"version\"}},{\"column_name\":\"date_removed\",\"type\":\"Client\",\"json_mapping\":{\"field\":\"attributes.dateRemoved\"}},{\"column_name\":\"entity_type\",\"type\":\"Event\",\"json_mapping\":{\"field\":\"entityType\"}}]}]}";
        Mockito.doReturn(ecClientFields).when(context).ReadFromfile(Mockito.eq("ec_client_fields.json"), Mockito.any(android.content.Context.class));

        ReflectionHelpers.setField(context, "bindtypes", new ArrayList<CommonRepositoryInformationHolder>());
        context.getEcBindtypes();

        // Execute the method being tested
        GizUtils.getCommonRepository(tableName);
        CommonRepository commonRepository = context.commonrepository("ec_child");

        Assert.assertEquals("ec_child", commonRepository.TABLE_NAME);
    }

    @Test
    public void testGetLocale() {
        Locale expectedLocaleNull = Locale.getDefault();
        Locale expectedLocaleNotNull = RuntimeEnvironment.application.getResources().getConfiguration().locale;
        Assert.assertEquals(expectedLocaleNull, GizUtils.getLocale(null));
        Assert.assertEquals(expectedLocaleNotNull, GizUtils.getLocale(RuntimeEnvironment.application));
    }

}