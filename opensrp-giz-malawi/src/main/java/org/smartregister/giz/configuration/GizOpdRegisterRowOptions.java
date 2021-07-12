package org.smartregister.giz.configuration;

import android.database.Cursor;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.giz.R;
import org.smartregister.opd.configuration.OpdRegisterRowOptions;
import org.smartregister.opd.dao.VisitDao;
import org.smartregister.opd.holders.OpdRegisterViewHolder;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-10-08
 */

public class GizOpdRegisterRowOptions implements OpdRegisterRowOptions {

    @Override
    public boolean isDefaultPopulatePatientColumn() {
        return false;
    }

    @Override
    public void populateClientRow(@NonNull Cursor cursor, @NonNull CommonPersonObjectClient commonPersonObjectClient
            , @NonNull SmartRegisterClient smartRegisterClient, @NonNull OpdRegisterViewHolder opdRegisterViewHolder) {
        Map<String, String> columnMaps = commonPersonObjectClient.getColumnmaps();

        Button dueButton = opdRegisterViewHolder.dueButton;
        if (VisitDao.getSeenToday(commonPersonObjectClient.getCaseId())) {
            dueButton.setText(R.string.seen_today);
            dueButton.setTextColor(dueButton.getContext().getResources().getColor(R.color.green_overlay));
        } else {
            dueButton.setBackgroundResource(R.color.transparent);
            dueButton.setText(" ");

        }
    }

    @Override
    public boolean isCustomViewHolder() {
        return false;
    }

    @Nullable
    @Override
    public OpdRegisterViewHolder createCustomViewHolder(@NonNull View itemView) {
        return null;
    }

    @Override
    public boolean useCustomViewLayout() {
        return false;
    }

    @Override
    public int getCustomViewLayoutId() {
        return 0;
    }
}