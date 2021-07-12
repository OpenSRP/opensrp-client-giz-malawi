package org.smartregister.giz.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.anc.library.event.PatientRemovedEvent;
import org.smartregister.anc.library.repository.PatientRepository;
import org.smartregister.anc.library.util.ANCJsonFormUtils;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.anc.library.util.DBConstantsUtils;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.child.interactor.ChildRegisterInteractor;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.SyncStatus;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.form.FormLocation;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.giz.BuildConfig;
import org.smartregister.giz.R;
import org.smartregister.giz.activity.GizAncProfileActivity;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.configuration.GizPncRegisterQueryProvider;
import org.smartregister.giz.event.BaseEvent;
import org.smartregister.giz.listener.OnLocationChangeListener;
import org.smartregister.giz.task.OpenMaternityProfileTask;
import org.smartregister.giz.view.NavigationMenu;
import org.smartregister.giz.widget.GizTreeViewDialog;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.maternity.utils.MaternityDbConstants;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.utils.OpdConstants;
import org.smartregister.pnc.PncLibrary;
import org.smartregister.pnc.pojo.PncMetadata;
import org.smartregister.pnc.utils.PncConstants;
import org.smartregister.reporting.job.RecurringIndicatorGeneratingJob;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.util.AssetHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class GizUtils extends Utils {

    public static final ArrayList<String> ALLOWED_LEVELS;
    public static final String FACILITY = "Facility";
    public static final String DEFAULT_LOCATION_LEVEL = "Health Facility";
    public static final SimpleDateFormat DB_DF = new SimpleDateFormat("yyyy-MM-dd");
    public static final String LANGUAGE = "language";
    private static final String PREFERENCES_FILE = "lang_prefs";

    static {
        ALLOWED_LEVELS = new ArrayList<>();
        ALLOWED_LEVELS.add(DEFAULT_LOCATION_LEVEL);
        ALLOWED_LEVELS.add(FACILITY);
    }

    public static void showDialogMessage(Context context, int title, int message) {
        showDialogMessage(context, title > 0 ? context.getResources().getString(title) : "",
                message > 0 ? context.getResources().getString(message) : "");
    }

    public static void showDialogMessage(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)

                .setPositiveButton(android.R.string.ok, null)

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static void saveLanguage(Context ctx, String language) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(LANGUAGE, language);
        editor.apply();
        saveGlobalLanguage(language, ctx);
    }

    public static void saveGlobalLanguage(String language, Context activity) {
        AllSharedPreferences allSharedPreferences = new AllSharedPreferences(PreferenceManager.getDefaultSharedPreferences(
                GizMalawiApplication.getInstance().getApplicationContext()));
        allSharedPreferences.saveLanguagePreference(language);
        setLocale(new Locale(language), activity);
    }

    public static void setLocale(Locale locale, Context activity) {
        Locale.setDefault(locale);
        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        GizMalawiApplication.getInstance().getApplicationContext().createConfigurationContext(configuration);
    }

    public static String getLanguage(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(LANGUAGE, "en");
    }

    public static Context setAppLocale(Context context, String language) {
        Context newContext = context;
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = newContext.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        newContext = newContext.createConfigurationContext(config);
        return newContext;
    }

    public static void postStickyEvent(
            BaseEvent event) {//Each Sticky event must be manually cleaned by calling GizUtils.removeStickyEvent
        // after
        // handling
        EventBus.getDefault().postSticky(event);
    }

    public static void removeStickyEvent(BaseEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
    }

    public static String childAgeLimitFilter() {
        return childAgeLimitFilter(GizConstants.KEY.DOB, GizConstants.KEY.FIVE_YEAR);
    }

    private static String childAgeLimitFilter(String dateColumn, int age) {
        return " ((( julianday('now') - julianday(" + dateColumn + "))/365.25) <" + age + ")";
    }

    public static boolean updateClientDeath(@NonNull EventClient eventClient) {
        Client client = eventClient.getClient();
        ContentValues values = new ContentValues();
        if (client != null) {
            if (client.getDeathdate() == null) {
                Timber.e(new Exception(), "Death event for %s cannot be processed because deathdate is NULL : %s"
                        , client.getFirstName() + " " + client.getLastName(), new Gson().toJson(eventClient));
                return false;
            }
            values.put(Constants.KEY.DOD, Utils.convertDateFormat(client.getDeathdate()));
            values.put(Constants.KEY.DATE_REMOVED, Utils.convertDateFormat(client.getDeathdate().toDate(), Utils.DB_DF));
            AllCommonsRepository allCommonsRepository = GizMalawiApplication.getInstance().context().allCommonsRepositoryobjects(GizConstants.TABLE_NAME.ALL_CLIENTS);
            if (allCommonsRepository != null) {
                allCommonsRepository.update(GizConstants.TABLE_NAME.ALL_CLIENTS, values, client.getBaseEntityId());
                allCommonsRepository.updateSearch(client.getBaseEntityId());
            }
            return true;
        }
        return false;
    }

    @NonNull
    public static Locale getLocale(Context context) {
        if (context == null) {
            return Locale.getDefault();
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    @NonNull
    public static ArrayList<String> getLocationLevels() {
        return new ArrayList<>(Arrays.asList(BuildConfig.LOCATION_LEVELS));
    }

    @NonNull
    public static ArrayList<String> getHealthFacilityLevels() {
        return new ArrayList<>(Arrays.asList(BuildConfig.HEALTH_FACILITY_LEVELS));
    }

    @NonNull
    public static String getCurrentLocality() {
        String selectedLocation = GizMalawiApplication.getInstance().context().allSharedPreferences().fetchCurrentLocality();
        if (StringUtils.isBlank(selectedLocation)) {
            selectedLocation = LocationHelper.getInstance().getDefaultLocation();
            GizMalawiApplication.getInstance().context().allSharedPreferences().saveCurrentLocality(selectedLocation);
        }
        return selectedLocation;
    }

    public static void showLocations(@Nullable Activity context,
                                     @NonNull OnLocationChangeListener onLocationChangeListener,
                                     @Nullable NavigationMenu navigationMenu) {
        try {
            ArrayList<String> allLevels = getLocationLevels();
            ArrayList<String> healthFacilities = getHealthFacilityLevels();
            ArrayList<String> defaultLocation = (ArrayList<String>) LocationHelper.getInstance().generateDefaultLocationHierarchy(allLevels);
            List<FormLocation> upToFacilities = LocationHelper.getInstance().generateLocationHierarchyTree(false, healthFacilities);
            String upToFacilitiesString = AssetHandler.javaToJsonString(upToFacilities, new TypeToken<List<FormLocation>>() {
            }.getType());
            GizTreeViewDialog treeViewDialog = new GizTreeViewDialog(context,
                    new JSONArray(upToFacilitiesString), defaultLocation, new ArrayList<>());

            treeViewDialog.setCancelable(true);
            treeViewDialog.setCanceledOnTouchOutside(true);
            treeViewDialog.setOnDismissListener(dialog -> {
                ArrayList<String> treeViewDialogName = treeViewDialog.getName();
                if (!treeViewDialogName.isEmpty()) {
                    String newLocation = treeViewDialogName.get(treeViewDialogName.size() - 1);
                    GizMalawiApplication.getInstance().context().allSharedPreferences().saveCurrentLocality(newLocation);
                    onLocationChangeListener.updateUi(newLocation);
                    if (navigationMenu != null) {
                        navigationMenu.updateUi(newLocation);
                    }
                }

            });
            treeViewDialog.show();
        } catch (JSONException e) {
            Timber.e(e);
        }
    }


    public static void startReportJob(Context context) {
        String reportJobExecutionTime = GizMalawiApplication.getInstance().context().allSharedPreferences().getPreference("report_job_execution_time");
        if (StringUtils.isBlank(reportJobExecutionTime) || timeBetweenLastExecutionAndNow(30, reportJobExecutionTime)) {
            GizMalawiApplication.getInstance().context().allSharedPreferences().savePreference("report_job_execution_time", String.valueOf(System.currentTimeMillis()));
            Toast.makeText(context, "Reporting Job Has Started, It will take some time", Toast.LENGTH_LONG).show();
            RecurringIndicatorGeneratingJob.scheduleJobImmediately(RecurringIndicatorGeneratingJob.TAG);
        } else {
            Toast.makeText(context, "Reporting Job Has Already Been Started, Try again in 30 mins", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean timeBetweenLastExecutionAndNow(int i, String reportJobExecutionTime) {
        try {
            long executionTime = Long.parseLong(reportJobExecutionTime);
            long now = System.currentTimeMillis();
            long diffNowExecutionTime = now - executionTime;
            return TimeUnit.MILLISECONDS.toMinutes(diffNowExecutionTime) > i;
        } catch (NumberFormatException e) {
            Timber.e(e);
            return false;
        }
    }

    public static boolean getSyncStatus() {
        String synComplete = GizMalawiApplication.getInstance().context().allSharedPreferences().getPreference("syncComplete");
        boolean isSyncComplete = false;
        if (StringUtils.isBlank(synComplete)) {
            GizMalawiApplication.getInstance().context().allSharedPreferences().savePreference("syncComplete", String.valueOf(false));
        } else {
            isSyncComplete = Boolean.parseBoolean(synComplete);
        }
        return isSyncComplete;
    }

    public static void updateSyncStatus(boolean isComplete) {
        GizMalawiApplication.getInstance().context().allSharedPreferences().savePreference("syncComplete", String.valueOf(isComplete));
    }

    public static void showAncMaternityNavigationDialog(@NonNull PatientRemovedEvent event, @NonNull Activity activity) {
        if (event.getClosedNature() != null && ConstantsUtils.ClosedNature.TRANSFERRED.equals(event.getClosedNature())) {
            AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AppThemeAlertDialog)
                    .setCancelable(true)
                    .setMessage(R.string.anc_migration_to_maternity_text)
                    .setNegativeButton("GO TO PROFILE", (dialog1, which) -> {
                        String baseEntityId = activity.getIntent().getStringExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID);
                        new OpenMaternityProfileTask(activity, baseEntityId).execute();
                    }).create();

            dialog.show();
        } else {
            if (activity instanceof GizAncProfileActivity) {
                activity.finish();
            }
        }
    }


    public static HashMap<String, String> generateKeyValuesFromEvent(@NonNull Event event) {
        HashMap<String, String> keyValues = new HashMap<>();
        List<Obs> obs = event.getObs();
        for (Obs observation : obs) {
            String key = observation.getFormSubmissionField();
            List<Object> humanReadableValues = observation.getHumanReadableValues();
            if (humanReadableValues.size() > 0) {
                String value = (String) humanReadableValues.get(0);
                if (!TextUtils.isEmpty(value)) {
                    if (humanReadableValues.size() > 1) {
                        value = humanReadableValues.toString();
                    }
                    keyValues.put(key, value);
                    continue;
                }
            }
            List<Object> values = observation.getValues();
            if (values.size() > 0) {
                String value = (String) values.get(0);
                if (!TextUtils.isEmpty(value)) {
                    if (values.size() > 1) {
                        value = values.toString();
                    }
                    keyValues.put(key, value);
                }
            }
        }
        return keyValues;
    }

    public static JSONArray getMultiStepFormFields(@NonNull JSONObject jsonForm) {
        JSONArray fields = new JSONArray();
        try {
            if (jsonForm.has(JsonFormConstants.COUNT)) {
                int stepCount = Integer.parseInt(jsonForm.getString(JsonFormConstants.COUNT));
                for (int i = 0; i < stepCount; i++) {
                    String stepName = JsonFormConstants.STEP + (i + 1);
                    JSONObject step = jsonForm.optJSONObject(stepName);
                    if (step != null) {
                        JSONArray stepFields = step.optJSONArray(JsonFormConstants.FIELDS);
                        if (stepFields != null) {
                            for (int k = 0; k < stepFields.length(); k++) {
                                JSONObject field = stepFields.optJSONObject(k);
                                if (field != null) {
                                    fields.put(field);
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e, " --> getMultiStepFormFields()");
        }
        return fields;
    }

    public static void openAncProfilePage(@NonNull final CommonPersonObjectClient commonPersonObjectClient, @NonNull final Context context) {
        final AppExecutors appExecutors = new AppExecutors();
        appExecutors.diskIO()
                .execute(new Runnable() {

                    @Override
                    public void run() {
                        final Map<String, String> ancUserDetails = PatientRepository.getWomanProfileDetails(commonPersonObjectClient.getCaseId());
                        ancUserDetails.putAll(commonPersonObjectClient.getColumnmaps());
                        appExecutors.mainThread().execute(() -> org.smartregister.anc.library.util.Utils.navigateToProfile(context, (HashMap<String, String>) ancUserDetails));
                    }
                });
    }

    public static void openPncProfile(@NonNull Context activity, @NonNull String baseEntityId) {
        GizMalawiApplication.getInstance().getAppExecutors().diskIO().execute(() -> {
            PncMetadata pncMetadata = PncLibrary.getInstance().getPncConfiguration().getPncMetadata();

            GizPncRegisterQueryProvider pncRegisterQueryProvider = new GizPncRegisterQueryProvider();
            String query = pncRegisterQueryProvider.mainSelectWhereIDsIn().replace("%s", "'" + baseEntityId + "'");
            HashMap<String, String> detailsFromQueryProvider = PncLibrary.getInstance()
                    .getPncRepository()
                    .getPncDetailsFromQueryProvider(query);
            if (detailsFromQueryProvider != null) {
                CommonPersonObjectClient pClient = new CommonPersonObjectClient(baseEntityId,
                        detailsFromQueryProvider, "");
                pClient.setColumnmaps(detailsFromQueryProvider);

                if (pncMetadata != null) {
                    Intent intent = new Intent(activity, pncMetadata.getProfileActivity());
                    intent.putExtra(PncConstants.IntentKey.CLIENT_OBJECT, pClient);
                    activity.startActivity(intent);
                }
            }
        });
    }

    public static void createOpdTransferEvent(@NonNull String eventType, @NonNull String baseEntityId) {
        try {
            FormTag formTag = GizJsonFormUtils.getFormTag(Utils.getAllSharedPreferences());
            JSONArray jsonArrayFields = new JSONArray();
            if (GizConstants.EventType.OPD_ANC_TRANSFER.equals(eventType)) {
                String nextContactDate = Utils.convertDateFormat(Calendar.getInstance().getTime(), Utils.DB_DF);

                EventClientRepository db = OpdLibrary.getInstance().eventClientRepository();

                JSONObject client = db.getClientByBaseEntityId(baseEntityId);

                JSONObject attributes = client.getJSONObject(OpdConstants.JSON_FORM_KEY.ATTRIBUTES);
                attributes.put(DBConstantsUtils.KeyUtils.NEXT_CONTACT, "1");
                attributes.put(DBConstantsUtils.KeyUtils.NEXT_CONTACT_DATE, nextContactDate);

                db.addorUpdateClient(baseEntityId, client);
            }
            org.smartregister.clientandeventmodel.Event event = ANCJsonFormUtils.createEvent(jsonArrayFields,
                    new JSONObject(), formTag, baseEntityId, eventType, "");

            GizJsonFormUtils.tagEventSyncMetadata(event);

            saveEvent(event, baseEntityId);

            initiateEventProcessing(Collections.singletonList(event.getFormSubmissionId()));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void saveEvent(@NonNull org.smartregister.clientandeventmodel.Event event, @NonNull String baseEntityId) throws JSONException {
        JSONObject eventJson = new JSONObject(GizJsonFormUtils.gson.toJson(event));
        GizMalawiApplication.getInstance().getEcSyncHelper().addEvent(baseEntityId, eventJson);
    }

    public static void initiateEventProcessing(@NonNull List<String> formSubmissionIds) throws Exception {
        long lastSyncTimeStamp = Utils.getAllSharedPreferences().fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);
        GizMalawiApplication.getInstance().getClientProcessor()
                .processClient(
                        GizMalawiApplication.getInstance()
                                .getEcSyncHelper()
                                .getEvents(formSubmissionIds));

        Utils.getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
    }

    public static String convertWeightToKgs(@Nullable String birthWeightEntered) {
        String weight = birthWeightEntered;
        if (StringUtils.isNotBlank(birthWeightEntered)) {
            try {
                int weightNum = Integer.parseInt(weight);
                double weightDouble = weightNum / 1000.0;
                weight = String.valueOf(weightDouble);
            } catch (IllegalArgumentException e) {
                Timber.e(e);
            }
        }
        return weight;
    }

    public static void createChildGrowthEventFromRepeatingGroup(@NonNull JSONObject clientJson,
                                                                @NonNull org.smartregister.clientandeventmodel.Client client,
                                                                @NonNull ChildRegisterInteractor interactor,
                                                                @NonNull HashMap<String, HashMap<String, String>> buildRepeatingGroupBorn) throws JSONException {
        UpdateRegisterParams params = new UpdateRegisterParams();
        params.setStatus(SyncStatus.PENDING.value());
        JSONObject tempForm = new JSONObject();
        JSONObject tempStep = new JSONObject();
        tempForm.put(JsonFormConstants.STEP1, tempStep);
        String height = "";
        String weight = "";
        for (Map.Entry<String, HashMap<String, String>> entrySet : buildRepeatingGroupBorn.entrySet()) {
            HashMap<String, String> details = entrySet.getValue();
            if (client.getBaseEntityId().equals(details.get(MaternityDbConstants.Column.MaternityChild.BASE_ENTITY_ID))) {
                height = details.get("birth_height_entered");
                weight = GizUtils.convertWeightToKgs(details.get("birth_weight_entered"));
                break;
            }
        }
        JSONArray jsonArray = new JSONArray();

        JSONObject heightObject = new JSONObject();
        heightObject.put(JsonFormConstants.KEY, Constants.KEY.BIRTH_HEIGHT);
        heightObject.put(JsonFormConstants.VALUE, height);
        jsonArray.put(heightObject);

        JSONObject weightObject = new JSONObject();
        weightObject.put(JsonFormConstants.KEY, Constants.KEY.BIRTH_WEIGHT);
        weightObject.put(JsonFormConstants.VALUE, weight);
        jsonArray.put(weightObject);

        tempStep.put(JsonFormConstants.FIELDS, jsonArray);
        interactor.processHeight(client.getIdentifiers(), tempForm.toString(), params, clientJson);
        interactor.processWeight(client.getIdentifiers(), tempForm.toString(), params, clientJson);
    }

    public static String getDuration(DateTime dateTime) {
        if (dateTime != null) {
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTime(dateTime.toDate());
            dateCalendar.set(Calendar.HOUR_OF_DAY, 0);
            dateCalendar.set(Calendar.MINUTE, 0);
            dateCalendar.set(Calendar.SECOND, 0);
            dateCalendar.set(Calendar.MILLISECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long timeDiff = Math.abs(dateCalendar.getTimeInMillis() - today.getTimeInMillis());

            DateTime todayDateTime = new DateTime();
            return getDuration(timeDiff, dateTime, todayDateTime, getLocale());
        }
        return null;
    }

    private static Locale getLocale() {
        Locale locale = CoreLibrary.getInstance().context().applicationContext().getResources().getConfiguration().locale;
        locale = locale != null ? Locale.ENGLISH : null;

        return locale;
    }

    public static String getDuration(String date) {
        DateTime duration;
        if (StringUtils.isNotBlank(date)) {
            try {
                duration = new DateTime(date);
                return getDuration(duration);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return "";
    }

    public static String toCSV(List<String> list) {
        String result = "";
        if (list != null && list.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s).append(", ");
            }
            result = sb.deleteCharAt(sb.length() - 2).toString();
        }
        return result;
    }

    public static String getStringResourceByName(String name, Context context) {
        String packageName = context.getPackageName();
        int resId = context.getResources().getIdentifier(name, "string", packageName);
        if (resId == 0) {
            return name;
        } else {
            return context.getString(resId);
        }
    }

    public static CommonRepository getCommonRepository(String tableName) {
        return Utils.context().commonrepository(tableName);
    }

    public static String getDuration(long timeDiff, DateTime dateTime, DateTime todayDateTime, Locale locale) {

        Context context = CoreLibrary.getInstance().context().applicationContext();
        String duration;
        if (timeDiff >= 0
                && timeDiff <= TimeUnit.MILLISECONDS.convert(13, TimeUnit.DAYS)) {
            // Represent in days
            long days = Math.abs(Days.daysBetween(dateTime.toLocalDate(), todayDateTime.toLocalDate()).getDays());
            duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_days), days);
        } else if (timeDiff > TimeUnit.MILLISECONDS.convert(13, TimeUnit.DAYS)
                && timeDiff <= TimeUnit.MILLISECONDS.convert(97, TimeUnit.DAYS)) {
            // Represent in weeks and days
            int weeks = (int) Math.floor((float) timeDiff /
                    TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
            int days = (int) Math.floor((float) (timeDiff -
                    TimeUnit.MILLISECONDS.convert(weeks * 7, TimeUnit.DAYS)) /
                    TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));

            if (days >= 7) {
                days = 0;
                weeks++;
            }

            if (days > 0) {
                duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_weeks_days), weeks, days);
            } else {
                duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_weeks), weeks);
            }

        } else if (timeDiff > TimeUnit.MILLISECONDS.convert(97, TimeUnit.DAYS)
                && timeDiff <= TimeUnit.MILLISECONDS.convert(363, TimeUnit.DAYS)) {
            // Represent in months and weeks
            int months = (int) Math.floor((float) timeDiff /
                    TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));
            int weeks = (int) Math.floor((float) (timeDiff - TimeUnit.MILLISECONDS.convert(
                    months * 30, TimeUnit.DAYS)) /
                    TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));

            if (weeks >= 4) {
                weeks = 0;
                months++;
            }

            if (months < 12) {
                if (weeks > 0) {
                    duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_months_weeks), months, weeks);
                } else {
                    duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_months), months);
                }
            } else {
                duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_years), 1);
            }
        } else {
            // Represent in years and months
            int years = Math.abs(Years.yearsBetween(dateTime.toLocalDate(), todayDateTime.toLocalDate()).getYears());
            int totalMonths = Math.abs(Months.monthsBetween(dateTime.toLocalDate(), todayDateTime.toLocalDate()).getMonths());
            int months = Math.abs(totalMonths - (12 * years));

            if (months > 0) {
                duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_years_months), years, months);
            } else {
                duration = String.format(locale, context.getResources().getString(org.smartregister.R.string.x_years), years);
            }
        }

        return duration;

    }

    public static String getObsValue(Obs obs) {
        List<Object> values = obs.getValues();
        if (values != null && values.size() > 0) {
            return (String) values.get(0);
        }
        return null;
    }

}
