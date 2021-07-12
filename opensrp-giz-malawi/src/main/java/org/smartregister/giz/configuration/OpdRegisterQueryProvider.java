package org.smartregister.giz.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.opd.OpdLibrary;
import org.smartregister.opd.configuration.OpdRegisterQueryProviderContract;
import org.smartregister.opd.utils.OpdDbConstants;
import org.smartregister.opd.utils.OpdUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-09-23
 */

public class OpdRegisterQueryProvider extends OpdRegisterQueryProviderContract {

    @NonNull
    @Override
    public String getObjectIdsQuery(@Nullable String filters, @Nullable String mainCondition) {
        Date latestValidCheckInDate = OpdLibrary.getInstance().getLatestValidCheckInDate();
        String oneDayAgo = OpdUtils.convertDate(latestValidCheckInDate, OpdDbConstants.DATE_FORMAT);
        String todayDate = new SimpleDateFormat(GizConstants.DateTimeFormat.yyyy_MM_dd, Locale.US).format(new Date());

        if (!TextUtils.isEmpty(filters)) {
            if (!TextUtils.isEmpty(mainCondition)) {
                String sql =
                        "SELECT object_id \n" +
                                "FROM (SELECT object_id, last_interacted_with \n" +
                                "FROM ec_client_search \n" +
                                "inner join client_register_type crt on crt.base_entity_id = ec_client_search.object_id  \n" +
                                "WHERE crt.register_type = 'opd' AND  ec_client_search.date_removed IS NULL AND phrase  MATCH '%s*')\n" +
                                "WHERE ec_client_search.object_id IN (SELECT base_entity_id\n" +
                                "FROM opd_client_visits where visit_group = '" + todayDate + "')\n" +
                                "ORDER BY last_interacted_with DESC";
                sql = sql.replace("%s", filters);
                return sql;

            } else {
                String sql = "SELECT object_id FROM " +
                        "(SELECT object_id, last_interacted_with FROM ec_client_search " +
                        "inner join client_register_type crt on crt.base_entity_id = ec_client_search.object_id  " +
                        "WHERE crt.register_type = 'opd' AND  ec_client_search.date_removed IS NULL AND phrase MATCH '%s*') " +
                        "ORDER BY last_interacted_with DESC";
                sql = sql.replace("%s", filters);
                return sql;
            }
        } else if (!TextUtils.isEmpty(mainCondition)) {
            String sqlQuery =
                    "Select ec_client.id as object_id\n" +
                            "FROM ec_client\n" +
                            "inner join client_register_type crt on crt.base_entity_id = ec_client.id  \n" +
                            "WHERE crt.register_type = 'opd'\n" +
                            "and  ec_client.id IN (SELECT base_entity_id FROM opd_client_visits where visit_group = '" + todayDate + "')\n" +
                            "ORDER BY last_interacted_with DESC";

            return sqlQuery;
        } else {
            return "SELECT object_id FROM " +
                    "(SELECT object_id, last_interacted_with FROM ec_client_search inner join client_register_type crt on crt.base_entity_id = ec_client_search.object_id WHERE crt.register_type = 'opd') " +
                    "ORDER BY last_interacted_with DESC";
        }
    }

    @NonNull
    @Override
    public String[] countExecuteQueries(@Nullable String filters, @Nullable String mainCondition) {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder();

        return new String[] {
                sqb.countQueryFts("ec_client", null, mainCondition, filters)
        };
    }

    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        Date latestValidCheckInDate = OpdLibrary.getInstance().getLatestValidCheckInDate();
        String oneDayAgo = OpdUtils.convertDate(latestValidCheckInDate, OpdDbConstants.DATE_FORMAT);
        String sqlQuery =
                "Select  d.id as _id , d.first_name , d.last_name , '' AS middle_name , d.gender , d.dob , '' AS home_address , c.first_name as mother_first_name, \n" +
                        "c.last_name as mother_last_name, \n" +
                        " null as middle_name, d.relationalid ,d.opensrp_id AS register_id, upper(rt.register_type) as register_type,\n" +
                        " d.last_interacted_with,'ec_client' as entity_table\n" +
                        " FROM ec_client d \n" +
                        "left join ec_child_details ecd on ecd.base_entity_id=d.id \n" +
                        "left join client_register_type rt on rt.base_entity_id = d.id \n" +
                        "left join ec_client c on ecd.relational_id=c.id \n" +
                        "WHERE rt.register_type = 'opd' and  d.id IN (%s) \n" +
                        "ORDER BY d.last_interacted_with DESC";

        return sqlQuery;
    }
}
