package org.smartregister.giz.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.AllConstants;
import org.smartregister.anc.library.repository.ContactTasksRepository;
import org.smartregister.anc.library.repository.PartialContactRepository;
import org.smartregister.anc.library.repository.PreviousContactRepository;
import org.smartregister.child.util.ChildDbMigrations;
import org.smartregister.child.util.Utils;
import org.smartregister.configurableviews.repository.ConfigurableViewsRepository;
import org.smartregister.domain.db.Column;
import org.smartregister.giz.BuildConfig;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.HeightZScoreRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.WeightZScoreRepository;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.IMDatabaseUtils;
import org.smartregister.maternity.repository.MaternityChildRepository;
import org.smartregister.maternity.repository.MaternityPartialFormRepository;
import org.smartregister.opd.repository.OpdDetailsRepository;
import org.smartregister.opd.repository.OpdDiagnosisAndTreatmentFormRepository;
import org.smartregister.opd.repository.OpdDiagnosisDetailRepository;
import org.smartregister.opd.repository.OpdTestConductedRepository;
import org.smartregister.opd.repository.OpdTreatmentDetailRepository;
import org.smartregister.opd.repository.OpdVisitRepository;
import org.smartregister.opd.repository.VisitDetailsRepository;
import org.smartregister.opd.repository.VisitRepository;
import org.smartregister.pnc.repository.PncChildRepository;
import org.smartregister.pnc.repository.PncMedicInfoRepository;
import org.smartregister.pnc.repository.PncOtherVisitRepository;
import org.smartregister.pnc.repository.PncPartialFormRepository;
import org.smartregister.pnc.repository.PncStillBornRepository;
import org.smartregister.pnc.repository.PncVisitChildStatusRepository;
import org.smartregister.pnc.repository.PncVisitInfoRepository;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.repository.DailyIndicatorCountRepository;
import org.smartregister.reporting.repository.IndicatorQueryRepository;
import org.smartregister.reporting.repository.IndicatorRepository;
import org.smartregister.reporting.util.ReportingUtils;
import org.smartregister.repository.AlertRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Hia2ReportRepository;
import org.smartregister.repository.Repository;
import org.smartregister.repository.SettingsRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.util.DatabaseMigrationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


public class GizMalawiRepository extends Repository {

    protected SQLiteDatabase readableDatabase;
    protected SQLiteDatabase writableDatabase;

    private Context context;
    private String indicatorsConfigFile = GizConstants.File.INDICATOR_CONFIG_FILE;
    private String indicatorDataInitialisedPref = GizConstants.Pref.INDICATOR_DATA_INITIALISED;
    private String appVersionCodePref = GizConstants.Pref.APP_VERSION_CODE;

    public GizMalawiRepository(@NonNull Context context, @NonNull org.smartregister.Context openSRPContext) {
        super(context, AllConstants.DATABASE_NAME, BuildConfig.DATABASE_VERSION, openSRPContext.session(),
                GizMalawiApplication
                        .createCommonFtsObject(context), openSRPContext.sharedRepositoriesArray());
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        super.onCreate(database);
        EventClientRepository
                .createTable(database, EventClientRepository.Table.client, EventClientRepository.client_column.values());
        EventClientRepository
                .createTable(database, EventClientRepository.Table.event, EventClientRepository.event_column.values());
        ConfigurableViewsRepository.createTable(database);
        UniqueIdRepository.createTable(database);

        PartialContactRepository.createTable(database);
        PreviousContactRepository.createTable(database);
        ContactTasksRepository.createTable(database);

        SettingsRepository.onUpgrade(database);
        WeightRepository.createTable(database);
        HeightRepository.createTable(database);
        VaccineRepository.createTable(database);

        OpdVisitRepository.createTable(database);
        OpdDetailsRepository.createTable(database);
        OpdDiagnosisAndTreatmentFormRepository.createTable(database);
        OpdDiagnosisDetailRepository.createTable(database);
        OpdTreatmentDetailRepository.createTable(database);
        OpdTestConductedRepository.createTable(database);
        ClientRegisterTypeRepository.createTable(database);
        ChildAlertUpdatedRepository.createTable(database);
        //reporting
        IndicatorRepository.createTable(database);
        IndicatorQueryRepository.createTable(database);
        DailyIndicatorCountRepository.createTable(database);
        GizMonthlyTalliesRepository.createTable(database);
        HIA2IndicatorsRepository.createTable(database);

        EventClientRepository.createTable(database, Hia2ReportRepository.Table.hia2_report, GizHia2ReportRepository.ReportColumn.values());

        runLegacyUpgrades(database);

        onUpgrade(database, 11, BuildConfig.DATABASE_VERSION);

        initializeReportIndicatorState(database);

        // Maternity
        MaternityPartialFormRepository.createTable(database);
        MaternityChildRepository.createTable(database);

        // Pnc
        PncChildRepository.createTable(database);
        PncStillBornRepository.createTable(database);
        PncVisitInfoRepository.createTable(database);
        PncVisitChildStatusRepository.createTable(database);
        PncOtherVisitRepository.createTable(database);
        PncPartialFormRepository.createTable(database);
        PncMedicInfoRepository.createTable(database);
    }

