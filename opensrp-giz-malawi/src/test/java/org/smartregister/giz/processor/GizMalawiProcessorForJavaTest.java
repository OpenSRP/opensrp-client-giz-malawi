package org.smartregister.giz.processor;

import android.content.ContentValues;
import android.content.Context;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.child.util.ChildDbUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.domain.db.Client;
import org.smartregister.domain.db.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.repository.ClientRegisterTypeRepository;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.growthmonitoring.domain.Height;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.view.activity.DrishtiApplication;

import java.util.Arrays;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class, VaccineSchedule.class, ServiceSchedule.class, ChildDbUtils.class})
public class GizMalawiProcessorForJavaTest {

    @Mock
    private GizMalawiApplication gizMalawiApplication;

    @Mock
    private HeightRepository heightRepository;

    @Mock
    private WeightRepository weightRepository;

    @Mock
    private ContentValues contentValues;

    @Captor
    private ArgumentCaptor<Weight> processWeightArgumentCaptor;

    @Captor
    private ArgumentCaptor<Height> processHeightArgumentCaptor;

    @Captor
    private ArgumentCaptor<ServiceRecord> recordServiceArgumentCaptor;

    @Mock
    private RecurringServiceTypeRepository recurringServiceTypeRepository;

    @Mock
    private RecurringServiceRecordRepository recurringServiceRecordRepository;

    private GizMalawiProcessorForJava processorForJava;

    @Before
    public void setUp() {
        processorForJava = Mockito.spy(GizMalawiProcessorForJava.getInstance(Mockito.mock(Context.class)));
    }

    @Test
    public void processHeightWithEventClientNullShouldReturnFalse() throws Exception {
        Object[] params = {null, null, true};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processHeight", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processHeightWithTableNullShouldReturnFalse() throws Exception {
        Object[] params = {new EventClient(new Event(), new Client("23")), null, true};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processHeight", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processHeightWithValidEventClientAndTableShouldReturnTrue() throws Exception {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        PowerMockito.when(gizMalawiApplication.heightRepository()).thenReturn(heightRepository);
        Mockito.when(processorForJava.processCaseModel(ArgumentMatchers.any(EventClient.class), ArgumentMatchers.any(Table.class))).thenReturn(contentValues);
        Mockito.when(contentValues.size()).thenReturn(7);
        Mockito.when(contentValues.getAsString(HeightRepository.DATE)).thenReturn("2019-09-27 09:45:44");
        Mockito.when(contentValues.getAsString(HeightRepository.BASE_ENTITY_ID)).thenReturn("234");
        Mockito.when(contentValues.containsKey(HeightRepository.CM)).thenReturn(true);
        Mockito.when(contentValues.getAsString(HeightRepository.CM)).thenReturn("230");
        Mockito.when(contentValues.getAsString(HeightRepository.ANMID)).thenReturn("provider");
        Mockito.when(contentValues.getAsString(HeightRepository.LOCATIONID)).thenReturn("lombwe");
        Mockito.when(contentValues.containsKey(HeightRepository.Z_SCORE)).thenReturn(true);
        Mockito.when(contentValues.getAsString(HeightRepository.Z_SCORE)).thenReturn("45.0");
        Mockito.when(contentValues.getAsString(HeightRepository.CREATED_AT)).thenReturn("2019-09-27 09:45:44");
        Table table = new Table();
        table.name = "heights";
        Event event = new Event();
        event.setEventId("231");
        event.setFormSubmissionId("343");
        Client client = new Client("234");
        EventClient eventClient = new EventClient(event, client);

        Boolean result = Whitebox.invokeMethod(processorForJava, "processHeight", eventClient, table, false);
        Mockito.verify(heightRepository).add(processHeightArgumentCaptor.capture());
        Height resultHeightobj = processHeightArgumentCaptor.getValue();
        Assert.assertEquals(java.util.Optional.of(0).get(), resultHeightobj.getOutOfCatchment());
        Assert.assertEquals(Float.valueOf("230.0"), resultHeightobj.getCm());
        Assert.assertEquals(Double.valueOf("45.0"), resultHeightobj.getZScore());
        Assert.assertEquals("lombwe", resultHeightobj.getLocationId());
        Assert.assertEquals(HeightRepository.TYPE_Synced, resultHeightobj.getSyncStatus());
        Assert.assertEquals(Utils.getDate("2019-09-27 09:45:44"), resultHeightobj.getDate());
        Assert.assertEquals(Utils.getDate("2019-09-27 09:45:44"), resultHeightobj.getCreatedAt());
        Assert.assertTrue(result);
    }

