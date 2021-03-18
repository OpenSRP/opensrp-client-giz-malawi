package org.smartregister.giz.model;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizJsonFormUtils;

import java.util.Date;

public class CheckChildDetailsModel {
    private boolean myResult;
    private JSONObject client;
    private String entityId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String dob;
    private String zeirId;
    private String inactive;
    private String lostToFollowUp;

    public CheckChildDetailsModel(JSONObject client) {
        this.client = client;
    }

    public boolean is() {
        return myResult;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getGender() {
        return gender;
    }

    public String getDob() {
        return dob;
    }

    public String getZeirId() {
        return zeirId;
    }

    public String getInactive() {
        return inactive;
    }

    public String getLostToFollowUp() {
        return lostToFollowUp;
    }

    public CheckChildDetailsModel invoke() {
        this.entityId = "";
        this.firstName = "";
        this.middleName = "";
        this.lastName = "";
        this.gender = "";
        this.dob = "";
        this.zeirId = "";
        this.inactive = "";
        this.lostToFollowUp = "";

        if (client.has(GizConstants.KEY.CHILD)) {
            JSONObject child = GizJsonFormUtils.getGizJsonObject(client, GizConstants.KEY.CHILD);

            // Skip deceased children
            if (StringUtils.isNotBlank(GizJsonFormUtils.getJsonString(child, GizConstants.KEY.DEATHDATE))) {
                myResult = true;
                return this;
            }

            entityId = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.BASE_ENTITY_ID);
            firstName = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.FIRSTNAME);
            middleName = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.MIDDLENAME);
            lastName = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.LASTNAME);

            gender = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.GENDER);
            dob = GizJsonFormUtils.getJsonString(child, GizConstants.KEY.BIRTHDATE);
            if (StringUtils.isNotBlank(dob) && StringUtils.isNumeric(dob)) {
                try {
                    Long dobLong = Long.valueOf(dob);
                    Date date = new Date(dobLong);
                    dob = DateUtil.yyyyMMddTHHmmssSSSZ.format(date);
                } catch (Exception e) {
                    Log.e(getClass().getName(), e.toString(), e);
                }
            }

            zeirId = GizJsonFormUtils.getJsonString(GizJsonFormUtils.getGizJsonObject(child, GizConstants.KEY.IDENTIFIERS), ChildJsonFormUtils.ZEIR_ID);
            if (StringUtils.isNotBlank(zeirId)) {
                zeirId = zeirId.replace("-", "");
            }

            inactive = GizJsonFormUtils.getJsonString(GizJsonFormUtils.getGizJsonObject(child, GizConstants.KEY.ATTRIBUTES), GizConstants.KEY.INACTIVE);
            lostToFollowUp = GizJsonFormUtils.getJsonString(GizJsonFormUtils.getGizJsonObject(child, GizConstants.KEY.ATTRIBUTES), GizConstants.KEY.LOST_TO_FOLLOW_UP);
        }
        myResult = false;
        return this;
    }
}