    @VisibleForTesting
    protected void initializeReportIndicatorState(SQLiteDatabase database) {
        // initialize from yml file
        ReportingLibrary reportingLibraryInstance = ReportingLibrary.getInstance();
        // Check if indicator data initialised
        boolean indicatorDataInitialised = Boolean.parseBoolean(reportingLibraryInstance.getContext()
                .allSharedPreferences().getPreference(indicatorDataInitialisedPref));
        boolean isUpdated = checkIfAppUpdated();
        if (!indicatorDataInitialised || isUpdated) {
            Timber.d("Initialising indicator repositories!!");
            reportingLibraryInstance.initIndicatorData(indicatorsConfigFile, database); // This will persist the data in the DB
            reportingLibraryInstance.getContext().allSharedPreferences().savePreference(indicatorDataInitialisedPref, "true");
            reportingLibraryInstance.getContext().allSharedPreferences().savePreference(appVersionCodePref, String.valueOf(BuildConfig.VERSION_CODE));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.w("Upgrading database from version %d to %d, which will destroy all old data", oldVersion, newVersion);

        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            switch (upgradeTo) {
                case 2:
                    upgradeToVersion2(db);
                    break;
                case 3:
                    upgradeToVersion3(db);
                    break;
                case 4:
                    upgradeToVersion4(db);
                    break;
                case 5:
                    upgradeToVersion5(db);
                    break;
                case 6:
                    upgradeToVersion6(db);
                    break;
                case 7:
                    upgradeToVersion7OutOfArea(db);
                    break;
                case 8:
                    upgradeToVersion8AddServiceGroupColumn(db);
                    break;
                case 9:
                    ChildDbMigrations.addShowBcg2ReminderAndBcgScarColumnsToEcChildDetails(db);
                    break;
                case 10:
                case 11:
                    upgradeToVersion11CreateHia2IndicatorsRepository(db);
                case 12:
                    EventClientRepository.createAdditionalColumns(db);
                    break;
                case 13:
                    upgradeToVersion13CreateReasonForDefaultingTable(db);
                    break;
                case 14:
                    upgradeToVersion14UpdateMissingColumns(db);
                    break;
                case 15:
                    upgradeToVersion15TriggerCreateNewTable(db);
                    break;
                case 16:
                    upgradeToVersion16CreateNewVisitTable(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }

        ChildDbMigrations.addShowBcg2ReminderAndBcgScarColumnsToEcChildDetails(db);

//        DailyIndicatorCountRepository.performMigrations(db);
        IndicatorQueryRepository.performMigrations(db);
        dumpHIA2IndicatorsCSV(db);
    }

    private void upgradeToVersion11CreateHia2IndicatorsRepository(SQLiteDatabase db) {
        if (!ReportingUtils.isTableExists(db, HIA2IndicatorsRepository.TABLE_NAME)) {
            HIA2IndicatorsRepository.createTable(db);
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        byte[] pass = GizMalawiApplication.getInstance().getPassword();
        if (pass != null && pass.length > 0) {
            return getReadableDatabase(pass);
        } else {
            throw new IllegalStateException("Password is blank");
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        byte[] pass = GizMalawiApplication.getInstance().getPassword();
        if (pass != null && pass.length > 0) {
            return getWritableDatabase(pass);
        } else {
            throw new IllegalStateException("Password is blank");
        }
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase(String password) {
        if (writableDatabase == null || !writableDatabase.isOpen()) {
            if (writableDatabase != null) {
                writableDatabase.close();
            }
            writableDatabase = super.getWritableDatabase(password);
        }
        return writableDatabase;
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase(String password) {
        try {
            if (readableDatabase == null || !readableDatabase.isOpen()) {
                if (readableDatabase != null) {
                    readableDatabase.close();
                }
                readableDatabase = super.getReadableDatabase(password);
            }
            return readableDatabase;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }

    }

    @Override
    public synchronized void close() {
        if (readableDatabase != null) {
            readableDatabase.close();
        }

        if (writableDatabase != null) {
            writableDatabase.close();
        }
        super.close();
    }

    private void runLegacyUpgrades(@NonNull SQLiteDatabase database) {
        upgradeToVersion2(database);
        upgradeToVersion3(database);
        upgradeToVersion4(database);
        upgradeToVersion5(database);
        upgradeToVersion6(database);
        upgradeToVersion7OutOfArea(database);
        upgradeToVersion7RecurringServiceUpdate(database);
        upgradeToVersion7EventWeightHeightVaccineRecurringChange(database);
        upgradeToVersion7VaccineRecurringServiceRecordChange(database);
        upgradeToVersion7WeightHeightVaccineRecurringServiceChange(database);
        upgradeToVersion7RemoveUnnecessaryTables(database);
    }

    /**
     * Version 16 added service_group column
     *
     * @param database
     */
    private void upgradeToVersion8AddServiceGroupColumn(@NonNull SQLiteDatabase database) {
        try {
            database.execSQL(RecurringServiceTypeRepository.ADD_SERVICE_GROUP_COLUMN);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion8AddServiceGroupColumn");
        }
    }

    /**
     * Version 2 added some columns to the ec_child table
     *
     * @param database
     */
    private void upgradeToVersion2(@NonNull SQLiteDatabase database) {
        try {
            // Run insert query
            ArrayList<String> newlyAddedFields = new ArrayList<>();
            newlyAddedFields.add("BCG_2");
            newlyAddedFields.add("inactive");
            newlyAddedFields.add("lost_to_follow_up");

            DatabaseMigrationUtils.addFieldsToFTSTable(database, commonFtsObject, Utils.metadata().childRegister.tableName,
                    newlyAddedFields);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion2");
        }
    }

    private void upgradeToVersion3(@NonNull SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_EVENT_ID_COL);
            db.execSQL(VaccineRepository.EVENT_ID_INDEX);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_EVENT_ID_COL);
            db.execSQL(WeightRepository.EVENT_ID_INDEX);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_EVENT_ID_COL);
            db.execSQL(HeightRepository.EVENT_ID_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL);
            db.execSQL(VaccineRepository.FORMSUBMISSION_INDEX);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL);
            db.execSQL(WeightRepository.FORMSUBMISSION_INDEX);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL);
            db.execSQL(HeightRepository.FORMSUBMISSION_INDEX);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion3");
        }
    }