    @Test
    public void processServiceWithEventClientNullShouldReturnFalse() throws Exception {
        Object[] params = {null, null};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processService", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processServiceWithTableNullShouldReturnFalse() throws Exception {
        Object[] params = {new EventClient(new Event(), new Client("23")), null};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processService", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processServiceWithNameNotHavingITNShouldReturnTrue() throws Exception {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        PowerMockito.when(gizMalawiApplication.recurringServiceTypeRepository()).thenReturn(recurringServiceTypeRepository);
        PowerMockito.when(gizMalawiApplication.recurringServiceRecordRepository()).thenReturn(recurringServiceRecordRepository);
        ServiceType serviceTypeDeworming2 = new ServiceType();
        serviceTypeDeworming2.setId(1L);
        serviceTypeDeworming2.setName("Deworming_2");
        serviceTypeDeworming2.setPrerequisite("prerequisite|Deworming 1");
        serviceTypeDeworming2.setPreOffset("+6m");
        serviceTypeDeworming2.setUnits("200 mg");
        serviceTypeDeworming2.setDateEntity("encounter");

        ServiceType serviceTypeDeworming3 = new ServiceType();
        serviceTypeDeworming3.setName("Deworming_3");
        serviceTypeDeworming3.setPrerequisite("prerequisite|Deworming 2");
        serviceTypeDeworming3.setPreOffset("+6m");
        serviceTypeDeworming3.setUnits("400 mg");
        serviceTypeDeworming3.setDateEntity("encounter");

        Mockito.when(recurringServiceTypeRepository.searchByName("Deworming 1")).thenReturn(Arrays.asList(serviceTypeDeworming2, serviceTypeDeworming3));
        Mockito.when(processorForJava.processCaseModel(ArgumentMatchers.any(EventClient.class), ArgumentMatchers.any(Table.class))).thenReturn(contentValues);
        Mockito.when(contentValues.size()).thenReturn(7);
        Mockito.when(contentValues.getAsString(HeightRepository.ANMID)).thenReturn("provider");
        Mockito.when(contentValues.getAsString(HeightRepository.LOCATIONID)).thenReturn("lombwe");
        Mockito.when(contentValues.getAsString(RecurringServiceTypeRepository.NAME)).thenReturn("Deworming_1");
        Mockito.when(contentValues.getAsString(RecurringServiceRecordRepository.DATE)).thenReturn("2019-09-27 09:45:44");

        Mockito.when(contentValues.getAsString(HeightRepository.CREATED_AT)).thenReturn("2019-09-27 09:45:44");

        Event event = new Event();
        event.setEventId("231");
        event.setFormSubmissionId("1234");

        Table table = new Table();
        table.name = "recurring_service_records";
        Object[] params = {new EventClient(event, new Client("23")), table};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processService", params);

        Mockito.verify(recurringServiceRecordRepository).add(recordServiceArgumentCaptor.capture());
        ServiceRecord resultServiceRecord = recordServiceArgumentCaptor.getValue();
        Assert.assertEquals(Utils.getDate("2019-09-27 09:45:44"), resultServiceRecord.getDate());
        Assert.assertEquals(java.util.Optional.of(1L).get(), resultServiceRecord.getRecurringServiceId());
        Assert.assertEquals("Deworming 1", resultServiceRecord.getName());
        Assert.assertEquals(RecurringServiceRecordRepository.TYPE_Synced, resultServiceRecord.getSyncStatus());

        Assert.assertEquals("231", resultServiceRecord.getEventId());
        Assert.assertEquals("1234", resultServiceRecord.getFormSubmissionId());
        Assert.assertEquals("1234", resultServiceRecord.getFormSubmissionId());
        Assert.assertTrue(result);
    }

//    @Test
//    public void processBCScarEventWithValidEventClientShouldPassCorrectArgsToDetailsRepo() throws Exception {
//        PowerMockito.mockStatic(GizMalawiApplication.class);
//        PowerMockito.mockStatic(ChildDbUtils.class);
//        PowerMockito.when(GizMalawiApplication.getInstance()).thenReturn(gizMalawiApplication);
//        Mockito.when(gizMalawiApplication.context()).thenReturn(openSrpContext);
//        Mockito.when(openSrpContext.detailsRepository()).thenReturn(detailsRepository);
//        Event event = new Event();
//        event.setBaseEntityId("23213");
//        event.setEventDate(new DateTime());
//        Client client = new Client("23213");
//        Whitebox.invokeMethod(processorForJava, "processBCGScarEvent", new EventClient(event, client));
//        PowerMockito.verifyStatic(ChildDbUtils.class);
//    }

    @Test
    public void processWeightWithEventClientNullShouldReturnFalse() throws Exception {
        Object[] params = {null, null, true};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processWeight", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processWeightWithTableNullShouldReturnFalse() throws Exception {
        Object[] params = {new EventClient(new Event(), new Client("23")), null, true};
        Boolean result = Whitebox.invokeMethod(processorForJava, "processWeight", params);
        Assert.assertFalse(result);
    }

