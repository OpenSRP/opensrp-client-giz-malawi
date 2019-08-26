package org.smartregister.giz.repository;

import android.content.Context;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.AllConstants;
import org.smartregister.child.util.Utils;
import org.smartregister.configurableviews.repository.ConfigurableViewsRepository;
import org.smartregister.domain.db.Column;
import org.smartregister.giz.BuildConfig;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.HeightZScoreRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.WeightZScoreRepository;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.IMDatabaseUtils;
import org.smartregister.repository.AlertRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Hia2ReportRepository;
import org.smartregister.repository.Repository;
import org.smartregister.repository.SettingsRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.util.DatabaseMigrationUtils;

import java.util.ArrayList;

import timber.log.Timber;


public class GizMalawiRepository extends Repository {


    private static final String TAG = GizMalawiApplication.class.getCanonicalName();

    protected SQLiteDatabase readableDatabase;
    protected SQLiteDatabase writableDatabase;
    private Context context;

    public GizMalawiRepository(Context context, org.smartregister.Context openSRPContext) {
        super(context, AllConstants.DATABASE_NAME, BuildConfig.DATABASE_VERSION, openSRPContext.session(),
                GizMalawiApplication
                        .createCommonFtsObject(), openSRPContext.sharedRepositoriesArray());
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

        SettingsRepository.onUpgrade(database);

        WeightRepository.createTable(database);
        HeightRepository.createTable(database);
        VaccineRepository.createTable(database);

        runLegacyUpgrades(database);

        onUpgrade(database, 8, BuildConfig.DATABASE_VERSION);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(WeightRepository.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
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
                    upgradeToVersion16AddServiceGroupColumn(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        String pass = GizMalawiApplication.getInstance().getPassword();
        if (StringUtils.isNotBlank(pass)) {
            return getReadableDatabase(pass);
        } else {
            throw new IllegalStateException("Password is blank");
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        String pass = GizMalawiApplication.getInstance().getPassword();
        if (StringUtils.isNotBlank(pass)) {
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
            Log.e(TAG, "Database Error. " + e.getMessage());
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

    private void runLegacyUpgrades(SQLiteDatabase database) {
        upgradeToVersion2(database);
        upgradeToVersion3(database);
        upgradeToVersion4(database);
        upgradeToVersion5(database);
        upgradeToVersion6(database);
        upgradeToVersion7OutOfArea(database);
        upgradeToVersion8RecurringServiceUpdate(database);
        //upgradeToVersion9(database);
        upgradeToVersion12(database);
        upgradeToVersion13(database);
        upgradeToVersion14(database);
        upgradeToVersion15RemoveUnnecessaryTables(database);
        upgradeToVersion16AddServiceGroupColumn(database);

    }

    /**
     * Version 16 added service_group column
     *
     * @param database
     */
    private void upgradeToVersion16AddServiceGroupColumn(SQLiteDatabase database) {
        try{
            database.execSQL(RecurringServiceTypeRepository.ADD_SERVICE_GROUP_COLUMN);
        }
        catch (Exception e){
            Timber.e(e,"upgradeToVersion16");
        }
    }

    /**
     * Version 2 added some columns to the ec_child table
     *
     * @param database
     */
    private void upgradeToVersion2(SQLiteDatabase database) {
        try {
            // Run insert query
            ArrayList<String> newlyAddedFields = new ArrayList<>();
            newlyAddedFields.add("BCG_2");
            newlyAddedFields.add("inactive");
            newlyAddedFields.add("lost_to_follow_up");

            DatabaseMigrationUtils.addFieldsToFTSTable(database, commonFtsObject, Utils.metadata().childRegister.tableName,
                    newlyAddedFields);
        } catch (Exception e) {
            Timber.e("upgradeToVersion2 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
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
            Timber.e("upgradeToVersion3 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion4(SQLiteDatabase db) {
        try {
            db.execSQL(AlertRepository.ALTER_ADD_OFFLINE_COLUMN);
            db.execSQL(AlertRepository.OFFLINE_INDEX);
        } catch (Exception e) {
            Timber.e("upgradeToVersion4 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion5(SQLiteDatabase db) {
        try {
            RecurringServiceTypeRepository.createTable(db);
            RecurringServiceRecordRepository.createTable(db);

            RecurringServiceTypeRepository recurringServiceTypeRepository = GizMalawiApplication.getInstance()
                    .recurringServiceTypeRepository();
            IMDatabaseUtils.populateRecurringServices(context, db, recurringServiceTypeRepository);
        } catch (Exception e) {
            Timber.e("upgradeToVersion5 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion6(SQLiteDatabase db) {
        try {
            WeightZScoreRepository.createTable(db);
            db.execSQL(WeightRepository.ALTER_ADD_Z_SCORE_COLUMN);

            HeightZScoreRepository.createTable(db);
            db.execSQL(HeightRepository.ALTER_ADD_Z_SCORE_COLUMN);
        } catch (Exception e) {
            Timber.e("upgradeToVersion6 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion7OutOfArea(SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(WeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL);
            db.execSQL(HeightRepository.UPDATE_TABLE_ADD_OUT_OF_AREA_COL_INDEX);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_HIA2_STATUS_COL);

        } catch (Exception e) {
            Timber.e("upgradeToVersion7 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion8RecurringServiceUpdate(SQLiteDatabase db) {
        try {

            // Recurring service json changed. update
            RecurringServiceTypeRepository recurringServiceTypeRepository = GizMalawiApplication.getInstance()
                    .recurringServiceTypeRepository();
            IMDatabaseUtils.populateRecurringServices(context, db, recurringServiceTypeRepository);

        } catch (Exception e) {
            Timber.e("upgradeToVersion8 %s", Log.getStackTraceString(e));
        }
    }

    /*private void upgradeToVersion9(SQLiteDatabase database) {
        try {
            String ALTER_CLIENT_TABLE_VALIDATE_COLUMN = "ALTER TABLE " + EventClientRepository.Table.client + " ADD COLUMN " + EventClientRepository.client_column.validationStatus + " VARCHAR";
            database.execSQL(ALTER_CLIENT_TABLE_VALIDATE_COLUMN);

            EventClientRepository
                    .createIndex(database, EventClientRepository.Table.client, EventClientRepository.client_column.values());

        } catch (Exception e) {
            Log.e(TAG, "upgradeToVersion9 " + e.getMessage());
        }
    }*/

    private void upgradeToVersion12(SQLiteDatabase db) {
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
            Timber.e("upgradeToVersion12 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion13(SQLiteDatabase db) {
        try {
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(VaccineRepository.UPDATE_TABLE_ADD_TEAM_COL);

            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_ID_COL);
            db.execSQL(RecurringServiceRecordRepository.UPDATE_TABLE_ADD_TEAM_COL);
        } catch (Exception e) {
            Timber.e("upgradeToVersion13%s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion14(SQLiteDatabase db) {
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
            Timber.e("upgradeToVersion14 %s", Log.getStackTraceString(e));
        }
    }

    private void upgradeToVersion15RemoveUnnecessaryTables(SQLiteDatabase db) {
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
            Timber.e("upgradeToVersion15 %s", Log.getStackTraceString(e));
        }
    }


}