    private void upgradeToVersion4(@NonNull SQLiteDatabase db) {
        try {
            db.execSQL(AlertRepository.ALTER_ADD_OFFLINE_COLUMN);
            db.execSQL(AlertRepository.OFFLINE_INDEX);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion4");
        }
    }

    private void upgradeToVersion5(@NonNull SQLiteDatabase db) {
        try {
            RecurringServiceTypeRepository.createTable(db);
            RecurringServiceRecordRepository.createTable(db);

            RecurringServiceTypeRepository recurringServiceTypeRepository = GizMalawiApplication.getInstance()
                    .recurringServiceTypeRepository();
            IMDatabaseUtils.populateRecurringServices(context, db, recurringServiceTypeRepository);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion5");
        }
    }

    private void upgradeToVersion6(@NonNull SQLiteDatabase db) {
        try {
            WeightZScoreRepository.createTable(db);
            db.execSQL(WeightRepository.ALTER_ADD_Z_SCORE_COLUMN);

            HeightZScoreRepository.createTable(db);
            db.execSQL(HeightRepository.ALTER_ADD_Z_SCORE_COLUMN);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion6");
        }
    }

    private void upgradeToVersion7OutOfArea(@NonNull SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_HIA2_STATUS_COL);

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7");
        }
    }

    private void upgradeToVersion7RecurringServiceUpdate(@NonNull SQLiteDatabase db) {
        try {

            // Recurring service json changed. update
            RecurringServiceTypeRepository recurringServiceTypeRepository = GizMalawiApplication.getInstance()
                    .recurringServiceTypeRepository();
            IMDatabaseUtils.populateRecurringServices(context, db, recurringServiceTypeRepository);

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7RecurringServiceUpdate");
        }
    }

    private void upgradeToVersion7EventWeightHeightVaccineRecurringChange(@NonNull SQLiteDatabase db) {
        try {
            Column[] columns = {EventClientRepository.event_column.formSubmissionId};
            EventClientRepository.createIndex(db, EventClientRepository.Table.event, columns);

            db.execSQL(WeightRepository.ALTER_ADD_CREATED_AT_COLUMN);
            WeightRepository.migrateCreatedAt(db);

            db.execSQL(HeightRepository.ALTER_ADD_CREATED_AT_COLUMN);
            HeightRepository.migrateCreatedAt(db);

            db.execSQL(VaccineRepository.ALTER_ADD_CREATED_AT_COLUMN);
            VaccineRepository.migrateCreatedAt(db);

            db.execSQL(RecurringServiceRecordRepository.ALTER_ADD_CREATED_AT_COLUMN);
            RecurringServiceRecordRepository.migrateCreatedAt(db);

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7EventWeightHeightVaccineRecurringChange");
        }
    }

    private void upgradeToVersion7VaccineRecurringServiceRecordChange(@NonNull SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_COL);

            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_COL);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7VaccineRecurringServiceRecordChange");
        }
    }

    private void upgradeToVersion7WeightHeightVaccineRecurringServiceChange(@NonNull SQLiteDatabase db) {
        try {

            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_TEAM_COL);

            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_TEAM_COL);

            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);

            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);

            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_CHILD_LOCATION_ID_COL);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7WeightHeightVaccineRecurringServiceChange");
        }
    }

    private void upgradeToVersion7RemoveUnnecessaryTables(@NonNull SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS address");
            db.execSQL("DROP TABLE IF EXISTS obs");
            if (DatabaseMigrationUtils.isColumnExists(db, "path_reports", Hia2ReportRepository.report_column.json.name()))
                db.execSQL("ALTER TABLE path_reports RENAME TO " + Hia2ReportRepository.Table.hia2_report.name() + ";");
            if (DatabaseMigrationUtils.isColumnExists(db, EventClientRepository.Table.client.name(), "firstName"))
                DatabaseMigrationUtils.recreateSyncTableWithExistingColumnsOnly(db, EventClientRepository.Table.client);
            if (DatabaseMigrationUtils.isColumnExists(db, EventClientRepository.Table.event.name(), "locationId"))
                DatabaseMigrationUtils.recreateSyncTableWithExistingColumnsOnly(db, EventClientRepository.Table.event);


        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion7RemoveUnnecessaryTables");
        }
    }

    private void upgradeToVersion13CreateReasonForDefaultingTable(@NonNull SQLiteDatabase db) {
        try {
            ReasonForDefaultingRepository.createTable(db);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion13CreateReasonForDefaultingTable");
        }
    }

    private void upgradeToVersion14UpdateMissingColumns(@NonNull SQLiteDatabase db) {
        db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_IS_VOIDED_COL);
        db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_IS_VOIDED_COL_INDEX);
        EventClientRepository.addEventTaskId(db);
        EventClientRepository.createIndex(db, EventClientRepository.Table.event, EventClientRepository.event_column.values());
    }

    private void upgradeToVersion15TriggerCreateNewTable(@NonNull SQLiteDatabase db) {
        try {
            DatabaseMigrationUtils.createAddedECTables(db,
                    new HashSet<>(Collections.singletonList("ec_family_member_location")),
                    GizMalawiApplication.createCommonFtsObject(context));

        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion15");
        }
    }

    private void upgradeToVersion16CreateNewVisitTable(@NonNull SQLiteDatabase db) {
        try {
            VisitRepository.createTable(db);
            VisitDetailsRepository.createTable(db);
        } catch (Exception e) {
            Timber.e(e, "upgradeToVersion16");
        }
    }

    private boolean checkIfAppUpdated() {
        String savedAppVersion = ReportingLibrary.getInstance().getContext().allSharedPreferences().getPreference(appVersionCodePref);
        if (savedAppVersion.isEmpty()) {
            return true;
        } else {
            int savedVersion = Integer.parseInt(savedAppVersion);
            return (BuildConfig.VERSION_CODE > savedVersion);
        }
    }

    @VisibleForTesting
    protected void dumpHIA2IndicatorsCSV(SQLiteDatabase db) {
        List<Map<String, String>> csvData = Utils.populateTableFromCSV(
                context,
                HIA2IndicatorsRepository.INDICATORS_CSV_FILE,
                HIA2IndicatorsRepository.CSV_COLUMN_MAPPING);
        HIA2IndicatorsRepository hIA2IndicatorsRepository = GizMalawiApplication.getInstance()
                .hIA2IndicatorsRepository();
        hIA2IndicatorsRepository.save(db, csvData);
    }
}
