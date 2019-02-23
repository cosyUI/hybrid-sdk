package com.wya.hybrid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.arialyy.aria.core.download.DownloadTask;
import com.google.gson.Gson;
import com.wya.hardware.camera.WYACameraView;
import com.wya.hybrid.base.ActivityManager;
import com.wya.hybrid.base.BaseApp;
import com.wya.hybrid.bean.AppIdle;
import com.wya.hybrid.bean.BaseEmitData;
import com.wya.hybrid.bean.Battery;
import com.wya.hybrid.bean.ContactsBean;
import com.wya.hybrid.bean.Foreground;
import com.wya.hybrid.bean.KeyBack;
import com.wya.hybrid.bean.Keyboard;
import com.wya.hybrid.bean.NetState;
import com.wya.hybrid.bean.PictureBean;
import com.wya.hybrid.bean.RecordBean;
import com.wya.hybrid.bean.ReturnPictureBean;
import com.wya.hybrid.bean.Shake;
import com.wya.hybrid.bean.TakeScreenshot;
import com.wya.hybrid.bean.VolumeDown;
import com.wya.hybrid.bean.VolumeUp;
import com.wya.hybrid.control.BatteryReceiver;
import com.wya.hybrid.control.NetworkReceiver;
import com.wya.hybrid.control.ScreenReceiver;
import com.wya.hybrid.control.ScreenShotListenManager;
import com.wya.hybrid.data.event.AppIdleEvent;
import com.wya.hybrid.data.event.BatteryEvent;
import com.wya.hybrid.data.event.ForegroundEvent;
import com.wya.hybrid.data.event.NetEvent;
import com.wya.hybrid.data.event.ShakeEvent;
import com.wya.hybrid.data.sp.BatterySp;
import com.wya.hybrid.data.sp.ForegroundStateSp;
import com.wya.hybrid.methods.cache.CacheData;
import com.wya.hybrid.methods.cache.SpaceData;
import com.wya.hybrid.methods.closewin.CloseWinData;
import com.wya.hybrid.methods.installapp.InstallAppData;
import com.wya.hybrid.methods.installed.InstalledData;
import com.wya.hybrid.methods.notification.AlarmReceiver;
import com.wya.hybrid.methods.notification.bean.NotificationData;
import com.wya.hybrid.methods.notification.bean.NotificationEmit;
import com.wya.hybrid.methods.notification.bean.NotificationsUtils;
import com.wya.hybrid.methods.notification.bean.Notify;
import com.wya.hybrid.methods.openapp.OpenAppData;
import com.wya.hybrid.methods.openwin.OpenWinActivity;
import com.wya.hybrid.methods.openwin.bean.OpenWinData;
import com.wya.hybrid.methods.sms.Sms;
import com.wya.hybrid.nativeUI.CameraActivity;
import com.wya.hybrid.util.CheckUtil;
import com.wya.hybrid.util.log.DebugLogger;
import com.wya.uikit.dialog.WYACustomDialog;
import com.wya.uikit.imagepicker.ImagePickerCreator;
import com.wya.uikit.imagepicker.PickerConfig;
import com.wya.uikit.toolbar.StatusBarUtil;
import com.wya.utils.utils.DataCleanUtil;
import com.wya.utils.utils.FileManagerUtil;
import com.wya.utils.utils.LogUtil;
import com.wya.utils.utils.PhoneUtil;
import com.wya.utils.utils.ScreenUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static com.wya.uikit.toolbar.StatusBarUtil.getStatusBarHeight;
import static com.wya.utils.utils.FileManagerUtil.TASK_COMPLETE;

/**
 * @author :
 */
public class HybridManager implements JsCallBack {

	private static final int CAMERA_PIC_REQUEST = 100;
	private static final int CAMERA_VIDEO_REQUEST = 101;
	private static final int ALBUM_PIC_REQUEST = 102;
	private static final int ALBUM_VIDEO_REQUEST = 103;

	private WYAWebView mWebView;
	private BaseEmitData<Object> mEmitData = new BaseEmitData<>();
	private Map<String, Integer> mEventMap;
	private boolean mIsDebugger = false;
	private Activity mContext;

	private boolean mIsFromBackground = false;
	private NetworkReceiver mNetworkReceiver;
	private BatteryReceiver mBatteryReceiver;
	private ScreenReceiver mScreenReceiver;

	private CloseWinData closeWinData;
	private MediaPlayer mMediaPlayer;
	private MediaRecorder mRecorder;
	private PictureBean mPictureBean;

	/**
	 * 软键盘的显示状态
	 */
	private boolean mShowKeyboard;

	/**
	 * 打开window
	 */
	private OpenWinData mOpenWinData;

	private String winName = "a";

	/**
	 * 关闭window
	 */
	private CloseWinData mCloseWinData;

	/**
	 * 软件下载安装
	 */
	private InstallAppData mInstallAppData;
	private FileManagerUtil mFileManagerUtil;
	private String fileRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

	/**
	 * 下载app判断
	 */
	private InstalledData mInstalledAppData;

	/**
	 * 打开app
	 */
	private OpenAppData mOpenAppData;

	/**
	 * 清理缓存
	 */
	private CacheData mCacheData;

	/**
	 * 通知对象
	 */
	private NotificationData mNotificationData;

	/**
	 * 短信对象
	 */
	private Sms sms;

	private boolean saveToPhotoAlbum;

	public HybridManager(Activity context, WYAWebView webView) {
		if (!CheckUtil.isValidate(context)) {
			return;
		}
		this.mWebView = webView;
		this.mContext = context;
		this.mEventMap = new HashMap<>();

		// event bus
		if (!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
		}

		// shake
		SensorManagerHelper sensorManagerHelper = new SensorManagerHelper(context);
		sensorManagerHelper.setOnShakeListener(() -> onShake(context));

		// take screenshot
		ScreenShotListenManager manager = ScreenShotListenManager.newInstance(context);
		manager.setListener(this::onScreenshot);
		manager.startListen();

		registerNetworkReceiver();
		registerBatteryReceiver();
		registerScreenReceiver();

	}

