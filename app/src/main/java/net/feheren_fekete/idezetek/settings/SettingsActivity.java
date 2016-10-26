package net.feheren_fekete.idezetek.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import net.feheren_fekete.idezetek.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
