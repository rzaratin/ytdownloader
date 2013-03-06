package dentex.youtube.downloader.utils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

import dentex.youtube.downloader.R;

public class SuCommand {
	
	//public final static File sdcard = Environment.getExternalStorageDirectory();
	//public static SharedPreferences settings = ShareActivity.settings;
	//public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	private final static String DEBUG_TAG = "SuCommand";
	static boolean BB = RootTools.isBusyboxAvailable();
	static boolean SU = RootTools.isRootAvailable();
	
	public static void copyToExtSdcard(Context context, String orig, String dest, String filename) {
		int res = 4;
		if (BB && SU) {
			CommandCapture command = new CommandCapture(0, "cp " + orig + filename + " " + dest);
			Log.d(DEBUG_TAG,                               "cp " + orig + filename + " " + dest);
			//CommandCapture command = new CommandCapture(0, "echo \"su test - safe to remove\" > /storage/sdcard1/test.txt"); // test

			try {
				RootTools.getShell(true).add(command).waitForFinish();
			} catch (InterruptedException e) {
				res = res - 1;
			} catch (IOException e) {
				res = res - 1;
			} catch (TimeoutException e) {
				res = res - 1;
			} catch (RootDeniedException e) {
				res = res - 1;
			} finally {
				if (res == 4) {
					Log.d(DEBUG_TAG, context.getString(R.string.su_copy_ok));
				} else {
					Log.d(DEBUG_TAG, "error during file copy");
					Toast.makeText(context, context.getString(R.string.su_copy_error), Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

}
