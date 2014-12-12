package com.mobagel.meeti.example;

import org.magiclen.crypt.Base64;
import org.magiclen.json.JSONArray;
import org.magiclen.json.JSONObject;
import org.magiclen.network.GET;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.mobagel.meeti.api.java.MeetiAPI;
import com.mobagel.meeti.api.java.MeetiCallback;
import com.mobagel.meeti.example.R;

public class ControllerActivity extends Activity implements View.OnClickListener, View.OnTouchListener, MeetiCallback {

	// TODO 類別常數
	private static final String LOG_TAG = "Robot";
	private static final String GROUP_ID = "picar";
	private static final String APP_NAME = "meeti@mobagel.com";
	private static final String API_KEY = "93473ac9156be6426c52f6bc588f04cc";
	private static final String ACCOUNT = "user2";
	private static final String PASSWORD = "fd4de22eef244fdd322fe938c8e2b00d";

	private static final int REQUEST_INITIAL_LOGIN = 0;
	private static final int REQUEST_INITIAL_GET_SERVER_TIME = 1;
	private static final int REQUEST_INITIAL_JOIN_GROUP = 2;
	private static final int REQUEST_GET_MESSAGE = 3;
	private static final int REQUEST_SET_MESSAGE = 4;
	private static final int REQUEST_SYSTEM_NOTIFICATION = 5;
	private static final MeetiAPI api = new MeetiAPI();

	// TODO 類別變數
	private static View loading;
	private static ImageView ivCar;
	private static Button btnF, btnLeft, btnRight, btnB, btnLightOn, btnLightOff, btnReset;
	private static String lastMoveCmd;
	private static long lastReceiveMessageTime = 0;
	private static boolean pausing = false;

	// TODO 物件方法
	private void findViews() {
		ivCar = (ImageView) findViewById(R.id.ivCar);
		btnF = (Button) findViewById(R.id.btnF);
		btnLeft = (Button) findViewById(R.id.btnLeft);
		btnRight = (Button) findViewById(R.id.btnRight);
		btnB = (Button) findViewById(R.id.btnB);
		btnLightOn = (Button) findViewById(R.id.btnLightOn);
		btnLightOff = (Button) findViewById(R.id.btnLightOff);
		btnReset = (Button) findViewById(R.id.btnReset);
		loading = findViewById(R.id.loading);
	}

	private void addListeners() {
		ivCar.setOnClickListener(this);
		btnF.setOnTouchListener(this);
		btnLeft.setOnTouchListener(this);
		btnRight.setOnTouchListener(this);
		btnB.setOnTouchListener(this);
		btnLightOff.setOnClickListener(this);
		btnLightOn.setOnClickListener(this);
		btnReset.setOnClickListener(this);
	}

	private void useCommand(final String cmd) {
		Log.i(LOG_TAG, cmd);
		final JSONObject obj = new JSONObject();
		obj.put("msg_groupid", GROUP_ID);
		obj.put("msg_senderid", ACCOUNT);
		obj.put("msg_content", cmd);
		obj.put("msg_type", "1");
		api.setMessage(REQUEST_SET_MESSAGE, GROUP_ID, obj.toString());
	}

