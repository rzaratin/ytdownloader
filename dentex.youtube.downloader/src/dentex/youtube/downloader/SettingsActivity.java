package dentex.youtube.downloader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }
        }
        
        @Override
        public void onResume(){
         super.onResume();
         // Set up a listener whenever a key changes            
         getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
       
        @Override
        public void onPause() {
         super.onPause();
         // Unregister the listener whenever a key changes            
         getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
        } 
        
        /*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("download_location")) {
                Preference d_loc_Pref = findPreference(key);
                d_loc_Pref.setSummary(sharedPreferences.getString(key, ""));
            }
            if (key.equals("user_location")) {
            	Preference u_loc_Pref = findPreference(key);
                u_loc_Pref.setSummary(sharedPreferences.getString(key, ""));
            }
        }*/
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	   updatePrefSummary(findPreference(key));
        }
        	 
        private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory)p;
        		for(int i=0;i<pCat.getPreferenceCount();i++){
        			initSummary(pCat.getPreference(i));
        	    }
        	}else{
        		updatePrefSummary(p);
        	}
        	 
        }
        
        private void updatePrefSummary(Preference p){
        	if (p instanceof ListPreference) {
        		ListPreference listPref = (ListPreference) p;
        	    p.setSummary(listPref.getEntry());
        	}
        	if (p instanceof EditTextPreference) {
        	    EditTextPreference editTextPref = (EditTextPreference) p;
        	    p.setSummary(editTextPref.getText());
        	}
        }
     }
}