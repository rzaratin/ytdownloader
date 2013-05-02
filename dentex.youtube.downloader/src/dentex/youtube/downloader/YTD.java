package dentex.youtube.downloader;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class YTD extends Application {
	
	static String DEBUG_TAG = "YTD";
	public static String BAK = "3bee3036";
	public SharedPreferences settings;
	public final String PREFS_NAME = "dentex.youtube.downloader_preferences";
	
	@Override
    public void onCreate() {
		Log.d(DEBUG_TAG, "onCreate");
		settings = getSharedPreferences(PREFS_NAME, 0);
		
		if (!settings.getBoolean("disable_bugsense", false)) {
        	//BugSenseHandler.initAndStartSession(getApplicationContext(), BAK);
		}
        	
        super.onCreate();
	}
}