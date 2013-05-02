package dentex.youtube.downloader.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.SettingsActivity.SettingsFragment;
import dentex.youtube.downloader.ShareActivity;

public class Utils {
	
	static final String DEBUG_TAG = "Utils";
	static SharedPreferences settings = ShareActivity.settings;
	static final String PREFS_NAME = ShareActivity.PREFS_NAME;
	InputStream isFromString;
	static MediaScannerConnection msc;
	static String onlineVersion;
    
	/* class VersionComparator from Stack Overflow:
	 * 
	 * http://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
	 * 
	 * Q: http://stackoverflow.com/users/1288/bill-the-lizard
	 * A: http://stackoverflow.com/users/57695/peter-lawrey
	 */
	
    public static class VersionComparator {

        public static String compare(String v1, String v2) {
            String s1 = normalisedVersion(v1);
            String s2 = normalisedVersion(v2);
            int cmp = s1.compareTo(s2);
            String cmpStr = cmp < 0 ? "<" : cmp > 0 ? ">" : "==";
            return cmpStr;
        }

        public static String normalisedVersion(String version) {
            return normalisedVersion(version, ".", 4);
        }

        public static String normalisedVersion(String version, String sep, int maxWidth) {
            String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
            StringBuilder sb = new StringBuilder();
            for (String s : split) {
                sb.append(String.format("%" + maxWidth + 's', s));
            }
            return sb.toString();
        }
    }

	public static int currentHashCode;
	
