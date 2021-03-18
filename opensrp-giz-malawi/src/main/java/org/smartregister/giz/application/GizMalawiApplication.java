package org.smartregister.giz.application;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.anc.library.AncLibrary;
import org.smartregister.anc.library.activity.ActivityConfiguration;
import org.smartregister.anc.library.util.AncMetadata;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.anc.library.util.DBConstantsUtils;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.child.util.DBConstants;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.configurableviews.helper.JsonSpecHelper;
import org.smartregister.giz.BuildConfig;
import org.smartregister.giz.activity.AllClientsRegisterActivity;
import org.smartregister.giz.activity.AncRegisterActivity;
import org.smartregister.giz.activity.ChildFormActivity;
import org.smartregister.giz.activity.ChildImmunizationActivity;
import org.smartregister.giz.activity.ChildProfileActivity;
import org.smartregister.giz.activity.ChildRegisterActivity;
import org.smartregister.giz.activity.GizAncProfileActivity;
import org.smartregister.giz.activity.GizMaternityProfileActivity;
import org.smartregister.giz.activity.GizOpdProfileActivity;
import org.smartregister.giz.activity.GizPncFormActivity;
import org.smartregister.giz.activity.GizPncProfileActivity;
import org.smartregister.giz.activity.HIA2ReportsActivity;
import org.smartregister.giz.activity.LoginActivity;
import org.smartregister.giz.activity.OpdFormActivity;
import org.smartregister.giz.configuration.GizAncMaternityTransferProcessor;
import org.smartregister.giz.configuration.GizMaternityOutcomeFormProcessing;
import org.smartregister.giz.configuration.GizMaternityRegisterQueryProvider;
import org.smartregister.giz.configuration.GizMaternityRegisterRowOptions;
import org.smartregister.giz.configuration.GizOpdRegisterRowOptions;
import org.smartregister.giz.configuration.GizOpdRegisterSwitcher;
import org.smartregister.giz.configuration.GizPncMedicInfoFormProcessing;
import org.smartregister.giz.configuration.GizPncRegisterQueryProvider;
import org.smartregister.giz.configuration.GizPncRegisterRowOptions;
import org.smartregister.giz.configuration.OpdRegisterQueryProvider;
import org.smartregister.giz.job.GizMalawiJobCreator;
import org.smartregister.giz.processor.GizMalawiProcessorForJava;
import org.smartregister.giz.processor.TripleResultProcessor;
import org.smartregister.giz.repository.ChildAlertUpdatedRepository;
import org.smartregister.giz.repository.ClientRegisterTypeRepository;
import org.smartregister.giz.repository.DailyTalliesRepository;
import org.smartregister.giz.repository.GizAncRegisterQueryProvider;
import org.smartregister.giz.repository.GizChildRegisterQueryProvider;
import org.smartregister.giz.repository.GizEventRepository;
import org.smartregister.giz.repository.GizHia2ReportRepository;
import org.smartregister.giz.repository.GizMalawiRepository;
import org.smartregister.giz.repository.HIA2IndicatorsRepository;
import org.smartregister.giz.repository.MonthlyTalliesRepository;
import org.smartregister.giz.util.AppExecutors;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizOpdRegisterProviderMetadata;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.giz.util.VaccineDuplicate;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.HeightZScoreRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.WeightZScoreRepository;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.maternity.MaternityLibrary;
import org.smartregister.maternity.activity.BaseMaternityFormActivity;
import org.smartregister.maternity.configuration.MaternityConfiguration;
import org.smartregister.maternity.pojo.MaternityMetadata;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.maternity.utils.MaternityDbConstants;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdConfiguration;
import org.smartregister.opd.pojo.OpdMetadata;
import org.smartregister.opd.utils.OpdConstants;
import org.smartregister.opd.utils.OpdDbConstants;
import org.smartregister.pnc.PncLibrary;
import org.smartregister.pnc.config.PncConfiguration;
import org.smartregister.pnc.pojo.PncMetadata;
import org.smartregister.pnc.utils.PncConstants;
import org.smartregister.pnc.utils.PncDbConstants;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.util.Constants;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Hia2ReportRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.DrishtiSyncScheduler;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.receiver.TimeChangedBroadcastReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

