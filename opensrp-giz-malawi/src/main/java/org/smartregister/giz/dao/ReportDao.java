package org.smartregister.giz.dao;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.CoreLibrary;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.domain.jsonmapping.Location;
import org.smartregister.domain.jsonmapping.util.LocationTree;
import org.smartregister.domain.jsonmapping.util.TreeNode;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.domain.EligibleChild;
import org.smartregister.giz.domain.VillageDose;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineCondition;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.domain.VaccineTrigger;
import org.smartregister.immunization.domain.jsonmapping.Condition;
import org.smartregister.immunization.domain.jsonmapping.Due;
import org.smartregister.immunization.domain.jsonmapping.Expiry;
import org.smartregister.immunization.domain.jsonmapping.Schedule;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.util.AssetHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import timber.log.Timber;

public class ReportDao extends AbstractDao {

    private static HashMap<String, HashMap<String, VaccineSchedule>> vaccineSchedules;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static final SimpleDateFormat sdf = new SimpleDateFormat(GizConstants.DateTimeFormat.yyyy_MM_dd, Locale.US);


    private static void readLocations(TreeNode<String, Location> node, Map<String, String> locations) {
        locations.put(node.getId(), node.getLabel());
        if (node.getChildren() != null) {
            for (Map.Entry<String, TreeNode<String, Location>> entry : node.getChildren().entrySet()) {
                readLocations(entry.getValue(), locations);
            }
        }
    }

    @NonNull
    public static Map<String, String> extractRecordedLocations() {
        return extractRecordedLocations(CoreLibrary.getInstance().context().anmLocationController().get());
    }

    @NonNull
    public static Map<String, String> extractRecordedLocations(String locationData) {
        Map<String, String> defaultTree = new HashMap<>();
        LocationTree locationTree = AssetHandler.jsonStringToJava(locationData, LocationTree.class);
        if (locationTree != null) {
            LinkedHashMap<String, TreeNode<String, Location>> locationHierarchyMap = locationTree.getLocationsHierarchy();
            for (Map.Entry<String, TreeNode<String, Location>> entry : locationHierarchyMap.entrySet()) {
                readLocations(entry.getValue(), defaultTree);
            }
        }

        Map<String, String> locations = new HashMap<>();
        String sql = "SELECT DISTINCT location_id FROM ec_family_member_location";
        DataMap<Void> dataMap = cursor -> {
            String locationId = getCursorValue(cursor, "location_id");
            if (StringUtils.isNotBlank(locationId) && defaultTree.containsKey(locationId))
                locations.put(locationId, defaultTree.get(locationId));
            return null;
        };

        readData(sql, dataMap);
        return locations;
    }

    private static HashMap<String, HashMap<String, VaccineSchedule>> getVaccineSchedules(String category) {

        List<VaccineGroup> vaccineGroups = GizMalawiApplication.getVaccineGroups(context);

        List<org.smartregister.immunization.domain.jsonmapping.Vaccine> specialVaccines =
                VaccinatorUtils.getSpecialVaccines(GizMalawiApplication.getInstance().getApplicationContext());

        return getSchedule(vaccineGroups, specialVaccines, category);
    }

    private static VaccineSchedule getVaccineSchedule(String vaccineName, String vaccineCategory, Schedule schedule) {
        ArrayList<VaccineTrigger> dueTriggers = new ArrayList<>();
        for (Due due : schedule.due) {
            VaccineTrigger curTrigger = VaccineTrigger.init(vaccineCategory, due);
            if (curTrigger != null) {
                dueTriggers.add(curTrigger);
            }
        }

        ArrayList<VaccineTrigger> expiryTriggers = new ArrayList<>();
        if (schedule.expiry != null) {
            for (Expiry expiry : schedule.expiry) {
                VaccineTrigger curTrigger = VaccineTrigger.init(expiry);
                if (curTrigger != null) {
                    expiryTriggers.add(curTrigger);
                }
            }
        }

        VaccineRepo.Vaccine vaccine = VaccineRepo.getVaccine(vaccineName, vaccineCategory);
        if (vaccine != null) {
            ArrayList<VaccineCondition> conditions = new ArrayList<>();
            if (schedule.conditions != null) {
                for (Condition condition : schedule.conditions) {
                    VaccineCondition curCondition = VaccineCondition.init(vaccineCategory,
                            condition);
                    if (curCondition != null) {
                        conditions.add(curCondition);
                    }
                }
            }

            return new VaccineSchedule(dueTriggers, expiryTriggers, vaccine, conditions);
        }

        return null;
    }

