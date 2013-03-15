package dentex.youtube.downloader.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.SettingsActivity.SettingsFragment;

public class PopUps {
	
	static int icon;

	public static void showPopUp(String title, String message, String type, Context context) {
	    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.BoxTheme));
	    helpBuilder.setTitle(title);
	    helpBuilder.setMessage(message);
	
	    if ( type == "alert" ) {
	        icon = android.R.drawable.ic_dialog_alert;
	    } else if ( type == "info" ) {
	        icon = android.R.drawable.ic_dialog_info;
	    }
	
	    helpBuilder.setIcon(icon);
	    helpBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	
	        public void onClick(DialogInterface dialog, int which) {
	            // Do nothing but close the dialog
	        }
	    });
	
	    AlertDialog helpDialog = helpBuilder.create();
	    helpDialog.show();
	}

	public static void showPopUpInFragment(String title, String message, String type, SettingsFragment sf) {
	    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(new ContextThemeWrapper(sf.getActivity(), R.style.BoxTheme));
	    helpBuilder.setTitle(title);
	    helpBuilder.setMessage(message);
	
	    if ( type == "alert" ) {
	        icon = android.R.drawable.ic_dialog_alert;
	    } else if ( type == "info" ) {
	        icon = android.R.drawable.ic_dialog_info;
	    }
	
	    helpBuilder.setIcon(icon);
	    helpBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	
	        public void onClick(DialogInterface dialog, int which) {
	            // Do nothing but close the dialog
	        }
	    });
	
	    AlertDialog helpDialog = helpBuilder.create();
	    helpDialog.show();
	}

}
