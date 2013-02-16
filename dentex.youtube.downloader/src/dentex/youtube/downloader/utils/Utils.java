package dentex.youtube.downloader.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Log;
import dentex.youtube.downloader.SettingsActivity.SettingsFragment;

public class Utils extends Activity{
	
	//private static final String DEBUG_TAG = "Utils";
	
	private static int icon;

	public static void showPopUp(String title, String message, String type, Context context) {
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(message);

        if ( type == "alert" ) {
            icon = android.R.drawable.ic_dialog_alert;
        } else if ( type == "info" ) {
            icon = android.R.drawable.ic_dialog_info;
        }

        helpBuilder.setIcon(icon);
        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
            }
        });

        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }
	
	public static void showPopUpInFragment(String title, String message, String type, SettingsFragment sf) {
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(sf.getActivity());
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(message);

        if ( type == "alert" ) {
            icon = android.R.drawable.ic_dialog_alert;
        } else if ( type == "info" ) {
            icon = android.R.drawable.ic_dialog_info;
        }

        helpBuilder.setIcon(icon);
        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
            }
        });

        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }
	
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
    
	public static int getSigHash(Context context) {
		try {
			Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for (Signature sig : sigs) {
				currentHashCode = sig.hashCode();
			}
		} catch (NameNotFoundException e) {
		    Log.e("signature not found", e.getMessage());
		    currentHashCode = 0;
		}
		return currentHashCode;
	}
	
	public static String getMD5EncryptedString(String encTarget){ //TODO
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        	Log.e("Exception while encrypting to md5", e.getMessage());
            e.printStackTrace();
        }
        // Encryption algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16) ;
        return md5;
    }
    
}