    @Test
    public void processWeightWithValidEventClientAndTableShouldReturnTrue() throws Exception {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);
        PowerMockito.when(gizMalawiApplication.weightRepository()).thenReturn(weightRepository);
        Mockito.when(processorForJava.processCaseModel(ArgumentMatchers.any(EventClient.class), ArgumentMatchers.any(Table.class))).thenReturn(contentValues);
        Mockito.when(contentValues.size()).thenReturn(7);
        Mockito.when(contentValues.getAsString(WeightRepository.DATE)).thenReturn("2019-09-27 09:45:44");
        Mockito.when(contentValues.getAsString(WeightRepository.BASE_ENTITY_ID)).thenReturn("234");
        Mockito.when(contentValues.containsKey(WeightRepository.KG)).thenReturn(true);
        Mockito.when(contentValues.getAsString(WeightRepository.KG)).thenReturn("20");
        Mockito.when(contentValues.getAsString(WeightRepository.ANMID)).thenReturn("provider");
        Mockito.when(contentValues.getAsString(WeightRepository.LOCATIONID)).thenReturn("lombwe");
        Mockito.when(contentValues.containsKey(WeightRepository.Z_SCORE)).thenReturn(true);
        Mockito.when(contentValues.getAsString(WeightRepository.Z_SCORE)).thenReturn("45.0");
        Mockito.when(contentValues.getAsString(WeightRepository.CREATED_AT)).thenReturn("2019-09-27 09:45:44");
        Table table = new Table();
        table.name = "weights";
        Event event = new Event();
        event.setEventId("231");
        event.setFormSubmissionId("343");
        Client client = new Client("234");
        EventClient eventClient = new EventClient(event, client);

        Boolean result = Whitebox.invokeMethod(processorForJava, "processWeight", eventClient, table, false);
        Mockito.verify(weightRepository).add(processWeightArgumentCaptor.capture());
        Weight resultWeightObj = processWeightArgumentCaptor.getValue();
        Assert.assertEquals(java.util.Optional.of(0).get(), resultWeightObj.getOutOfCatchment());
        Assert.assertEquals(Float.valueOf("20.0"), resultWeightObj.getKg());
        Assert.assertEquals(Double.valueOf("45.0"), resultWeightObj.getZScore());
        Assert.assertEquals("lombwe", resultWeightObj.getLocationId());
        Assert.assertEquals(WeightRepository.TYPE_Synced, resultWeightObj.getSyncStatus());
        Assert.assertEquals(Utils.getDate("2019-09-27 09:45:44"), resultWeightObj.getDate());
        Assert.assertEquals(Utils.getDate("2019-09-27 09:45:44"), resultWeightObj.getCreatedAt());
        Assert.assertTrue(result);
    }

    @Test
    public void processGizCoreEvents() {
    }

    @Test
    public void testOpdTransferHandlerShouldUpdateRegisterType() throws Exception {

        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", gizMalawiApplication);

        String baseEntityId = "324-sdr32423-234";
        Event event = new Event();
        event.setBaseEntityId(baseEntityId);
        Client client = new Client(baseEntityId);
        ClientClassification clientClassification = new ClientClassification();

        ClientRegisterTypeRepository clientRegisterTypeRepositoryMock = Mockito.mock(ClientRegisterTypeRepository.class);
        Mockito.doReturn(clientRegisterTypeRepositoryMock).when(gizMalawiApplication).registerTypeRepository();

        event.setEventType(GizConstants.EventType.OPD_ANC_TRANSFER);
        processorForJava.opdTransferHandler(new EventClient(event, client), GizConstants.EventType.OPD_ANC_TRANSFER, clientClassification);
        Mockito.verify(clientRegisterTypeRepositoryMock).add(Mockito.eq(GizConstants.RegisterType.ANC), Mockito.eq(baseEntityId));

        event.setEventType(GizConstants.EventType.OPD_CHILD_TRANSFER);
        processorForJava.opdTransferHandler(new EventClient(event, client), GizConstants.EventType.OPD_CHILD_TRANSFER, clientClassification);
        Mockito.verify(clientRegisterTypeRepositoryMock).add(Mockito.eq(GizConstants.RegisterType.CHILD), Mockito.eq(baseEntityId));

        event.setEventType(GizConstants.EventType.OPD_MATERNITY_TRANSFER);
        processorForJava.opdTransferHandler(new EventClient(event, client), GizConstants.EventType.OPD_MATERNITY_TRANSFER, clientClassification);
        Mockito.verify(clientRegisterTypeRepositoryMock).add(Mockito.eq(GizConstants.RegisterType.MATERNITY), Mockito.eq(baseEntityId));

        event.setEventType(GizConstants.EventType.OPD_PNC_TRANSFER);
        processorForJava.opdTransferHandler(new EventClient(event, client), GizConstants.EventType.OPD_PNC_TRANSFER, clientClassification);
        Mockito.verify(clientRegisterTypeRepositoryMock).add(Mockito.eq(GizConstants.RegisterType.PNC), Mockito.eq(baseEntityId));

        Mockito.verify(clientRegisterTypeRepositoryMock, Mockito.times(4)).removeAll(Mockito.eq(baseEntityId));

        Mockito.verify(processorForJava, Mockito.times(4)).processEvent(Mockito.eq(event),
                Mockito.eq(client), Mockito.eq(clientClassification));
    }

    @Test
    public void updateFTSsearch() {
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(DrishtiApplication.class, "mInstance", null);
    }
}