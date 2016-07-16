package com.karmeleon.wearbatterymonitor;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by shawn on 7/13/16.
 */
public class BatteryInfoListenerService extends WearableListenerService {

	private static final String TAG = "BatteryInfoListener";
	private static final String DATA_ITEM_RECEIVED_PATH = "/battery_info";

	private BatteryManager mBatteryManager;

	private GoogleApiClient mGoogleApiClient;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "Started service");
		mBatteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "Destroyed service");
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		String nodeId = messageEvent.getSourceNodeId();

		if(mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(Wearable.API)
					.build();

			ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

			if (!connectionResult.isSuccess()) {
				Log.e(TAG, "Failed to connect to GoogleApiClient.");
				return;
			}
		}

		// Retrieve the battery info from Android

		JSONObject batteryInfo = new JSONObject();
		try {
			// Determine the power source
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = this.registerReceiver(null, ifilter);

			batteryInfo.put("source", batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1));
			batteryInfo.put("temperature", batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));
			batteryInfo.put("capacity", mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
			batteryInfo.put("current", mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000);
			batteryInfo.put("voltage", batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));

		} catch (JSONException e) {
			Log.e(TAG, e.getStackTrace().toString());
		}

		// Send the RPC
		Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, DATA_ITEM_RECEIVED_PATH, batteryInfo.toString().getBytes());
	}
}
