package dentex.youtube.downloader.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.SettingsActivity.SettingsFragment;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.service.DownloadsService;

public class Utils extends Activity{
	
	static final String DEBUG_TAG = "Utils";
	public static SharedPreferences settings = ShareActivity.settings;
	public final static String PREFS_NAME = ShareActivity.PREFS_NAME;

	static String onlineVersion;
    
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
				Log.d(DEBUG_TAG, "getSigHash: App signature " + currentHashCode);
			}
		} catch (NameNotFoundException e) {
		    Log.e(DEBUG_TAG, "getSigHash: App signature not found; " + e.getMessage());
		}
		return currentHashCode;
	}

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
    
    @SuppressWarnings("resource")
	public static void copyFile(File src, File dst, Context context) throws IOException {
	    FileChannel inChannel = new FileInputStream(src).getChannel();
	    FileChannel outChannel = new FileOutputStream(dst).getChannel();
	    try {
	        inChannel.transferTo(0, inChannel.size(), outChannel);
	    } finally {
	        if (inChannel != null) inChannel.close();
	        if (outChannel != null) outChannel.close();
	        
	        //if (inChannel.size() == outChannel.size() {
		        
				
	    }
	}
    
    public static void removeIdUpdateNotification(long id) {
		if (id != 0) {
			if (ShareActivity.sequence.remove(id)) {
				Log.d(Utils.DEBUG_TAG, "_ID " + id + " REMOVED from Notification");
			} else {
				Log.d(Utils.DEBUG_TAG, "_ID " + id + " Already REMOVED from Notification");
			}
		} else {
			Log.e(Utils.DEBUG_TAG, "_ID  not found!");
		}
		
		if (ShareActivity.sequence.size() > 0) {
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			Log.d(DEBUG_TAG, "Notification: no downloads in progress");
		}
	}
}