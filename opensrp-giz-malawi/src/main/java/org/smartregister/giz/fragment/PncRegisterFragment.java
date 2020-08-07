package org.smartregister.giz.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.giz.R;
import org.smartregister.giz.activity.PncRegisterActivity;
import org.smartregister.giz.configuration.GizPncRegisterQueryProvider;
import org.smartregister.giz.view.NavDrawerActivity;
import org.smartregister.opd.utils.ConfigurationInstancesHelper;
import org.smartregister.pnc.PncLibrary;
import org.smartregister.pnc.fragment.BasePncRegisterFragment;
import org.smartregister.pnc.pojo.PncMetadata;
import org.smartregister.pnc.repository.PncMedicInfoRepository;
import org.smartregister.pnc.utils.PncConstants;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-11-29
 */

public class PncRegisterFragment extends BasePncRegisterFragment {

    private GizPncRegisterQueryProvider pncRegisterQueryProvider;

    public PncRegisterFragment() {
        super();
        pncRegisterQueryProvider = (GizPncRegisterQueryProvider) ConfigurationInstancesHelper.newInstance(PncLibrary.getInstance().getPncConfiguration().getPncRegisterQueryProvider());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            /*
            SwitchCompat switchSelection = view.findViewById(R.id.switch_selection);
            if (switchSelection != null) {
                switchSelection.setText(getDueOnlyText());
                switchSelection.setOnClickListener(registerActionHandler);
            }*/

            View topLeftLayout = view.findViewById(R.id.top_left_layout);
            topLeftLayout.setVisibility(View.VISIBLE);

            ImageView addPatientBtn = view.findViewById(R.id.add_child_image_view);

            if (addPatientBtn != null) {
                addPatientBtn.setOnClickListener(v -> startRegistration());
            }

            ImageView hamburgerMenu = view.findViewById(R.id.left_menu);
            if (hamburgerMenu != null) {
                hamburgerMenu.setOnClickListener(v -> {
                    if (getActivity() instanceof NavDrawerActivity) {
                        ((NavDrawerActivity) getActivity()).openDrawer();
                    }
                });
            }

            // Disable go-back on clicking the Pnc Register title
            view.findViewById(R.id.title_layout).setOnClickListener(null);
        }

        return view;
    }

    @Override
    protected void startRegistration() {
        PncRegisterActivity pncRegisterActivity = (PncRegisterActivity) getActivity();
        PncMetadata pncMetadata = PncLibrary.getInstance().getPncConfiguration().getPncMetadata();

        if (pncMetadata != null && pncRegisterActivity != null) {
            pncRegisterActivity.startFormActivity(pncMetadata.getPncRegistrationFormName()
                    , null
                    , null);
        }
    }

    @Override
    protected void performPatientAction(@NonNull CommonPersonObjectClient commonPersonObjectClient, @NonNull String formName) {
        Map<String, String> clientColumnMaps = commonPersonObjectClient.getColumnmaps();

        PncRegisterActivity pncRegisterActivity = (PncRegisterActivity) getActivity();

        if (pncRegisterActivity != null) {
            String entityTable = clientColumnMaps.get(PncConstants.IntentKey.ENTITY_TABLE);
            String currentHivStatus = clientColumnMaps.get(PncMedicInfoRepository.Property.hiv_status_current.name());

            HashMap<String, String> injectableFormValues = new HashMap<>();
            injectableFormValues.put(PncConstants.JsonFormField.MOTHER_HIV_STATUS, currentHivStatus);


            pncRegisterActivity.startFormActivityFromFormName(formName, commonPersonObjectClient.getCaseId(), null, injectableFormValues, entityTable);
        }
    }

    @Override
    protected void goToClientDetailActivity(@NonNull CommonPersonObjectClient commonPersonObjectClient) {
        final Context context = getActivity();
        PncMetadata pncMetadata = PncLibrary.getInstance().getPncConfiguration().getPncMetadata();

        if (context != null && pncMetadata != null) {
            Intent intent = new Intent(getActivity(), pncMetadata.getProfileActivity());
            intent.putExtra(PncConstants.IntentKey.CLIENT_OBJECT, commonPersonObjectClient);
            startActivity(intent);
        }
    }


    @Override
    public void countExecute() {
        try {
            int totalCount = 0;
            String sql = pncRegisterQueryProvider.getCountExecuteQuery(mainCondition, filters);
            Timber.i(sql);
            totalCount += commonRepository().countSearchIds(sql);
            clientAdapter.setTotalcount(totalCount);
            Timber.i("Total Register Count %d", clientAdapter.getTotalcount());

            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

}
