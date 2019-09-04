package org.smartregister.giz.application;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.SyncConfiguration;
import org.smartregister.SyncFilter;
import org.smartregister.giz.BuildConfig;
import org.smartregister.location.helper.LocationHelper;

import java.util.ArrayList;

public class GizMalawiSyncConfiguration extends SyncConfiguration {
    @Override
    public int getSyncMaxRetries() {
        return BuildConfig.MAX_SYNC_RETRIES;
    }

    @Override
    public SyncFilter getSyncFilterParam() {
        return SyncFilter.LOCATION;
    }

    @Override
    public String getSyncFilterValue() {
        LocationHelper locationHelper = LocationHelper.getInstance();
        ArrayList<String> locationIds = locationHelper.locationsFromHierarchy(true, locationHelper.getDefaultLocation());
        if (locationIds.size() > 0) {
            return StringUtils.join(locationIds, ",");
        }

        return "";
    }

    @Override
    public int getUniqueIdSource() {
        return BuildConfig.OPENMRS_UNIQUE_ID_SOURCE;
    }

    @Override
    public int getUniqueIdBatchSize() {
        return BuildConfig.OPENMRS_UNIQUE_ID_BATCH_SIZE;
    }

    @Override
    public int getUniqueIdInitialBatchSize() {
        return BuildConfig.OPENMRS_UNIQUE_ID_INITIAL_BATCH_SIZE;
    }

    @Override
    public boolean isSyncSettings() {
        return BuildConfig.IS_SYNC_SETTINGS;
    }

    @Override
    public SyncFilter getEncryptionParam() {
        return SyncFilter.TEAM;
    }

    @Override
    public boolean updateClientDetailsTable() {
        return true;
    }
}

