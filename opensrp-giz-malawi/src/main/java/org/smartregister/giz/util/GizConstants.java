package org.smartregister.giz.util;

public class GizConstants {

    public static final int HOW_BABY_OLD_IN_DAYS = 28;
    public static final String MOTHER_TDV_DOSES = "mother_tdv_doses";
    public static final String PROTECTED_AT_BIRTH = "protected_at_birth";
    public static final String REACTION_VACCINE = "Reaction_Vaccine";
    public static final String FORM_CONFIG_LOCATION = "json.form/json.form.config.json";
    public static final String NATIONAL_ID = "national_id";
    public static final String BHT_MID = "bht_mid";
    public static final String MOTHER_HIV_STATUS = "mother_hiv_status";
    public static final String BIRTH_REGISTRATION_NUMBER = "birth_registration_number";
    public static final String REGISTER_TYPE = "register_type";
    public static final String IS_FROM_MATERNITY = "isFromMaternity";

    public static final String METADATA = "metadata";

    public static final String DETAILS = "details";
    public static final String RELATIONSHIPS = "relationships";


    public interface Properties {
        String TASK_IDENTIFIER = "taskIdentifier";
        String TASK_BUSINESS_STATUS = "taskBusinessStatus";
        String TASK_STATUS = "taskStatus";
        String TASK_CODE = "taskCode";
        String LOCATION_UUID = "locationUUID";
        String LOCATION_VERSION = "locationVersion";
        String LOCATION_TYPE = "locationType";
        String LOCATION_PARENT = "locationParent";
        String LOCATION_ID = "location_id";
        String FEATURE_SELECT_TASK_BUSINESS_STATUS = "featureSelectTaskBusinessStatus";
        String BASE_ENTITY_ID = "baseEntityId";
        String FORM_SUBMISSION_ID = "formSubmissionId";
        String STRUCTURE_NAME = "structure_name";
        String APP_VERSION_NAME = "appVersionName";
        String FORM_VERSION = "form_version";
        String TASK_CODE_LIST = "task_code_list";
        String FAMILY_MEMBER_NAMES = "family_member_names";
        String PLAN_IDENTIFIER = "planIdentifier";
        String LOCATION_STATUS = "status";
        String LOCATION_NAME = "name";
    }


    public interface Columns {
        interface RegisterType {
            String BASE_ENTITY_ID = "base_entity_id";
            String REGISTER_TYPE = "register_type";
            String DATE_REMOVED = "date_removed";
            String DATE_CREATED = "date_created";
        }
    }

    public interface DateTimeFormat {
        String HH_MM_AMPM = "h:mm a";
        String YYYY_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
        String dd_MMM_yyyy = "dd MMM yyyy";
        String hh_mm_ss = "hh:mm:ss";
        String hh_mm = "hh:mm";
        String dd_MM_yyyy = "dd/MM/yyyy";
        String ddMMyyyy = "ddMMyyyy";
        String yyyy_MM_dd = "yyyy-MM-dd";
    }

    public interface RegisterType {
        String ANC = "anc";
        String CHILD = "child";
        String OPD = "opd";
        String ALL_CLIENTS = "all_clients";
        String MATERNITY = "maternity";
        String PNC = "pnc";
    }

    public interface MultiResultProcessor {
        String GROUPING_SEPARATOR = "_";
    }

    public interface IntentKey {
        String REPORT_GROUPING = "report-grouping";
    }

    public interface Pref {
        String APP_VERSION_CODE = "APP_VERSION_CODE";
        String INDICATOR_DATA_INITIALISED = "INDICATOR_DATA_INITIALISED";
    }

    public interface File {
        String INDICATOR_CONFIG_FILE = "config/indicator-definitions.yml";
    }

    public interface ReportKeys {
        String REPORT_JSON = "reportJson";
        String REPORT_DATE = "reportDate";
        String GROUPING = "grouping";
        String PROVIDER_ID = "providerId";
        String DATE_CREATED = "dateCreated";
        String HIA2_INDICATORS = "hia2Indicators";
        String VALUE = "value";
        String INDICATOR_CODE = "indicatorCode";
    }

