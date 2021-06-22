package org.smartregister.giz.task;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.FormEntityConstants;
import org.smartregister.domain.Client;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.util.CallableInteractor;
import org.smartregister.util.CallableInteractorCallBack;
import org.smartregister.util.GenericInteractor;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Callable;

import timber.log.Timber;

public class SaveReasonForDefaultingEventTask {
    private final String jsonString;
    private final String baseEntityId;
    private final EventClientRepository eventClientRepository;
    private final CallableInteractor interactor = new GenericInteractor();


    public SaveReasonForDefaultingEventTask(String jsonString, String baseEntityId,
                                            EventClientRepository eventClientRepository) {


        this.jsonString = jsonString;
        this.baseEntityId = baseEntityId;
        this.eventClientRepository = eventClientRepository;
    }

    public void run() {
        Callable<Void> callable = () -> {
            processReasonForDefaultingEvent();
            return null;
        };
        interactor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                Timber.v("Completed");
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
            }
        });
    }

    private void processReasonForDefaultingEvent() throws Exception {
        JSONObject jsonForm = new JSONObject(jsonString);


        JSONArray fields = ChildJsonFormUtils.fields(jsonForm);
        if (fields == null) {
            return;
        }

        String encounterType = ChildJsonFormUtils.getString(jsonForm, ChildJsonFormUtils.ENCOUNTER_TYPE);
        JSONObject metadata = ChildJsonFormUtils.getJSONObject(jsonForm, ChildJsonFormUtils.METADATA);

        FormTag formTag = ChildJsonFormUtils.formTag(GizUtils.getAllSharedPreferences());
        Event event = ChildJsonFormUtils.createEvent(fields, metadata,
                formTag, baseEntityId, encounterType, "");
        //add metadata
        addMetadata(fields, event, metadata);
    }

    private void addMetadata(JSONArray fields, Event event, JSONObject metadata) throws Exception {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = ChildJsonFormUtils.getJSONObject(fields, i);
            String value = ChildJsonFormUtils.getString(jsonObject, ChildJsonFormUtils.VALUE);
            if (StringUtils.isNotBlank(value)) {
                ChildJsonFormUtils.addObservation(event, jsonObject);
            }
        }

        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = ChildJsonFormUtils.getJSONObject(metadata, key);
                String value = ChildJsonFormUtils.getString(jsonObject, ChildJsonFormUtils.VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityValue = ChildJsonFormUtils.getString(jsonObject, ChildJsonFormUtils.OPENMRS_ENTITY);
                    if (entityValue != null) {
                        if (entityValue.equals(ChildJsonFormUtils.CONCEPT)) {
                            ChildJsonFormUtils.addToJSONObject(jsonObject, Constants.KEY.KEY, key);
                            ChildJsonFormUtils.addObservation(event, jsonObject);
                        } else if (ChildJsonFormUtils.ENCOUNTER_TYPE.equals(entityValue)) {
                            String entityIdValue = ChildJsonFormUtils.getString(jsonObject, ChildJsonFormUtils.OPENMRS_ENTITY_ID);
                            if (entityIdValue.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eventDate = ChildJsonFormUtils.formatDate(value, false);
                                if (eventDate != null) {
                                    event.setEventDate(eventDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (event != null) {
            JSONObject eventJson = new JSONObject(ChildJsonFormUtils.gson.toJson(event));
            eventClientRepository.addEvent(event.getBaseEntityId(), eventJson);
            GizMalawiApplication.getInstance().getClientProcessor().processClient(Arrays.asList(new EventClient(ChildJsonFormUtils.gson.fromJson(eventJson.toString(), org.smartregister.domain.Event.class), new Client(event.getBaseEntityId()))));
        }
    }

}
