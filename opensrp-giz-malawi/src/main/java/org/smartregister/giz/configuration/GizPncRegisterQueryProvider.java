package org.smartregister.giz.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.pnc.config.PncRegisterQueryProviderContract;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 2020-03-31
 */

public class GizPncRegisterQueryProvider extends PncRegisterQueryProviderContract {

    @NonNull
    @Override
    public String getObjectIdsQuery(@Nullable String filters, @Nullable String mainCondition) {
        if (StringUtils.isBlank(filters) && StringUtils.isBlank(mainCondition)) {
            return "SELECT object_id, last_interacted_with FROM ec_client_search INNER JOIN client_register_type ON ec_client_search.object_id = client_register_type.base_entity_id " +
                    "WHERE client_register_type.register_type = 'pnc' and ec_client_search.is_closed = 0 ORDER BY ec_client_search.last_interacted_with DESC";
        } else {
            String sql = "SELECT object_id, cast (((julianday('now' ,'localtime')- julianday((substr(pmi.delivery_date, 7) || \"-\" || substr(pmi.delivery_date,4,2) || \"-\" || substr(pmi.delivery_date, 1,2)), 'localtime'))) as INTEGER) as ddNow,(SELECT MAX(pvi.visit_date)  FROM pnc_visit_info AS pvi WHERE pvi.mother_base_entity_id = object_id) AS latest_visit_date  FROM ec_client_search INNER JOIN client_register_type ON ec_client_search.object_id = client_register_type.base_entity_id " +
                    " LEFT JOIN pnc_medic_info pmi ON ec_client_search.object_id = pmi.base_entity_id " +
                    getMainCondition(mainCondition) + getFilters(filters) +
                    "ORDER BY ec_client_search.last_interacted_with DESC";
            sql = sql.replace("%s", filters);
            return sql;
        }
    }

    @NonNull
    @Override
    public String[] countExecuteQueries(@Nullable String filters, @Nullable String mainCondition) {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder();
        return new String[]{
                sqb.countQueryFts("ec_client", null, mainCondition, filters)
        };
    }

    public String getFilters(String filters) {
        String filter = "";
        if (StringUtils.isNotBlank(filters)) {
            filter = " AND ec_client_search.phrase MATCH '%s*' ";
        }
        return filter;
    }

    public String getMainCondition(String mainCondition) {
        String condition = "";
        if (StringUtils.isNotBlank(mainCondition)) {
            condition = " AND " + mainCondition;
        }
        return " WHERE client_register_type.register_type = 'pnc' and ec_client_search.is_closed = 0 AND ec_client_search.date_removed IS NULL " + condition;
    }

    public String getCountExecuteQuery(String tempMainCondition, String tempFilters) {
        String filters = tempFilters;
        String mainCondition = tempMainCondition;
        if (!filters.isEmpty()) {
            filters = String.format(" and ec_client_search.phrase MATCH '*%s*'", filters);
        }

        if (!StringUtils.isBlank(mainCondition)) {
            mainCondition = " and " + mainCondition;
        }

        return "select count(ec_client_search.object_id) from ec_client_search " +
                "join ec_mother_details on ec_client_search.object_id =  ec_mother_details.id " +
                "join client_register_type on ec_client_search.object_id =client_register_type.base_entity_id " +
                "where register_type='pnc' " + mainCondition + filters;
    }

    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        return "SELECT ec_client.id AS _id , pmi.base_entity_id AS pmi_base_entity_id, ec_client.first_name , ec_client.last_name , '' AS middle_name , ec_client.gender , ec_client.dob , '' AS home_address, ec_client.relationalid , ec_client.opensrp_id AS register_id , ec_client.last_interacted_with, 'ec_client' as entity_table, register_type,pmi.hiv_status_current, pmi.delivery_date , ppf.base_entity_id as ppf_id, ppf.form_type as ppf_form_type," +
                " (SELECT MAX(pvi.visit_date)  FROM pnc_visit_info AS pvi WHERE pvi.mother_base_entity_id = ec_client.base_entity_id) AS latest_visit_date  FROM ec_client " +
                " LEFT JOIN pnc_medic_info AS pmi ON pmi.base_entity_id = ec_client.base_entity_id " +
                " LEFT JOIN pnc_partial_form ppf ON ppf.base_entity_id = ec_client.base_entity_id " +
                " INNER JOIN client_register_type ON ec_client.base_entity_id = client_register_type.base_entity_id " +
                " WHERE client_register_type.register_type = 'pnc' AND ec_client.is_closed = 0 AND ec_client.id IN (%s) " +
                "ORDER BY ec_client.last_interacted_with DESC";
    }
}
