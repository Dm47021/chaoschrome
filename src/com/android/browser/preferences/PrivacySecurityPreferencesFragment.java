/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.browser.preferences;

import com.android.browser.PreferenceKeys;
import com.android.browser.R;
import com.android.browser.mdm.DoNotTrackRestriction;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;

import org.codeaurora.swe.PermissionsServiceFactory;
import org.codeaurora.swe.WebRefiner;

public class PrivacySecurityPreferencesFragment extends SWEPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_and_security_preferences);

        PreferenceScreen websiteSettings = (PreferenceScreen) findPreference(
                PreferenceKeys.PREF_WEBSITE_SETTINGS);
        websiteSettings.setFragment(WebsiteSettingsFragment.class.getName());
        websiteSettings.setOnPreferenceClickListener(this);

        Preference e = findPreference(PreferenceKeys.PREF_CLEAR_SELECTED_DATA);
        e.setOnPreferenceChangeListener(this);

        readAndShowPermission("enable_geolocation",
                PermissionsServiceFactory.PermissionType.GEOLOCATION);

        readAndShowPermission("microphone", PermissionsServiceFactory.PermissionType.VOICE);

        readAndShowPermission("camera", PermissionsServiceFactory.PermissionType.VIDEO);

        // since webrefiner and distracting_contents are paradoxes
        // the value needs to be flipped
        Preference pref = findPreference("distracting_contents");
        pref.setOnPreferenceChangeListener(this);
        showPermission(pref,
            !PermissionsServiceFactory.getDefaultPermissions(
                PermissionsServiceFactory.PermissionType.WEBREFINER));

        readAndShowPermission("popup_windows", PermissionsServiceFactory.PermissionType.POPUP);

        readAndShowPermission("accept_cookies", PermissionsServiceFactory.PermissionType.COOKIE);

        readAndShowPermission("accept_third_cookies",
                PermissionsServiceFactory.PermissionType.THIRDPARTYCOOKIES);

        // Register Preference objects with their MDM restriction handlers
        DoNotTrackRestriction.getInstance().
                registerPreference(findPreference(PreferenceKeys.PREF_DO_NOT_TRACK));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Un-register Preference objects from their MDM restriction handlers
        DoNotTrackRestriction.getInstance().registerPreference(null);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        FragmentManager fragmentManager = getFragmentManager();

        if (preference.getKey().equals(PreferenceKeys.PREF_WEBSITE_SETTINGS)) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment newFragment = new WebsiteSettingsFragment();
            fragmentTransaction.replace(getId(), newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        boolean flag = (boolean) objValue;
        if (pref.getKey().equals(PreferenceKeys.PREF_CLEAR_SELECTED_DATA)) {
            if (pref.getPreferenceManager().getDefaultSharedPreferences(
                    (Context) getActivity()).getBoolean(
                    PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY, false)) {
                // Need to tell the browser to remove the parent/child relationship
                // between tabs
                getActivity().setResult(Activity.RESULT_OK,
                        (new Intent()).putExtra(Intent.EXTRA_TEXT, pref.getKey()));
                return true;
            }
        }

        if (pref.getKey().toString().equalsIgnoreCase("enable_geolocation")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.GEOLOCATION, flag);
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("microphone")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.VOICE, flag);
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("camera")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.VIDEO, flag);
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("distracting_contents")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.WEBREFINER, !flag);
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("popup_windows")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.POPUP, flag);
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("accept_cookies")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.COOKIE, flag);

            if (!flag) {
                // Disable third party cookies as well
                PermissionsServiceFactory.setDefaultPermissions(
                        PermissionsServiceFactory.PermissionType.THIRDPARTYCOOKIES, flag);
                showPermission(findPreference("accept_third_cookies"), flag);
            }
            return true;
        }

        if (pref.getKey().toString().equalsIgnoreCase("accept_third_cookies")) {
            PermissionsServiceFactory.setDefaultPermissions(
                    PermissionsServiceFactory.PermissionType.THIRDPARTYCOOKIES, flag);
            return true;
        }

        return false;
    }

    private void readAndShowPermission(CharSequence key,
                                       PermissionsServiceFactory.PermissionType type) {
        Preference pref = findPreference(key);
        pref.setOnPreferenceChangeListener(this);
        showPermission(pref, PermissionsServiceFactory.getDefaultPermissions(type));
    }

    private void showPermission(Preference pref, boolean perm) {
        if (pref instanceof TwoStatePreference) {
            TwoStatePreference twoStatePreference = (TwoStatePreference) pref;
            if (twoStatePreference.isChecked() != perm) {
                twoStatePreference.setChecked(perm);
            }
        } else {
            if (!perm) {
                pref.setSummary(R.string.pref_security_not_allowed);
            } else {
                pref.setSummary(R.string.pref_security_ask_before_using);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pref_privacy_security_title);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setHomeButtonEnabled(false);
        }
    }
}
