package edu.berkeley.cs.amplab.carat.android.sampling;

import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.storage.SampleDB;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Boolean3;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.thrift.BatteryDetails;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

public class SamplerService extends IntentService {
    private static final String TAG = "SamplerService";
	private static final long DUPLICATE_INTERVAL = 1000;
	private AlarmManager alarmManager;
	private Intent receiver;
    
    public SamplerService() {
        super(TAG);
    }
    
    @SuppressLint("CommitPrefEdits")
	@Override
    protected void onHandleIntent(Intent intent) {
    	Sampler sampler = Sampler.getInstance();

		// At this point SimpleWakefulReceiver is still holding a wake lock
		// for us. We can do whatever we need to here and then tell it that
		// it can release the wakelock. This sample just does some slow work,
		// but more complicated implementations could take their own wake
		// lock here before releasing the receiver's.
		//
		// Note that when using this approach you should be aware that if your
		// service gets killed and restarted while in the middle of such work
		// (so the Intent gets re-delivered to perform the work again), it will
		// at that point no longer be holding a wake lock since we are depending
		// on SimpleWakefulReceiver to that for us. If this is a concern, you
		// can acquire a separate wake lock here.
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wl.acquire();

		Context context = getApplicationContext();
		alarmManager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		receiver = new Intent(this, SamplerService.class);
		receiver.setAction(Constants.RAPID_SAMPLING);

		String action = intent.getAction();
		switch (action) {
			case Intent.ACTION_BATTERY_CHANGED:
				Boolean3 charging = BatteryUtils.isCharging(intent);
				if (charging == Boolean3.YES && !BatteryUtils.isFull(intent)) {
					startRapidSampling(context);
				} else if (charging == Boolean3.NO) {
					stopRapidSampling(context);
				}

				if (batteryLevelChanged(intent, context)) {
					sample(intent.getAction(), context);
				} else if(isRapidSampling(context)){
					sample(Constants.RAPID_SAMPLING, context);
				}
				break;
			case Constants.RAPID_SAMPLING:
				Intent check = context
						.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				if(check == null) break;
				if (BatteryUtils.isCharging(check) == Boolean3.NO || BatteryUtils.isFull(check)) {
					Logger.d(TAG, "User stopped charging or battery was full");
					stopRapidSampling(context);
				} else {
					this.sample(Constants.RAPID_SAMPLING, context);
				}
				break;
			default:
				Logger.d(TAG, "Creating sample after receiving " + action);
				sample(action, context);
				break;
		}
        wl.release();
        if (sampler != null){
			Sampler.completeWakefulIntent(intent);
		}
    }

	private void startRapidSampling(Context context){
		if(!isRapidSampling(context)){
			PendingIntent rapidSampling = PendingIntent.getService(context, 0, receiver, 0);
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(),
					TimeUnit.SECONDS.toMillis(60), rapidSampling);
			Logger.d(TAG, "Started rapid sampling!");
		}
	}

	private void stopRapidSampling(Context context){
		if(isRapidSampling(context)){
			PendingIntent rapidSampling = PendingIntent.getService(context, 0, receiver, 0);
			rapidSampling.cancel();
			alarmManager.cancel(rapidSampling);
			Logger.d(TAG, "Stopped rapid sampling!");
		}
	}

	private boolean isRapidSampling(Context context){
		int peekFlag = PendingIntent.FLAG_NO_CREATE;
		return PendingIntent.getService(context, 0, receiver, peekFlag) != null;
	}

	private void sample(String action, Context context){
		Logger.d(TAG, "New sample candidate for " + action + "!");

		SampleDB sampleDB = SampleDB.getInstance(context);
		Sample lastSample = sampleDB.getLastSample(context);

		String lastBatteryState = lastSample != null ? lastSample.getBatteryState() : "Unknown";
		Sample sample = SamplingLibrary.sample(context, action, lastBatteryState);
		if(sample == null || nothingChanged(sample)){
			Logger.d(TAG, "Skipped a duplicate or null sample!");
			return;
		}
		sample.setDistanceTraveled(Sampler.getInstance().getDistanceSinceLastSample());
		Sampler.getInstance().setLastSample(sample);
		long id = sampleDB.putSample(sample);
		notifyIfNeeded(context);
		Logger.i(TAG, "Took sample " + id + " for " + action + ".");
	}

	private boolean nothingChanged(Sample s1){
		Sample s2 = Sampler.getInstance().getLastSample();
		if(s2 != null && s2.getTriggeredBy().equals(s1.getTriggeredBy())){
			if(s1.getTimestamp() - s2.getTimestamp() < 1){
				Logger.d(TAG, "Sample was triggered within 1 second (diff: " +
						(s1.getTimestamp() - s2.getTimestamp()) + " s) of the last one " +
						"and by the same event. Checking if it's a duplicate..");
				BatteryDetails bd1 = s1.getBatteryDetails();
				BatteryDetails bd2 = s2.getBatteryDetails();
				boolean isDuplicate =
							s1.getBatteryLevel() == s2.getBatteryLevel()
						&& 	s1.getBatteryState().equals(s2.getBatteryState())
						&& 	s1.getTimeZone().equals(s2.getTimeZone())
						&& 	bd1.getBatteryTemperature() == bd2.getBatteryTemperature()
						&& 	bd1.getBatteryCapacity() == bd2.getBatteryCapacity()
						&& 	bd1.getBatteryVoltage() == bd2.getBatteryVoltage()
						&& 	bd1.getBatteryTechnology().equals(bd2.getBatteryTechnology())
						&& 	bd1.getBatteryCharger().equals(bd2.getBatteryCharger())
						&& 	bd1.getBatteryHealth().equals(bd2.getBatteryHealth());
				Logger.d(TAG, isDuplicate ? "Discarding as a duplicate.." :
											"Not a duplicate, proceeding..");
				return isDuplicate;
			}
		}
		return false;
	}

	private boolean batteryLevelChanged(Intent intent, Context context){
		double batteryLevel = BatteryUtils.getBatteryLevel(intent)/100;
		if(batteryLevel <= 0){
			Logger.d(TAG, "Battery level was zero or negative");
			return false;
		}
		double lastBatteryLevel = SamplingLibrary.getLastSampledBatteryLevel(context);
		return batteryLevel != lastBatteryLevel;
	}
    
    private void notifyIfNeeded(Context context){
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean disableNotifications = p.getBoolean("noNotifications", false);
		if(disableNotifications){
			return;
		}

        long now = System.currentTimeMillis();
        long lastNotify = Sampler.getInstance().getLastNotify();
        
        // Do not notify if it is less than 2 days from last notification
        if (lastNotify + Constants.FRESHNESS_TIMEOUT_QUICKHOGS > now)
            return;
        
        int samples = SampleDB.getInstance(context).countSamples();
        if (samples >= Sampler.MAX_SAMPLES){
            Sampler.getInstance().setLastNotify(now);
        PendingIntent launchCarat = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context)
                .setSmallIcon(R.drawable.carat_notif_icon)
                .setContentTitle("Please open Carat")
                .setContentText("Please open Carat. Samples to send:")
                .setNumber(samples);
        mBuilder.setContentIntent(launchCarat);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
        }
    }
}
