package com.draw.hackmit;

import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.Menu;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.GestureDetector;

public class VideoActivity extends Activity implements OnHoverListener,
		GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;
	private Camera camera = null;
	private boolean inPreview = false;
	private boolean cameraConfigured = false;
	// Declare the global text variable used across methods
	private TextView text;
	private GestureDetector mDetector;
	private MediaRecorder mediaRecorder;
	private boolean recording = false;
	private int prevX = -9999;
	private int prevY = -9999;
	private int prevT;
	private boolean focusing = false;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_activity);
		// Initialize the layout variable and listen to hover events on it
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
		layout.setOnHoverListener(this);
		// Initialize the text widget so we can edit the text inside
		text = (TextView) findViewById(R.id.text);

		preview = (SurfaceView) findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mDetector = new GestureDetector(this, this);

	}

	@Override
	public void onResume() {
		super.onResume();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			Camera.CameraInfo info = new Camera.CameraInfo();

			for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
				Camera.getCameraInfo(i, info);

				if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					camera = Camera.open(i);
				}
			}
		}

		if (camera == null) {
			camera = Camera.open();
		}

		if (mediaRecorder == null) {
			mediaRecorder = new MediaRecorder();
			mediaRecorder.setCamera(camera);
		}

		startPreview();
	}

	@Override
	public void onPause() {
		if (inPreview) {
			camera.stopPreview();
		}

		mediaRecorder.release();
		camera.release();
		camera = null;
		inPreview = false;

		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.camera) {
			if (inPreview) {
				camera.takePicture(null, null, photoCallback);
				inPreview = false;
			}
		}

		return (super.onOptionsItemSelected(item));
	}

	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	private Camera.Size getBestPictureSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPictureSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	private void initPreview(int width, int height) {
		if (camera != null && previewHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback",
						"Exception in setPreviewDisplay()", t);
				Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG).show();
			}

			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				Camera.Size size = getBestPreviewSize(width, height, parameters);
				Camera.Size pictureSize = getBestPictureSize(width, height,
						parameters);

				if (size != null && pictureSize != null) {
					parameters.setPreviewSize(size.width, size.height);
					parameters.setPictureSize(pictureSize.width,
							pictureSize.height);
					parameters.setPictureFormat(ImageFormat.JPEG);
					parameters
							.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					camera.setParameters(parameters);
					cameraConfigured = true;
				}
			}
		}
	}

	private void startPreview() {
		if (cameraConfigured && camera != null) {
			camera.startPreview();
			inPreview = true;
		}
	}

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			// no-op -- wait until surfaceChanged()
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {

			camera.stopPreview();
			Camera.Parameters params = camera.getParameters();

			WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			android.view.Display display = window.getDefaultDisplay();

			if (display.getRotation() == Surface.ROTATION_0) {
				params.setPreviewSize(height, width);
				camera.setDisplayOrientation(90);
			}

			if (display.getRotation() == Surface.ROTATION_90) {
				params.setPreviewSize(height, width);
			}

			if (display.getRotation() == Surface.ROTATION_180) {
				params.setPreviewSize(height, width);
			}

			if (display.getRotation() == Surface.ROTATION_270) {
				params.setPreviewSize(height, width);
				camera.setDisplayOrientation(180);
			}
			initPreview(width, height);

			startPreview();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			mediaRecorder.release();
		}
	};

	Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SavePhotoTask().execute(data);
			camera.startPreview();
			inPreview = true;
		}
	};

	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		@Override
		protected String doInBackground(byte[]... jpeg) {
			File photo = new File(Environment.getExternalStorageDirectory(),
					"DCIM" + File.separator + "Camera");

			if (!photo.exists()) {
				if (!photo.mkdirs()) {
					Log.d("Camera", "failed to create directory");
					return null;
				}
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			File mediaFile;
			mediaFile = new File(photo.getPath() + File.separator + "IMG_"
					+ timeStamp + ".jpg");

			try {
				FileOutputStream fos = new FileOutputStream(mediaFile.getPath());

				fos.write(jpeg[0]);
				fos.close();
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
						Uri.parse("file://"
								+ Environment.getExternalStorageDirectory())));
			} catch (java.io.IOException e) {
				Log.e("Camera", "Exception in photoCallback", e);
			}

			return (null);
		}
	}

	// For whenever a hover event is triggered on an element being listened to
	public boolean onHover(View v, MotionEvent e) {
		// Depending on what action is performed, set the text to that action
		switch (e.getActionMasked()) {
		case MotionEvent.ACTION_HOVER_ENTER:
			text.setText("ACTION_HOVER_ENTER");
			break;
		case MotionEvent.ACTION_HOVER_MOVE:
			// focus camera
			int x = getXCoord(e.getX());
			if (x < -900) {
				x = -900;
			} else if (x > 900) {
				x = 900;
			}
			int y = getYCoord(e.getY());
			if (y < -900) {
				y = -900;
			} else if (y > 900) {
				y = 900;
			}

			int t = (int) System.currentTimeMillis();
			if (Math.abs(prevX - x) > 100 || Math.abs(prevY - y) > 100) {
				prevX = x;
				prevY = y;
				prevT = t;
				focusing = false;
			}

			if (t - prevT > 1000 && !focusing) {

				focusing = true;

				Camera.Parameters parameters = camera.getParameters();
				List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();

				Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);
				Camera.Area area = new Camera.Area(rect, 500);
				focusAreas.add(area);
				parameters.setFocusAreas(focusAreas);
				camera.setParameters(parameters);
				camera.autoFocus(new Camera.AutoFocusCallback() {

					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						// TODO Auto-generated method stub
						(new MediaActionSound())
								.play(MediaActionSound.FOCUS_COMPLETE);
						// camera.takePicture(shutter, null, jpeg)
					}
				});
			}
			
			final ImageView focusIm = (ImageView) findViewById(R.id.focus);
			LayoutParams focusImParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			focusImParams.setMargins(getXPixel(x-100), getYPixel(y-100), getXPixel(x + 100), getYPixel(y + 100));
			focusIm.setLayoutParams(focusImParams);
			focusIm.setImageResource(R.drawable.focus);
			focusIm.postDelayed(new Runnable() {
				@Override
				public void run() {
					focusIm.setImageResource(0);
				}}, 2000);

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

	private int getXCoord(float hoverX) {
		return (int) (Math.round(hoverX / 1920. * 2000. - 1000.));
	}
	
	private int getXPixel(int cameraX) {
		return (int) ((((float)cameraX) + 1000.) / 2000. * 1920.);
	}

	private int getYCoord(float hoverY) {
		return (int) (Math.round(hoverY / 1080. * 2000. - 1000.));
	}
	
	private int getYPixel(int cameraY) {
		return (int) ((((float)cameraY) + 1000.) / 2000. * 1080.);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onTouchEvent(MotionEvent event) {
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		Log.d("GestureRecognizer", "onFling: " + e1.toString() + e2.toString());
		if (e1.getY() > e2.getY()) { // THIS NEEDS TO BE CHANGED TO GETX ONCE WE
										// FIX THE CAMERA ORIENTATION
			Log.d("GestureRecognizer", "This is a swipe to the right");
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
	        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);

        }
        return true;
        }

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	protected void startRecording() throws java.io.IOException {
		camera.unlock();
		mediaRecorder.setCamera(camera);

		mediaRecorder.setPreviewDisplay(previewHolder.getSurface());
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

		mediaRecorder.setProfile(CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH));
		mediaRecorder.setPreviewDisplay(previewHolder.getSurface());
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory()
				+ File.separator + "DCIM" + File.separator + "Camera"
				+ File.separator + timeStamp + ".mp4");

		mediaRecorder.prepare();
		mediaRecorder.start();
	}

	protected void stopRecording() {
		mediaRecorder.stop();
		// mediaRecorder.release();
		// camera.release();
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}

	public boolean onSingleTapUp(MotionEvent e) {
		Log.d("Recording?", "" + recording);
		Log.d("Gesture Rec", "onSingleTapUp: " + e.toString());
		ImageView recIndicator = (ImageView) findViewById(R.id.grey_red_video);

		if (recording) {
			stopRecording();
			recIndicator.setImageResource(R.drawable.grey_video);
			Log.d("Recorder", "omg it stopped");
			recording = !recording;
		} 
			else {
	            try {
	                startRecording();
					Log.d("Recorder", "omg it started");
					recIndicator.setImageResource(R.drawable.red_video);
					recording = !recording;
	            } catch (Exception err) {
	                String message = err.getMessage();
	                Log.i(null, "Problem Start"+message);
	                mediaRecorder.release();
	            }
			}
		return true;
	}


}
