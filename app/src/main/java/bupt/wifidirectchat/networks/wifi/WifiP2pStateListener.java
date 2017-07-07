package bupt.wifidirectchat.networks.wifi;

/*
 * Created by Maou on 2017/7/5.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import bupt.wifidirectchat.networks.wifi.handler.ConnectionChangedHandler;
import bupt.wifidirectchat.networks.wifi.handler.DeviceChangedHandler;
import bupt.wifidirectchat.networks.wifi.handler.PeersListChangedHandler;
import bupt.wifidirectchat.networks.wifi.handler.WifiStateHandler;

public abstract class WifiP2pStateListener
		extends BroadcastReceiver
		implements WifiStateHandler,
		           PeersListChangedHandler,
		           ConnectionChangedHandler,
				   DeviceChangedHandler {

	public static final String TAG = "WifiP2pStateListener";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.e(TAG, TAG + " onReceive " + action);

		switch (action) {
			case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION : {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				onStateChangeTo(
					WifiP2pManager.WIFI_P2P_STATE_ENABLED == state,
					WifiP2pStateListener.this
				);
				return;
			}

			case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION : {
				onPeersListChanged(WifiP2pStateListener.this);
				return;
			}

			case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION : {
				NetworkInfo info =
						(NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				Log.e(TAG, "current state: " + info.isConnected());
				onConnectionChanged(info, WifiP2pStateListener.this);
				return;
			}

			case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION : {
				WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
				onDeviceChanged(device, WifiP2pStateListener.this);
				return;
			}
		}
	}
}
