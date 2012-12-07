package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingsActivity extends Activity {
	
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary = "Long-press a folder to select location.";
    
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
    	
    	private static final String DEBUG_TAG = "ShareActivity";
    	private CheckBoxPreference standardLoc;
		private CheckBoxPreference chooserLoc;
		private Preference filechooser;
		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }

            filechooser = (Preference) getPreferenceScreen().findPreference("open_chooser");
            filechooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  FileChooserActivity.class);
            		// by default, if not specified, default rootpath is sdcard, if sdcard is not available, "/" will be used
            		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile("/storage/sdcard0"));
            		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
            		intent.putExtra(FileChooserActivity._Theme, android.R.style.Theme_Dialog);
            		startActivityForResult(intent, _ReqChooseFile);
                    return true;
                }
            });
            
            standardLoc = (CheckBoxPreference) findPreference("enable_standard_location");
            standardLoc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    unCheckAll();
                    standardLoc.setChecked(true);
                    return true;
                }
            });
            
            chooserLoc = (CheckBoxPreference) findPreference("enable_chooser_location");
            chooserLoc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    unCheckAll();
                    chooserLoc.setChecked(true);
                    return true;
                }
            });
        }
        
        private void unCheckAll() {
        	standardLoc.setChecked(false);
        	chooserLoc.setChecked(false);
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
        	if (p instanceof PreferenceScreen) {
        		//PreferenceScreen pref = (PreferenceScreen) p;
        		p.setSummary(chooserSummary);
        	}
        }

        @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
            case _ReqChooseFile:
                if (resultCode == RESULT_OK) {
                    @SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
                    for (File f : files) {
                    	
                    	chooserSummary = f.toString();
                    	Log.d(DEBUG_TAG, "file-chooser selection: " + chooserSummary);
                    	for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                            initSummary(getPreferenceScreen().getPreference(i));
                        }
                    }
                }
                break;
            }
        }

     }
}