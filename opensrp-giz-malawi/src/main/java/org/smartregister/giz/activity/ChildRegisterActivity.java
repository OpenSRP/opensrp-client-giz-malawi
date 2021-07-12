package org.smartregister.giz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import org.smartregister.child.activity.BaseChildRegisterActivity;
import org.smartregister.child.model.BaseChildRegisterModel;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.giz.R;
import org.smartregister.giz.contract.NavigationMenuContract;
import org.smartregister.giz.event.LoginEvent;
import org.smartregister.giz.fragment.ChildRegisterFragment;
import org.smartregister.giz.fragment.GizMeFragment;
import org.smartregister.giz.presenter.GizChildRegisterPresenter;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.util.GizUtils;
import org.smartregister.giz.view.NavDrawerActivity;
import org.smartregister.giz.view.NavigationMenu;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.lang.ref.WeakReference;

public class ChildRegisterActivity extends BaseChildRegisterActivity implements NavDrawerActivity, NavigationMenuContract {

    private NavigationMenu navigationMenu;

    @Override
    public NavigationMenu getNavigationMenu() {
        return navigationMenu;
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        // get language from prefs
        String lang = GizUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(GizUtils.setAppLocale(base, lang));
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check if from opd
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getBoolean("from_opd", false)) {
            startRegistration();
        }
    }

    @Override
    protected Fragment[] getOtherFragments() {
        ME_POSITION = 1;

        Fragment[] fragments = new Fragment[1];
        fragments[ME_POSITION - 1] = new GizMeFragment();

        return fragments;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void registerBottomNavigation() {
        //do nothing
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public String getRegistrationForm() {
        return GizConstants.JSON_FORM.CHILD_ENROLLMENT;
    }

    @Override
    public void startNFCCardScanner() {
        // Todo
    }

    @Override
    protected void initializePresenter() {
        presenter = new GizChildRegisterPresenter(this, new BaseChildRegisterModel());
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        WeakReference<ChildRegisterFragment> childRegisterFragmentWeakReference = new WeakReference<>(
                new ChildRegisterFragment());

        return childRegisterFragmentWeakReference.get();
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        createDrawer();
    }

    protected void createDrawer() {
        WeakReference<ChildRegisterActivity> childRegisterActivityWeakReference = new WeakReference<>(this);
        navigationMenu = NavigationMenu.getInstance(childRegisterActivityWeakReference.get(), null, null);
        navigationMenu.getNavigationAdapter().setSelectedView(GizConstants.DrawerMenu.CHILD_CLIENTS);
        navigationMenu.runRegisterCount();
    }

    @Override
    public void openDrawer() {
        if (navigationMenu != null) {
            navigationMenu.openDrawer();
        }
    }

    @Override
    public void closeDrawer() {
        if (navigationMenu != null) {
            navigationMenu.closeDrawer();
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void showNfcNotInstalledDialog(LoginEvent event) {
        if (event != null) {
            GizUtils.removeStickyEvent(event);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    showNfcDialog();
                }
            });
        }
    }

    private void showNfcDialog() {
        GizUtils.showDialogMessage(this, R.string.nfc_sdk_missing, R.string.please_install_nfc_sdk);
    }

    public void refresh() {
        Intent intent = new Intent(ChildRegisterActivity.this, ChildRegisterActivity.class);
        getApplicationContext()
                .startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, Utils.metadata().childFormActivity);
        if (jsonForm.has(GizConstants.KEY.ENCOUNTER_TYPE) && jsonForm.optString(GizConstants.KEY.ENCOUNTER_TYPE).equals(
                GizConstants.KEY.BIRTH_REGISTRATION)) {
            ChildJsonFormUtils.addRegistrationFormLocationHierarchyQuestions(jsonForm);
        }
        intent.putExtra(Constants.INTENT_KEY.JSON, jsonForm.toString());

        Form form = new Form();
        form.setWizard(false);
        form.setHideSaveLabel(true);
        form.setNextLabel("");

        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, ChildJsonFormUtils.REQUEST_CODE_GET_JSON);
    }


    @Override
    public void finishActivity() {
        finish();
    }
}
