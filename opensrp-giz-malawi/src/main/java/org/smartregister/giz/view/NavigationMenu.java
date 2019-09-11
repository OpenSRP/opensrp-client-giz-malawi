package org.smartregister.giz.view;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.FadingCircle;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.giz.R;
import org.smartregister.giz.adapter.NavigationAdapter;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.contract.NavigationContract;
import org.smartregister.giz.model.NavigationOption;
import org.smartregister.giz.presenter.NavigationPresenter;
import org.smartregister.p2p.activity.P2pModeSelectActivity;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.view.LocationPickerView;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class NavigationMenu implements NavigationContract.View, SyncStatusBroadcastReceiver.SyncStatusListener {
    private static NavigationMenu instance;
    private static WeakReference<Activity> activityWeakReference;
    private String TAG = NavigationMenu.class.getCanonicalName();
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationAdapter navigationAdapter;
    private RecyclerView recyclerView;
    private TextView tvLogout;
    private View rootView = null;
    private ImageView ivSync;
    private ProgressBar syncProgressBar;
    private NavigationContract.Presenter mPresenter;
    private RelativeLayout settingsLayout;

    private View parentView;
    private List<NavigationOption> navigationOptions = new ArrayList<>();

    private NavigationMenu() {

    }

    public static NavigationMenu getInstance(Activity activity, View parentView, Toolbar myToolbar) {
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(instance);
        activityWeakReference = new WeakReference<>(activity);
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (instance == null) {
                instance = new NavigationMenu();
            }
            SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(instance);
            instance.init(activity, parentView, myToolbar);
            return instance;
        } else {
            return null;
        }
    }

    private void init(Activity activity, View myParentView, Toolbar myToolbar) {
        try {
            setParentView(activity, parentView);
            toolbar = myToolbar;
            parentView = myParentView;
            mPresenter = new NavigationPresenter(this);
            prepareViews(activity);
        } catch (Exception e) {
            Timber.e(e, "NavigationMenu --> init");
        }
    }

    private void setParentView(Activity activity, View parentView) {
        if (parentView != null) {
            rootView = parentView;
        } else {
            // get current view
            ViewGroup current = (ViewGroup) ((ViewGroup) (activity.findViewById(android.R.id.content))).getChildAt(0);
            if (!(current instanceof DrawerLayout)) {
                if (current.getParent() != null) {
                    ((ViewGroup) current.getParent()).removeView(current); // <- fix
                }

                // swap content view
                LayoutInflater mInflater = LayoutInflater.from(activity);
                ViewGroup contentView = (ViewGroup) mInflater.inflate(R.layout.navigation_drawer, null);
                activity.setContentView(contentView);

                rootView = activity.findViewById(R.id.nav_view);
                RelativeLayout mainView = activity.findViewById(R.id.nav_content);

                if (current.getParent() != null) {
                    ((ViewGroup) current.getParent()).removeView(current); // <- fix
                }

                if (current instanceof RelativeLayout) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                    current.setLayoutParams(params);
                    mainView.addView(current);
                } else {
                    mainView.addView(current);
                }
            } else {
                rootView = current;
            }
        }
    }

    private void registerDrawer(Activity parentActivity) {
        if (drawer != null) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    parentActivity, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

        }
    }

    @Override
    public void prepareViews(Activity activity) {
        drawer = activity.findViewById(R.id.drawer_layout);
        recyclerView = rootView.findViewById(R.id.rvOptions);
        tvLogout = rootView.findViewById(R.id.tvLogout);
        recyclerView = rootView.findViewById(R.id.rvOptions);
        ivSync = rootView.findViewById(R.id.ivSyncIcon);
        syncProgressBar = rootView.findViewById(R.id.pbSync);
        settingsLayout = rootView.findViewById(R.id.rlSettings);

        ImageView ivLogo = rootView.findViewById(R.id.ivLogo);
        LocationPickerView locationPickerView = rootView.findViewById(R.id.clinic_selection);
        locationPickerView.init();
        ivLogo.setContentDescription(activity.getString(R.string.nav_logo));
        ivLogo.setImageResource(R.drawable.ic_logo);

        TextView tvLogo = rootView.findViewById(R.id.tvLogo);
        tvLogo.setText(activity.getString(R.string.nav_logo));

        if (syncProgressBar != null) {
            FadingCircle circle = new FadingCircle();
            syncProgressBar.setIndeterminateDrawable(circle);

            if (toolbar != null) {
                toolbar.setNavigationIcon(circle);
            }
        }

        // register all objects
        registerDrawer(activity);
        registerNavigation(activity);
        registerLogout(activity);
        registerSync(activity);
        registerLanguageSwitcher(activity);

        registerDeviceToDeviceSync(activity);
        registerSettings(activity);

        // update all actions
        mPresenter.refreshLastSync();
    }

    private void registerSettings(@NonNull final Activity activity) {
        if (settingsLayout != null) {
            settingsLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activity instanceof BaseRegisterActivity) {
                        ((BaseRegisterActivity) activity).switchToFragment(BaseRegisterActivity.ME_POSITION);
                        closeDrawer();
                    } else {
                        Timber.e(new Exception("Cannot open Settings since this activity is not a child of BaseRegisterActivity"));
                    }
                }
            });
        }
    }

    private void registerNavigation(Activity parentActivity) {
        if (recyclerView != null) {
            navigationOptions = mPresenter.getOptions();
            if (navigationAdapter == null) {
                navigationAdapter = new NavigationAdapter(navigationOptions, parentActivity);
            }

            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(parentActivity);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(navigationAdapter);
        }
    }

    private void registerLogout(final Activity parentActivity) {
        mPresenter.displayCurrentUser();
        tvLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout(parentActivity);
            }
        });
    }

    private void registerSync(final Activity parentActivity) {

        TextView tvSync = rootView.findViewById(R.id.tvSync);
        ivSync = rootView.findViewById(R.id.ivSyncIcon);
        syncProgressBar = rootView.findViewById(R.id.pbSync);

        View.OnClickListener syncClicker = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(parentActivity, parentActivity.getResources().getText(R.string.action_start_sync),
                        Toast.LENGTH_SHORT).show();
                mPresenter.sync(parentActivity);
            }
        };


        tvSync.setOnClickListener(syncClicker);
        ivSync.setOnClickListener(syncClicker);

        refreshSyncProgressSpinner();
    }

    private void registerLanguageSwitcher(final Activity context) {
        final TextView tvLang = rootView.findViewById(R.id.tvLang);
        Locale current = context.getResources().getConfiguration().locale;
        tvLang.setText(StringUtils.capitalize(current.getDisplayLanguage()));
    }

    private void registerDeviceToDeviceSync(@NonNull final Activity activity) {
        rootView.findViewById(R.id.rlIconDevice)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startP2PActivity(activity);
                    }
                });
    }

    protected void refreshSyncProgressSpinner() {
        if (SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
            syncProgressBar.setVisibility(View.VISIBLE);
            ivSync.setVisibility(View.INVISIBLE);
        } else {
            syncProgressBar.setVisibility(View.INVISIBLE);
            ivSync.setVisibility(View.VISIBLE);
        }
    }

    public void startP2PActivity(@NonNull Activity activity) {
        activity.startActivity(new Intent(activity, P2pModeSelectActivity.class));
    }

    @Override
    public void refreshLastSync(Date lastSync) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa, MMM d", Locale.getDefault());
        if (rootView != null) {
            TextView tvLastSyncTime = rootView.findViewById(R.id.tvSyncTime);
            if (lastSync != null) {
                tvLastSyncTime.setVisibility(View.VISIBLE);
                tvLastSyncTime.setText(MessageFormat.format(" {0}", sdf.format(lastSync)));
            } else {
                tvLastSyncTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void refreshCurrentUser(String name) {
        if (tvLogout != null && activityWeakReference.get() != null) {
            tvLogout.setText(
                    String.format("%s %s", activityWeakReference.get().getResources().getString(R.string.log_out_as), name));
        }
    }

    @Override
    public void logout(Activity activity) {
        Toast.makeText(activity.getApplicationContext(), activity.getResources().getText(R.string.action_log_out),
                Toast.LENGTH_SHORT).show();
        GizMalawiApplication.getInstance().logoutCurrentUser();
    }

    @Override
    public void refreshCount() {
        navigationAdapter.notifyDataSetChanged();
    }

    public NavigationAdapter getNavigationAdapter() {
        return navigationAdapter;
    }

    public void runRegisterCount() {
        mPresenter.refreshNavigationCount();
    }

    @Override
    public void onSyncStart() {
        // set the sync icon to be a rotating menu
        refreshSyncProgressSpinner();
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        Log.v(TAG, "onSyncInProgress");
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        // hide the rotating menu
        refreshSyncProgressSpinner();
        // update the time
        mPresenter.refreshLastSync();
        // refreshLastSync(new Date());
        mPresenter.refreshNavigationCount();
    }

    public void openDrawer() {
        drawer.openDrawer(GravityCompat.START);
    }

    public DrawerLayout getDrawer() {
        return drawer;
    }

    public static void closeDrawer() {
        if (instance != null && instance.getDrawer() != null) {
            instance.getDrawer().closeDrawer(Gravity.START);
        }
    }
}
