package com.lemontruck.thermo.helpers;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.lemontruck.thermo.ConfigFragment;
import com.lemontruck.thermo.R;
import com.lemontruck.thermo.StatisticFragment;

public class AppSectionsPagerAdapter extends FragmentPagerAdapter {
	private final static String LOG = "com.lemontruck.thermo";
	private Resources res;
	
    public AppSectionsPagerAdapter(FragmentManager fm, Resources res) {
        super(fm);
        this.res = res;
    }

    @Override
    public Fragment getItem(int i) {
    	Fragment fragment = null;
    	Bundle args = null;
    	
        switch (i) {
            case 0:
            	fragment = new ConfigFragment();
                args = new Bundle();
                args.putInt(ConfigFragment.ARG_SECTION_NUMBER, i + 1);
                fragment.setArguments(args);
                return fragment;
            case 1:
                fragment = new StatisticFragment();
                args = new Bundle();
                args.putInt(StatisticFragment.ARG_SECTION_NUMBER, i + 1);
                fragment.setArguments(args);
                return fragment;
            default:
                Log.e(LOG, "Unkown fragement item");
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
    	switch (position) {
    		case 0:
    			return res.getString(R.string.label_tab_settings);
    		case 1:
    			return res.getString(R.string.label_tab_statistics);
    		default:
    			return "Unkown Tab";
    	}
    }
}
