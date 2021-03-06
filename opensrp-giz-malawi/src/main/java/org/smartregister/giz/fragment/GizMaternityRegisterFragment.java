package org.smartregister.giz.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.giz.R;
import org.smartregister.giz.activity.MaternityRegisterActivity;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.configuration.GizMaternityRegisterQueryProvider;
import org.smartregister.giz.util.AppExecutors;
import org.smartregister.giz.view.NavDrawerActivity;
import org.smartregister.maternity.MaternityLibrary;
import org.smartregister.maternity.fragment.BaseMaternityRegisterFragment;
import org.smartregister.maternity.pojo.MaternityMetadata;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 31-03-2020.
 */

public class GizMaternityRegisterFragment extends BaseMaternityRegisterFragment {

    private GizMaternityRegisterQueryProvider maternityRegisterQueryProvider;

    private AppExecutors appExecutors;

    public GizMaternityRegisterFragment() {
        super();
        appExecutors = GizMalawiApplication.getInstance().getAppExecutors();
        maternityRegisterQueryProvider = ((GizMaternityRegisterQueryProvider) ConfigurationInstancesHelper.newInstance(MaternityLibrary.getInstance().getMaternityConfiguration().getMaternityRegisterQueryProvider()));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            SwitchCompat switchSelection = view.findViewById(R.id.switch_selection);
            if (switchSelection != null) {
                switchSelection.setVisibility(View.GONE);
            }

            View topLeftLayout = view.findViewById(R.id.top_left_layout);
            topLeftLayout.setVisibility(View.VISIBLE);

            ImageView addPatientBtn = view.findViewById(R.id.add_child_image_view);

            if (addPatientBtn != null) {
                addPatientBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startRegistration();
                    }
                });
            }

            ImageView hamburgerMenu = view.findViewById(R.id.left_menu);
            if (hamburgerMenu != null) {
                hamburgerMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity() instanceof NavDrawerActivity) {
                            ((NavDrawerActivity) getActivity()).openDrawer();
                        }
                    }
                });
            }

            // Disable go-back on clicking the Maternity Register title
            view.findViewById(R.id.title_layout).setOnClickListener(null);
        }

        return view;
    }

    @Override
    protected void startRegistration() {
        MaternityRegisterActivity maternityRegisterActivity = (MaternityRegisterActivity) getActivity();
        MaternityMetadata maternityMetadata = MaternityLibrary.getInstance().getMaternityConfiguration().getMaternityMetadata();

        if (maternityMetadata != null && maternityRegisterActivity != null) {
            maternityRegisterActivity.startFormActivity(maternityMetadata.getMaternityRegistrationFormName()
                    , null
                    , "");
        }
    }

    @Override
    protected void performPatientAction(@NonNull CommonPersonObjectClient commonPersonObjectClient, String formName) {
        Map<String, String> clientColumnMaps = commonPersonObjectClient.getColumnmaps();

        MaternityRegisterActivity maternityRegisterActivity = (MaternityRegisterActivity) getActivity();

        if (maternityRegisterActivity != null) {
            String entityTable = clientColumnMaps.get(MaternityConstants.IntentKey.ENTITY_TABLE);
            String currentHivStatus = clientColumnMaps.get("hiv_status_current");

            HashMap<String, String> injectableFormValues = new HashMap<>();
            injectableFormValues.put(MaternityConstants.JsonFormField.MOTHER_HIV_STATUS, currentHivStatus);


            maternityRegisterActivity.startFormActivityFromFormName(formName, commonPersonObjectClient.getCaseId(), null, injectableFormValues, entityTable);
        }
    }

    @Override
    protected void goToClientDetailActivity(@NonNull CommonPersonObjectClient commonPersonObjectClient) {
        final Context context = getActivity();
        MaternityMetadata maternityMetadata = MaternityLibrary.getInstance().getMaternityConfiguration().getMaternityMetadata();

        if (context != null && maternityMetadata != null) {
            Intent intent = new Intent(getActivity(), maternityMetadata.getProfileActivity());
            intent.putExtra(MaternityConstants.IntentKey.CLIENT_OBJECT, commonPersonObjectClient);
            startActivity(intent);
        }
    }


    @Override
    public void countExecute() {
        try {
            appExecutors.diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    String sql = maternityRegisterQueryProvider.getCountExecuteQuery(mainCondition, filters);
                    Timber.i(sql);
                    int totalCount = commonRepository().countSearchIds(sql);
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            clientAdapter.setTotalcount(totalCount);
                            Timber.i("Total Register Count %d", clientAdapter.getTotalcount());
                            clientAdapter.setCurrentlimit(20);
                            clientAdapter.setCurrentoffset(0);
                        }
                    });

                }
            });

        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