	private void move(final int action, final String cmd) {
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (lastMoveCmd == null) {
				lastMoveCmd = cmd;
			}
			useCommand(cmd);
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		default:
			if (lastMoveCmd != null && !lastMoveCmd.equals(cmd)) {
				useCommand(lastMoveCmd);
			} else {
				lastMoveCmd = null;
				useCommand("sm");
			}
			break;
		}
	}

	private void forward(final int action) {
		move(action, "f");
	}

	private void back(final int action) {
		move(action, "b");
	}

	private void left(final int action) {
		move(action, "ll+");
	}

	private void right(final int action) {
		move(action, "rr+");
	}

	private void takePicture() {
		useCommand("t");
	}

	private void doFinish() {
		runOnUiThread(new Runnable() {
			public void run() {
				finish();
			}
		});
	}

	// TODO 生命周期
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller);
		findViews();
		addListeners();
		if (savedInstanceState == null) {
			Log.i(LOG_TAG, APP_NAME);
			Log.i(LOG_TAG, API_KEY);
			Log.i(LOG_TAG, ACCOUNT);
			Log.i(LOG_TAG, PASSWORD);
			if (!api.initial(REQUEST_INITIAL_LOGIN, APP_NAME, API_KEY, ACCOUNT, PASSWORD, this)) {
				doFinish();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		pausing = true;
	}

	@Override
	public void onResume() {
		super.onResume();
		pausing = false;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.controller, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = item.getItemId();
		switch (id) {

		}
		return super.onOptionsItemSelected(item);
	}

	// TODO Listener
	@Override
	public void onClick(final View v) {
		final int viewID = v.getId();
		switch (viewID) {
		case R.id.btnLightOn:
			useCommand("ol");
			break;
		case R.id.btnLightOff:
			useCommand("sl");
			break;
		case R.id.ivCar:
			takePicture();
			break;
		case R.id.btnReset:
			useCommand("1");
			break;
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(final View v, final MotionEvent event) {
		final int viewID = v.getId();
		final int action = event.getAction();
		switch (viewID) {
		case R.id.btnF:
			forward(action);
			break;
		case R.id.btnB:
			back(action);
			break;
		case R.id.btnLeft:
			left(action);
			break;
		case R.id.btnRight:
			right(action);
			break;
		}
		return false;
	}

	@Override
	public void meetiResult(final int requestCode, final String result) {
		switch (requestCode) {
		case REQUEST_INITIAL_LOGIN:
			Log.i(LOG_TAG, result);
			try {
				final JSONObject json = new JSONObject(result);
				final String token = json.getString("token");
				api.setToken(token);
				if (!api.setGroupJoin(REQUEST_INITIAL_JOIN_GROUP, GROUP_ID)) {
					doFinish();
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
				doFinish();
			}
			break;
		case REQUEST_INITIAL_GET_SERVER_TIME:
			try {
				final long time = Long.parseLong(result);
				lastReceiveMessageTime = time - 60000;
				runOnUiThread(new Runnable() {
					public void run() {
						loading.setVisibility(View.INVISIBLE);
					}
				});
				api.getSystemNotification(REQUEST_SYSTEM_NOTIFICATION);
			} catch (final Exception ex) {

			}
			break;
		case REQUEST_INITIAL_JOIN_GROUP:
			if (!("ok".equals(result) || "".equals(result))) {
				doFinish();
			} else {
				if (!api.getServertime(REQUEST_INITIAL_GET_SERVER_TIME)) {
					doFinish();
				}
			}
			break;
		case REQUEST_GET_MESSAGE:
			try {
				if (result != null && result.length() > 0) {
					final JSONObject messagesObj = new JSONObject(result);
					final JSONArray messages = messagesObj.getJSONArray("messages");
					final int l = messages.length();

					for (int i = 0; i < l; i++) {
						final JSONObject message = messages.getJSONObject(i);
						handleMessage(message, true);
					}
				}
			} catch (final Exception ex) {

			}
			break;
		case REQUEST_SYSTEM_NOTIFICATION:
			try {
				final JSONObject notifications = new JSONObject(result);
				final JSONArray messagesArray = notifications.getJSONArray("messages");
				final int l = messagesArray.length();
				for (int i = 0; i < l; i++) {
					final JSONObject notification = messagesArray.getJSONObject(i);
					final int type = notification.getInt("msg_type");
					switch (type) {
					case 5000:
						api.getMessage(REQUEST_GET_MESSAGE, notification.getString("msg_content"), String.valueOf(lastReceiveMessageTime));
						break;
					case 1000:
						break;
					}
				}
			} catch (final Exception ex) {

			}
			try {
				Thread.sleep(1000);
			} catch (final Exception ex) {

			}
			while (pausing) {
				try {
					Thread.sleep(1000);
				} catch (final Exception ex) {

				}
			}
			api.getSystemNotification(REQUEST_SYSTEM_NOTIFICATION);
			break;
		}
	}

	public void handleMessage(final JSONObject message, final boolean ignoreSelf) {
		final String senderID = message.getString("msg_senderid");
		final String content = message.getString("msg_content");
		final String type = message.getString("msg_type");
		final long time = message.getLong("msg_time");
		lastReceiveMessageTime = time + 1;
		switch (type) {
		case "2":
			final GET getImageBase64 = new GET(content);
			new Thread() {
				public void run() {
					String temp;
					try {
						getImageBase64.open();
						final String s = new String((byte[]) getImageBase64.getResult(), "UTF-8");
						final byte[] b = Base64.getDecoder().decode(s);
						final Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
						runOnUiThread(new Runnable() {

							public void run() {
								ivCar.setImageBitmap(bitmap);
							}
						});

						temp = "取得圖片";
					} catch (final Exception ex) {
						temp = "圖片接收錯誤";
					}
					Log.i(LOG_TAG, String.format("%s：%s", senderID, temp));
				}
			}.start();
			break;
		case "1":
			Log.i(LOG_TAG, String.format("%s：%s", senderID, content));
			break;
		}
	}
}