/*import com.amitshekhar.utils.DatabaseFileProvider;
import com.amitshekhar.utils.DbPasswordProvider;*/

public class GizMalawiApplication extends DrishtiApplication implements TimeChangedBroadcastReceiver.OnTimeChangedListener {

    private static CommonFtsObject commonFtsObject;
    private static JsonSpecHelper jsonSpecHelper;
    private static List<VaccineGroup> vaccineGroups;
    private EventClientRepository eventClientRepository;
    private HIA2IndicatorsRepository hia2IndicatorsRepository;
    private DailyTalliesRepository dailyTalliesRepository;
    private MonthlyTalliesRepository monthlyTalliesRepository;
    private Hia2ReportRepository hia2ReportRepository;
    private String password;
    private boolean lastModified;
    private ECSyncHelper ecSyncHelper;
    private ClientRegisterTypeRepository registerTypeRepository;
    private ChildAlertUpdatedRepository childAlertUpdatedRepository;
    private GizEventRepository gizEventRepository;
    private AppExecutors appExecutors;

    public static JsonSpecHelper getJsonSpecHelper() {
        return jsonSpecHelper;
    }

    public static CommonFtsObject createCommonFtsObject(android.content.Context context) {
        if (commonFtsObject == null) {
            commonFtsObject = new CommonFtsObject(getFtsTables());
            for (String ftsTable : commonFtsObject.getTables()) {
                commonFtsObject.updateSearchFields(ftsTable, getFtsSearchFields(ftsTable));
                commonFtsObject.updateSortFields(ftsTable, getFtsSortFields(ftsTable, context));
            }
        }
        commonFtsObject.updateAlertScheduleMap(getAlertScheduleMap(context));

        return commonFtsObject;
    }

    private static String[] getFtsTables() {
        return new String[]{OpdDbConstants.KEY.TABLE, "ec_mother_details", DBConstants.RegisterTable.CHILD_DETAILS, MaternityDbConstants.Table.MATERNITY_REGISTRATION_DETAILS};
    }

    private static String[] getFtsSearchFields(String tableName) {
        if (tableName.equalsIgnoreCase(DBConstantsUtils.DEMOGRAPHIC_TABLE_NAME)) {
            return new String[]{DBConstantsUtils.KeyUtils.FIRST_NAME, DBConstantsUtils.KeyUtils.LAST_NAME, DBConstantsUtils.KeyUtils.ANC_ID, GizConstants.KEY.ZEIR_ID};
        } else if ("ec_mother_details".equals(tableName)) {
            return new String[]{"next_contact"};
        } else if (tableName.equals(DBConstants.RegisterTable.CHILD_DETAILS)) {
            return new String[]{DBConstants.KEY.LOST_TO_FOLLOW_UP, DBConstants.KEY.INACTIVE};
        } else if (tableName.equals(MaternityDbConstants.Table.MATERNITY_REGISTRATION_DETAILS)) {
            return new String[]{MaternityDbConstants.KEY.CONCEPTION_DATE};
        }

        return null;
    }