	public static int getSigHash(SettingsFragment sf) {

		try {
			Signature[] sigs = sf.getActivity().getPackageManager().getPackageInfo(sf.getActivity().getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for (Signature sig : sigs) {
				currentHashCode = sig.hashCode();
				logger("d", "getSigHash: App signature " + currentHashCode, DEBUG_TAG);
			}
		} catch (NameNotFoundException e) {
		    Log.e(DEBUG_TAG, "getSigHash: App signature not found; " + e.getMessage());
		}
		return currentHashCode;
	}

	/*
	 * checkMD5(String md5, File file)
	 * -------------------------------
	 * 
	 * Copyright (C) 2012 The CyanogenMod Project
	 *
	 * * Licensed under the GNU GPLv2 license
	 *
	 * The text of the license can be found in the LICENSE_GPL file
	 * or at https://www.gnu.org/licenses/gpl-2.0.txt
	 */
	
	public static boolean checkMD5(String md5, File file) {
        if (md5 == null || md5.equals("") || file == null) {
            Log.e(DEBUG_TAG, "MD5 String NULL or File NULL");
            return false;
        }

        String calculatedDigest = calculateMD5(file);
        if (calculatedDigest == null) {
            Log.e(DEBUG_TAG, "calculatedDigest NULL");
            return false;
        }

        Log.i(DEBUG_TAG, "Calculated digest: " + calculatedDigest);
        Log.i(DEBUG_TAG, "Provided digest: " + md5);

        return calculatedDigest.equalsIgnoreCase(md5);
    }

	/*
	 * calculateMD5(File file)
	 * -----------------------
	 * 
	 * Copyright (C) 2012 The CyanogenMod Project
	 *
	 * * Licensed under the GNU GPLv2 license
	 *
	 * The text of the license can be found in the LICENSE_GPL file
	 * or at https://www.gnu.org/licenses/gpl-2.0.txt
	 */
	
    public static String calculateMD5(File file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(DEBUG_TAG, "Exception while getting Digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(DEBUG_TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }
    
    /* method copyFile(File src, File dst, Context context) adapted from Stack Overflow:
	 * 
	 * http://stackoverflow.com/questions/4770004/how-to-move-rename-file-from-internal-app-storage-to-external-storage-on-android
	 * 
	 * Q: http://stackoverflow.com/users/131871/codefusionmobile
	 * A: http://stackoverflow.com/users/472270/barmaley
	 */
    
    @SuppressWarnings("resource")
	public static void copyFile(File src, File dst) throws IOException {
	    FileChannel inChannel = new FileInputStream(src).getChannel();
	    FileChannel outChannel = new FileOutputStream(dst).getChannel();
	    //if (!dst.exists()) {
		    try {
		        inChannel.transferTo(0, inChannel.size(), outChannel);
		    } finally {
		        if (inChannel != null) inChannel.close();
		        if (outChannel != null) outChannel.close();
		    }
	    /*} else {
	    	logger("w", "copyFile: destination already exists", DEBUG_TAG);
	    }*/
	}
    
    /*
     * getCpuInfo() from:
     *   http://www.roman10.net/how-to-get-cpu-information-on-android/
     * by:
     *   Liu Feipeng 
     */
    
    public static String getCpuInfo() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("abi: ").append(Build.CPU_ABI).append("\n");
    	if (new File("/proc/cpuinfo").exists()) {
        	try {
        		BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
	        	String aLine;
				while ((aLine = br.readLine()) != null) {
					sb.append(aLine + "\n");
				}
				if (br != null) {
		    		br.close();
		    	}
			} catch (IOException e) {
				e.printStackTrace();
			} 
        }
    	return sb.toString();
    }
    
    /* method mediaScan adapted from Stack Overflow:
	 * 
	 * http://stackoverflow.com/questions/9707572/android-how-to-get-and-setchange-id3-tagmetadata-of-audio-files/11035755#11035755
	 * 
	 * Q: http://stackoverflow.com/users/849664/chirag-shah
	 * A: http://stackoverflow.com/users/1456506/shtrule
	 */
    
    public static void scanMedia(Context context, final File[] file, final String[] mime) {
		msc = new MediaScannerConnection(context, new MediaScannerConnectionClient() {
			public void onScanCompleted(String path, Uri uri) {
				Utils.logger("d", "Scanned " + path + ":", DEBUG_TAG);
				Utils.logger("d", "-> uri: " + uri, DEBUG_TAG);
				msc.disconnect();  
			}
			public void onMediaScannerConnected() {
				for (int i = 0; i < file.length; i++) {
					msc.scanFile(file[i].getAbsolutePath(), mime[i]);
				}
			}
		});
		msc.connect();
	}
    
    // --------------- !!!
    
    public static void themeInit(Context context) {
    	settings = context.getSharedPreferences(PREFS_NAME, 0);
		String theme = settings.getString("choose_theme", "D");
    	if (theme.equals("D")) {
    		context.setTheme(R.style.AppThemeDark);
    	} else {
    		context.setTheme(R.style.AppThemeLight);
    	}
	}
    
    public static void langInit(Context context) {
		String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        context.getResources().updateConfiguration(config, null);
        }
	}
    
    public static void logger(String type, String msg, String tag) {
    	if (settings.getBoolean("enable_logging", false)) {
	    	if (type.equals("v")) {
	    		Log.v(tag, msg);
	    	} else if (type.equals("d")) {
	    		Log.d(tag, msg);
	    	} else if (type.equals("i")) {
	    		Log.i(tag, msg);
	    	} else if (type.equals("w")) {
	    		Log.w(tag, msg);
	    	}
    	}
    }
    
    public static void createLogFile(File destDir, String filename, String content) {
    	File file = new File(destDir, filename);
    	try {
	        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
	        OutputStream os = new FileOutputStream(file);
	        byte[] data = new byte[is.available()];
	        is.read(data);
	        os.write(data);
	        is.close();
	        os.close();
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Error creating '" + filename + "' Log file", e);
		}
	}
    
    public static void setNotificationDefaults(NotificationCompat.Builder aBuilder) {
    	String def = settings.getString("notification_defaults", "0");
    	if (def.equals("0")) {
    		aBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
    	}
    	if (def.equals("1")) {
    		aBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
    	}
    	if (def.equals("2")) {
    		aBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
    	}
    	if (def.equals("3")) {
    		aBuilder.setDefaults(Notification.DEFAULT_ALL);
    	}
    	if (def.equals("4")) {
    		aBuilder.setDefaults(Notification.DEFAULT_SOUND);
    	}
    	if (def.equals("5")) { 
    		aBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
    	}
    	if (def.equals("6")) {
    		aBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
    	}
    	if (def.equals("7")) {
    		// nothing...
    	}
    }
}