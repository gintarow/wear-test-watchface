package com.gintarow.wearable.pocketpikachuwatchface;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

public class WearLifeLogService extends Service implements SensorEventListener{

	private SQLiteDatabase db;
	private WearLifeLogSQLiteOpenHelper helper;

	SensorManager mSensorManager;
	Sensor sensorStepCounter;
	private boolean sensorState = false;

	static float prevStepCount;
	public static float todayStepCount;

	private SharedPreferences sharedPreferences;
	static final String KEY_PREFERENCES = "WearLifeLog";
	static final String KEY_PREF_TODAY_STEP_COUNT = "today_step_count";
	static final String KEY_PREF_STEP_COUNT_SINCE_YESTERDAY = "since_yesterday";
	static final int MSG_UPDATE_STEP = 0;
	static final int MSG_SAVE_DAILY_STEP = 1;

	static final long StepCountUpdateIntervalMs = 15 * 60 * 1000;

	Time time;
	static String date = "";
	long tmpTimeMs;


	final Handler LifeLogUpdater = new Handler(){
		@Override
		public void handleMessage(Message message){
			if(message!=null) {
				switch (message.what){
					case MSG_UPDATE_STEP:
						//15分おきに更新+sharedPreference書き込み
						//午前２時にリセット
						Log.d("Pika","update step count");
						updateStepCount();
						long timeMs = System.currentTimeMillis();
						long delayMs = StepCountUpdateIntervalMs - (timeMs % StepCountUpdateIntervalMs);
						LifeLogUpdater.sendEmptyMessageDelayed(MSG_UPDATE_STEP, delayMs);
						break;
					case MSG_SAVE_DAILY_STEP:
						//午前2時1分にsharedPreferenceからDBに記録
						break;
				}
			}else{
				return;
			}
		}
	};

	public WearLifeLogService() {
	}

	@Override
	public void onCreate(){
		super.onCreate();
		helper = new WearLifeLogSQLiteOpenHelper(getApplicationContext());
		db = helper.getWritableDatabase();

		sharedPreferences = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE);

		//センサー設定
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		sensorStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

		time = new Time("Asia/Tokyo");
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.d("Pika","LifeLogService onDestroy");
		LifeLogUpdater.removeMessages(MSG_UPDATE_STEP);
		if(sensorState){
			mSensorManager.unregisterListener(this);
			sensorState=false;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.d("Pika","life log service start");

		time.setToNow();
		date = time.year+String.format("%02d",time.month+1)+String.format("%02d",time.monthDay);
		prevStepCount = sharedPreferences.getInt(KEY_PREF_STEP_COUNT_SINCE_YESTERDAY,0);
		LifeLogUpdater.removeMessages(MSG_UPDATE_STEP);
		LifeLogUpdater.sendEmptyMessage(MSG_UPDATE_STEP);

		return START_NOT_STICKY;		//再起動しない
	}

	public void updateStepCount(){
		tmpTimeMs =  System.currentTimeMillis();
		mSensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
		sensorState=true;
	}

	//1日分の歩数をDBに記録
	public void saveDailyStepCount(){
		SharedPreferences sp = getSharedPreferences(KEY_PREFERENCES,Context.MODE_PRIVATE);
		int today = sp.getInt(KEY_PREF_TODAY_STEP_COUNT, 0);
		db.execSQL("insert into "+WearLifeLogSQLiteOpenHelper.TABLE_NAME
				+"(date, step_count) values('"+date+"', "+today+");");
		db.execSQL("insert into startAppList(name,pkg_name,conf) values ('アプリ１', 'com.pioneer...', 1);");
		//新しい日にち
		time.setToNow();
		date = time.year+String.format("%02d",time.month+1)+String.format("%02d",time.monthDay);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
			Log.d("Pika", "stepCounter:" + event.values[0]);
//			prevStepCount = event.values[0];
			if(sensorState) {
				mSensorManager.unregisterListener(this, sensorStepCounter);
				todayStepCount += (event.values[0] - prevStepCount);	//差分を追加
				if(todayStepCount<0){	//電源OFFでtotalが0になった場合
					todayStepCount=0;
				}
				//SharedPreferenceに記録
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putInt(KEY_PREF_TODAY_STEP_COUNT, (int) todayStepCount);
//				editor.putString(KEY_PREF_DATE,time.year+String.format("%02d",time.month+1)+String.format("%02d",time.monthDay));
				editor.apply();
				prevStepCount = event.values[0];

				//午前2時にリセット
				int TimeMin = (int)tmpTimeMs/(1000*60);
				int dayAmari = TimeMin%(60*24);
				if(dayAmari==120){	//2:00:00~2:00:59
//				if(dayAmari==0){	//0:00:00~0:00:59
					todayStepCount = 0;
					editor.putInt(KEY_PREF_STEP_COUNT_SINCE_YESTERDAY,(int)event.values[0]).apply();
					saveDailyStepCount();	//DBに記録
					Log.d("Pika","AM2:00 reset");
				}
				sensorState=false;
				Log.d("Pika","Sensor unregistered [sensorChanged] today's step:"+todayStepCount);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