    private static String[] getFtsSortFields(String tableName, android.content.Context context) {
        if (tableName.equals(GizConstants.TABLE_NAME.ALL_CLIENTS)) {
            List<String> names = new ArrayList<>();
            names.add(GizConstants.KEY.FIRST_NAME);
            names.add(OpdDbConstants.KEY.LAST_NAME);
            names.add(GizConstants.KEY.DOB);
            names.add(GizConstants.KEY.ZEIR_ID);
            names.add(GizConstants.KEY.LAST_INTERACTED_WITH);
            names.add(GizConstants.KEY.DOD);
            names.add(GizConstants.KEY.DATE_REMOVED);
            return names.toArray(new String[0]);
        } else if ("ec_mother_details".equals(tableName)) {
            return new String[]{DBConstantsUtils.KeyUtils.NEXT_CONTACT};
        } else if (tableName.equals(DBConstants.RegisterTable.CHILD_DETAILS)) {
            List<VaccineGroup> vaccineList = VaccinatorUtils.getVaccineGroupsFromVaccineConfigFile(context, VaccinatorUtils.vaccines_file);
            List<String> names = new ArrayList<>();
            names.add(DBConstants.KEY.INACTIVE);
            names.add("relational_id");
            names.add(DBConstants.KEY.LOST_TO_FOLLOW_UP);

            for (VaccineGroup vaccineGroup : vaccineList) {
                populateAlertColumnNames(vaccineGroup.vaccines, names);
            }

            return names.toArray(new String[0]);
        } else if (tableName.equals(MaternityDbConstants.Table.MATERNITY_REGISTRATION_DETAILS)) {
            return new String[]{MaternityDbConstants.KEY.CONCEPTION_DATE};
        }

        return null;
    }

    private static void populateAlertColumnNames(List<Vaccine> vaccines, List<String> names) {
        for (Vaccine vaccine : vaccines) {
            if (vaccine.getVaccineSeparator() != null && vaccine.getName().contains(vaccine.getVaccineSeparator().trim())) {
                String[] individualVaccines = vaccine.getName().split(vaccine.getVaccineSeparator().trim());

                List<Vaccine> vaccineList = new ArrayList<>();
                for (String individualVaccine : individualVaccines) {
                    Vaccine vaccineClone = new Vaccine();
                    vaccineClone.setName(individualVaccine.trim());
                    vaccineList.add(vaccineClone);

                }
                populateAlertColumnNames(vaccineList, names);
            } else {
                names.add("alerts." + VaccinateActionUtils.addHyphen(vaccine.getName()));
            }
        }
    }

    private static void populateAlertScheduleMap(List<Vaccine> vaccines, Map<String, Pair<String, Boolean>> map) {
        for (Vaccine vaccine : vaccines)
            if (vaccine.getVaccineSeparator() != null && vaccine.getName().contains(vaccine.getVaccineSeparator().trim())) {
                String[] individualVaccines = vaccine.getName().split(vaccine.getVaccineSeparator().trim());

                List<Vaccine> vaccineList = new ArrayList<>();
                for (String individualVaccine : individualVaccines) {
                    Vaccine vaccineClone = new Vaccine();
                    vaccineClone.setName(individualVaccine.trim());
                    vaccineList.add(vaccineClone);

                }
                populateAlertScheduleMap(vaccineList, map);


            } else {

                // TODO: This needs to be fixed because it is a configuration & not a hardcoded string
                map.put(vaccine.name, Pair.create("ec_child_details", false));
            }
    }

    private static Map<String, Pair<String, Boolean>> getAlertScheduleMap(android.content.Context context) {
        List<VaccineGroup> vaccines = getVaccineGroups(context);

        Map<String, Pair<String, Boolean>> map = new HashMap<>();

        for (VaccineGroup vaccineGroup : vaccines) {
            populateAlertScheduleMap(vaccineGroup.vaccines, map);
        }
        return map;
    }

    public static synchronized GizMalawiApplication getInstance() {
        return (GizMalawiApplication) mInstance;
    }

    public static List<VaccineGroup> getVaccineGroups(android.content.Context context) {
        if (vaccineGroups == null) {

            vaccineGroups = VaccinatorUtils.getVaccineGroupsFromVaccineConfigFile(context, VaccinatorUtils.vaccines_file);
        }

        return vaccineGroups;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        context = Context.getInstance();

        String lang = GizUtils.getLanguage(getApplicationContext());
        Locale locale = new Locale(lang);
        Resources res = getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);

        context.updateApplicationContext(getApplicationContext());
        context.updateCommonFtsObject(createCommonFtsObject(context.applicationContext()));

