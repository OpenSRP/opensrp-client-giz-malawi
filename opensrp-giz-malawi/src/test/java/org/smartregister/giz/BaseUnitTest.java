package org.smartregister.giz;

import android.os.Build;

import androidx.annotation.NonNull;

import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.Context;
import org.smartregister.giz.shadow.ShadowAssetHandler;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.service.AlertService;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(RobolectricTestRunner.class)
@Config(application = TestGizMalawiApplication.class, sdk = Build.VERSION_CODES.O_MR1, shadows = {ShadowAssetHandler.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public abstract class BaseUnitTest {

    protected static final String DUMMY_USERNAME = "myusername";
    protected static final String DUMMY_PASSWORD = "mypassword";

    public void mockImmunizationLibrary(@NonNull ImmunizationLibrary immunizationLibrary, @NonNull Context context, @NonNull VaccineRepository vaccineRepository, @NonNull AlertService alertService) {
        PowerMockito.mockStatic(ImmunizationLibrary.class);
        PowerMockito.when(ImmunizationLibrary.getInstance()).thenReturn(immunizationLibrary);
        PowerMockito.when(ImmunizationLibrary.getInstance().context()).thenReturn(context);
        PowerMockito.when(ImmunizationLibrary.getInstance().vaccineRepository()).thenReturn(vaccineRepository);
        PowerMockito.when(ImmunizationLibrary.getInstance().vaccineRepository().findByEntityId(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        PowerMockito.when(ImmunizationLibrary.getInstance().context().alertService()).thenReturn(alertService);
    }
}
