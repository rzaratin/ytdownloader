package dentex.youtube.downloader.docs;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import dentex.youtube.downloader.R;

public class ChangelogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_changelog);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_changelog, menu);
		return true;
	}

}
