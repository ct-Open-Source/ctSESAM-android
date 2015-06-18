package de.pinyto.passwordgenerator;

import android.test.ActivityInstrumentationTestCase2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Testing the management of password settings.
 */
public class PasswordSettingsManagerTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private PasswordSettingsManager settingsManager;

    public PasswordSettingsManagerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        settingsManager = new PasswordSettingsManager(getActivity().getBaseContext());
    }

    public void testSaveSetting() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        settingsManager.saveSetting(setting);
        PasswordSetting checkSetting = settingsManager.getSetting("unit.test");
        assertTrue(checkSetting.useDigits());
        assertFalse(checkSetting.useLetters());
        assertFalse(checkSetting.useExtra());
        assertEquals(4, checkSetting.getLength());
        assertEquals(4096, checkSetting.getIterations());
        settingsManager.deleteSetting(checkSetting.getDomain());
        boolean found = false;
        for (String domain : settingsManager.getDomainList()) {
            if (domain.equals("unit.test")) {
                found = true;
            }
        }
        assertFalse(found);
    }

    public void testGetBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        settingsManager.saveSetting(setting);
        byte[] password = "some secret".getBytes();
        byte[] blob = settingsManager.getExportData(password);
        Crypter crypter = new Crypter(password);
        byte[] decrypted = crypter.decrypt(blob);
        try {
            JSONArray data = new JSONArray(Packer.decompress(decrypted));
            boolean found = false;
            for (int i = 0; i < data.length(); i++) {
                JSONObject dataset = data.getJSONObject(i);
                if (dataset.getString("domain").equals("unit.test")) {
                    found = true;
                    assertTrue(dataset.getBoolean("useDigits"));
                    assertFalse(dataset.getBoolean("useUpperCase"));
                    assertFalse(dataset.getBoolean("useLowerCase"));
                    assertFalse(dataset.getBoolean("useExtra"));
                    assertEquals(4, dataset.getInt("length"));
                    assertEquals(4096, dataset.getInt("iterations"));
                }
            }
            assertTrue(found);
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testUpdateBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.saveSetting(setting);
        byte[] password = "some secret".getBytes();
        Crypter crypter = new Crypter(password);
        try {
            JSONArray data = new JSONArray(Packer.decompress(crypter.decrypt(
                    settingsManager.getExportData(password))));
            JSONObject remoteDataset = new JSONObject();
            remoteDataset.put("domain", "unit.test");
            remoteDataset.put("length", 12);
            remoteDataset.put("iterations", 4097);
            remoteDataset.put("useDigits", false);
            remoteDataset.put("useUpperCase", true);
            remoteDataset.put("useLowerCase", true);
            remoteDataset.put("useExtra", true);
            remoteDataset.put("mDate", "2012-04-13T11:45:10");
            for (int i = 0; i < data.length(); i++) {
                if (remoteDataset.getString("domain").equals(
                        data.getJSONObject(i).getString("domain"))) {
                    data.put(i, remoteDataset);
                }
            }
            byte[] remoteBlob = crypter.encrypt(Packer.compress(data.toString()));
            settingsManager.updateFromExportData(password, remoteBlob);
            PasswordSetting updated = settingsManager.getSetting("unit.test");
            assertEquals("2012-04-13T11:45:10", updated.getModificationDate());
            assertEquals("2001-01-01T02:14:12", updated.getCreationDate());
            assertEquals(4097, updated.getIterations());
            assertEquals(12, updated.getLength());
            assertFalse(updated.useDigits());
            assertTrue(updated.useLetters());
            assertTrue(updated.useExtra());
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        settingsManager.deleteSetting("unit.test");
    }

}