    private static void initVaccine(String vaccineCategory,
                                    org.smartregister.immunization.domain.jsonmapping.Vaccine curVaccine) {
        if (TextUtils.isEmpty(curVaccine.vaccine_separator)) {
            String vaccineName = curVaccine.name;
            VaccineSchedule vaccineSchedule;
            if (curVaccine.schedule != null) {
                vaccineSchedule = getVaccineSchedule(vaccineName, vaccineCategory, curVaccine.schedule);
                vaccineSchedules.get(vaccineCategory).put(vaccineName.toUpperCase(), vaccineSchedule);
            }
        } else {
            String[] splitNames = curVaccine.name
                    .split(curVaccine.vaccine_separator);
            for (int nameIndex = 0; nameIndex < splitNames.length; nameIndex++) {
                String vaccineName = splitNames[nameIndex];
                VaccineSchedule vaccineSchedule = getVaccineSchedule(vaccineName, vaccineCategory, curVaccine.schedules.get(vaccineName));
                if (vaccineSchedule != null) {
                    vaccineSchedules.get(vaccineCategory).put(vaccineName.toUpperCase(), vaccineSchedule);
                }
            }
        }
    }

    private static HashMap<String, HashMap<String, VaccineSchedule>> getSchedule(
            List<VaccineGroup> vaccines,
            List<org.smartregister.immunization.domain.jsonmapping.Vaccine> specialVaccines,
            String vaccineCategory
    ) {
        if (vaccineSchedules == null) {
            vaccineSchedules = new HashMap<>();
        }
        vaccineSchedules.put(vaccineCategory, new HashMap<>());

        for (VaccineGroup vaccineGroup : vaccines) {
            for (org.smartregister.immunization.domain.jsonmapping.Vaccine vaccine : vaccineGroup.vaccines) {
                initVaccine(vaccineCategory, vaccine);
            }
        }

        if (specialVaccines != null) {
            for (org.smartregister.immunization.domain.jsonmapping.Vaccine vaccine : specialVaccines) {
                initVaccine(vaccineCategory, vaccine);
            }
        }

        return vaccineSchedules;
    }

