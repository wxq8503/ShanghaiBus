package com.alan.shanghaibus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weia on 2018/1/10.
 */

public class ShanghaiBusPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static String prefName;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private List<String> lstEntries;
    private List<String> lstEntryValues;
    private static final String SEPERATOR = "SePeRaToR";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_setting_shanghaibus);
        //getPreferenceManager().setSharedPreferencesName(Config.PREFERENCE_NAME);

        EditTextPreference editTextPreference_BUS_NO = (EditTextPreference) findPreference(Config.KEY_BUS_NO);
        EditTextPreference editTextPreference_BUS_STOP_ID = (EditTextPreference) findPreference(Config.KEY_BUS_STOP_ID);
        /*
        final ListPreference lp_bus = setListPreferenceData((ListPreference) findPreference(Config.KEY_BUS_DIRECTIONS_LIST), getActivity());
        lp_bus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListPreferenceData(lp_bus, getActivity());
                return false;
            }
        });
        */
        //ListPreference listPreference_Devices = (ListPreference) findPreference(Config.KEY_USER_AGENT_LIST);
        //final ListPreference dynamicListPreference = (ListPreference) findPreference(Config.KEY_HCM_USER_LIST);
/*
        final ListPreference lp = MutableListPreference((ListPreference) findPreference(Config.KEY_HCM_USER_LIST), getContext());
        //final ListPreference lp = setListPreferenceData((ListPreference) findPreference(Config.KEY_HCM_USER_LIST), getActivity());
        lp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListPreferenceData(lp, getActivity());
                return false;
            }
        });

        String auth_code = editTextPreference_AUTH_CODE.getText();
        if("0".equals(String.valueOf(auth_code))) {
            editTextPreference_AUTH_CODE.setSummary("N/A");
        } else {
            editTextPreference_AUTH_CODE.setSummary(auth_code);
        }

        String LONGITUDE = editTextPreference_LONGITUDE.getText();
        if("0".equals(String.valueOf(LONGITUDE))) {
            editTextPreference_LONGITUDE.setSummary("N/A");
        } else {
            editTextPreference_LONGITUDE.setSummary(LONGITUDE);
        }

        String LATITUDE = editTextPreference_LATITUDE.getText();
        if("0".equals(String.valueOf(LONGITUDE))) {
            editTextPreference_LATITUDE.setSummary("N/A");
        } else {
            editTextPreference_LATITUDE.setSummary(LATITUDE);
        }
*/
        String bus_no = editTextPreference_BUS_NO.getText();
        if("0".equals(String.valueOf(bus_no))) {
            editTextPreference_BUS_NO.setSummary("69");
        } else {
            editTextPreference_BUS_NO.setSummary(bus_no);
        }

        String bus_stop_id = editTextPreference_BUS_STOP_ID.getText();
        if("0".equals(String.valueOf(bus_stop_id))) {
            editTextPreference_BUS_STOP_ID.setSummary("4");
        } else {
            editTextPreference_BUS_STOP_ID.setSummary(bus_stop_id);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public ListPreference MutableListPreference(ListPreference lp, Context context) {
        prefName = lp.getKey();
        lstEntries = new ArrayList<String>();
        lstEntryValues = new ArrayList<String>();
        mSharedPreferences = context.getSharedPreferences(prefName,0);
        mEditor = mSharedPreferences.edit();
        int count = mSharedPreferences.getInt("count", 0);
        if(count == 0){
            mEntries = lp.getEntries();
            mEntryValues = lp.getEntryValues();
            for(int i=0; i<mEntries.length; i++){
                mEditor.putString("" + i, mEntries[i] + SEPERATOR + mEntryValues[i]);
                if(Config.debug) Log.i("-----User " + i, mEntries[i] + SEPERATOR + mEntryValues[i]);
            }
            mEditor.putInt("count", mEntries.length);
            mEditor.commit();
        }else{
            String[] temp;
            mEntries = new String[count];
            mEntryValues = new String[count];
            for(int i=0; i<count; i++){
                temp = mSharedPreferences.getString(""+i, null).split(SEPERATOR);
                mEntries[i] = temp[0];
                mEntryValues[i] = temp[1];
            }
            lp.setEntries(mEntries);
            lp.setEntryValues(mEntryValues);
        }
        for(CharSequence cs: mEntries)
            lstEntries.add(cs.toString());
        for(CharSequence cs: mEntryValues)
            lstEntryValues.add(cs.toString());
        return lp;
    }

    public ListPreference addEntry(ListPreference lp, String key, String value){
        mEditor.putString(lstEntries.size()+"", key+SEPERATOR+value);
        mEditor.putInt("count", lstEntries.size()+1);
        mEditor.commit();
        lstEntries.add(key);
        lstEntryValues.add(value);
        mEntries = new CharSequence[lstEntries.size()];
        mEntryValues = new CharSequence[lstEntryValues.size()];
        for(int i=0; i< lstEntries.size(); i++){
            mEntries[i] = lstEntries.get(i);
            mEntryValues[i] = lstEntryValues.get(i);
        }
        lp.setEntries(mEntries);
        lp.setEntryValues(mEntryValues);
        return lp;
    }

    public ListPreference removeEntry(ListPreference lp, String key){
        for(int i=0; i< lstEntries.size(); i++){
            if(key.equals(lstEntries.get(i))){
                lstEntries.remove(i);
                lstEntryValues.remove(i);
                --i;
            }
        }
        mEntries = new CharSequence[lstEntries.size()];
        mEntryValues = new CharSequence[lstEntryValues.size()];
        for(int i=0; i< lstEntries.size(); i++){
            mEntries[i] = lstEntries.get(i);
            mEntryValues[i] = lstEntryValues.get(i);
        }
        lp.setEntries(mEntries);
        lp.setEntryValues(mEntryValues);
        for(int i=0; i< mEntries.length; i++){
            mEditor.putString(""+i, mEntries[i] + SEPERATOR + mEntryValues[i]);
        }
        mEditor.putInt("count", lstEntries.size());
        mEditor.commit();
        return lp;
    }

    public ListPreference updateEntry(ListPreference lp, String key, String new_value){
        for(int i=0; i< lstEntries.size(); i++){
            if(key.equals(lstEntries.get(i))){
                lstEntries.remove(i);
                lstEntryValues.remove(i);
                --i;
            }
        }
        mEntries = new CharSequence[lstEntries.size()];
        mEntryValues = new CharSequence[lstEntryValues.size()];
        for(int i=0; i< lstEntries.size(); i++){
            mEntries[i] = lstEntries.get(i);
            if(key.equals(lstEntries.get(i))){
                mEntryValues[i] = new_value;
            }else{
                mEntryValues[i] = lstEntryValues.get(i);
            }
        }
        lp.setEntries(mEntries);
        lp.setEntryValues(mEntryValues);
        for(int i=0; i< mEntries.length; i++){
            mEditor.putString(""+i, mEntries[i] + SEPERATOR + mEntryValues[i]);
        }
        mEditor.putInt("count", lstEntries.size());
        mEditor.commit();
        return lp;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key){

            case Config.KEY_BUS_ENABLE_ALL: {
                Preference connectionPref = findPreference(key);
                Preference bus_stop_id = findPreference(Config.KEY_BUS_STOP_ID);
                boolean checked = ((SwitchPreference) connectionPref).isChecked();
                if(checked) {
                    bus_stop_id.setEnabled(false);
                }else{
                    bus_stop_id.setEnabled(true);
                }
                break;
            }
            case Config.KEY_BUS_NO:
            case Config.KEY_BUS_STOP_ID:
            {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                //editor.putString(Config.KEY_AUTH_CODES, "5a069698214826e690fbe082eb0da8fd01af64670-5a069698214826e690fbe082eb0da8fd01af64671-17a7c3d1ee1bfee70c2eb9a22c8ff783365bc651");
                //editor.commit();

                Preference connectionPref = findPreference(key);
                String new_code = sharedPreferences.getString(key, "");
                connectionPref.setSummary(new_code);

                editor.commit();
                break;
            }

            case Config.KEY_USER_AGENT_LIST:
            case Config.KEY_BUS_DIRECTIONS_LIST:
            {
                // Preference connectionPref = findPreference(key);
                ListPreference listPreference = (ListPreference) findPreference(key);
                if(Config.debug) Log.i("-----" + key + " Change", listPreference.getValue());
                listPreference.setSummary(listPreference.getEntry());
                break;
            }

        }
    }
}