package dentex.youtube.downloader;

import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import dentex.youtube.downloader.docs.ChangelogActivity;
import dentex.youtube.downloader.docs.CreditsShowActivity;
import dentex.youtube.downloader.docs.GplShowActivity;
import dentex.youtube.downloader.docs.MitShowActivity;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class AboutActivity extends Activity {
	
	public static final String DEBUG_TAG = "AboutActivity";
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_about);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.about, false);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new AboutFragment())
                .commit();
    }
    
    public static class AboutFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
    	private Preference gpl;
		private Preference mit;
		private Preference credits;
		private Preference git;
		private Preference hg;
		private Preference gc;
		private Preference share;
		private Preference cl;
		private Preference loc;
		private Preference tw;
		
		@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.about);
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }
		
			gpl = (Preference) findPreference("gpl");
	        gpl.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	Intent intent = new Intent(getActivity(),  GplShowActivity.class);
	        		startActivity(intent);
	                return true;
	            }
	        });
	        
	        mit = (Preference) findPreference("mit");
	        mit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	Intent intent = new Intent(getActivity(),  MitShowActivity.class);
	        		startActivity(intent);
	                return true;
	            }
	        });
	        
	        credits = (Preference) findPreference("credits");
	        credits.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	Intent intent = new Intent(getActivity(),  CreditsShowActivity.class);
	        		startActivity(intent);
	                return true;
	            }
	        });
	        
	        git = (Preference) findPreference("ytd_code_git");
	        git.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	String url = "https://github.com/dentex/ytdownloader/";
	            	Intent i = new Intent(Intent.ACTION_VIEW);
	            	i.setData(Uri.parse(url));
	            	startActivity(i);
	            	return true;
	            }
	        });
	        
	        hg = (Preference) findPreference("ytd_code_hg");
	        hg.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	String url = "https://sourceforge.net/projects/ytdownloader/";
	            	Intent i = new Intent(Intent.ACTION_VIEW);
	            	i.setData(Uri.parse(url));
	            	startActivity(i);
	            	return true;
	            }
	        });
	        
	        gc = (Preference) findPreference("chooser_code");
	        gc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	            	String url = "https://code.google.com/p/android-filechooser/";
	            	Intent i = new Intent(Intent.ACTION_VIEW);
	            	i.setData(Uri.parse(url));
	            	startActivity(i);
	            	return true;
	            }
	        });
	        
	        share = (Preference) findPreference("share");
	        share.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        	
	            public boolean onPreferenceClick(Preference preference) {
	                try {
	                	Intent shareIntent =   
	                	new Intent(android.content.Intent.ACTION_SEND);   
	                	shareIntent.setType("text/plain");  
	                	shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "YouTube Downloader");
	                	String shareMessage = getString(R.string.share_message);
	                	shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
	                	startActivity(Intent.createChooser(shareIntent, "Share this YTD"));
	                } catch (final ActivityNotFoundException e) {
	                	Utils.logger("d", "No suitable Apps found.", DEBUG_TAG);
	                	PopUps.showPopUp(getString(R.string.attention), getString(R.string.share_warning), "alert", AboutFragment.this.getActivity());
	                }
	            	return true;
	            }
	        });
	        
	        loc = (Preference) findPreference("help_translate");
            loc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	String url = "http://www.getlocalization.com/ytdownloader/";
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse(url));
                	startActivity(i);
                	return true;
                }
            });
            
            cl = (Preference) findPreference("changelog");
            try {
				cl.setSummary("v" + AboutFragment.this.getActivity().getPackageManager().getPackageInfo(AboutFragment.this.getActivity().getPackageName(), 0).versionName);
			} catch (NameNotFoundException e1) {
				Log.e(DEBUG_TAG, "version not read: " + e1.getMessage());
				cl.setSummary("");
			}
            cl.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  ChangelogActivity.class);
                	startActivity(intent);
                    return true;
                }
            });
            
            tw = (Preference) findPreference("tweet");
            tw.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	
                	/*
                	 * http://www.androidsnippets.com/open-twitter-via-intent
                	 * http://www.androidsnippets.com/users/hyperax
                	 */
                	try {
                		Utils.logger("d", "twitter direct link", DEBUG_TAG);
                		startActivity(new Intent(Intent.ACTION_VIEW, 
                				Uri.parse("twitter://user?screen_name=@twidentex")));
                	}catch (Exception e) {
                		Utils.logger("d", "twitter WEB link", DEBUG_TAG);
                		startActivity(new Intent(Intent.ACTION_VIEW, 
                				Uri.parse("https://twitter.com/#!/@twidentex")));
                	}
                    return true;
                }
            });
            
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
        }
    }
}
