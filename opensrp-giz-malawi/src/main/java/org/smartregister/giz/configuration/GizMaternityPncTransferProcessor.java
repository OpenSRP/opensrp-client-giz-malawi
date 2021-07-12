package org.smartregister.giz.configuration;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.anc.library.util.Utils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.contract.ClientTransferProcessor;
import org.smartregister.giz.util.GizJsonFormUtils;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.maternity.utils.MaternityDbConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.child.util.ChildJsonFormUtils.METADATA;

public class GizMaternityPncTransferProcessor implements ClientTransferProcessor {

    private String baseEntityId;

    @Override
    public void startTransferProcessing(@NonNull JSONObject maternityOutcome) throws Exception {
        String baseEntityId = maternityOutcome.optString("entity_id");
        setBaseEntityId(baseEntityId);
        Event maternityTransferEvent = createMaternityPncTransferEvent(maternityOutcome);

        JSONObject eventJson = new JSONObject(GizJsonFormUtils.gson.toJson(maternityTransferEvent));
        GizMalawiApplication.getInstance().getEcSyncHelper().addEvent(baseEntityId, eventJson);

        //update sync time & initiateEventProcessing
        initEventProcessing(maternityTransferEvent);
    }

    public void initEventProcessing(Event maternityTransferEvent) throws Exception {
        long lastSyncTimeStamp = Utils.getAllSharedPreferences().fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);
        GizMalawiApplication.getInstance().getClientProcessor()
                .processClient(GizMalawiApplication.getInstance().getEcSyncHelper().getEvents(Collections.singletonList(maternityTransferEvent.getFormSubmissionId())));
        Utils.getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
    }

    protected Event createMaternityPncTransferEvent(@NonNull JSONObject outcomeForm) {
        JSONObject jsonFormObject = populateTransferForm(outcomeForm);
        FormTag formTag = GizJsonFormUtils.getFormTag(Utils.getAllSharedPreferences());
        Event event = GizJsonFormUtils.createEvent(FormUtils.getMultiStepFormFields(jsonFormObject),
                jsonFormObject.optJSONObject(METADATA), formTag, getBaseEntityId(), jsonFormObject.optString(JsonFormConstants.ENCOUNTER_TYPE), "");
        GizJsonFormUtils.tagEventSyncMetadata(event);
        return event;
    }

    @Override
    public String transferForm() {
        return "maternity_pnc_transfer";
    }

    @Override
    public Map<String, String> columnMap() {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> details() {
        //fetch maternity medic info
        try {
            if (StringUtils.isNotBlank(baseEntityId)) {
                return GizMalawiApplication.getInstance().eventClientRepository().rawQuery(GizMalawiApplication.getInstance().eventClientRepository().getReadableDatabase(),
                        "select * from " + "maternity_medic_info mmi left join maternity_registration_details mrd on mmi.base_entity_id = mrd.base_entity_id " +
                                " where mmi." + MaternityDbConstants.Column.MaternityDetails.BASE_ENTITY_ID + " = '" + getBaseEntityId() + "' limit 1").get(0);
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            Timber.e(e);
        }
        return new HashMap<>();
    }

    @Override
    public JSONObject populateTransferForm(JSONObject maternityOutComeForm) {
        JSONObject jsonForm = null;
        try {
            if (StringUtils.isNotBlank(transferForm())) {
                jsonForm = Utils.getFormUtils().getFormJson(transferForm());
                if (jsonForm != null) {
                    Map<String, String> clientDetails = details();
                    Map<String, String> columnMap = columnMap();
                    JSONArray fields = jsonForm.optJSONObject(JsonFormConstants.STEP1)
                            .optJSONArray(JsonFormConstants.FIELDS);

                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject object = fields.optJSONObject(i);
                        String key = object.optString(JsonFormConstants.KEY);

                        if (clientDetails.get(key) != null) {
                            updateValue(object, clientDetails.get(key));
                        }

                        if (columnMap.get(key) != null) {
                            object.put(JsonFormConstants.KEY, columnMap.get(key));
                        }
                    }

                    JSONArray outcomeFormFields = GizUtils.getMultiStepFormFields(maternityOutComeForm);
                    List<String> closeFormFieldsList = getOutcomeFormFieldsList();
                    for (int i = 0; i < outcomeFormFields.length() && !closeFormFieldsList.isEmpty(); i++) {
                        JSONObject object = outcomeFormFields.optJSONObject(i);
                        String key = object.optString(JsonFormConstants.KEY);
                        if (closeFormFieldsList.contains(key)) {
                            calculatedFields(fields, object);
                            fields.put(object);
                            closeFormFieldsList.remove(key);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
        return jsonForm;
    }

    private void calculatedFields(JSONArray fields, JSONObject object) {
        try {
            String key = object.optString(JsonFormConstants.KEY);
            if (MaternityConstants.JSON_FORM_KEY.BABIES_BORN_MAP.equals(key) && object.has(JsonFormConstants.VALUE)) {
                Pair<Integer, Integer> integerPair = getNumberOfDischarged(object.optString(JsonFormConstants.VALUE));
                JSONObject survivingChildJsonObject = new JSONObject();
                survivingChildJsonObject.put(JsonFormConstants.KEY, "surviving_child");
                survivingChildJsonObject.put(JsonFormConstants.TYPE, "hidden");
                survivingChildJsonObject.put(JsonFormConstants.VALUE, "yes");
                fields.put(survivingChildJsonObject);

                JSONObject survivingChildCountJsonObject = new JSONObject();
                survivingChildCountJsonObject.put(JsonFormConstants.KEY, "surviving_child_count");
                survivingChildCountJsonObject.put(JsonFormConstants.TYPE, "hidden");
                survivingChildCountJsonObject.put(JsonFormConstants.VALUE, String.valueOf(integerPair.getLeft()));
                fields.put(survivingChildCountJsonObject);


                JSONObject neonatalobject = new JSONObject();
                neonatalobject.put(JsonFormConstants.KEY, "neonatal_death");
                neonatalobject.put(JsonFormConstants.TYPE, "hidden");
                neonatalobject.put(JsonFormConstants.VALUE, "yes");
                fields.put(neonatalobject);

                JSONObject neonatalDeathCountObject = new JSONObject();
                neonatalDeathCountObject.put(JsonFormConstants.KEY, "neonatal_death_count");
                neonatalDeathCountObject.put(JsonFormConstants.TYPE, "hidden");
                neonatalDeathCountObject.put(JsonFormConstants.VALUE, String.valueOf(integerPair.getRight()));
                fields.put(neonatalDeathCountObject);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    private Pair<Integer, Integer> getNumberOfDischarged(String strObject) {
        if (StringUtils.isNotBlank(strObject)) {
            int alive = 0;
            int dead = 0;
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(strObject);
                Iterator<String> repeatingGroupKeys = jsonObject.keys();
                while (repeatingGroupKeys.hasNext()) {
                    JSONObject jsonChildObject = jsonObject.optJSONObject(repeatingGroupKeys.next());
                    String dischargedAlive = jsonChildObject.optString(MaternityConstants.JSON_FORM_KEY.DISCHARGED_ALIVE);
                    if (StringUtils.isNotBlank(dischargedAlive) && dischargedAlive.equalsIgnoreCase("yes")) {
                        ++alive;
                    } else {
                        ++dead;
                    }
                }
                return Pair.of(alive, dead);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return Pair.of(0, 0);

    }

    private List<String> getOutcomeFormFieldsList() {
        return new ArrayList<>(Arrays.asList("mother_tdv_doses", "protected_at_birth", "delivery_date", "delivery_time"
                , "delivery_place", "delivery_person", "delivery_person_other", "delivery_mode",
                "delivery_mode_other", "obstetic _complications",
                "obstretic_complications_other", "obstretic_care", "emergency_obstretic_care_other", "referred_out"
                , "vit_a", "discharge_status", "hiv_status_previous", "hiv_status_current", "on_art_treatment",
                "art_clinic_number", "hiv_treatment_start", "not_art_reason", "not_art_reasons_other", MaternityConstants.JSON_FORM_KEY.BABIES_BORN_MAP, MaternityConstants.JSON_FORM_KEY.BABIES_STILL_BORN_MAP));
    }


    protected void updateValue(@NonNull JSONObject object, @NonNull String value) {
        try {
            String type = object.optString(JsonFormConstants.TYPE);

            if (JsonFormConstants.CHECK_BOX.equals(type) && !value.isEmpty()) {
                JSONArray jsonArray = new JSONArray(value);
                object.put(JsonFormConstants.VALUE, jsonArray);
            } else {
                object.put(JsonFormConstants.VALUE, ((Object) value).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }
}