    public static final class KEY {
        public static final String MOTHER_BASE_ENTITY_ID = "mother_base_entity_id";
        public static final String CHILD = "child";
        public static final String CHILD_OVER_FIVE = "child_over_5";
        public static final String MOTHER_FIRST_NAME = "mother_first_name";
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String BIRTHDATE = "birthdate";
        public static final String DEATHDATE = "deathdate";
        public static final String MOTHER_LAST_NAME = "mother_last_name";
        public static final String ZEIR_ID = "zeir_id";
        public static final String LOST_TO_FOLLOW_UP = "lost_to_follow_up";
        public static final String GENDER = "gender";
        public static final String INACTIVE = "inactive";
        public static final String LAST_INTERACTED_WITH = "last_interacted_with";
        public static final String MOTHER = "mother";
        public static final String ENTITY_ID = "entity_id";
        public static final String VALUE = "value";
        public static final String STEPNAME = "stepName";
        public static final String TITLE = "title";
        public static final String HIA_2_INDICATOR = "hia2_indicator";
        public static final String RELATIONALID = "relationalid";
        public static final String ID_LOWER_CASE = "_id";
        public static final String BASE_ENTITY_ID = "base_entity_id";
        public static final String DOB = "dob";//Date Of Birth
        public static final String DOD = "dod";//Date Of Birth
        public static final String DATE_REMOVED = "date_removed";
        public static final String MOTHER_NRC_NUMBER = "nrc_number";
        public static final String MOTHER_GUARDIAN_NUMBER = "mother_guardian_number";
        public static final String MOTHER_SECOND_PHONE_NUMBER = "second_phone_number";
        public static final String VIEW_CONFIGURATION_PREFIX = "ViewConfiguration_";
        public static final String HOME_FACILITY = "home_address";
        public static final String MALAWI_ID = "mer_id";
        public static final String MIDDLE_NAME = "middle_name";
        public static final String OTHER = "other";
        public static final String BIRTH_FACILITY_NAME_OTHER = "Birth_Facility_Name_Other";
        public static final String ADDRESS_3 = "address3";
        public static final String BIRTH_FACILITY_NAME = "Birth_Facility_Name";
        public static final String RESIDENTIAL_AREA = "Residential_Area";
        public static final String MOTHER_ = "mother_";
        public static final String ENCOUNTER_TYPE = "encounter_type";
        public static final String BIRTH_REGISTRATION = "Birth Registration";
        public static final String REGISTRATION_HOME_ADDRESS = "home_address";
        public static final String IDENTIFIERS = "identifiers";
        public static final String FIRSTNAME = "firstName";
        public static final String MIDDLENAME = "middleName";
        public static final String LASTNAME = "lastName";
        public static final String ATTRIBUTES = "attributes";
        public static final int FIVE_YEAR = 5;
        public static final String OPD_REGISTRATION = "Opd Registration";
        public static final String FIELDS = "fields";
        public static final String KEY = "key";
        public static final String IS_VACCINE_GROUP = "is_vaccine_group";
        public static final String OPTIONS = "options";
        public static String SITE_CHARACTERISTICS = "site_characteristics";
        public static String HIDDEN_FIELDS = "hidden_fields";
        public static String DISABLED_FIELDS = "disabled_fields";
        public static final double ONE_YEAR = 365.4;
        public static final String ALL_COMMUNITIES = "All communities";
    }

    public static final class DrawerMenu {
        public static final String ALL_FAMILIES = "All Families";
        public static final String ALL_CLIENTS = "All Clients";
        public static final String ANC_CLIENTS = "ANC Clients";
        public static final String CHILD_CLIENTS = "Child Clients";
        public static final String OPD_CLIENTS = "OPD Clients";
        public static final String MATERNITY_CLIENTS = "Maternity Clients";
        public static final String PNC_CLIENTS = "PNC Clients";
        public static final String ANC = "ANC";
    }

    public static final class FormTitleUtil {
        public static final String UPDATE_CHILD_FORM = "Update Child Registration";
    }

    public static final class RQ_CODE {
        public static final int STORAGE_PERMISIONS = 1;
    }

    public static class CONFIGURATION {
        public static final String LOGIN = "login";
        public static final String CHILD_REGISTER = "child_register";

    }

    public static final class EventType {
        public static final String CHILD_REGISTRATION = "Birth Registration";
        public static final String UPDATE_CHILD_REGISTRATION = "Update Birth Registration";
        public static final String OUT_OF_CATCHMENT = "Out of Catchment";

        public static final String REPORT_CREATION = "report_creation";
        public static final String MATERNITY_PNC_TRANSFER = "maternity_pnc_transfer";

        public static final String OPD_PNC_TRANSFER = "opd_pnc_transfer";
        public static final String OPD_MATERNITY_TRANSFER = "opd_maternity_transfer";
        public static final String OPD_ANC_TRANSFER = "opd_anc_transfer";
        public static final String OPD_CHILD_TRANSFER = "opd_child_transfer";
        public static final String REASON_FOR_DEFAULTING = "Reason For Defaulting";
    }

    public static class JSON_FORM {
        public static String CHILD_ENROLLMENT = "child_enrollment";
        public static String OUT_OF_CATCHMENT_SERVICE = "out_of_catchment_service";
        public static String REASON_FOR_DEFAULTING = "report_reason_for_defaulting";

    }

    public static class RELATIONSHIP {
        public static final String MOTHER = "mother";

    }

    public static class TABLE_NAME {
        public static final String CHILD = "ec_client";
        public static final String ALL_CLIENTS = "ec_client";
        public static final String REGISTER_TYPE = "client_register_type";
        public static final String CHILD_UPDATED_ALERTS = "child_updated_alerts";
        public static final String REASON_FOR_DEFAULTING = "reason_for_defaulting";
    }

    public static final class EntityType {
        public static final String CHILD = "child";
    }

    public static class IntentKeyUtil {
        public static final String IS_REMOTE_LOGIN = "is_remote_login";
    }

    public static class ReportParametersHelper {
        public static String COMMUNITY = "COMMUNITY";
        public static String COMMUNITY_ID = "COMMUNITY_ID";
        public static String REPORT_DATE = "REPORT_DATE";
    }

    public static class JsonAssetsHelper {
        public static final String ID = "id";
        public static final String OUTREACH_DATE = "outreach_date";
        public static final String FOLLOWUP_DATE = "followup_date";
        public static final String OUTREACH_DEFAULTING_REASON = "outreach_defaulting_rsn";
        public static final String OTHER_DEFAULTING_REASON = "other_defaulting_rsn";
        public static final String ADDITIONAL_DEFAULTING_NOTES = "additional_defaulting_notes";
        public static final String BASE_ENTITY_ID = "base_entity_id";
        public static final String EVENT_DATE = "date";
    }

    public static class EmailParameterHelper {
        public static String REPORTS = "Reports";
    }

    public static class ReasonForDefaultingHelper {
        public static String REPORT_REASON_FOR_DEFAULTING = "report_reason_for_defaulting";
    }

}
