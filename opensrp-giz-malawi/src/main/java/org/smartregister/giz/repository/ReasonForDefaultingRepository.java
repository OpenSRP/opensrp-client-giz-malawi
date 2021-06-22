package org.smartregister.giz.repository;

import android.content.ContentValues;
import android.database.Cursor;

import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.giz.model.ReasonForDefaultingModel;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;

import timber.log.Timber;

public class ReasonForDefaultingRepository extends BaseRepository {
    public static final String TABLE_NAME = "reason_for_defaulting";

    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + "(" +
            GizConstants.JsonAssetsHelper.ID + "  VARCHAR NOT NULL PRIMARY KEY, " +
            GizConstants.JsonAssetsHelper.OUTREACH_DATE + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.FOLLOWUP_DATE + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.OUTREACH_DEFAULTING_REASON + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.ADDITIONAL_DEFAULTING_NOTES + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.BASE_ENTITY_ID + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.EVENT_DATE + "  VARCHAR, " +
            GizConstants.JsonAssetsHelper.OTHER_DEFAULTING_REASON + "  VARCHAR )";
    public static final String BASE_ID_INDEX = "CREATE UNIQUE INDEX " + TABLE_NAME + "_" + GizConstants.JsonAssetsHelper.ID + "_index ON " + TABLE_NAME + "(" + GizConstants.JsonAssetsHelper.ID + " COLLATE NOCASE " + ")";


    public static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_SQL);
        database.execSQL(BASE_ID_INDEX);
    }

    public void addOrUpdate(ReasonForDefaultingModel reasonForDefaultingModel) {
        addOrUpdateReasonForDefaulting(reasonForDefaultingModel);
    }

    public void addOrUpdateReasonForDefaulting(ReasonForDefaultingModel reasonForDefaultingModel) {
        SQLiteDatabase database = getWritableDatabase();
        if (reasonForDefaultingModel == null) {
            return;
        }
        database.replace(TABLE_NAME, null, createValues(reasonForDefaultingModel));
    }

    private ContentValues createValues(ReasonForDefaultingModel reasonForDefaultingModel) {
        ContentValues values = new ContentValues();
        values.put(GizConstants.JsonAssetsHelper.ID, reasonForDefaultingModel.getId());
        values.put(GizConstants.JsonAssetsHelper.OUTREACH_DATE, reasonForDefaultingModel.getOutreachDate());
        values.put(GizConstants.JsonAssetsHelper.FOLLOWUP_DATE, reasonForDefaultingModel.getFollowupDate());
        values.put(GizConstants.JsonAssetsHelper.OUTREACH_DEFAULTING_REASON, reasonForDefaultingModel.getOutreachDefaultingReason());
        values.put(GizConstants.JsonAssetsHelper.ADDITIONAL_DEFAULTING_NOTES, reasonForDefaultingModel.getAdditionalDefaultingNotes());
        values.put(GizConstants.JsonAssetsHelper.BASE_ENTITY_ID, reasonForDefaultingModel.getBaseEntityId());
        values.put(GizConstants.JsonAssetsHelper.EVENT_DATE, reasonForDefaultingModel.getDateCreated());
        values.put(GizConstants.JsonAssetsHelper.OTHER_DEFAULTING_REASON, reasonForDefaultingModel.getOtherOutreachDefaultingReason());
        return values;
    }

    public ArrayList<ReasonForDefaultingModel> readAllReasonsForDefaulting() {
        String[] columns = {GizConstants.JsonAssetsHelper.ID, GizConstants.JsonAssetsHelper.OUTREACH_DATE,
                GizConstants.JsonAssetsHelper.FOLLOWUP_DATE, GizConstants.JsonAssetsHelper.OUTREACH_DEFAULTING_REASON, GizConstants.JsonAssetsHelper.ADDITIONAL_DEFAULTING_NOTES,
                GizConstants.JsonAssetsHelper.BASE_ENTITY_ID, GizConstants.JsonAssetsHelper.EVENT_DATE, GizConstants.JsonAssetsHelper.OTHER_DEFAULTING_REASON};
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, columns, null, null, null, null, null, null);
        ArrayList<ReasonForDefaultingModel> reasonForDefaultingModels = new ArrayList<>();

        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    reasonForDefaultingModels.add(new ReasonForDefaultingModel(
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.ID)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.OUTREACH_DATE)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.FOLLOWUP_DATE)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.OUTREACH_DEFAULTING_REASON)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.ADDITIONAL_DEFAULTING_NOTES)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.BASE_ENTITY_ID)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.EVENT_DATE)),
                            cursor.getString(cursor.getColumnIndex(GizConstants.JsonAssetsHelper.OTHER_DEFAULTING_REASON))));

                } while (cursor.moveToNext());
            }

        } catch (SQLException e) {
            Timber.e(e, "Could not add records");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return reasonForDefaultingModels;
    }

    public void purgeReasonForDefaulting(String baseEntityID) {
        try {
            getWritableDatabase().execSQL("DELETE FROM reason_for_defaulting WHERE id ='" + baseEntityID + "'");
        } catch (SQLException e) {
            Timber.e(e, "Could not delete records");
        }
    }

    public long getReasonForDefaultingCount() {
        SQLiteDatabase db = getWritableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return count;
    }

}