	private static void playVibreate(Context context) {
		try {
			Vibrator vibrator = (Vibrator) context.getSystemService(android.content.Context.VIBRATOR_SERVICE);
			if (null != vibrator) {
				vibrator.vibrate(200);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registerNetworkReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		filter.addAction("android.net.wifi.STATE_CHANGE");
		if (null == mNetworkReceiver) {
			mNetworkReceiver = new NetworkReceiver();
		}
		mContext.registerReceiver(mNetworkReceiver, filter);
	}

	private void registerBatteryReceiver() {
		IntentFilter filter = new IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
		if (null == mBatteryReceiver) {
			mBatteryReceiver = new BatteryReceiver();
		}
		mContext.registerReceiver(mBatteryReceiver, filter);
	}

	public void registerScreenReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(android.content.Intent.ACTION_SCREEN_ON);
		filter.addAction(android.content.Intent.ACTION_SCREEN_OFF);
		filter.addAction(android.content.Intent.ACTION_USER_PRESENT);
		if (null == mScreenReceiver) {
			mScreenReceiver = new ScreenReceiver();
		}
		mContext.registerReceiver(mScreenReceiver, filter);
	}

	/**
	 * 低电量
	 *
	 * @param id :
	 */
	private void onBatteryLow(int id) {
		mEventMap.put(Battery.EVENT_BATTERY_LOW, id);
		Battery battery = new Battery();
		battery.setIsPlugged(BatterySp.get().isPlugged());
		battery.setLevel(BatterySp.get().getLevel());
		setEmitData(1, "响应成功", battery);
		send(Battery.EVENT_BATTERY_LOW, getEmitData());
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onBatteryEvent(BatteryEvent event) {
		if (null == event) {
			return;
		}

		Battery battery = new Battery();
		battery.setIsPlugged(event.isPlugged());
		battery.setLevel(event.getLevel());

		if (event.isBatteryLow()) {
			setEmitData(1, "响应成功", battery);
			send(Battery.EVENT_BATTERY_LOW, getEmitData());
		}
	}

	/**
	 * 电池状态
	 *
	 * @param id :
	 */
	private void onBatteryStatus(int id) {
		mEventMap.put(Battery.EVENT_BATTERY_STATUS, id);
		Battery battery = new Battery();
		battery.setIsPlugged(BatterySp.get().isPlugged());
		battery.setLevel(BatterySp.get().getLevel());
		setEmitData(1, "响应成功", battery);
		send(Battery.EVENT_BATTERY_STATUS, getEmitData());
	}

	/**
	 * 断开网络
	 *
	 * @param id :
	 */
	private void onOffline(int id) {
		mEventMap.put(NetState.EVENT_OFFLINE, id);
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onNetEvent(NetEvent event) {
		if (event == null) {
			return;
		}
		setEmitData(1, "响应成功", new NetState());
		send(event.isOnline() ? NetState.EVENT_ONLINE : NetState.EVENT_OFFLINE, getEmitData());
	}

	/**
	 * 连接网络
	 *
	 * @param id :
	 */
	private void onOnline(int id) {
		mEventMap.put(NetState.EVENT_ONLINE, id);
	}

	/**
	 * 应用进入后台
	 *
	 * @param id :
	 */
	private void onPause(int id) {
		mEventMap.put(Foreground.EVENT_PAUSE, id);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onForegroundEvent(ForegroundEvent event) {
		if (event == null) {
			return;
		}
		setEmitData(1, "响应成功", new Foreground());
		send(event.isPause() ? Foreground.EVENT_PAUSE : Foreground.EVENT_RESUME, getEmitData());
	}

	/**
	 * 应用从后台回到前台
	 *
	 * @param id :
	 */
	private void onResume(int id) {
		mEventMap.put(Foreground.EVENT_RESUME, id);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onShakeEvent(ShakeEvent event) {
		if (event == null) {
			return;
		}
		setEmitData(1, "响应成功", new Shake());
		send(Shake.EVENT_SHAKE, getEmitData());
	}

	/**
	 * 摇动事件
	 *
	 * @param id :
	 */
	private void onShake(int id) {
		mEventMap.put(Shake.EVENT_SHAKE, id);
	}

	private void onShake(Context context) {
		playVibreate(context);
		setEmitData(1, "响应成功", new Shake());
		send(Shake.EVENT_SHAKE, getEmitData());
	}

	/**
	 * 多长时间不操作屏幕
	 *
	 * @param id :
	 */
	private void onAppIdle(int id) {
		mEventMap.put(AppIdle.EVENT_APP_IDLE, id);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onAppIdleEvent(AppIdleEvent event) {
		if (event == null) {
			return;
		}
		DebugLogger.logEvent("onScreenEvent state = %s", event.stateScreenOff);
		if (event.stateScreenOff) {
			setEmitData(1, "响应成功", new AppIdle());
			send(AppIdle.EVENT_APP_IDLE, getEmitData());
			// TODO: 2019/1/19 ZCQ TEST
			awake(BaseApp.getApp());
		}
	}

	private void awake(Context context) {
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unlock");
		kl.disableKeyguard();

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		@SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "bright");
		wl.acquire();
		wl.release();
	}

	/**
	 * 用户屏幕截图
	 *
	 * @param id :
	 */
	private void onScreenshot(int id) {
		mEventMap.put(TakeScreenshot.EVENT_TAKE_SCREENSHOT, id);
	}

	private void onScreenshot(String imagePath) {
		DebugLogger.logEvent("onShot ... imagePath = %s", imagePath);
		TakeScreenshot screenshot = new TakeScreenshot();
		screenshot.setImage(imagePath);
		setEmitData(1, "响应成功", screenshot);
		send(TakeScreenshot.EVENT_TAKE_SCREENSHOT, getEmitData());
	}

	private <T> void setEmitData(int status, String msg, T data) {
		if (null == mEmitData) {
			mEmitData = new BaseEmitData<>();
		}
		mEmitData.setMsg(msg);
		mEmitData.setData(data);
		if (null != data) {
			status = 1;
		}
		mEmitData.setStatus(status);
	}

	private void send(String event, BaseEmitData emitData) {
		if (null == mWebView) {
			return;
		}

		if (mIsDebugger) {
			if (null != mEventMap && CheckUtil.isNotEmpty(event) && mEventMap.containsKey(event)) {
				int id = mEventMap.get(event);
				mWebView.send(id, emitData);
				DebugLogger.logEvent("WYAEventManager .[debugger true] id = %s, emitData = %s", id, emitData);
			}
		} else {
			if (null != mEventMap && CheckUtil.isNotEmpty(event) && mEventMap.containsKey(event)) {
				int id = mEventMap.get(event);
				mWebView.send(id, emitData);
				DebugLogger.logEvent("WYAEventManager .[debugger false] id = %s, emitData = %s", id, emitData);
			} else {
				mWebView.send(event, emitData);
				DebugLogger.logEvent("WYAEventManager .[debugger false] event = %s, emitData = %s", event, emitData);
			}
		}
	}

	private BaseEmitData getEmitData() {
		return mEmitData;
	}

	public void release() {
		if (EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().unregister(this);
		}
		if (null != mWebView) {
			mWebView.removeAllViews();
			mWebView = null;
		}
	}

	/**
	 * 音量加按钮
	 *
	 * @param keyCode :
	 */
	public void volumeDown(int keyCode) {
		VolumeDown volumeDown = new VolumeDown();
		volumeDown.setKeyCode(keyCode);
		volumeDown.setLongPress(false);
		setEmitData(1, "响应成功", volumeDown);
		send(VolumeDown.EVENT_VOLUME_DOWN, getEmitData());
	}

	/**
	 * 音量减按钮
	 *
	 * @param keyCode :
	 */
	public void volumeUp(int keyCode) {
		VolumeUp volumeUp = new VolumeUp();
		volumeUp.setKeyCode(keyCode);
		volumeUp.setLongPress(false);
		setEmitData(1, "响应成功", volumeUp);
		send(VolumeUp.EVENT_VOLUME_UP, getEmitData());
	}

	public void onActivityCreate() {

	}

	public void onActivityStart() {

	}

	public void onActivityPause() {

	}

	public void onActivityResume() {
		if (ActivityManager.getInstance().isForeground() && mIsFromBackground) {
			DebugLogger.logEvent("onResume ... ");
			mIsFromBackground = false;
			ForegroundStateSp.get().setIsResume(true);
			ForegroundStateSp.get().setIsPause(false);

			ForegroundEvent event = new ForegroundEvent();
			event.setPause(false);
			event.setResume(true);
			EventBus.getDefault().post(event);
		}
	}

	public void onActivityStop() {
		if (!ActivityManager.getInstance().isForeground()) {
			DebugLogger.logEvent("onStop ... ");

			mIsFromBackground = true;
			ForegroundStateSp.get().setIsPause(true);
			ForegroundStateSp.get().setIsResume(false);

			ForegroundEvent event = new ForegroundEvent();
			event.setPause(true);
			event.setResume(false);
			EventBus.getDefault().post(event);
		}
	}

	public void onActivityDestroy() {
		unRegisterReceiver(mContext, mNetworkReceiver);
		unRegisterReceiver(mContext, mBatteryReceiver);
		unRegisterReceiver(mContext, mScreenReceiver);
	}

	private void unRegisterReceiver(Context context, BroadcastReceiver receiver) {
		if (context == null || null == receiver) {
			return;
		}
		context.unregisterReceiver(receiver);
	}

	/**
	 * 返回按钮
	 *
	 * @param keyCode :
	 */
	public void keyBack(int keyCode) {
		KeyBack keyBack = new KeyBack();
		keyBack.setKeyCode(keyCode);
		keyBack.setLongPress(false);
		setEmitData(1, "响应成功", keyBack);
		send(KeyBack.EVENT_KEY_BACK, getEmitData());
	}

	public void onKeyBoardListener(String event, int id) {
		mEventMap.put(event, id);
		mWebView.getRootView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				// 应用可以显示的区域。此处包括应用占用的区域，包括标题栏不包括状态栏
				Rect r = new Rect();
				mWebView.getRootView().getWindowVisibleDisplayFrame(r);
				// 键盘最小高度
				int minKeyboardHeight = 150;
				// 获取状态栏高度
				int statusBarHeight = getStatusBarHeight(mContext);
				// 屏幕高度,不含虚拟按键的高度
				int screenHeight = ScreenUtil.getScreenHeight(mContext) - statusBarHeight;
				// 在不显示软键盘时，height等于状态栏的高度
				int height = screenHeight - (r.bottom - r.top);
				Keyboard keyboard = new Keyboard();
				keyboard.setHeight(height);
				setEmitData(1, "响应成功", keyboard);
				send(VolumeUp.EVENT_VOLUME_UP, getEmitData());

				if (mShowKeyboard) {
					// 如果软键盘是弹出的状态，并且height小于等于状态栏高度，
					// 说明这时软键盘已经收起
					if (height - statusBarHeight < minKeyboardHeight) {
						mShowKeyboard = false;
						send(Keyboard.EVENT_KEYBOARD_HIDE, getEmitData());
					}
				} else {
					// 如果软键盘是收起的状态，并且height大于状态栏高度，
					// 说明这时软键盘已经弹出
					if (height - statusBarHeight > minKeyboardHeight) {
						mShowKeyboard = true;
						send(Keyboard.EVENT_KEYBOARD_SHOW, getEmitData());
					}
				}
			}
		});
	}

	/**
	 * @param data 返回数据
	 * @param id   id
	 * @param name
	 */
	@Override
	public void response(String data, int id, String name) {
		LogUtil.e(data + "default--------" + id + "--------" + name);
		mIsDebugger = false;
		switch (name) {
			case "debugger":
				mIsDebugger = true;
				DebugLogger.logEvent("data = %s , id = %s", data, id);
				Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
				if (data.contains(Battery.EVENT_BATTERY_LOW)) {
					HybridManager.this.onBatteryLow(id);
				} else if (data.contains(Battery.EVENT_BATTERY_STATUS)) {
					HybridManager.this.onBatteryStatus(id);
				} else if (data.contains(NetState.EVENT_OFFLINE)) {
					HybridManager.this.onOffline(id);
				} else if (data.contains(NetState.EVENT_ONLINE)) {
					HybridManager.this.onOnline(id);
				} else if (data.contains(Foreground.EVENT_PAUSE)) {
					HybridManager.this.onPause(id);
				} else if (data.contains(Foreground.EVENT_RESUME)) {
					HybridManager.this.onResume(id);
				} else if (data.contains(Shake.EVENT_SHAKE)) {
					onShake(id);
				} else if (data.contains(AppIdle.EVENT_APP_IDLE)) {
					HybridManager.this.onAppIdle(id);
				} else if (data.contains(TakeScreenshot.EVENT_TAKE_SCREENSHOT)) {
					onScreenshot(id);
				} else if (data.contains(KeyBack.EVENT_KEY_BACK)) {
					mEventMap.put(KeyBack.EVENT_KEY_BACK, id);
				} else if (data.contains(VolumeDown.EVENT_VOLUME_DOWN)) {
					mEventMap.put(VolumeDown.EVENT_VOLUME_DOWN, id);
				} else if (data.contains(VolumeUp.EVENT_VOLUME_UP)) {
					mEventMap.put(VolumeUp.EVENT_VOLUME_UP, id);
				} else if (data.contains(Keyboard.EVENT_KEYBOARD_SHOW)) {
					HybridManager.this.onKeyBoardListener(Keyboard.EVENT_KEYBOARD_SHOW, id);
				} else if (data.contains(Keyboard.EVENT_KEYBOARD_HIDE)) {
					HybridManager.this.onKeyBoardListener(Keyboard.EVENT_KEYBOARD_HIDE, id);
				}
				break;
			case "push":
				push(name, id, data);
				break;
			case "pop":
				pop(name, id, data);
				break;
			case "openVideo":
				openVideo(data, id, name);
				break;
			case "startPlay":
				startPlay(data, id, name);
				break;
			case "stopPlay":
				stopPlay(data, id, name);
				break;
			case "startRecord":
				startRecording(data, id, name);
				break;
			case "stopRecord":
				stopRecording(data, id, name);
				break;
			case "saveMediaToAlbum":
				savePicture(data, id, name);
				break;
			case "getPicture":
				getPicture(data, id, name);
				break;
			case "showFloatBox":
				showFloatBox(data, id, name);
				break;
			case "toLauncher":
				toLauncher(data, id, name);
				break;
			case "setKeepScreenOn":
				setKeepScreenOn(data, id, name);
				break;
			case "setScreenOrientation":
				setScreenOrientation(data, id, name);
				break;
			case "setStatusBarStyle":
				setStatusBarStyle(data, id, name);
				break;
			case "openContacts":
//				Intent i = new Intent();
////				i.setAction(Intent.ACTION_PICK);
////				i.setData(ContactsContract.Contacts.CONTENT_URI);
////				mContext.startActivityForResult(i, 1);

				openContacts(data, id, name);
				break;
			case "installApp":
				installApp(name, id, data);
				break;
			case "openApp":
				openApp(name, id, data);
				break;
			case "appInstalled":
				appInstalled(name, id, data);
				break;
			case "clearCache":
				clearCache(name, id, data);
				break;
			case "getCacheSize":
				getCacheSize(name, id, data);
				break;
			case "getTotalSpace":
				getTotalSpace(name, id, data);
				break;
			case "getFreeDiskSpace":
				getFreeDiskSpace(name, id, data);
				break;
			case "notification":
				notification(name, id, data);
				break;
			case "cancelNotification":
				cancelNotification(name, id, data);
				break;
			case "sms":
				sms(name, id, data);
				break;
			case "mail":
				mail(name, id, data);
				break;
			default:
				break;
		}
	}

	/**
	 * 联系人
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void openContacts(String data, int id, String name) {
		mEventMap.put(name, id);

		ContactsBean contactsBean = new ContactsBean();
		List<ContactsBean.Contacts> list = new ArrayList<>();
		Cursor cursor = mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "display_name COLLATE LOCALIZED");
		while (cursor.moveToNext()) {
			String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
			Cursor phones = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);
			String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			Log.i("test", "response: " + displayName);
			if (phones.moveToFirst()) {
				do {
					String phonesNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					ContactsBean.Contacts contacts = new ContactsBean.Contacts();
					contacts.setName(displayName);
					contacts.setPhone(phonesNumber);
					list.add(contacts);
					Log.i("test", "response: " + phonesNumber);
				} while (phones.moveToNext());
			}
		}

		contactsBean.setList(list);
		setEmitData(1, "响应成功", contactsBean);
		send(name, getEmitData());
	}

	private void showFloatBox(String data, int id, String name) {
		mEventMap.put(name, id);
//		startFloat();
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 保持屏幕亮度
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void setKeepScreenOn(String data, int id, String name) {
		mEventMap.put(name, id);
		mContext.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	private void getPicture(String data, int id, String name) {
		mEventMap.put(name, id);
//		PictureBean pictureBean = new Gson().fromJson(data, PictureBean.class);\
		PictureBean pictureBean = new PictureBean();
		saveToPhotoAlbum = pictureBean.isSaveToPhotoAlbum();
		if ("camera".equals(pictureBean.getSourceType())) {
			switch (pictureBean.getMediaValue()) {
				case "pic":
					Intent intent = new Intent(mContext, CameraActivity.class);
					intent.putExtra("state", WYACameraView.BUTTON_STATE_ONLY_CAPTURE);
					intent.putExtra("duration", 1000);
					intent.putExtra("direction", pictureBean.isDirection());
					intent.putExtra("videoQuality", pictureBean.getVideoQuality());
					mContext.startActivityForResult(intent, CAMERA_PIC_REQUEST);
					break;
				case "video":
					Intent intent2 = new Intent(mContext, CameraActivity.class);
					intent2.putExtra("state", WYACameraView.BUTTON_STATE_ONLY_RECORDER);
					intent2.putExtra("duration", 10000);
					intent2.putExtra("direction", pictureBean.isDirection());
					intent2.putExtra("videoQuality", pictureBean.getVideoQuality());
					mContext.startActivityForResult(intent2, CAMERA_VIDEO_REQUEST);
					break;
				default:
					break;
			}
		} else {

			switch (pictureBean.getMediaValue()) {
				case "pic":
					ImagePickerCreator
						.create(mContext)
						.setMediaType(PickerConfig.MEDIA_IMAGE)
						.maxImages(1)
						.forResult(ALBUM_PIC_REQUEST);
					break;
				case "video":
					ImagePickerCreator
						.create(mContext)
						.setMediaType(PickerConfig.MEDIA_VIDEO)
						.maxImages(1)
						.forResult(ALBUM_VIDEO_REQUEST);
					break;
				default:
					break;
			}
		}

	}

	/**
	 * 保存图片
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void savePicture(String data, int id, String name) {
		mEventMap.put(name, id);
		mFileManagerUtil = new FileManagerUtil();
		mFileManagerUtil.getDownloadReceiver().
			load("http://pic43.nipic.com/20140711/19187786_140828149528_2.jpg").
			setFilePath(Environment.getExternalStorageDirectory().getPath() + "/Recordings/test.jpg").start();
		mFileManagerUtil.setOnDownLoaderListener(new FileManagerUtil.OnDownLoaderListener() {
			@Override
			public void onDownloadState(int state, DownloadTask task, Exception e) {
				if (state == TASK_COMPLETE) {
					setEmitData(1, "响应成功", null);
					send(name, getEmitData());
				}
			}
		});

	}

	/**
	 * 设置状态栏颜色
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void setStatusBarStyle(String data, int id, String name) {
		mEventMap.put(name, id);
		StatusBarUtil.setColor(mContext, mContext.getResources().getColor(R.color.black));
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 设置屏幕旋转方向
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void setScreenOrientation(String data, int id, String name) {
		mEventMap.put(name, id);
		mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 回到手机桌面
	 *
	 * @param data
	 * @param id
	 * @param name
	 */
	private void toLauncher(String data, int id, String name) {
		mEventMap.put(name, id);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		mContext.startActivity(intent);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * @param data
	 * @param id
	 * @param name
	 */
	private void openVideo(String data, int id, String name) {
		mEventMap.put(name, id);
		//本地视频暂不考虑
		String url = "https://vd1.bdstatic.com/mda-hgvt5nvfzpftdxcs/sc/mda-hgvt5nvfzpftdxcs.mp4";
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
		mContext.startActivity(mediaIntent);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 开始录音
	 */
	private void startRecording(String data, int id, String name) {
		mEventMap.put(name, id);
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		//设置封装格式
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/Recordings/test.amr");
		//设置编码格式
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
//			Log.e(TAG, "prepare() failed");
		}
		//录音
		mRecorder.start();

		RecordBean recordBean = new RecordBean();
		recordBean.setPath(Environment.getExternalStorageDirectory().getPath() + "/Recordings/test.amr");
		setEmitData(1, "响应成功", recordBean);
		send(name, getEmitData());
	}

	/**
	 * 停止录音
	 */
	private void stopRecording(String data, int id, String name) {
		if (mRecorder == null) {
			return;
		}
		mEventMap.put(name, id);
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		MediaPlayer mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/Recordings/test.amr");
			mediaPlayer.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int duration = mediaPlayer.getDuration();

		RecordBean recordBean = new RecordBean();
		recordBean.setPath(Environment.getExternalStorageDirectory().getPath() + "/Recordings/test.amr");
		recordBean.setDuration(duration);
		setEmitData(1, "响应成功", recordBean);
		send(name, getEmitData());
	}

	/**
	 * 停止播放本地音频
	 */
	private void stopPlay(String data, int id, String name) {
		if (mMediaPlayer == null) {
			return;
		}
		mEventMap.put(name, id);
		mMediaPlayer.stop();
		mMediaPlayer.release();
		mMediaPlayer = null;
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 播放本地音频
	 */
	private void startPlay(String data, int id, String name) {
		mEventMap.put(name, id);
		mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/Recordings/Honor.mp3");
			mMediaPlayer.prepareAsync();
			mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					mMediaPlayer.start();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 发送邮件
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void mail(String name, int id, String data) {
		mEventMap.put(name, id);
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setData(Uri.parse("mailto:"));
		intent.putExtra(Intent.EXTRA_SUBJECT, "");
		intent.putExtra(Intent.EXTRA_TEXT, "");
		mContext.startActivity(intent);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 发送短信
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void sms(String name, int id, String data) {
		mEventMap.put(name, id);
		sms = new Gson().fromJson(data, Sms.class);
		List<String> numbers = new ArrayList<>();
		numbers.add("10086");
		numbers.add("15858394228");
		sms.setNumbers(numbers);
		sms.setText("test");
		sms.setSilent(false);
		if (sms != null && sms.isSilent()) {
			// 后台直接发送
			for (int i = 0; i < sms.getNumbers().size(); i++) {
				// 获取短信管理器
				SmsManager smsManager = SmsManager.getDefault();
				// 拆分短信内容（手机短信长度限制）
				List<String> divideContents = smsManager.divideMessage(sms.getText());
				for (String text : divideContents) {
					smsManager.sendTextMessage(sms.getNumbers().get(i), null, text, null, null);
				}
			}
		} else if (sms != null) {
			// 调用系统的短信发送页面
			sendSms(sms);
			setEmitData(1, "响应成功", null);
			send(name, getEmitData());
		}
	}

	/**
	 * 调起系统发短信功能,多机型通用，兼容VIVO
	 *
	 * @param sms
	 */
	public void sendSms(Sms sms) {
		String phoneNumber = "";
		for (String response : sms.getNumbers()) {
			phoneNumber = phoneNumber + response + ";";
		}
		Intent smsIntent = new Intent(Intent.ACTION_VIEW);
		smsIntent.setData(Uri.parse("smsto:"));
		smsIntent.setType("vnd.android-dir/mms-sms");
		smsIntent.putExtra("address", phoneNumber);
		smsIntent.putExtra("sms_body", sms.getText());
		mContext.startActivity(smsIntent);
	}

	/**
	 * 取消通知
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void cancelNotification(String name, int id, String data) {
		mEventMap.put(name, id);
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(mContext, id, intent, 0);
		AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
		am.cancel(pi);
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 设置通知
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void notification(String name, int id, String data) {
		mEventMap.put(name, id);
		mNotificationData = new Gson().fromJson(data, NotificationData.class);
		mNotificationData.setVibrate(new long[]{0, 1300, 800, 300});
		mNotificationData.setSound("default");
		mNotificationData.setLight(false);
		mNotificationData.setTimestamp(System.currentTimeMillis() + 10000);
		Notify notify = new Notify();
		notify.setTitle(id + "");
		notify.setContent(mNotificationData.getTimestamp() + "");
		notify.setCover(true);
		mNotificationData.setNotify(notify);
		if (!NotificationsUtils.isNotificationEnabled(mContext)) {
			WYACustomDialog notificationDialog = new WYACustomDialog.Builder(mContext)
				.title("提示")
				.message("检测到您没有打开通知权限，是否去打开")
				.width(ScreenUtil.getScreenWidth(mContext) * 3 / 4)
				.build();
			notificationDialog.setNoClickListener(new WYACustomDialog.NoClickListener() {
				@Override
				public void onNoClick() {
					notificationDialog.dismiss();
				}
			});
			notificationDialog.setYesClickListener(new WYACustomDialog.YesClickListener() {
				@Override
				public void onYesClick() {
					Intent localIntent = new Intent();
					localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					if (Build.VERSION.SDK_INT >= 9) {
						localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
						localIntent.setData(Uri.fromParts("package", mContext.getPackageName(), null));
					} else if (Build.VERSION.SDK_INT <= 8) {
						localIntent.setAction(Intent.ACTION_VIEW);
						localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
						localIntent.putExtra("com.android.settings.ApplicationPkgName", mContext.getPackageName());
					}
					mContext.startActivity(localIntent);
				}
			});
			notificationDialog.show();
		} else {
			//AlarmReceiver.class为广播接受者
			Intent intent = new Intent(mContext, AlarmReceiver.class);
			intent.setAction("com.start");
			Bundle bundle = new Bundle();
			bundle.putInt("id", id);
			bundle.putString("title", mNotificationData.getNotify().getTitle());
			bundle.putString("content", mNotificationData.getNotify().getContent());
			bundle.putString("sound", mNotificationData.getSound());
			bundle.putBoolean("cover", mNotificationData.getNotify().isCover());
			bundle.putLongArray("vibrate", mNotificationData.getVibrate());
			intent.putExtras(bundle);
			PendingIntent pi = PendingIntent.getBroadcast(mContext, id, intent, 0);
			//得到AlarmManager实例
			AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
			/**
			 * 单次提醒
			 * mCalendar.getTimeInMillis() 上面设置的15点21分0秒的时间点
			 */
			alarmManager.set(AlarmManager.RTC_WAKEUP, mNotificationData.getTimestamp(), pi);
			NotificationEmit notificationEmit = new NotificationEmit();
			notificationEmit.setId(id);
			setEmitData(1, "响应成功", notificationEmit);
			send(name, getEmitData());
		}
	}

	/**
	 * `
	 * 获取剩余存储空间
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void getFreeDiskSpace(String name, int id, String data) {
		mEventMap.put(name, id);
		mCacheData = new Gson().fromJson(data, CacheData.class);
		mCacheData.setType("storageDir");
		if (mCacheData != null && mCacheData.getType() != null && !mCacheData.getType().equals("")) {
			long freeDiskSpace = 0;
			SpaceData freeDiskSpaceData = new SpaceData();
			switch (mCacheData.getType()) {
				case "dataDir":
					freeDiskSpace = getAvailableInternalMemorySize(mContext);
					freeDiskSpaceData.setSize(String.valueOf(freeDiskSpace));
					freeDiskSpaceData.setLabel(Formatter.formatFileSize(mContext, freeDiskSpace));
					setEmitData(1, "响应成功", freeDiskSpaceData);
					send(name, getEmitData());
					break;
				case "storageDir":
					if (isExternalStorageAvailable()) {
						freeDiskSpace = getAvailableExternalMemorySize(mContext);
						freeDiskSpaceData.setSize(String.valueOf(freeDiskSpace));
						freeDiskSpaceData.setLabel(Formatter.formatFileSize(mContext, freeDiskSpace));
						setEmitData(1, "响应成功", freeDiskSpaceData);
						send(name, getEmitData());
					} else {
						setEmitData(0, "SD卡异常", null);
						send(name, getEmitData());
					}
					break;
				case "total":
					freeDiskSpace = getAvailableExternalMemorySize(mContext) + getAvailableInternalMemorySize(mContext);
					freeDiskSpaceData.setSize(String.valueOf(freeDiskSpace));
					freeDiskSpaceData.setLabel(Formatter.formatFileSize(mContext, freeDiskSpace));
					setEmitData(1, "响应成功", freeDiskSpaceData);
					send(name, getEmitData());
					break;
				default:
					break;
			}
		}
	}

	/**
	 * 获取总存储空间大小
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void getTotalSpace(String name, int id, String data) {
		mEventMap.put(name, id);
		mEventMap.put(name, id);
		mCacheData = new Gson().fromJson(data, CacheData.class);
		mCacheData.setType("storageDir");
		if (mCacheData != null && mCacheData.getType() != null && !mCacheData.getType().equals("")) {
			long totalSpace = 0;
			SpaceData totalSpaceData = new SpaceData();
			switch (mCacheData.getType()) {
				case "dataDir":
					totalSpace = getInternalMemorySize(mContext);
					totalSpaceData.setSize(String.valueOf(totalSpace));
					totalSpaceData.setLabel(Formatter.formatFileSize(mContext, totalSpace));
					setEmitData(1, "响应成功", totalSpaceData);
					send(name, getEmitData());
					break;
				case "storageDir":
					if (isExternalStorageAvailable()) {
						totalSpace = getExternalMemorySize(mContext);
						totalSpaceData.setSize(String.valueOf(totalSpace));
						totalSpaceData.setLabel(Formatter.formatFileSize(mContext, totalSpace));
						setEmitData(1, "响应成功", totalSpaceData);
						send(name, getEmitData());
					} else {
						setEmitData(0, "SD卡异常", null);
						send(name, getEmitData());
					}
					break;
				case "total":
					totalSpace = getExternalMemorySize(mContext) + getInternalMemorySize(mContext);
					totalSpaceData.setSize(String.valueOf(totalSpace));
					totalSpaceData.setLabel(Formatter.formatFileSize(mContext, totalSpace));
					setEmitData(1, "响应成功", totalSpaceData);
					send(name, getEmitData());
					break;
				default:
					break;
			}
		}
	}

	/**
	 * 判断sd卡是否可用
	 */
	private boolean isExternalStorageAvailable() {
		return Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED);
	}

	/**
	 * 获取手机内部存储空间
	 *
	 * @param context
	 * @return 以B为单位的容量
	 */
	private long getInternalMemorySize(Context context) {
		File file = Environment.getDataDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long blockSizeLong = statFs.getBlockSizeLong();
		long blockCountLong = statFs.getBlockCountLong();
		long size = blockCountLong * blockSizeLong;
		return size;
	}

	/**
	 * 获取手机内部可用存储空间
	 *
	 * @param context
	 * @return 以B为单位的容量
	 */
	private long getAvailableInternalMemorySize(Context context) {
		File file = Environment.getDataDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long availableBlocksLong = statFs.getAvailableBlocksLong();
		long blockSizeLong = statFs.getBlockSizeLong();
		long size = availableBlocksLong * blockSizeLong;
		return size;
	}

	/**
	 * 获取手机外部存储空间
	 *
	 * @param context
	 * @return 以B为单位的容量
	 */
	public long getExternalMemorySize(Context context) {
		File file = Environment.getExternalStorageDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long blockSizeLong = statFs.getBlockSizeLong();
		long blockCountLong = statFs.getBlockCountLong();
		long size = blockSizeLong * blockCountLong;
		return size;
	}

	/**
	 * 获取手机外部可用存储空间
	 *
	 * @param context
	 * @return 以B单位的容量
	 */
	private long getAvailableExternalMemorySize(Context context) {
		File file = Environment.getExternalStorageDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long availableBlocksLong = statFs.getAvailableBlocksLong();
		long blockSizeLong = statFs.getBlockSizeLong();
		long size = blockSizeLong * availableBlocksLong;
		return size;
	}

	/**
	 * 获取缓存占用空间大小
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void getCacheSize(String name, int id, String data) {
		mEventMap.put(name, id);
		mCacheData = new Gson().fromJson(data, CacheData.class);
		mCacheData.setPath(fileRootPath + "/yzws");
		if (mCacheData != null && mCacheData.getPath() != null && !mCacheData.getPath().equals("")) {
			File file = new File(mCacheData.getPath());
			if (file.exists()) {
				long size = DataCleanUtil.getFolderSize(file);
				setEmitData(1, "响应成功", null);
				send(name, getEmitData());
			} else {
				setEmitData(0, "文件不存在", null);
				send(name, getEmitData());
			}
		}
	}

	/**
	 * 清理缓存
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void clearCache(String name, int id, String data) {
		mEventMap.put(name, id);
		mCacheData = new Gson().fromJson(data, CacheData.class);
		mCacheData.setPath(fileRootPath + "/yzws");
		if (mCacheData != null && mCacheData.getPath() != null && !mCacheData.getPath().equals("")) {
			DataCleanUtil.cleanCustomCache(mCacheData.getPath());
			setEmitData(1, "响应成功", null);
			send(name, getEmitData());
		}
	}

	/**
	 * 判断app是否下载
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void appInstalled(String name, int id, String data) {
		boolean isAppInstalled = false;
		mEventMap.put(name, id);
		mInstalledAppData = new Gson().fromJson(data, InstalledData.class);
		mInstalledAppData.setScheme("com.wya.shanda");
		if (mInstalledAppData != null && mInstalledAppData.getScheme() != null && !mInstalledAppData.getScheme().equals("")) {
			isAppInstalled = PhoneUtil.getInstance().isApkInstalled(mContext, mInstalledAppData.getScheme());
		}
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 打开app
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void openApp(String name, int id, String data) {
		mEventMap.put(name, id);
		mOpenAppData = new Gson().fromJson(data, OpenAppData.class);
		mOpenAppData.setScheme("com.wya.shanda");
		if (mOpenAppData != null && mOpenAppData.getScheme() != null && !mOpenAppData.getScheme().equals("")) {
			Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mOpenAppData.getScheme());
			if (intent != null) {
				mContext.startActivity(intent);
				setEmitData(1, "响应成功", null);
				send(name, getEmitData());
			} else {
				setEmitData(0, "未安装应用", null);
				send(name, getEmitData());
			}
		}
	}

	/**
	 * 下载app
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void installApp(String name, int id, String data) {
		mEventMap.put(name, id);
		mInstallAppData = new Gson().fromJson(data, InstallAppData.class);
		mInstallAppData.setUrl("https://oss.ruishan666.com/file/xcx/190219/560100273267/apprelease(1).apk");
		mFileManagerUtil = new FileManagerUtil();
		if (mInstallAppData != null && mInstallAppData.getUrl() != null && !mInstallAppData.getUrl().equals("")) {
			String fileName = mInstallAppData.getUrl().split("/")[mInstallAppData.getUrl().split("/").length - 1];
			mFileManagerUtil.getDownloadReceiver().load(mInstallAppData.getUrl()).setFilePath(fileRootPath + "/" + fileName).start();
			mFileManagerUtil.setOnDownLoaderListener(new FileManagerUtil.OnDownLoaderListener() {
				@Override
				public void onDownloadState(int state, DownloadTask task, Exception e) {
					if (state == TASK_COMPLETE) {
						installAPK(fileRootPath + "/" + fileName);
						setEmitData(1, "响应成功", null);
						send(name, getEmitData());
					}
				}
			});
		}
	}

	/**
	 * 下载到本地后执行安装
	 */
	protected void installAPK(String filePath) {
		File apkFile = new File(filePath);
		if (!apkFile.exists()) {
			return;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW);
		// 安装完成后，启动app（源码中少了这句话）
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.parse("file://" + apkFile.toString());
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		mContext.startActivity(intent);
	}

	/**
	 * 关闭到页面到某个界面
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void pop(String name, int id, String data) {
		mEventMap.put(name, id);
		mCloseWinData = new Gson().fromJson(data, CloseWinData.class);
		mCloseWinData.setName(winName);
		mCloseWinData.setAnimation("card");
		if (mCloseWinData != null && mCloseWinData.getName() != null && !mCloseWinData.getName().equals("")) {
			ActivityManager.getInstance().closeToWinByName(mCloseWinData.getName());
		} else {
			ActivityManager.getInstance().finishTopActivity();
		}
		setEmitData(1, "响应成功", null);
		send(name, getEmitData());
	}

	/**
	 * 打开新页面
	 *
	 * @param name
	 * @param id
	 * @param data
	 */
	private void push(String name, int id, String data) {
		mEventMap.put(name, id);
		mOpenWinData = new Gson().fromJson(data, OpenWinData.class);
		mOpenWinData.setTitle(winName);
		mOpenWinData.setName(winName);
		mOpenWinData.setHideBottomBar(false);
		mOpenWinData.setHideTopBar(true);
		mOpenWinData.sethScrollBarEnabled(false);
		mOpenWinData.setvScrollBarEnabled(false);
		mOpenWinData.setReplace(false);
		mOpenWinData.setUrl("https://wya-team.github.io/hybrid-sdk/html5/examples/dist/");
		mOpenWinData.setScaleEnabled(false);
		Intent intent = new Intent(mContext, OpenWinActivity.class);
		intent.putExtra("mOpenWinData", mOpenWinData);
		mContext.startActivity(intent);
		if (mOpenWinData.isReplace()) {
			mContext.finish();
		}
		send(name, getEmitData());
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case CAMERA_PIC_REQUEST:
				if (resultCode == RESULT_OK) {
					String path = data.getStringExtra("path");

					if (saveToPhotoAlbum) {
						final File file = new File(path);
						mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
					}
					ReturnPictureBean returnPictureBean = new ReturnPictureBean();
					ReturnPictureBean.PictureUrl pictureUrl = new ReturnPictureBean.PictureUrl();
					pictureUrl.setUrl(path);
					List<ReturnPictureBean.PictureUrl> list = new ArrayList<>();
					list.add(pictureUrl);
					returnPictureBean.setList(list);
					setEmitData(1, "响应成功", returnPictureBean);
					send("getPicture", getEmitData());
				}
				break;
			case CAMERA_VIDEO_REQUEST:
				if (resultCode == RESULT_OK) {
					String path = data.getStringExtra("url");
					if (saveToPhotoAlbum) {
						final File file = new File(path);
						mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
					}
					ReturnPictureBean returnPictureBean = new ReturnPictureBean();
					ReturnPictureBean.PictureUrl pictureUrl = new ReturnPictureBean.PictureUrl();
					pictureUrl.setUrl(path);
					MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
					metadataRetriever.setDataSource(path);
					String duration = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever
						.METADATA_KEY_DURATION);
					pictureUrl.setDuration(Long.parseLong(duration));
					metadataRetriever.release();
					List<ReturnPictureBean.PictureUrl> list = new ArrayList<>();
					list.add(pictureUrl);
					returnPictureBean.setList(list);
					setEmitData(1, "响应成功", returnPictureBean);
					send("getPicture", getEmitData());
				}
				break;
			case ALBUM_PIC_REQUEST:
				if (resultCode == RESULT_OK) {
					if (data != null && data.hasExtra(PickerConfig.IMAGE_SELECTED)) {
						Bundle extras = data.getExtras();
						ArrayList<String> lists = extras.getStringArrayList(PickerConfig.IMAGE_SELECTED);

						ReturnPictureBean returnPictureBean = new ReturnPictureBean();
						ReturnPictureBean.PictureUrl pictureUrl = new ReturnPictureBean.PictureUrl();
						List<ReturnPictureBean.PictureUrl> list = new ArrayList<>();
						for (String path : lists) {
							pictureUrl.setUrl(path);
							list.add(pictureUrl);
						}
						returnPictureBean.setList(list);
						setEmitData(1, "响应成功", returnPictureBean);
						send("getPicture", getEmitData());
					}
				}
				break;
			case ALBUM_VIDEO_REQUEST:
				if (resultCode == RESULT_OK) {
					if (data != null && data.hasExtra(PickerConfig.IMAGE_SELECTED)) {
						Bundle extras = data.getExtras();
						ArrayList<String> lists = extras.getStringArrayList(PickerConfig.IMAGE_SELECTED);

						ReturnPictureBean returnPictureBean = new ReturnPictureBean();
						ReturnPictureBean.PictureUrl pictureUrl = new ReturnPictureBean.PictureUrl();
						List<ReturnPictureBean.PictureUrl> list = new ArrayList<>();

						pictureUrl.setUrl(lists.get(0));
						list.add(pictureUrl);
						MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
						metadataRetriever.setDataSource(lists.get(0));
						String duration = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever
							.METADATA_KEY_DURATION);
						pictureUrl.setDuration(Long.parseLong(duration));
						metadataRetriever.release();
						returnPictureBean.setList(list);
						setEmitData(1, "响应成功", returnPictureBean);
						send("getPicture", getEmitData());
					}
				}
				break;
			default:
				break;
		}
	}
}
