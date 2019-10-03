package org.smartregister.giz.activity;

import android.support.v4.app.Fragment;

import org.smartregister.anc.library.activity.BaseHomeRegisterActivity;
import org.smartregister.anc.library.fragment.MeFragment;
import org.smartregister.giz.fragment.AncRegisterFragment;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.giz.view.NavigationMenu;
import org.smartregister.giz.view.NavDrawerActivity;
import org.smartregister.view.activity.BaseRegisterActivity;
import org.smartregister.view.fragment.BaseRegisterFragment;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-09-09
 */

public class AncRegisterActivity extends BaseHomeRegisterActivity implements NavDrawerActivity {

    private NavigationMenu navigationMenu;

    @Override
    protected void registerBottomNavigation() {
        // Do nothing because the bottom navigation was removed and overriden by someone
    }

    @Override
    public void setSelectedBottomBarMenuItem(int itemId) {
        // Do nothing here
    }

    public void createDrawer() {
        navigationMenu = NavigationMenu.getInstance(this, null, null);

        if (navigationMenu != null) {
            navigationMenu.getNavigationAdapter().setSelectedView(GizConstants.DrawerMenu.ANC_CLIENTS);
            navigationMenu.runRegisterCount();
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        createDrawer();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        int posCounter = 0;
        if (this.isMeItemEnabled()) {
            ++posCounter;
            BaseRegisterActivity.ME_POSITION = 1;
        }

        Fragment[] fragments = new Fragment[posCounter];
        if (this.isMeItemEnabled()) {
            fragments[BaseRegisterActivity.ME_POSITION - 1] = new MeFragment();
        }

        return fragments;
    }

    @Override
    public BaseRegisterFragment getRegisterFragment() {
        return new AncRegisterFragment();
    }

    @Override
    public boolean isLibraryItemEnabled() {
        return false;
    }

    @Override
    public boolean isMeItemEnabled() {
        return true;
    }

    @Override
    public boolean isAdvancedSearchEnabled() {
        return false;
    }

    public void openDrawer() {
        if (navigationMenu != null) {
            navigationMenu.openDrawer();
        }
    }

    public void closeDrawer() {
        if (navigationMenu != null) {
            navigationMenu.closeDrawer();
        }
    }

    @Override
    public void finishActivity() {
        finish();
    }
}
