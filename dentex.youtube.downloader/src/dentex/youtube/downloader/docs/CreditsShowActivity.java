package dentex.youtube.downloader.docs;

import android.app.Activity;
import android.os.Bundle;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.utils.Utils;

public class CreditsShowActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Theme init
    	Utils.themeInit(this);
    	
		setContentView(R.layout.activity_credits_show);
	}

}