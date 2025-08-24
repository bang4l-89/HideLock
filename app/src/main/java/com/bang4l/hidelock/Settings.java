package com.bang4l.hidelock;


import java.io.File;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import de.robv.android.xposed.XposedBridge;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class Settings extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = true;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
        //makePreferencesReadable();
    }

    private void makePreferencesReadable() {
        try {
            String prefsName = getPreferenceManager().getSharedPreferencesName();
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, prefsName + ".xml");

            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false); // Readable for all
                XposedBridge.log("Settings Made preferences file readable: " + prefsFile.getAbsolutePath());
            }
        } catch (Exception e) {
            XposedBridge.log("Settings Error making prefs readable: " + e.getMessage());
        }
    }
    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    @SuppressWarnings("deprecation")
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        // Make them world readable so the xposed module can reload them at runtime
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
        addPreferencesFromResource(R.xml.pref_general);

        Preference supportPref = (Preference) findPreference("support");
        supportPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=2587192")));
                return false;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            ListPreference typePref = (ListPreference) findPreference("lockscreentype");
            typePref.setEntries(R.array.lockscreenTypePreJellyBean);
            typePref.setEntryValues(R.array.lockscreenTypeValuesPreJellyBean);
            typePref.setDefaultValue("none");
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            ListPreference typePref = (ListPreference) findPreference("lockscreentype");
            typePref.setEntries(R.array.lockscreenTypePreLollipop);
            typePref.setEntryValues(R.array.lockscreenTypeValuesPreLollipop);
            typePref.setDefaultValue("none");
        } else
        {
            // If set to 'SMART', we don't support the lockOnBoot option (it's always true), or timeout, so set
            // it as such and disable things while on SMART
            final CheckBoxPreference lockOnBoot = (CheckBoxPreference)findPreference("lockscreenonboot");
            final Preference lockTimeout = findPreference("lockscreentimeout");
            if (Lockscreen.LOCK_SCREEN_TYPE_SMART.equals(((ListPreference)findPreference("lockscreentype")).getValue()))
            {
                lockOnBoot.setChecked(true);
                lockOnBoot.setEnabled(false);
                lockTimeout.setEnabled(false);
            } else
            {
                lockOnBoot.setEnabled(true);
                lockTimeout.setEnabled(true);
            }

            findPreference("lockscreentype").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Lockscreen.LOCK_SCREEN_TYPE_SMART.equals((String)newValue))
                    {
                        lockOnBoot.setChecked(true);
                        lockOnBoot.setEnabled(false);
                        lockTimeout.setEnabled(false);
                    } else
                    {
                        lockOnBoot.setEnabled(true);
                        lockTimeout.setEnabled(true);
                    }

                    return true;
                }
            });
        }
    }


    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    @SuppressLint("InlinedApi")
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    @SuppressWarnings("unused")
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Make them world readable so the xposed module can reload them at runtime
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            addPreferencesFromResource(R.xml.pref_general);

            Preference supportPref = (Preference) findPreference("support");
            supportPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=2587192")));
                    return false;
                }
            });

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                ListPreference typePref = (ListPreference) findPreference("lockscreentype");
                typePref.setEntries(R.array.lockscreenTypePreJellyBean);
                typePref.setEntryValues(R.array.lockscreenTypeValuesPreJellyBean);
                typePref.setDefaultValue("none");
            }
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            {
                ListPreference typePref = (ListPreference) findPreference("lockscreentype");
                typePref.setEntries(R.array.lockscreenTypePreLollipop);
                typePref.setEntryValues(R.array.lockscreenTypeValuesPreLollipop);
                typePref.setDefaultValue("none");
            } else
            {
                // If set to 'SMART', we don't support the lockOnBoot option (it's always true), or timeout, so set
                // it as such and disable things while on SMART
                final CheckBoxPreference lockOnBoot = (CheckBoxPreference)findPreference("lockscreenonboot");
                final Preference lockTimeout = findPreference("lockscreentimeout");
                if (Lockscreen.LOCK_SCREEN_TYPE_SMART.equals(((ListPreference)findPreference("lockscreentype")).getValue()))
                {
                    lockOnBoot.setChecked(true);
                    lockOnBoot.setEnabled(false);
                    lockTimeout.setEnabled(false);
                } else
                {
                    lockOnBoot.setEnabled(true);
                    lockTimeout.setEnabled(true);
                }

                findPreference("lockscreentype").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (Lockscreen.LOCK_SCREEN_TYPE_SMART.equals((String)newValue))
                        {
                            lockOnBoot.setChecked(true);
                            lockOnBoot.setEnabled(false);
                            lockTimeout.setEnabled(false);
                        } else
                        {
                            lockOnBoot.setEnabled(true);
                            lockTimeout.setEnabled(true);
                        }

                        return true;
                    }
                });
            }
        }
    }
}