    private static List<Alert> getInMemoryAlerts(
            HashMap<String, HashMap<String, VaccineSchedule>> vaccineSchedules,
            String baseEntityId,
            DateTime dob,
            String vaccineCategory,
            List<Vaccine> issuedVaccines
    ) {
        List<Alert> generatedAlerts = new ArrayList<>();
        try {
            if (vaccineSchedules != null && vaccineSchedules.containsKey(vaccineCategory)) {
                for (VaccineSchedule curSchedule : vaccineSchedules.get(vaccineCategory).values()) {
                    if (curSchedule == null) continue;
                    Alert curAlert = curSchedule.getOfflineAlert(baseEntityId, dob.toDate(), issuedVaccines);
                    if (curAlert != null && curAlert.startDate() != null) {
                        generatedAlerts.add(curAlert);
                    }
                }
            }
            return generatedAlerts;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<Alert> computeChildAlerts(int age, DateTime anchorDate, String baseEntityId, @Nullable List<Vaccine> issuedVaccines) {
        try {
            String category = age < 5 ? GizConstants.KEY.CHILD : GizConstants.KEY.CHILD_OVER_FIVE;
            HashMap<String, HashMap<String, VaccineSchedule>> vaccineSchedules = getVaccineSchedules(category);
            return getInMemoryAlerts(vaccineSchedules, baseEntityId, anchorDate, category, issuedVaccines == null ? new ArrayList<>() : issuedVaccines);
        } catch (Exception e) {
            Timber.e(e);
        }
        return new ArrayList<>();
    }

    public static List<EligibleChild> fetchLiveEligibleChildrenReport(@Nullable String communityId, @Nullable Date dueDate) {
        // fetch all children in the village
        String _communityId = "('" + StringUtils.join(communityId, "','") + "')";
        int days = Days.daysBetween(new DateTime().toLocalDate(), new DateTime(dueDate).toLocalDate()).getDays();
        String sql = "select cd.base_entity_id , c.first_name , c.last_name family_name, c.middle_name ," +
                "c.dob , c.gender , l.location_id " +
                "from ec_child_details cd " +
                "inner join ec_client c on cd.base_entity_id = c.base_entity_id and c.is_closed = 0 COLLATE NOCASE " +
                "inner join ec_family_member_location l on l.base_entity_id = cd.base_entity_id COLLATE NOCASE";

        if (communityId != null && !communityId.isEmpty())
            sql += " and ( l.location_id IN " + _communityId + " or '" + communityId + "' = '') ";
        sql += "order by c.first_name , c.last_name , c.middle_name ";
        Map<String, List<Vaccine>> allVaccines = fetchAllVaccines();
        List<EligibleChild> eligibleChildren = new ArrayList<>();
        DataMap<Void> dataMap = getData(eligibleChildren, days, allVaccines, dueDate);
        readData(sql, dataMap);
        return eligibleChildren;
    }

    private static DataMap<Void> getData(List<EligibleChild> eligibleChildren, int days, Map<String, List<Vaccine>> allVaccines, Date dueDate) {

        AbstractDao.DataMap<Void> dataMap = cursor -> {
            // compute constants
            String baseEntityId = getCursorValue(cursor, "base_entity_id");
            Date dob = getCursorValueAsDate(cursor, "dob", sdf);
            Date adjustedDob = new DateTime(dob).minusDays(days).toDate();
            String name = getCursorValue(cursor, "first_name", "") + " " + getCursorValue(cursor, "middle_name", "");
            name = name.trim() + " " + getCursorValue(cursor, "family_name", "");
            int age = (int) Math.floor(Days.daysBetween(new DateTime(dob).toLocalDate(), new DateTime(dueDate).toLocalDate()).getDays() / GizConstants.KEY.ONE_YEAR);
            if (age < 5) {
                List<Vaccine> rawVaccines = allVaccines.get(baseEntityId);
                List<Vaccine> myVaccines = new ArrayList<>();
                if (rawVaccines != null) {
                    for (Vaccine vaccine : rawVaccines) {
                        vaccine.setDate(new DateTime(vaccine.getDate()).minusDays(days).toDate());
                        myVaccines.add(vaccine);
                    }
                }

                List<Alert> raw_alerts = computeChildAlerts(age, new DateTime(dob).minusDays(days), baseEntityId, myVaccines);
                Set<String> myGivenVaccines = new HashSet<>();
                for (Vaccine vaccine : myVaccines) {
                    myGivenVaccines.add(cleanName(vaccine.getName()));
                }
                List<Alert> alerts = new ArrayList<>();
                for (Alert alert : raw_alerts) {
                    if (alert.startDate() != null && alert.status() != AlertStatus.complete && !myGivenVaccines.contains(cleanName(alert.visitCode()))) {
                        if (!"OPV 0".equalsIgnoreCase(alert.scheduleName())) {
                            alerts.add(alert);
                        } else {
                            if (alert.status() != AlertStatus.expired)
                                alerts.add(alert);
                        }
                    }
                }
                String[] dueVaccines = new String[alerts.size()];
                int x = 0;
                while (x < alerts.size()) {
                    dueVaccines[x] = alerts.get(x).scheduleName();
                    x++;
                }
                // create return object
                EligibleChild child = new EligibleChild();
                child.setID(baseEntityId);
                child.setDateOfBirth(adjustedDob);
                child.setFullName(name.trim());
                child.setFamilyName(getCursorValue(cursor, "family_name") + " Family");
                child.setDueVaccines(dueVaccines);
                child.setAlerts(alerts);
                if (dueVaccines.length > 0) {
                    eligibleChildren.add(child);
                }
            }
            return null;
        };
        return dataMap;
    }

    private static String cleanName(String name) {
        return name.toLowerCase().replace("_", "").replace(" ", "");
    }

    protected static Map<String, List<Vaccine>> fetchAllVaccines() {
        Map<String, List<Vaccine>> result = new HashMap<>();

        String sql = "select * from vaccines";
        AbstractDao.DataMap<Void> dataMap = cursor -> {
            String vaccineName = cursor.getString(cursor.getColumnIndex(VaccineRepository.NAME));
            if (vaccineName != null) {
                vaccineName = VaccineRepository.removeHyphen(vaccineName);
            }

            Date createdAt = null;
            String dateCreatedString = cursor.getString(cursor.getColumnIndex(VaccineRepository.CREATED_AT));
            if (StringUtils.isNotBlank(dateCreatedString)) {
                try {
                    createdAt = EventClientRepository.dateFormat.parse(dateCreatedString);
                } catch (ParseException e) {
                    Timber.e(e);
                }
            }
            Vaccine vaccine = new Vaccine(cursor.getLong(cursor.getColumnIndex(VaccineRepository.ID_COLUMN)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.BASE_ENTITY_ID)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.PROGRAM_CLIENT_ID)),
                    vaccineName,
                    cursor.getInt(cursor.getColumnIndex(VaccineRepository.CALCULATION)),
                    new Date(cursor.getLong(cursor.getColumnIndex(VaccineRepository.DATE))),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.ANMID)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.LOCATION_ID)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.SYNC_STATUS)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.HIA2_STATUS)),
                    cursor.getLong(cursor.getColumnIndex(VaccineRepository.UPDATED_AT_COLUMN)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.EVENT_ID)),
                    cursor.getString(cursor.getColumnIndex(VaccineRepository.FORMSUBMISSION_ID)),
                    cursor.getInt(cursor.getColumnIndex(VaccineRepository.OUT_OF_AREA)),
                    createdAt
                    // cursor.getInt(cursor.getColumnIndex(VaccineRepository.IS_VOIDED))
            );

            vaccine.setTeam(cursor.getString(cursor.getColumnIndex(VaccineRepository.TEAM)));
            vaccine.setTeamId(cursor.getString(cursor.getColumnIndex(VaccineRepository.TEAM_ID)));
            vaccine.setChildLocationId(cursor.getString(cursor.getColumnIndex(VaccineRepository.CHILD_LOCATION_ID)));

            List<Vaccine> vaccines = result.get(vaccine.getBaseEntityId());
            if (vaccines == null) vaccines = new ArrayList<>();
            vaccines.add(vaccine);
            result.put(vaccine.getBaseEntityId(), vaccines);
            return null;
        };

        readData(sql, dataMap);

        return result;
    }

    public static void setContext(Context context) {
        ReportDao.context = context;
    }

    @NonNull
    public static List<VillageDose> fetchLiveVillageDosesReport(String communityId, Date dueDate, boolean includeAll, String villageName, Map<String, String> locationMap) {
        List<EligibleChild> children = fetchLiveEligibleChildrenReport(communityId, dueDate);

        Map<String, Integer> allLocation = new TreeMap<>();

        Map<String, TreeMap<String, Integer>> resultMap = new HashMap<>();
        for (EligibleChild child : children) {
            if (child.getAlerts() == null) continue;

            for (Alert alert : child.getAlerts()) {
                TreeMap<String, Integer> vaccineMaps = resultMap.get(child.getLocationId());
                if (vaccineMaps == null) vaccineMaps = new TreeMap<>();
                String scheduleName = alert.scheduleName().replaceAll("\\d", "").trim();

                Integer count = vaccineMaps.get(scheduleName);
                count = count == null ? 1 : count + 1;
                vaccineMaps.put(scheduleName, count);

                resultMap.put(child.getLocationId(), vaccineMaps);

                // count defaults
                if (includeAll) {
                    Integer allCount = allLocation.get(alert.scheduleName());
                    allCount = allCount == null ? 1 : allCount + 1;
                    allLocation.put(alert.scheduleName(), allCount);
                }
            }
        }

        List<VillageDose> result = new ArrayList<>();
        if (includeAll) {
            VillageDose villageDose = new VillageDose();
            villageDose.setVillageName(villageName);
            villageDose.setID("");
            villageDose.setRecurringServices(allLocation);

            result.add(villageDose);
        }

        for (Map.Entry<String, TreeMap<String, Integer>> entry : resultMap.entrySet()) {
            VillageDose villageDose = new VillageDose();
            villageDose.setVillageName(locationMap.get(entry.getKey()));
            villageDose.setID(entry.getKey());
            villageDose.setRecurringServices(entry.getValue());
            result.add(villageDose);
        }

        return result;
    }

}
