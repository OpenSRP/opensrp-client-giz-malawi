package org.smartregister.giz.configuration;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.child.interactor.ChildRegisterInteractor;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.pnc.PncLibrary;
import org.smartregister.pnc.config.PncMedicInfoFormProcessing;
import org.smartregister.pnc.pojo.PncEventClient;
import org.smartregister.pnc.utils.PncJsonFormUtils;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class GizPncMedicInfoFormProcessing extends PncMedicInfoFormProcessing {

    private ChildRegisterInteractor interactor;

    private HashMap<String, HashMap<String, String>> buildRepeatingGroupBorn;

    @Override
    protected void createChild(JSONObject jsonFormObject, String baseEntityId, HashMap<String, HashMap<String, String>> buildRepeatingGroupBorn) {
        setBuildRepeatingGroupBorn(buildRepeatingGroupBorn);
        super.createChild(jsonFormObject, baseEntityId, buildRepeatingGroupBorn);
    }

    @Override
    protected void saveAndProcessChildEvents(@NonNull List<PncEventClient> pncEventClients) {
        try {
            List<String> currentFormSubmissionIds = new ArrayList<>();
            for (PncEventClient eventClient : pncEventClients) {
                try {
                    Client baseClient = eventClient.getClient();
                    Event baseEvent = eventClient.getEvent();
                    JSONObject clientJson = new JSONObject(PncJsonFormUtils.gson.toJson(baseClient));
                    PncLibrary.getInstance().getEcSyncHelper().addClient(baseClient.getBaseEntityId(), clientJson);
                    JSONObject eventJson = new JSONObject(PncJsonFormUtils.gson.toJson(baseEvent));
                    PncLibrary.getInstance().getEcSyncHelper().addEvent(baseEvent.getBaseEntityId(), eventJson);
                    currentFormSubmissionIds.add(baseEvent.getFormSubmissionId());
                    createGrowthEvents(eventClient, clientJson);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            long lastSyncTimeStamp = Utils.getAllSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PncLibrary.getInstance().getClientProcessorForJava().processClient(PncLibrary.getInstance().getEcSyncHelper().getEvents(currentFormSubmissionIds));
            Utils.getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void createGrowthEvents(@NonNull PncEventClient eventClient, @NonNull JSONObject clientJson) throws JSONException {
        Client client = eventClient.getClient();
        GizUtils.createChildGrowthEventFromRepeatingGroup(clientJson, client, interactor(), getBuildRepeatingGroupBorn());
    }

    private ChildRegisterInteractor interactor() {
        if (interactor == null) {
            interactor = new ChildRegisterInteractor();
        }
        return interactor;
    }

    @NonNull
    public String childRegistrationEvent() {
        return MaternityConstants.EventType.BIRTH_REGISTRATION;
    }

    @NonNull
    public String getChildFormName() {
        return "child_enrollment";
    }

    @NonNull
    public String childOpensrpId() {
        return MaternityConstants.JSON_FORM_KEY.ZEIR_ID;
    }

    @NonNull
    public HashMap<String, String> childFormKeyToKeyMap() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("first_name", "baby_first_name");
        hashMap.put("last_name", "baby_last_name");
        hashMap.put("Date_Birth", "baby_dob");
        hashMap.put("Sex", "baby_gender");
        hashMap.put("Birth_Height", "birth_height_entered");
        hashMap.put("Birth_Weight", "birth_weight_entered");
        return hashMap;
    }

    @NonNull
    public HashMap<String, String> childKeyToColumnMap() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("mother_guardian_first_name", "first_name");
        hashMap.put("mother_guardian_last_name", "last_name");
        hashMap.put("mother_guardian_date_birth", "dob");
        hashMap.put("village", "home_address");
        hashMap.put("home_address", "village");
        hashMap.put("mother_hiv_status", "mother_hiv_status");
        hashMap.put("mother_tdv_doses", "mother_tdv_doses");
        return hashMap;
    }

    public HashMap<String, HashMap<String, String>> getBuildRepeatingGroupBorn() {
        return buildRepeatingGroupBorn;
    }

    public void setBuildRepeatingGroupBorn(HashMap<String, HashMap<String, String>> buildRepeatingGroupBorn) {
        this.buildRepeatingGroupBorn = buildRepeatingGroupBorn;
    }
}