        //Initialize Modules
        CoreLibrary.init(context, new GizMalawiSyncConfiguration(), BuildConfig.BUILD_TIMESTAMP);

        GrowthMonitoringLibrary.init(context, getRepository(), BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
        GrowthMonitoringLibrary.getInstance().setGrowthMonitoringSyncTime(3, TimeUnit.MINUTES);
        ImmunizationLibrary.init(context, getRepository(), createCommonFtsObject(context.applicationContext()), BuildConfig.VERSION_CODE,
                BuildConfig.DATABASE_VERSION);
        ImmunizationLibrary.getInstance().setVaccineSyncTime(3, TimeUnit.MINUTES);
        fixHardcodedVaccineConfiguration();

        ConfigurableViewsLibrary.init(context);
        ChildLibrary.init(context, getRepository(), getMetadata(), BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
        // Init Reporting library
        ReportingLibrary.init(context, getRepository(), null, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
        ReportingLibrary.getInstance().addMultiResultProcessor(new TripleResultProcessor());

        ActivityConfiguration activityConfiguration = new ActivityConfiguration();
        activityConfiguration.setHomeRegisterActivityClass(AncRegisterActivity.class);
        activityConfiguration.setLandingPageActivityClass(AllClientsRegisterActivity.class);
        activityConfiguration.setProfileActivityClass(GizAncProfileActivity.class);

        AncMetadata ancMetadata = new AncMetadata();
        ancMetadata.setLocationLevels(GizUtils.getLocationLevels());
        ancMetadata.setHealthFacilityLevels(GizUtils.getHealthFacilityLevels());
        ancMetadata.setFieldsWithLocationHierarchy(new HashSet<>(Collections.singletonList("village")));
        ancMetadata.addTransferProcessorToHashMap(ConstantsUtils.EventTypeUtils.ANC_MATERNITY_TRANSFER, new GizAncMaternityTransferProcessor());
        AncLibrary.init(context, BuildConfig.DATABASE_VERSION, activityConfiguration, null, new GizAncRegisterQueryProvider(), ancMetadata);

        setupOPDLibrary();
        setupMaternityLibrary();
        setupPncLibrary();

        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

        initRepositories();
        initOfflineSchedules();

        SyncStatusBroadcastReceiver.init(this);
        LocationHelper.init(GizUtils.ALLOWED_LEVELS, GizUtils.DEFAULT_LOCATION_LEVEL);
        jsonSpecHelper = new JsonSpecHelper(this);

        //init Job Manager
        JobManager.create(this).addJobCreator(new GizMalawiJobCreator());
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        initMinimumDateForReportGeneration();
    }

    private void initMinimumDateForReportGeneration() {
        AllSharedPreferences allSharedPreferences = context().allSharedPreferences();
        if (StringUtils.isBlank(allSharedPreferences.getPreference(Constants.ReportingConfig.MIN_REPORT_DATE))) {
            Calendar startDate = Calendar.getInstance();
            startDate.set(Calendar.DAY_OF_MONTH, 1);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
            startDate.set(Calendar.MILLISECOND, 0);
            startDate.add(Calendar.MONTH, -1 * HIA2ReportsActivity.MONTH_SUGGESTION_LIMIT);
            String dateFormatted = new SimpleDateFormat(GizConstants.DateTimeFormat.YYYY_MM_dd_HH_mm_ss).format(startDate.getTime());
            allSharedPreferences.savePreference(Constants.ReportingConfig.MIN_REPORT_DATE, dateFormatted);
        }
    }

    private void setupMaternityLibrary() {
        //Maternity Initialization
        MaternityMetadata maternityMetadata = new MaternityMetadata(MaternityConstants.Form.MATERNITY_REGISTRATION
                , MaternityDbConstants.KEY.TABLE
                , MaternityConstants.EventType.MATERNITY_REGISTRATION
                , MaternityConstants.EventType.UPDATE_MATERNITY_REGISTRATION
                , MaternityConstants.CONFIG
                , BaseMaternityFormActivity.class
                , GizMaternityProfileActivity.class
                , true);
        maternityMetadata.setFieldsWithLocationHierarchy(new HashSet<>(Collections.singletonList("village")));
        maternityMetadata.setLocationLevels(GizUtils.getLocationLevels());
        maternityMetadata.setHealthFacilityLevels(GizUtils.getHealthFacilityLevels());

        MaternityConfiguration maternityConfiguration = new MaternityConfiguration
                .Builder(GizMaternityRegisterQueryProvider.class)
                .setMaternityMetadata(maternityMetadata)
                .setMaternityRegisterRowOptions(GizMaternityRegisterRowOptions.class)
                .addMaternityFormProcessingTask(MaternityConstants.EventType.MATERNITY_OUTCOME, GizMaternityOutcomeFormProcessing.class)
                .build();
        MaternityLibrary.init(context, getRepository(), maternityConfiguration, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
    }

    private void setupPncLibrary() {
        PncMetadata pncMetadata = new PncMetadata(PncConstants.Form.PNC_REGISTRATION
                , PncDbConstants.KEY.TABLE
                , PncConstants.EventTypeConstants.PNC_REGISTRATION
                , PncConstants.EventTypeConstants.UPDATE_PNC_REGISTRATION
                , PncConstants.CONFIG
                , GizPncFormActivity.class
                , GizPncProfileActivity.class
                , true);
        pncMetadata.setLocationLevels(GizUtils.getLocationLevels());
        pncMetadata.setHealthFacilityLevels(GizUtils.getHealthFacilityLevels());
        pncMetadata.setFieldsWithLocationHierarchy(new HashSet<>(Collections.singletonList("village")));

        PncConfiguration pncConfiguration = new PncConfiguration
                .Builder(GizPncRegisterQueryProvider.class)
                .setPncMetadata(pncMetadata)
                .setPncRegisterRowOptions(GizPncRegisterRowOptions.class)
                .addPncFormProcessingTask(PncConstants.EventTypeConstants.PNC_MEDIC_INFO, GizPncMedicInfoFormProcessing.class)
                .build();
        PncLibrary.init(context, getRepository(), pncConfiguration, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
    }

    private void setupOPDLibrary() {
        OpdMetadata opdMetadata = new OpdMetadata(OpdConstants.JSON_FORM_KEY.NAME, OpdDbConstants.KEY.TABLE,
                OpdConstants.EventType.OPD_REGISTRATION, OpdConstants.EventType.UPDATE_OPD_REGISTRATION,
                OpdConstants.CONFIG, OpdFormActivity.class, GizOpdProfileActivity.class, true);

        opdMetadata.setFieldsWithLocationHierarchy(new HashSet<>(Collections.singletonList("village")));
        opdMetadata.setLookUpQueryForOpdClient(String.format("select id as _id, %s, %s, %s, %s, %s, %s, %s, national_id from " + OpdDbConstants.KEY.TABLE + " where [condition] ", OpdConstants.KEY.RELATIONALID, OpdConstants.KEY.FIRST_NAME,
                OpdConstants.KEY.LAST_NAME, OpdConstants.KEY.GENDER, OpdConstants.KEY.DOB, OpdConstants.KEY.BASE_ENTITY_ID, OpdDbConstants.KEY.OPENSRP_ID));
        OpdConfiguration opdConfiguration = new OpdConfiguration.Builder(OpdRegisterQueryProvider.class)
                .setOpdMetadata(opdMetadata)
                .setOpdRegisterProviderMetadata(GizOpdRegisterProviderMetadata.class)
                .setOpdRegisterRowOptions(GizOpdRegisterRowOptions.class)
                .setOpdRegisterSwitcher(GizOpdRegisterSwitcher.class)
                .build();

        OpdLibrary.init(context, getRepository(), opdConfiguration, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
    }

    private ChildMetadata getMetadata() {
        ChildMetadata metadata = new ChildMetadata(ChildFormActivity.class, ChildProfileActivity.class,
                ChildImmunizationActivity.class, ChildRegisterActivity.class, true, new GizChildRegisterQueryProvider());
        metadata.updateChildRegister(GizConstants.JSON_FORM.CHILD_ENROLLMENT, GizConstants.TABLE_NAME.ALL_CLIENTS,
                GizConstants.TABLE_NAME.ALL_CLIENTS, GizConstants.EventType.CHILD_REGISTRATION,
                GizConstants.EventType.UPDATE_CHILD_REGISTRATION, GizConstants.EventType.OUT_OF_CATCHMENT, GizConstants.CONFIGURATION.CHILD_REGISTER,
                GizConstants.RELATIONSHIP.MOTHER, GizConstants.JSON_FORM.OUT_OF_CATCHMENT_SERVICE);
        metadata.setFieldsWithLocationHierarchy(new HashSet<>(Collections.singletonList("home_address")));
        metadata.setLocationLevels(GizUtils.getLocationLevels());
        metadata.setHealthFacilityLevels(GizUtils.getHealthFacilityLevels());
        return metadata;
    }

    private void initRepositories() {
        weightRepository();
        heightRepository();
        vaccineRepository();
        weightZScoreRepository();
        heightZScoreRepository();
    }

    private void initOfflineSchedules() {
        try {
            List<VaccineGroup> childVaccines = VaccinatorUtils.getSupportedVaccines(this);
            List<Vaccine> specialVaccines = VaccinatorUtils.getSpecialVaccines(this);
            VaccineSchedule.init(childVaccines, specialVaccines, GizConstants.KEY.CHILD);
            //  VaccineSchedule.vaccineSchedules.get(GizConstants.KEY.CHILD).remove("BCG 2");
        } catch (Exception e) {
            Timber.e(e, "GizMalawiApplication --> initOfflineSchedules");
        }
    }

    public WeightRepository weightRepository() {
        return GrowthMonitoringLibrary.getInstance().weightRepository();
    }

    public HeightRepository heightRepository() {
        return GrowthMonitoringLibrary.getInstance().heightRepository();
    }

    public VaccineRepository vaccineRepository() {
        return ImmunizationLibrary.getInstance().vaccineRepository();
    }

    public WeightZScoreRepository weightZScoreRepository() {
        return GrowthMonitoringLibrary.getInstance().weightZScoreRepository();
    }

    public HeightZScoreRepository heightZScoreRepository() {
        return GrowthMonitoringLibrary.getInstance().heightZScoreRepository();
    }

    @Override
    public void logoutCurrentUser() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(intent);
        context.userService().logoutSession();
    }

    @Override
    public Repository getRepository() {
        try {
            if (repository == null) {
                repository = new GizMalawiRepository(getInstance().getApplicationContext(), context);
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.e(e, "GizMalawiApplication --> getRepository");
        }
        return repository;
    }

    public Context getContext() {
        return context;
    }

    @NotNull
    @Override
    public ClientProcessorForJava getClientProcessor() {
        return GizMalawiProcessorForJava.getInstance(this);
    }

    @Override
    public void onTerminate() {
        Timber.i("Application is terminating. Stopping sync scheduler and resetting isSyncInProgress setting.");
        cleanUpSyncState();
        TimeChangedBroadcastReceiver.destroy(this);
        SyncStatusBroadcastReceiver.destroy(this);
        super.onTerminate();
    }

    protected void cleanUpSyncState() {
        try {
            DrishtiSyncScheduler.stop(getApplicationContext());
            context.allSharedPreferences().saveIsSyncInProgress(false);
        } catch (Exception e) {
            Timber.e(e, "GizMalawiApplication --> cleanUpSyncState");
        }
    }

    @Override
    public void onTimeChanged() {
        String username = getContext().userService().getAllSharedPreferences().fetchRegisteredANM();
        context.userService().forceRemoteLogin(username);
        logoutCurrentUser();
    }

    @Override
    public void onTimeZoneChanged() {
        String username = getContext().userService().getAllSharedPreferences().fetchRegisteredANM();
        context.userService().forceRemoteLogin(username);
        logoutCurrentUser();
    }

    public Context context() {
        return context;
    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository();
        }
        return eventClientRepository;
    }

    public RecurringServiceTypeRepository recurringServiceTypeRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
    }

    public RecurringServiceRecordRepository recurringServiceRecordRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
    }

    public boolean isLastModified() {
        return lastModified;
    }

    public void setLastModified(boolean lastModified) {
        this.lastModified = lastModified;
    }

    public ECSyncHelper getEcSyncHelper() {
        if (ecSyncHelper == null) {
            ecSyncHelper = ECSyncHelper.getInstance(getApplicationContext());
        }
        return ecSyncHelper;
    }

    @VisibleForTesting
    protected void fixHardcodedVaccineConfiguration() {
        VaccineRepo.Vaccine[] vaccines = ImmunizationLibrary.getInstance().getVaccines("child");

        HashMap<String, VaccineDuplicate> replacementVaccines = new HashMap<>();
        replacementVaccines.put("MR 2", new VaccineDuplicate("MR 2", VaccineRepo.Vaccine.mr1, -1, 548, 183, "child"));
        replacementVaccines.put("BCG 2", new VaccineDuplicate("BCG 2", VaccineRepo.Vaccine.bcg, 1825, 0, 42, "child"));

        for (VaccineRepo.Vaccine vaccine : vaccines) {
            if (replacementVaccines.containsKey(vaccine.display())) {
                VaccineDuplicate vaccineDuplicate = replacementVaccines.get(vaccine.display());

                vaccine.setCategory(vaccineDuplicate.category());
                vaccine.setExpiryDays(vaccineDuplicate.expiryDays());
                vaccine.setMilestoneGapDays(vaccineDuplicate.milestoneGapDays());
                vaccine.setPrerequisite(vaccineDuplicate.prerequisite());
                vaccine.setPrerequisiteGapDays(vaccineDuplicate.prerequisiteGapDays());
            }
        }

        ImmunizationLibrary.getInstance().setVaccines(vaccines, "child");
    }

    public DailyTalliesRepository dailyTalliesRepository() {
        if (dailyTalliesRepository == null) {
            dailyTalliesRepository = new DailyTalliesRepository();
        }
        return dailyTalliesRepository;
    }

    public HIA2IndicatorsRepository hIA2IndicatorsRepository() {
        if (hia2IndicatorsRepository == null) {
            hia2IndicatorsRepository = new HIA2IndicatorsRepository();
        }
        return hia2IndicatorsRepository;
    }

    public MonthlyTalliesRepository monthlyTalliesRepository() {
        if (monthlyTalliesRepository == null) {
            monthlyTalliesRepository = new MonthlyTalliesRepository();
        }

        return monthlyTalliesRepository;
    }

    public Hia2ReportRepository hia2ReportRepository() {
        if (hia2ReportRepository == null) {
            hia2ReportRepository = new GizHia2ReportRepository();
        }
        return hia2ReportRepository;
    }

    @VisibleForTesting
    public void setVaccineGroups(List<VaccineGroup> vaccines) {
        this.vaccineGroups = vaccines;
    }

    public ClientRegisterTypeRepository registerTypeRepository() {
        if (registerTypeRepository == null) {
            this.registerTypeRepository = new ClientRegisterTypeRepository();
        }
        return this.registerTypeRepository;
    }

    public ChildAlertUpdatedRepository alertUpdatedRepository() {
        if (childAlertUpdatedRepository == null) {
            this.childAlertUpdatedRepository = new ChildAlertUpdatedRepository();
        }
        return this.childAlertUpdatedRepository;
    }

    public GizEventRepository gizEventRepository() {
        if (gizEventRepository == null) {
            gizEventRepository = new GizEventRepository();
        }
        return gizEventRepository;
    }

    public AppExecutors getAppExecutors() {
        if (appExecutors == null) {
            appExecutors = new AppExecutors();
        }
        return appExecutors;
    }
}

