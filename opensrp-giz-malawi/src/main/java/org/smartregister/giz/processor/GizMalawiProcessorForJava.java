package org.smartregister.giz.processor;

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.anc.library.sync.BaseAncClientProcessorForJava;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.child.util.ChildDbUtils;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.MoveToMyCatchmentUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.ClientField;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.domain.MonthlyTally;
import org.smartregister.giz.model.ReasonForDefaultingModel;
import org.smartregister.giz.repository.MonthlyTalliesRepository;
import org.smartregister.giz.repository.ReasonForDefaultingRepository;
import org.smartregister.giz.util.AppExecutors;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizReportUtils;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.growthmonitoring.domain.Height;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.service.intent.HeightIntentService;
import org.smartregister.growthmonitoring.service.intent.WeightIntentService;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.service.intent.RecurringIntentService;
import org.smartregister.immunization.service.intent.VaccineIntentService;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.opd.processor.OpdMiniClientProcessorForJava;
import org.smartregister.opd.utils.OpdConstants;
import org.smartregister.opd.utils.VisitUtils;
import org.smartregister.pnc.PncLibrary;
import org.smartregister.pnc.pojo.PncBaseDetails;
import org.smartregister.pnc.pojo.PncChild;
import org.smartregister.pnc.pojo.PncStillBorn;
import org.smartregister.pnc.processor.PncMiniClientProcessorForJava;
import org.smartregister.pnc.utils.PncConstants;
import org.smartregister.pnc.utils.PncDbConstants;
import org.smartregister.pnc.utils.PncUtils;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.MiniClientProcessorForJava;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class GizMalawiProcessorForJava extends ClientProcessorForJava {

    private static GizMalawiProcessorForJava instance;

    private HashMap<String, MiniClientProcessorForJava> processorMap = new HashMap<>();
    private HashMap<MiniClientProcessorForJava, List<Event>> unsyncEventsPerProcessor = new HashMap<>();
    private AppExecutors appExecutors = new AppExecutors();
    private HashMap<String, DateTime> clientsForAlertUpdates = new HashMap<>();
    private List<String> coreProcessedEvents = Arrays.asList(Constants.EventType.BITRH_REGISTRATION, Constants.EventType.UPDATE_BITRH_REGISTRATION,
            Constants.EventType.NEW_WOMAN_REGISTRATION, OpdConstants.EventType.OPD_REGISTRATION, OpdConstants.EventType.UPDATE_OPD_REGISTRATION,
            Constants.EventType.AEFI, Constants.EventType.ARCHIVE_CHILD_RECORD, ConstantsUtils.EventTypeUtils.ANC_MATERNITY_TRANSFER,
            ConstantsUtils.EventTypeUtils.CLOSE, GizConstants.EventType.OPD_CHILD_TRANSFER, GizConstants.EventType.OPD_MATERNITY_TRANSFER,
            GizConstants.EventType.OPD_ANC_TRANSFER, GizConstants.EventType.OPD_PNC_TRANSFER, GizConstants.EventType.REASON_FOR_DEFAULTING);

    private GizMalawiProcessorForJava(Context context) {
        super(context);

        BaseAncClientProcessorForJava baseAncClientProcessorForJava = new BaseAncClientProcessorForJava(context);
        GizMaternityProcessorForJava maternityMiniClientProcessorForJava = new GizMaternityProcessorForJava(context);
        PncMiniClientProcessorForJava pncMiniClientProcessorForJava = new PncMiniClientProcessorForJava(context);
        OpdMiniClientProcessorForJava opdMiniClientProcessorForJava = new OpdMiniClientProcessorForJava(context);

        addMiniProcessors(baseAncClientProcessorForJava, opdMiniClientProcessorForJava, maternityMiniClientProcessorForJava, pncMiniClientProcessorForJava);
    }

    public static GizMalawiProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new GizMalawiProcessorForJava(context);
        }
        return instance;
    }

    public void addMiniProcessors(MiniClientProcessorForJava... miniClientProcessorsForJava) {
        for (MiniClientProcessorForJava miniClientProcessorForJava : miniClientProcessorsForJava) {
            unsyncEventsPerProcessor.put(miniClientProcessorForJava, new ArrayList<Event>());

            HashSet<String> eventTypes = miniClientProcessorForJava.getEventTypes();

            for (String eventType : eventTypes) {
                processorMap.put(eventType, miniClientProcessorForJava);
            }
        }
    }

    @Override
    public synchronized void processClient(List<EventClient> eventClientList, boolean localSubmission) throws Exception {
        this.processClient(eventClientList);
    }

    @Override
    public void processClient(List<EventClient> eventClients) throws Exception {
        ClientClassification clientClassification = assetJsonToJava("ec_client_classification.json",
                ClientClassification.class);
        Table vaccineTable = assetJsonToJava("ec_client_vaccine.json", Table.class);
        Table weightTable = assetJsonToJava("ec_client_weight.json", Table.class);
        Table heightTable = assetJsonToJava("ec_client_height.json", Table.class);
        Table serviceTable = assetJsonToJava("ec_client_service.json", Table.class);
        if (!eventClients.isEmpty()) {
            List<Event> unsyncEvents = new ArrayList<>();
            for (EventClient eventClient : eventClients) {
                Event event = eventClient.getEvent();
                if (event == null) {
                    return;
                }

                String eventType = event.getEventType();
                if (eventType == null) {
                    continue;
                }

                if (eventType.equals(OpdConstants.OpdModuleEvents.OPD_CHECK_IN) || eventType.equals(OpdConstants.OpdModuleEvents.OPD_VITAL_DANGER_SIGNS_CHECK)|| eventType.equals(OpdConstants.OpdModuleEvents.OPD_DIAGNOSIS)
                        || eventType.equals(OpdConstants.OpdModuleEvents.OPD_PHARMACY) || eventType.equals(OpdConstants.OpdModuleEvents.OPD_TREATMENT) || eventType.equals(OpdConstants.OpdModuleEvents.OPD_LABORATORY)
                        || eventType.equals(OpdConstants.OpdModuleEvents.OPD_FINAL_OUTCOME) || eventType.equals(OpdConstants.OpdModuleEvents.OPD_SERVICE_CHARGE)) {
                    processVisitEvent(eventClients);
                }
                else if (eventType.equals(VaccineIntentService.EVENT_TYPE) || eventType
                        .equals(VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT)) {
                    processVaccinationEvent(vaccineTable, eventClient);
                } else if (eventType.equals(WeightIntentService.EVENT_TYPE) || eventType
                        .equals(WeightIntentService.EVENT_TYPE_OUT_OF_CATCHMENT)) {
                    processGMEvent(weightTable, heightTable, eventClient, eventType);
                } else if (eventType.equals(RecurringIntentService.EVENT_TYPE)) {
                    if (serviceTable == null) {
                        continue;
                    }
                    processService(eventClient, serviceTable);
                } else if (eventType.equals(ChildJsonFormUtils.BCG_SCAR_EVENT)) {
                    processBCGScarEvent(eventClient);
                } else if (eventType.equals(GizConstants.EventType.REASON_FOR_DEFAULTING)) {
                    clientProcessReasonForDefault(event);
                } else if (eventType.equals(MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT)) {
                    unsyncEvents.add(event);
                } else if (eventType.equalsIgnoreCase(Constants.EventType.DEATH)) {
                    if (processDeathEvent(eventClient, clientClassification)) {
                        unsyncEvents.add(event);
                    }
                } else if (eventType.equals(GizConstants.EventType.REPORT_CREATION)) {
                    processReport(event);
                    CoreLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(eventClient.getEvent().getFormSubmissionId());
                } else if (eventType.equals(GizConstants.EventType.MATERNITY_PNC_TRANSFER)) {
                    processMaternityPncTransfer(eventClient, clientClassification);
                    CoreLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(eventClient.getEvent().getFormSubmissionId());
                } else if (coreProcessedEvents.contains(eventType)) {
                    processGizCoreEvents(clientClassification, eventClient, event, eventType);
                } else if (processorMap.containsKey(eventType)) {
                    try {
                        if (eventType.equals(ConstantsUtils.EventTypeUtils.REGISTRATION) && eventClient.getClient() != null) {
                            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.ANC, event.getBaseEntityId());
                        } else if (eventType.equals(MaternityConstants.EventType.MATERNITY_REGISTRATION) && eventClient.getClient() != null) {
                            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.MATERNITY, event.getBaseEntityId());
                        } else if (eventType.equals(PncConstants.EventTypeConstants.PNC_REGISTRATION) && eventClient.getClient() != null) {
                            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.PNC, event.getBaseEntityId());
                        } else if (eventType.equals(MaternityConstants.EventType.MATERNITY_CLOSE)) {
                            HashMap<String, String> keyValues = GizUtils.generateKeyValuesFromEvent(event);
                            String closeReason = keyValues.get(MaternityConstants.JSON_FORM_KEY.MATERNITY_CLOSE_REASON);
                            if (StringUtils.isNotBlank(closeReason) && !("woman_died".equalsIgnoreCase(closeReason) || "wrong_entry".equalsIgnoreCase(closeReason))) {
                                GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                                GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.OPD, event.getBaseEntityId());
                            }
                        } else if (eventType.equals(PncConstants.EventTypeConstants.PNC_CLOSE)) {
                            HashMap<String, String> keyValues = GizUtils.generateKeyValuesFromEvent(event);
                            String closeReason = keyValues.get(PncConstants.JsonFormKeyConstants.PNC_CLOSE_REASON);
                            if (StringUtils.isNotBlank(closeReason) && !("woman_died".equalsIgnoreCase(closeReason) || "wrong_entry".equalsIgnoreCase(closeReason))) {
                                GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                                GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.OPD, event.getBaseEntityId());
                            }
                        }

                        processEventUsingMiniprocessor(clientClassification, eventClient, eventType);
                    } catch (Exception ex) {
                        Timber.e(ex);
                    }
                }
            }

            // Unsync events that are should not be in this device
            processUnsyncEvents(unsyncEvents);

            // Process alerts for clients
            /**
             Runnable runnable = () -> updateClientAlerts(clientsForAlertUpdates);

             appExecutors.diskIO().execute(runnable);
             **/
        }

    }

    private void processVisitEvent(List<EventClient> eventClients) {
        for (EventClient eventClient : eventClients) {
            VisitUtils.processVisit(eventClient);
        }
    }

    public void processGizCoreEvents(ClientClassification clientClassification, EventClient eventClient, Event event, String eventType) throws Exception {
        if (eventType.equals(OpdConstants.EventType.OPD_REGISTRATION) && eventClient.getClient() != null) {
            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.OPD, event.getBaseEntityId());
        } else if (eventType.equals(Constants.EventType.BITRH_REGISTRATION) && eventClient.getClient() != null &&
                (GizMalawiApplication.getInstance().context().getEventClientRepository().getEventsByBaseEntityIdAndEventType(event.getBaseEntityId(), Constants.EventType.ARCHIVE_CHILD_RECORD) == null)) {
            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.CHILD, event.getBaseEntityId());
        } else if (eventType.equals(Constants.EventType.NEW_WOMAN_REGISTRATION) && eventClient.getClient() != null) {
            GizMalawiApplication.getInstance().registerTypeRepository().addUnique(GizConstants.RegisterType.OPD, event.getBaseEntityId());
        } else if (eventType.equals(Constants.EventType.ARCHIVE_CHILD_RECORD) && eventClient.getClient() != null) {
            GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
        } else if (eventType.equals(ConstantsUtils.EventTypeUtils.CLOSE) && eventClient.getClient() != null) {
            GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
            if (!GizMalawiApplication.getInstance().gizEventRepository().hasEvent(event.getBaseEntityId(), ConstantsUtils.EventTypeUtils.ANC_MATERNITY_TRANSFER)) {
                HashMap<String, String> keyValues = GizUtils.generateKeyValuesFromEvent(event);
                String closeReason = keyValues.get("anc_close_reason");
                if (StringUtils.isNotBlank(closeReason) && !("woman_died".equalsIgnoreCase(closeReason) || "in_labour".equalsIgnoreCase(closeReason) || "wrong_entry".equalsIgnoreCase(closeReason))) {
                    GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                    GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.OPD, event.getBaseEntityId());
                }
            } else {
                GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.MATERNITY, event.getBaseEntityId());
            }
        } else if (eventType.equals(ConstantsUtils.EventTypeUtils.ANC_MATERNITY_TRANSFER) && eventClient.getClient() != null) {
            GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
            GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.MATERNITY, event.getBaseEntityId());
        } else {
            opdTransferHandler(eventClient, eventType, clientClassification);
        }

        if (clientClassification != null) {
            processEventClient(clientClassification, eventClient, event);
        }
    }

    public void opdTransferHandler(@NonNull EventClient eventClient,
                                   @NonNull String eventType,
                                   @NonNull ClientClassification clientClassification) throws Exception {
        Event event = eventClient.getEvent();
        if (eventClient.getClient() != null) {
            switch (eventType) {
                case GizConstants.EventType.OPD_ANC_TRANSFER:
                    GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                    GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.ANC, event.getBaseEntityId());
                    break;
                case GizConstants.EventType.OPD_CHILD_TRANSFER:
                    GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                    GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.CHILD, event.getBaseEntityId());
                    break;
                case GizConstants.EventType.OPD_MATERNITY_TRANSFER:
                    GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                    GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.MATERNITY, event.getBaseEntityId());
                    break;
                case GizConstants.EventType.OPD_PNC_TRANSFER:
                    GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
                    GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.PNC, event.getBaseEntityId());
                    break;
                default:
                    break;
            }
            processEvent(event, eventClient.getClient(), clientClassification);
        }
    }

    private void processMaternityPncTransfer(@NonNull EventClient eventClient, @NonNull ClientClassification clientClassification) throws Exception {
        if (eventClient.getClient() == null)
            return;
        Event event = eventClient.getEvent();
        HashMap<String, String> fieldsMap = GizUtils.generateKeyValuesFromEvent(event);
        String babiesBornMap = fieldsMap.get(MaternityConstants.JSON_FORM_KEY.BABIES_BORN_MAP);
        if (StringUtils.isNotBlank(babiesBornMap)) {
            processBabiesBorn(babiesBornMap, event);
        }

        String stillBornMap = fieldsMap.get(MaternityConstants.JSON_FORM_KEY.BABIES_STILL_BORN_MAP);
        if (StringUtils.isNotBlank(stillBornMap)) {
            processStillBorn(stillBornMap, event);
        }

        PncBaseDetails pncDetails = new PncBaseDetails(eventClient.getClient().getBaseEntityId(), event.getEventDate().toDate(), fieldsMap);
        pncDetails.setCreatedAt(new Date());

        PncLibrary.getInstance().getPncMedicInfoRepository().saveOrUpdate(pncDetails);

        GizMalawiApplication.getInstance().registerTypeRepository().removeAll(event.getBaseEntityId());
        GizMalawiApplication.getInstance().registerTypeRepository().add(GizConstants.RegisterType.PNC, event.getBaseEntityId());

        processEvent(event, eventClient.getClient(), clientClassification);
    }

    //TODO make it available from PNC
    private void processBabiesBorn(@androidx.annotation.Nullable String strBabiesBorn, @NonNull Event event) {
        if (StringUtils.isNotBlank(strBabiesBorn)) {
            try {
                JSONObject jsonObject = new JSONObject(strBabiesBorn);
                Iterator<String> repeatingGroupKeys = jsonObject.keys();
                while (repeatingGroupKeys.hasNext()) {
                    JSONObject jsonChildObject = jsonObject.optJSONObject(repeatingGroupKeys.next());
                    PncChild pncChild = new PncChild();
                    pncChild.setMotherBaseEntityId(event.getBaseEntityId());
                    pncChild.setDischargedAlive(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.DISCHARGED_ALIVE));
                    pncChild.setChildRegistered(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.CHILD_REGISTERED));
                    pncChild.setBirthRecordDate(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BIRTH_RECORD));
                    pncChild.setFirstName(jsonChildObject.optString(PncDbConstants.Column.PncBaby.BABY_FIRST_NAME));
                    pncChild.setLastName(jsonChildObject.optString(PncDbConstants.Column.PncBaby.BABY_LAST_NAME));
                    pncChild.setDob(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_DOB));
                    pncChild.setGender(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_GENDER));
                    pncChild.setWeightEntered(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BIRTH_WEIGHT_ENTERED));
                    pncChild.setWeight(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BIRTH_WEIGHT));
                    pncChild.setHeightEntered(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BIRTH_HEIGHT_ENTERED));
                    pncChild.setApgar(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.APGAR));
                    pncChild.setFirstCry(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_FIRST_CRY));
                    pncChild.setComplications(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_COMPLICATIONS));
                    pncChild.setComplicationsOther(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_COMPLICATIONS_OTHER));
                    pncChild.setCareMgt(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_CARE_MGMT));
                    pncChild.setCareMgtSpecify(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_CARE_MGMT_SPECIFY));
                    pncChild.setRefLocation(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BABY_REF_LOCATION));
                    pncChild.setBfFirstHour(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.BF_FIRST_HOUR));
                    pncChild.setChildHivStatus(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.CHILD_HIV_STATUS));
                    pncChild.setNvpAdministration(jsonChildObject.optString(PncConstants.JsonFormKeyConstants.NVP_ADMINISTRATION));
                    pncChild.setBaseEntityId(jsonChildObject.optString(PncDbConstants.Column.PncBaby.BASE_ENTITY_ID));
                    pncChild.setEventDate(PncUtils.convertDate(event.getEventDate().toDate(), PncDbConstants.DATE_FORMAT));
                    if (StringUtils.isNotBlank(pncChild.getBaseEntityId()) && StringUtils.isNotBlank(pncChild.getMotherBaseEntityId()))
                        PncLibrary.getInstance().getPncChildRepository().saveOrUpdate(pncChild);
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    //TODO make it available from PNC
    private void processStillBorn(@androidx.annotation.Nullable String strStillBorn, @NonNull Event event) {
        if (StringUtils.isNotBlank(strStillBorn)) {
            try {
                JSONObject jsonObject = new JSONObject(strStillBorn);
                Iterator<String> repeatingGroupKeys = jsonObject.keys();
                while (repeatingGroupKeys.hasNext()) {
                    JSONObject jsonTestObject = jsonObject.optJSONObject(repeatingGroupKeys.next());
                    PncStillBorn pncStillBorn = new PncStillBorn();
                    pncStillBorn.setMotherBaseEntityId(event.getBaseEntityId());
                    pncStillBorn.setStillBirthCondition(jsonTestObject.optString(PncConstants.JsonFormKeyConstants.STILL_BIRTH_CONDITION));
                    pncStillBorn.setEventDate(PncUtils.convertDate(event.getEventDate().toDate(), PncDbConstants.DATE_FORMAT));
                    PncLibrary.getInstance().getPncStillBornRepository().saveOrUpdate(pncStillBorn);
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }


    private void processReport(@NonNull Event event) {
        try {
            String reportJson = event.getDetails().get(GizConstants.ReportKeys.REPORT_JSON);
            if (StringUtils.isNotBlank(reportJson)) {
                JSONObject reportJsonObject = new JSONObject(reportJson);
                String reportMonth = reportJsonObject.optString(GizConstants.ReportKeys.REPORT_DATE);
                String reportGrouping = reportJsonObject.optString(GizConstants.ReportKeys.GROUPING);
                String providerId = reportJsonObject.optString(GizConstants.ReportKeys.PROVIDER_ID);
                String dateCreated = reportJsonObject.optString(GizConstants.ReportKeys.DATE_CREATED);
                DateTime dateSent = new DateTime(dateCreated);
                Date dReportMonth = MonthlyTalliesRepository.DF_YYYYMM.parse(reportMonth);
                JSONArray hia2Indicators = reportJsonObject.optJSONArray(GizConstants.ReportKeys.HIA2_INDICATORS);
                if (hia2Indicators != null) {
                    for (int i = 0; i < hia2Indicators.length(); i++) {
                        JSONObject jsonObject1 = hia2Indicators.optJSONObject(i);
                        String indicator = jsonObject1.optString(GizConstants.ReportKeys.INDICATOR_CODE);
                        String value = jsonObject1.optString(GizConstants.ReportKeys.VALUE);
                        MonthlyTally monthlyTally = new MonthlyTally();
                        monthlyTally.setEdited(false);
                        monthlyTally.setGrouping(reportGrouping);
                        monthlyTally.setIndicator(indicator);
                        monthlyTally.setProviderId(providerId);
                        monthlyTally.setValue(value);
                        monthlyTally.setDateSent(dateSent.toDate());
                        monthlyTally.setCreatedAt(dateSent.toDate());
                        monthlyTally.setMonth(dReportMonth);
                        GizMalawiApplication.getInstance().monthlyTalliesRepository().save(monthlyTally);
                    }
                }
            }

        } catch (JSONException e) {
            Timber.e(e);
        } catch (ParseException e) {
            Timber.e(e);
        }

    }

    private void updateClientAlerts(@NonNull HashMap<String, DateTime> clientsForAlertUpdates) {
        try {
            HashMap<String, DateTime> stringDateTimeHashMap = SerializationUtils.clone(clientsForAlertUpdates);
            for (String baseEntityId : stringDateTimeHashMap.keySet()) {
                DateTime birthDateTime = clientsForAlertUpdates.get(baseEntityId);
                if (birthDateTime != null) {
                    updateOfflineAlerts(baseEntityId, birthDateTime);
                }
            }
            clientsForAlertUpdates.clear();
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    private boolean processDeathEvent(@NonNull EventClient eventClient, ClientClassification clientClassification) {
        try {
            GizMalawiApplication.getInstance().registerTypeRepository().removeAll(eventClient.getEvent().getBaseEntityId());
            processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
        } catch (Exception e) {
            Timber.e(e);
        }
        return GizUtils.updateClientDeath(eventClient);
    }

    private void processUnsyncEvents(@NonNull List<Event> unsyncEvents) {
        if (!unsyncEvents.isEmpty()) {
            unSync(unsyncEvents);
        }

        for (MiniClientProcessorForJava miniClientProcessorForJava : unsyncEventsPerProcessor.keySet()) {
            List<Event> processorUnsyncEvents = unsyncEventsPerProcessor.get(miniClientProcessorForJava);
            miniClientProcessorForJava.unSync(processorUnsyncEvents);
        }
    }

    private void processEventClient(@NonNull ClientClassification clientClassification, @NonNull EventClient eventClient, @NonNull Event event) {
        Client client = eventClient.getClient();
        if (client != null) {
            try {
                processEvent(event, client, clientClassification);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void processEventUsingMiniprocessor(ClientClassification clientClassification, EventClient eventClient, String eventType) throws Exception {
        MiniClientProcessorForJava miniClientProcessorForJava = processorMap.get(eventType);
        if (miniClientProcessorForJava != null) {
            List<Event> processorUnsyncEvents = unsyncEventsPerProcessor.get(miniClientProcessorForJava);
            if (processorUnsyncEvents == null) {
                processorUnsyncEvents = new ArrayList<Event>();
                unsyncEventsPerProcessor.put(miniClientProcessorForJava, processorUnsyncEvents);
            }

            completeProcessing(eventClient.getEvent());
            miniClientProcessorForJava.processEventClient(eventClient, processorUnsyncEvents, clientClassification);
        }
    }

    private void processGMEvent(Table weightTable, Table heightTable, EventClient eventClient, String eventType) throws Exception {
        if (weightTable == null) {
            return;
        }

        if (heightTable == null) {
            return;
        }
        Event event = eventClient.getEvent();
        if (event != null && StringUtils.isNotBlank(event.getEntityType())) {
            String entityType = event.getEntityType();
            if (HeightIntentService.ENTITY_TYPE.equalsIgnoreCase(entityType)) {
                processHeight(eventClient, heightTable,
                        eventType.equals(HeightIntentService.EVENT_TYPE_OUT_OF_CATCHMENT));
            } else if (WeightIntentService.ENTITY_TYPE.equalsIgnoreCase(entityType)) {
                processWeight(eventClient, weightTable,
                        eventType.equals(WeightIntentService.EVENT_TYPE_OUT_OF_CATCHMENT));
            } else {
                Timber.e("GM event %s has an unknown entity type -> %s", event.getFormSubmissionId(), event.getEntityType());
            }
        } else {
            Timber.e("GM event %s does not have entity type", event.getFormSubmissionId());
        }
    }

    private void processVaccinationEvent(Table vaccineTable, EventClient eventClient) throws Exception {
        if (vaccineTable != null) {

            Client client = eventClient.getClient();
            Event event = eventClient.getEvent();
            if (!childExists(client.getBaseEntityId())) {
                List<String> createCase = new ArrayList<>();
                createCase.add(GizConstants.TABLE_NAME.ALL_CLIENTS);
                processCaseModel(event, client, createCase);
            }

            processVaccine(eventClient, vaccineTable,
                    VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT.equals(event.getEventType()));

            scheduleUpdatingClientAlerts(client.getBaseEntityId(), client.getBirthdate());
        }
    }

    private boolean childExists(String entityId) {
        return GizMalawiApplication.getInstance().eventClientRepository().checkIfExists(EventClientRepository.Table.client, entityId);
    }

    private Boolean processVaccine(@Nullable EventClient vaccine, @Nullable Table vaccineTable, boolean outOfCatchment) {
        try {
            if (vaccine == null || vaccine.getEvent() == null) {
                return false;
            }

            if (vaccineTable == null) {
                return false;
            }

            Timber.d("Starting processVaccine table: %s", vaccineTable.name);

            ContentValues contentValues = processCaseModel(vaccine, vaccineTable);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(contentValues.getAsString(VaccineRepository.DATE));

                VaccineRepository vaccineRepository = GizMalawiApplication.getInstance().vaccineRepository();
                Vaccine vaccineObj = new Vaccine();
                vaccineObj.setBaseEntityId(contentValues.getAsString(VaccineRepository.BASE_ENTITY_ID));
                vaccineObj.setName(contentValues.getAsString(VaccineRepository.NAME));
                if (contentValues.containsKey(VaccineRepository.CALCULATION)) {
                    vaccineObj.setCalculation(parseInt(contentValues.getAsString(VaccineRepository.CALCULATION)));
                }
                vaccineObj.setDate(date);
                vaccineObj.setAnmId(contentValues.getAsString(VaccineRepository.ANMID));
                vaccineObj.setLocationId(contentValues.getAsString(VaccineRepository.LOCATION_ID));
                vaccineObj.setSyncStatus(VaccineRepository.TYPE_Synced);
                vaccineObj.setFormSubmissionId(vaccine.getEvent().getFormSubmissionId());
                vaccineObj.setEventId(vaccine.getEvent().getEventId());
                vaccineObj.setOutOfCatchment(outOfCatchment ? 1 : 0);

                String createdAtString = contentValues.getAsString(VaccineRepository.CREATED_AT);
                Date createdAt = getDate(createdAtString);
                vaccineObj.setCreatedAt(createdAt);

                Utils.addVaccine(vaccineRepository, vaccineObj);

                Timber.d("Ending processVaccine table: %s", vaccineTable.name);
            }
            return true;

        } catch (Exception e) {
            Timber.e(e, "Process Vaccine Error");
            return null;
        }
    }

    private void processWeight(@NonNull EventClient weightEventClient,
                               @Nullable Table weightTable, boolean outOfCatchment) {

        try {

            if (weightTable == null) {
                return;
            }

            Timber.d("Starting processWeight table: %s", weightTable.name);

            ContentValues contentValues = processCaseModel(weightEventClient, weightTable);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                String eventDateStr = contentValues.getAsString(WeightRepository.DATE);
                Date date = getDate(eventDateStr);

                WeightRepository weightRepository = GizMalawiApplication.getInstance().weightRepository();
                Weight weightObj = new Weight();
                weightObj.setBaseEntityId(contentValues.getAsString(WeightRepository.BASE_ENTITY_ID));
                if (contentValues.containsKey(WeightRepository.KG)) {
                    weightObj.setKg(parseFloat(contentValues.getAsString(WeightRepository.KG)));
                }
                weightObj.setDate(date);
                weightObj.setAnmId(contentValues.getAsString(WeightRepository.ANMID));
                weightObj.setLocationId(contentValues.getAsString(WeightRepository.LOCATIONID));
                weightObj.setSyncStatus(WeightRepository.TYPE_Synced);
                weightObj.setFormSubmissionId(weightEventClient.getEvent().getFormSubmissionId());
                weightObj.setEventId(weightEventClient.getEvent().getEventId());
                weightObj.setOutOfCatchment(outOfCatchment ? 1 : 0);

                if (contentValues.containsKey(WeightRepository.Z_SCORE)) {
                    String zscoreString = contentValues.getAsString(WeightRepository.Z_SCORE);
                    if (NumberUtils.isNumber(zscoreString)) {
                        weightObj.setZScore(Double.valueOf(zscoreString));
                    }
                }

                String createdAtString = contentValues.getAsString(WeightRepository.CREATED_AT);
                Date createdAt = getDate(createdAtString);
                weightObj.setCreatedAt(createdAt);

                weightRepository.add(weightObj);

                Timber.d("Ending processWeight table: %s", weightTable.name);
            }

        } catch (Exception e) {
            Timber.e(e, "Process Weight Error");
        }
    }

    private void processHeight(@NonNull EventClient heightEventClient, @Nullable Table heightTable,
                               boolean outOfCatchment) {

        try {

            if (heightTable == null) {
                return;
            }

            Timber.d("Starting processHeight table: %s", heightTable.name);

            ContentValues contentValues = processCaseModel(heightEventClient, heightTable);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                String eventDateStr = contentValues.getAsString(HeightRepository.DATE);
                Date date = getDate(eventDateStr);

                HeightRepository heightRepository = GizMalawiApplication.getInstance().heightRepository();
                Height heightObject = new Height();
                heightObject.setBaseEntityId(contentValues.getAsString(WeightRepository.BASE_ENTITY_ID));
                if (contentValues.containsKey(HeightRepository.CM)) {
                    heightObject.setCm(parseFloat(contentValues.getAsString(HeightRepository.CM)));
                }
                heightObject.setDate(date);
                heightObject.setAnmId(contentValues.getAsString(HeightRepository.ANMID));
                heightObject.setLocationId(contentValues.getAsString(HeightRepository.LOCATIONID));
                heightObject.setSyncStatus(HeightRepository.TYPE_Synced);
                heightObject.setFormSubmissionId(heightEventClient.getEvent().getFormSubmissionId());
                heightObject.setEventId(heightEventClient.getEvent().getEventId());
                heightObject.setOutOfCatchment(outOfCatchment ? 1 : 0);

                if (contentValues.containsKey(HeightRepository.Z_SCORE)) {
                    String zScoreString = contentValues.getAsString(HeightRepository.Z_SCORE);
                    if (NumberUtils.isNumber(zScoreString)) {
                        heightObject.setZScore(Double.valueOf(zScoreString));
                    }
                }

                String createdAtString = contentValues.getAsString(HeightRepository.CREATED_AT);
                Date createdAt = getDate(createdAtString);
                heightObject.setCreatedAt(createdAt);

                heightRepository.add(heightObject);

                Timber.d("Ending processHeight table: %s", heightTable.name);
            }

        } catch (Exception e) {
            Timber.e(e, "Process Height Error");
        }
    }

    private Boolean processService(EventClient service, Table serviceTable) {
        try {
            if (service == null || service.getEvent() == null) {
                return false;
            }

            if (serviceTable == null) {
                return false;
            }

            Timber.d("Starting processService table: %s", serviceTable.name);

            ContentValues contentValues = processCaseModel(service, serviceTable);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                String name = getServiceTypeName(contentValues);

                String eventDateStr = contentValues.getAsString(RecurringServiceRecordRepository.DATE);
                Date date = getDate(eventDateStr);

                String value = null;

                if (StringUtils.containsIgnoreCase(name, "ITN")) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String itnDateString = contentValues.getAsString("itn_date");
                    if (StringUtils.isNotBlank(itnDateString)) {
                        date = simpleDateFormat.parse(itnDateString);
                    }

                    value = getServiceValue(contentValues);

                }

                List<ServiceType> serviceTypeList = getServiceTypes(name);
                if (serviceTypeList == null || serviceTypeList.isEmpty()) {
                    return false;
                }

                if (date == null) {
                    return false;
                }

                recordServiceRecord(service, contentValues, name, date, value, serviceTypeList);

                Timber.d("Ending processService table: %s", serviceTable.name);
            }
            return true;

        } catch (Exception e) {
            Timber.e(e, "Process Service Error");
            return null;
        }
    }

    @NotNull
    private String getServiceValue(ContentValues contentValues) {
        String value;
        value = RecurringIntentService.ITN_PROVIDED;
        if (contentValues.getAsString("itn_has_net") != null) {
            value = RecurringIntentService.CHILD_HAS_NET;
        }
        return value;
    }

    @Nullable
    private String getServiceTypeName(ContentValues contentValues) {
        String name = contentValues.getAsString(RecurringServiceTypeRepository.NAME);
        if (StringUtils.isNotBlank(name)) {
            name = name.replaceAll("_", " ").replace("dose", "").trim();
        }
        return name;
    }

    private void recordServiceRecord(EventClient service, ContentValues contentValues, String name, Date date, String value, List<ServiceType> serviceTypeList) {
        RecurringServiceRecordRepository recurringServiceRecordRepository = GizMalawiApplication.getInstance()
                .recurringServiceRecordRepository();
        ServiceRecord serviceObj = getServiceRecord(service, contentValues, name, date, value, serviceTypeList);
        String createdAtString = contentValues.getAsString(RecurringServiceRecordRepository.CREATED_AT);
        Date createdAt = getDate(createdAtString);
        serviceObj.setCreatedAt(createdAt);

        recurringServiceRecordRepository.add(serviceObj);
    }

    private List<ServiceType> getServiceTypes(String name) {
        RecurringServiceTypeRepository recurringServiceTypeRepository = GizMalawiApplication.getInstance()
                .recurringServiceTypeRepository();
        return recurringServiceTypeRepository.searchByName(name);
    }

    private void processBCGScarEvent(EventClient bcgScarEventClient) {
        if (bcgScarEventClient == null || bcgScarEventClient.getEvent() == null) {
            return;
        }

        Event event = bcgScarEventClient.getEvent();
        String baseEntityId = event.getBaseEntityId();
        DateTime eventDate = event.getEventDate();
        long date = 0;
        if (eventDate != null) {
            date = eventDate.getMillis();
        }

        ChildDbUtils.updateChildDetailsValue(Constants.SHOW_BCG_SCAR, String.valueOf(date), baseEntityId);
    }

    private boolean unSync(List<Event> events) {
        try {

            if (events == null || events.isEmpty()) {
                return false;
            }

            ClientField clientField = assetJsonToJava("ec_client_fields.json", ClientField.class);
            return clientField != null;

        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }

    @VisibleForTesting
    ContentValues processCaseModel(EventClient eventClient, Table table) {
        try {
            List<Column> columns = table.columns;
            ContentValues contentValues = new ContentValues();

            for (Column column : columns) {
                processCaseModel(eventClient.getEvent(), eventClient.getClient(), column, contentValues);
            }

            return contentValues;
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private Integer parseInt(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            Timber.e(e, e.toString());
        }
        return null;
    }

    @Nullable
    private Date getDate(@Nullable String eventDateStr) {
        Date date = null;
        if (StringUtils.isNotBlank(eventDateStr)) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
                date = dateFormat.parse(eventDateStr);
            } catch (ParseException e) {
                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    date = dateFormat.parse(eventDateStr);
                } catch (ParseException pe) {
                    try {
                        date = DateUtil.parseDate(eventDateStr);
                    } catch (ParseException pee) {
                        Timber.e(e);
                    }
                }
            }
        }
        return date;
    }

    private Float parseFloat(String string) {
        try {
            return Float.valueOf(string);
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        return null;
    }

    @NotNull
    private ServiceRecord getServiceRecord(EventClient service, ContentValues contentValues, String name, Date date,
                                           String value, List<ServiceType> serviceTypeList) {
        ServiceRecord serviceObj = new ServiceRecord();
        serviceObj.setBaseEntityId(contentValues.getAsString(RecurringServiceRecordRepository.BASE_ENTITY_ID));
        serviceObj.setName(name);
        serviceObj.setDate(date);
        serviceObj.setAnmId(contentValues.getAsString(RecurringServiceRecordRepository.ANMID));
        serviceObj.setLocationId(contentValues.getAsString(RecurringServiceRecordRepository.LOCATION_ID));
        serviceObj.setSyncStatus(RecurringServiceRecordRepository.TYPE_Synced);
        serviceObj.setFormSubmissionId(service.getEvent().getFormSubmissionId());
        serviceObj.setEventId(service.getEvent().getEventId()); //FIXME hard coded id
        serviceObj.setValue(value);
        serviceObj.setRecurringServiceId(serviceTypeList.get(0).getId());
        return serviceObj;
    }

    @Override
    public void updateFTSsearch(String tableName, String entityId, ContentValues contentValues) {

        Timber.d("Starting updateFTSsearch table: %s", tableName);

        AllCommonsRepository allCommonsRepository = GizMalawiApplication.getInstance().context().
                allCommonsRepositoryobjects(tableName);

        if (allCommonsRepository != null) {
            allCommonsRepository.updateSearch(entityId);
        }

        boolean isInRegister = GizMalawiApplication.getInstance().registerTypeRepository().findByRegisterType(entityId, GizConstants.RegisterType.CHILD);

        if (contentValues != null &&
                GizConstants.TABLE_NAME.ALL_CLIENTS.equals(tableName) &&
                isInRegister) {
            String dobString = contentValues.getAsString(Constants.KEY.DOB);
            if (StringUtils.isNotBlank(dobString)) {
                DateTime birthDateTime = Utils.dobStringToDateTime(dobString);
                if (birthDateTime != null) {
                    updateOfflineAlerts(entityId, birthDateTime);
                }
            }
        }
        Timber.d("Finished updateFTSsearch table: %s", tableName);
    }

    protected void updateOfflineAlerts(@NonNull String entityId, @NonNull DateTime birthDateTime) {
        VaccineSchedule.updateOfflineAlerts(entityId, birthDateTime, GizConstants.RegisterType.CHILD);
        ServiceSchedule.updateOfflineAlerts(entityId, birthDateTime);
    }

    @Override
    public String[] getOpenmrsGenIds() {
        return new String[]{"zeir_id"};
    }

    private void scheduleUpdatingClientAlerts(@NonNull String baseEntityId, @NonNull DateTime dateTime) {
        if (!clientsForAlertUpdates.containsKey(baseEntityId)) {
            clientsForAlertUpdates.put(baseEntityId, dateTime);
        }
    }

    private void clientProcessReasonForDefault(Event event) throws JsonProcessingException {
        if (event != null) {
            List<Obs> reasonForDefaultingObs = event.getObs();
            ReasonForDefaultingModel reasonForDefaultingModel = GizReportUtils.getReasonForDefaultingModel(reasonForDefaultingObs);
            reasonForDefaultingModel.setDateCreated(event.getEventDate().toString());
            reasonForDefaultingModel.setBaseEntityId(event.getBaseEntityId());
            if (reasonForDefaultingModel != null) {
                ReasonForDefaultingRepository repo = GizMalawiApplication.getInstance().reasonForDefaultingRepository();
                String formSubmissionId = event.getFormSubmissionId();
                reasonForDefaultingModel.setId(formSubmissionId);
                repo.addOrUpdate(reasonForDefaultingModel);
            }
        }
    }


}