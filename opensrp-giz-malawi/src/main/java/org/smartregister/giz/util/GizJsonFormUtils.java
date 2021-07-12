package org.smartregister.giz.util;

import android.content.Context;

import com.google.common.reflect.TypeToken;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.form.FormLocation;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.giz.BuildConfig;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class GizJsonFormUtils extends ChildJsonFormUtils {

    public static String getMetadataForEditForm(Context context, Map<String, String> childDetails, List<String> nonEditableFields) {

        try {
            JSONObject birthRegistrationForm = FormUtils.getInstance(context)
                    .getFormJson(Utils.metadata().childRegister.formName);
            updateRegistrationEventType(birthRegistrationForm);
            ChildJsonFormUtils.addRegistrationFormLocationHierarchyQuestions(birthRegistrationForm);

            if (birthRegistrationForm != null) {
                birthRegistrationForm.put(ChildJsonFormUtils.ENTITY_ID, childDetails.get(Constants.KEY.BASE_ENTITY_ID));
                birthRegistrationForm.put(ChildJsonFormUtils.ENCOUNTER_TYPE, Utils.metadata().childRegister.updateEventType);
                birthRegistrationForm.put(ChildJsonFormUtils.RELATIONAL_ID, childDetails.get(RELATIONAL_ID));
                birthRegistrationForm.put(ChildJsonFormUtils.CURRENT_ZEIR_ID,
                        Utils.getValue(childDetails, GizConstants.KEY.MALAWI_ID, true).replace("-",
                                ""));
                birthRegistrationForm.put(ChildJsonFormUtils.CURRENT_OPENSRP_ID,
                        Utils.getValue(childDetails, Constants.JSON_FORM_KEY.UNIQUE_ID, false));

                JSONObject metadata = birthRegistrationForm.getJSONObject(ChildJsonFormUtils.METADATA);
                metadata.put(ChildJsonFormUtils.ENCOUNTER_LOCATION,
                        ChildLibrary.getInstance().getLocationPickerView(context).getSelectedItem());

                //inject zeir id into the birthRegistrationForm
                JSONObject stepOne = birthRegistrationForm.getJSONObject(ChildJsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(ChildJsonFormUtils.FIELDS);
                updateFormDetailsForEdit(childDetails, jsonArray, nonEditableFields);
                return birthRegistrationForm.toString();
            }
        } catch (Exception e) {
            Timber.e(e, "GizJsonFormUtils --> getMetadataForEditForm");
        }

        return "";
    }

    private static void updateFormDetailsForEdit(Map<String, String> childDetails, JSONArray jsonArray, List<String> nonEditableFields)
            throws JSONException {
        String prefix;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            prefix = getPrefix(jsonObject);

            if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(Constants.KEY.PHOTO)) {
                processPhoto(childDetails.get(Constants.KEY.BASE_ENTITY_ID), jsonObject);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase("dob_unknown")) {
                getDobUnknown(childDetails, jsonObject);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(Constants.JSON_FORM_KEY.AGE)) {
                processAge(Utils.getValue(childDetails, Constants.KEY.DOB, false), jsonObject);
            } else if (jsonObject.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.DATE_PICKER)) {
                processDate(childDetails, prefix, jsonObject);
            } else if (jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY).equalsIgnoreCase(ChildJsonFormUtils.PERSON_INDENTIFIER)) {
                jsonObject.put(ChildJsonFormUtils.VALUE, Utils.getValue(childDetails,
                        jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID).toLowerCase(), true).replace("-", ""));
            } else if (jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY).equalsIgnoreCase(ChildJsonFormUtils.CONCEPT)) {
                jsonObject.put(ChildJsonFormUtils.VALUE,
                        getMappedValue(jsonObject.getString(ChildJsonFormUtils.KEY), childDetails));
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(GizConstants.KEY.MIDDLE_NAME)) {
                String middleName = Utils.getValue(childDetails, GizConstants.KEY.MIDDLE_NAME, true);
                jsonObject.put(ChildJsonFormUtils.VALUE, middleName);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(GizConstants.KEY.MOTHER_NRC_NUMBER)) {
                String nidNumber = Utils.getValue(childDetails, GizConstants.KEY.MOTHER_NRC_NUMBER, true);
                jsonObject.put(ChildJsonFormUtils.VALUE, nidNumber);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(GizConstants.KEY.MOTHER_SECOND_PHONE_NUMBER)) {
                String secondaryNumber = Utils.getValue(childDetails, GizConstants.KEY.MOTHER_SECOND_PHONE_NUMBER, true);
                jsonObject.put(ChildJsonFormUtils.VALUE, secondaryNumber);
            } else if (jsonObject.has(JsonFormConstants.TREE)) {
                processLocationTree(childDetails, jsonObject);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase("mother_guardian_first_name")) {
                String secondaryNumber = Utils.getValue(childDetails, GizConstants.KEY.MOTHER_FIRST_NAME, true);
                jsonObject.put(ChildJsonFormUtils.VALUE, secondaryNumber);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase("mother_guardian_last_name")) {
                String secondaryNumber = Utils.getValue(childDetails, GizConstants.KEY.MOTHER_LAST_NAME, true);
                jsonObject.put(ChildJsonFormUtils.VALUE, secondaryNumber);
            } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase("Sex")) {
                jsonObject.put(ChildJsonFormUtils.VALUE,
                        StringUtil.humanize(childDetails.get(ChildJsonFormUtils.GENDER)));
            } else {
                jsonObject.put(ChildJsonFormUtils.VALUE,
                        childDetails.get(jsonObject.optString(ChildJsonFormUtils.KEY)));
            }
            jsonObject.put(ChildJsonFormUtils.READ_ONLY, nonEditableFields.contains(jsonObject.getString(ChildJsonFormUtils.KEY)));
        }
    }

    private static void getDobUnknown(Map<String, String> childDetails, JSONObject jsonObject) throws JSONException {
        JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
        optionsObject.put(ChildJsonFormUtils.VALUE,
                Utils.getValue(childDetails, "dob_unknown", false));
    }

    @NotNull
    private static String getPrefix(JSONObject jsonObject) throws JSONException {
        String prefix;
        prefix = jsonObject.has(ChildJsonFormUtils.ENTITY_ID) && jsonObject.getString(ChildJsonFormUtils.ENTITY_ID)
                .equalsIgnoreCase(GizConstants.KEY.MOTHER) ? GizConstants.KEY.MOTHER_ : "";
        return prefix;
    }

    public static void tagEventSyncMetadata(Event event) {
        tagSyncMetadata(event);
    }

    private static void processLocationTree(Map<String, String> childDetails, JSONObject jsonObject) throws JSONException {
        updateHomeFacilityHierarchy(childDetails, jsonObject);
    }

    private static void updateHomeFacilityHierarchy(Map<String, String> childDetails, JSONObject jsonObject) throws JSONException {
        if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(GizConstants.KEY.HOME_FACILITY)) {
            List<String> homeFacilityHierarchy = LocationHelper.getInstance()
                    .getOpenMrsLocationHierarchy(Utils.getValue(childDetails,
                            GizConstants.KEY.HOME_FACILITY, false), false);
            String homeFacilityHierarchyString = AssetHandler
                    .javaToJsonString(homeFacilityHierarchy, new TypeToken<List<String>>() {
                    }.getType());
            ArrayList<String> allLevels = GizUtils.getHealthFacilityLevels();
            List<FormLocation> entireTree = LocationHelper.getInstance().generateLocationHierarchyTree(true, allLevels);
            String entireTreeString = AssetHandler.javaToJsonString(entireTree, new TypeToken<List<FormLocation>>() {
            }.getType());
            if (StringUtils.isNotBlank(homeFacilityHierarchyString)) {
                jsonObject.put(ChildJsonFormUtils.VALUE, homeFacilityHierarchyString);
                jsonObject.put(JsonFormConstants.TREE, new JSONArray(entireTreeString));
            }
        }
    }

    private static void updateRegistrationEventType(JSONObject form) throws JSONException {
        if (form.has(ChildJsonFormUtils.ENCOUNTER_TYPE) && form.getString(ChildJsonFormUtils.ENCOUNTER_TYPE)
                .equals(Constants.EventType.BITRH_REGISTRATION)) {
            form.put(ChildJsonFormUtils.ENCOUNTER_TYPE, Constants.EventType.UPDATE_BITRH_REGISTRATION);
        }

        if (form.has(ChildJsonFormUtils.STEP1) && form.getJSONObject(ChildJsonFormUtils.STEP1).has(GizConstants.KEY.TITLE) && form.getJSONObject(ChildJsonFormUtils.STEP1).getString(GizConstants.KEY.TITLE)
                .equals(Constants.EventType.BITRH_REGISTRATION)) {
            form.getJSONObject(ChildJsonFormUtils.STEP1).put(GizConstants.KEY.TITLE, GizConstants.FormTitleUtil.UPDATE_CHILD_FORM);
        }
    }

    public static String getJsonString(JSONObject jsonObject, String field) {
        try {
            if (jsonObject != null && jsonObject.has(field)) {
                String string = jsonObject.getString(field);
                if (StringUtils.isBlank(string)) {
                    return "";
                }

                return string;
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        return "";
    }

    public static JSONObject getGizJSonObject(JSONObject jsonObject, String field) {
        try {
            if (jsonObject != null && jsonObject.has(field)) {
                return jsonObject.getJSONObject(field);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        return null;
    }

    public static FormTag getFormTag(AllSharedPreferences allSharedPreferences) {
        FormTag formTag = new FormTag();
        formTag.providerId = allSharedPreferences.fetchRegisteredANM();
        formTag.appVersion = BuildConfig.VERSION_CODE;
        formTag.databaseVersion = BuildConfig.DATABASE_VERSION;
        return formTag;
    }
}
