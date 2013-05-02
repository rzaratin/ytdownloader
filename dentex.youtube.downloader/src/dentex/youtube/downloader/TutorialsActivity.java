package dentex.youtube.downloader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class TutorialsActivity extends Activity {
	
	public static final String DEBUG_TAG = "TutorialsActivity";
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_tutorials);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
    	Utils.langInit(this);
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.tutorials, false);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new TutorialsFragment())
                .commit();
        setupActionBar();
	}
	
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    public static class TutorialsFragment extends PreferenceFragment /*implements OnSharedPreferenceChangeListener */{
    	
    	private Preference quickStart;
    	private Preference audioTutorial;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.tutorials);
            
            /*for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }*/
            
            quickStart = (Preference) getPreferenceScreen().findPreference("quick_start");
            quickStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					PopUps.showPopUp(getString(R.string.quick_start_title), getString(R.string.quick_start_text), "info", getActivity());
					return true;
				}
			});
            
            audioTutorial = (Preference) getPreferenceScreen().findPreference("audio_tutorial");
            audioTutorial.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					PopUps.showPopUp(getString(R.string.audio_tutorial_title), getString(R.string.audio_tutorial_text), "info", getActivity());
					return true;
				}
			});
    	}

	    /*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
	    }*/
    }
}
