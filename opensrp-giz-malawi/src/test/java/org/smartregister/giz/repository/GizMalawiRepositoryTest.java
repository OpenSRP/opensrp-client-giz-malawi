package org.smartregister.giz.repository;

import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.giz.BaseRobolectricTest;
import org.smartregister.giz.application.GizMalawiApplication;
import org.smartregister.giz.shadow.ShadowSQLiteDatabase;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 06-03-2020.
 */
@Config(shadows = {ShadowSQLiteDatabase.class})
public class GizMalawiRepositoryTest extends BaseRobolectricTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private GizMalawiRepository gizMalawiRepository;

    @Mock
    private SQLiteDatabase sqLiteDatabase;

    @Before
    public void setUp() throws Exception {
        gizMalawiRepository = Mockito.spy((GizMalawiRepository) GizMalawiApplication.getInstance().getRepository());

        Mockito.doReturn(sqLiteDatabase).when(gizMalawiRepository).getReadableDatabase();
        Mockito.doReturn(sqLiteDatabase).when(gizMalawiRepository).getReadableDatabase(Mockito.anyString());
        Mockito.doReturn(sqLiteDatabase).when(gizMalawiRepository).getWritableDatabase();
        Mockito.doReturn(sqLiteDatabase).when(gizMalawiRepository).getWritableDatabase(Mockito.anyString());

        ReflectionHelpers.setField(GizMalawiApplication.getInstance(), "repository", gizMalawiRepository);
    }

    @Test
    public void onCreateShouldCreate62tables() {
        Mockito.doNothing().when(gizMalawiRepository).onUpgrade(Mockito.any(SQLiteDatabase.class), Mockito.anyInt(), Mockito.anyInt());
        SQLiteDatabase database = Mockito.mock(SQLiteDatabase.class);
        Mockito.doNothing().when(gizMalawiRepository).initializeReportIndicatorState(database);
        gizMalawiRepository.onCreate(database);
        Mockito.verify(database, Mockito.times(63)).execSQL(Mockito.contains("CREATE TABLE"));
    }
}