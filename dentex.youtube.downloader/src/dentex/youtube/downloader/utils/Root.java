package dentex.youtube.downloader.utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

public class Root {
	
	//public final static File sdcard = Environment.getExternalStorageDirectory();
	//public static SharedPreferences settings = ShareActivity.settings;
	//public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	static boolean BB = RootTools.isBusyboxAvailable();
	static boolean SU = RootTools.isRootAvailable();
	
	public static void copyToExtSdcard(Context context, File orig, File dest) {
		int res = 4;
		if (BB && SU) {
			CommandCapture command = new CommandCapture(0, 
					"cp " + orig.getPath() + dest.getPath());
			try {
				RootTools.getShell(true).add(command).waitForFinish();
			} catch (InterruptedException e) {
				res = res - 1;
				Toast.makeText(context, "Errors in CopyToExtSdcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				res = res - 1;
				Toast.makeText(context, "Errors in CopyToExtSdcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			} catch (TimeoutException e) {
				res = res - 1;
				Toast.makeText(context, "Errors in CopyToExtSdcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			} catch (RootDeniedException e) {
				res = res - 1;
				Toast.makeText(context, "Errors in CopyToExtSdcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			} finally {
				if (res == 4) {
					Toast.makeText(context, "CopyToExtSdcard finished without errors", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

}
