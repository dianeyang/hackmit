package com.draw.hackmit;

import android.os.Bundle;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity implements OnHoverListener {

	// Declare the global text variable used across methods
	private TextView text;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Initialize the layout variable and listen to hover events on it
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
		layout.setOnHoverListener(this);
		// Initialize the text widget so we can edit the text inside
		text = (TextView) findViewById(R.id.text);
	}

	// For whenever a hover event is triggered on an element being listened to
	public boolean onHover(View v, MotionEvent e) {
		// Depending on what action is performed, set the text to that action
		switch (e.getActionMasked()) {
		case MotionEvent.ACTION_HOVER_ENTER:
			text.setText("ACTION_HOVER_ENTER");
			break;
		case MotionEvent.ACTION_HOVER_MOVE:
			text.setText("ACTION_HOVER_MOVE");
			break;
		case MotionEvent.ACTION_HOVER_EXIT:
			text.setText("ACTION_HOVER_EXIT");
			break;
		}
		// Along with the event name, also print the XY location of the data
		text.setText(text.getText() + " - X: " + e.getX() + " - Y: " + e.getY());
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
