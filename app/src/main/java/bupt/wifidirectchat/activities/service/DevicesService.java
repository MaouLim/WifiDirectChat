package bupt.wifidirectchat.activities.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import bupt.wifidirectchat.adapter.pair;
import bupt.wifidirectchat.service.tcp.Communicator;
import bupt.wifidirectchat.service.tcp.Connector;
import bupt.wifidirectchat.service.tcp.Listener;
import bupt.wifidirectchat.service.tcp.TCPHelper;
import bupt.wifidirectchat.service.wifi.WifiP2pServiceManager;
import bupt.wifidirectchat.service.wifi.WifiP2pStateListener;
import bupt.wifidirectchat.service.wifi.handler.ResultListener;

/*
 * Created by Liu Cong on 2017/7/6.
 */

public class DevicesService extends Service {

	public static String TAG = "DevicesService";

	IBinder binder = new MBinder();
	private IntentFilter intentFilter = null;

	public class MBinder extends Binder {

		public DevicesService getService() {
			return DevicesService.this;
		}
	}

	Context context;
	Collection<WifiP2pDevice> devices = new ArrayList<>();
	List<pair> pairs = new ArrayList<>();
	Communicator communicator = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		setupIntentFilter();

		Log.e(TAG, TAG + " onStartCommand");

		return super.onStartCommand(intent, flags, startId);
	}

	public void pauseListener(){
		unregisterReceiver(p2PListener);

	}

	public void startListener(){
		registerReceiver(p2PListener, intentFilter);
		Log.e(TAG, TAG + " startListener");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public interface DeviceListener {
		void onDeviceDiscover(List<pair> devices);
	}

	DeviceListener dl;

	public void setDeviceListener(DeviceListener dl) {
		this.dl = dl;
	}

	WifiP2pServiceManager wpsm;

	public void initManager(Context context, WifiP2pManager wpm) {
		this.context = context;
		this.wpsm = new WifiP2pServiceManager(context, wpm);
	}

	public void searchDevices() {
		pairs.clear();
		devices.clear();
		wpsm.initDiscover();
		Log.e(TAG, TAG + " searchDevices");
	}

	public interface ConnectListener {

		void onConnectToSuccess(pair _pair);
		void onConnectToFail(pair _pair);
	}

	ConnectListener connectListener;

	public void connectToDevice(final String deviceAddress, ConnectListener _cl) {
		connectListener = _cl;
		Log.e("ConnectDevice", " Before Loop");
		assert wpsm != null;
		WifiP2pDevice wifiP2pDevice = null;
		for (WifiP2pDevice wpd : devices) {
			if (wpd.deviceAddress.equals(deviceAddress)) {
				wifiP2pDevice = wpd;
				break;
			}
		}

		final pair _pair = new pair(wifiP2pDevice.deviceName, wifiP2pDevice.deviceAddress);
		Log.e("ConnectDevice", " Begin Connect to" + wifiP2pDevice.deviceAddress);

		wpsm.connect(wifiP2pDevice, new ResultListener() {
			@Override
			public void onSuccess(Object sender) {
				Log.e(TAG, TAG + " wpsm.connect success " + connectListener);

				if (connectListener != null) {
					connectListener.onConnectToSuccess(_pair);
				}
			}

			@Override
			public void onFailure(int errorCode, Object sender) {
				Log.e(TAG, TAG + " wpsm.connect failed");

				if (connectListener != null) {
					connectListener.onConnectToFail(_pair);
				}
			}
		});
	}

	public interface Messages {
		void onMessageArrived(String message);
	}

	Messages messagesListener;

	public void setMessagesListener(Messages messagesListener) {
		this.messagesListener = messagesListener;
	}

	public void startTCPConnect(InetAddress ip) {
		TCPHelper.startConnector(new Connector(ip) {
			@Override
			public void handleConnectionEstablished(Socket socket, Object sender) {
				communicator = new Communicator(socket) {
					@Override
					public void handleMessageArrived(String content, Object sender) {
						messagesListener.onMessageArrived(content);
					}

					@Override
					public void handleConnectionReset(Throwable throwable, Object sender) {
						// empty
					}
				};
				TCPHelper.startCommunicator(communicator);
			}
		});
	}

	public void startTCPListener(InetAddress address) {
		TCPHelper.startListener(new Listener(address, 1) {
			@Override
			public void handleConnectionEstablished(Socket socket, Object sender) {
				communicator = new Communicator(socket) {
					@Override
					public void handleMessageArrived(String content, Object sender) {
						messagesListener.onMessageArrived(content);
					}

					@Override
					public void handleConnectionReset(Throwable throwable, Object sender) {
						// empty
					}
				};
				TCPHelper.startCommunicator(communicator);
			}
		});
	}

	public void stopTCPConnect() {
		communicator.close();
	}

	public void sendMessage(String content) {
		communicator.send(content);
	}


	public interface P2PConnectStatus {
		void onConnectStopped();

		void onConnected();
	}

	P2PConnectStatus p2PConnectStatus;

	public void setP2PConnectStatus(P2PConnectStatus p2PConnectStatus) {
		this.p2PConnectStatus = p2PConnectStatus;
	}

	P2PListener p2PListener = new P2PListener();


	public class P2PListener extends WifiP2pStateListener {

		@Override
		public void onStateChangeTo(boolean enable, Object sender) { }

		@Override
		public void onPeersListChanged(Object sender) {
			wpsm.requestPeerList(new WifiP2pManager.PeerListListener() {
				@Override
				public void onPeersAvailable(WifiP2pDeviceList peers) {
					pairs.clear();
					devices = peers.getDeviceList();

					Log.e(TAG, TAG + " onPeersAvailable " + devices.size());

					for (WifiP2pDevice wd : devices) {
						pairs.add(new pair(wd.deviceName, wd.deviceAddress));
					}
					dl.onDeviceDiscover(pairs);
					Log.e(TAG, pairs.size() + " ");
				}
			});
		}

		@Override
		public void onConnectionChanged(NetworkInfo info, Object sender) {
			Log.e("Listener", "ConnectionChanged " + info.isConnected());

			if (info.isConnected()) {
				p2PConnectStatus.onConnected();
				wpsm.requestConnectionInfo(new WifiP2pManager.ConnectionInfoListener() {
					@Override
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						InetAddress ip = info.groupOwnerAddress;
						if (info.isGroupOwner) {
							startTCPListener(ip);
						}
						else {
							Log.e(TAG, "try to connect to " + ip.toString());
							startTCPConnect(ip);
						}
					}
				});
			}
			else {
				p2PConnectStatus.onConnectStopped();
			}
		}

		@Override
		public void onDeviceChanged(WifiP2pDevice device, Object sender) { }
	}

	public void cancelDiscover() {
		wpsm.cancelDiscover();
	}

	private void setupIntentFilter() {
		if (null == intentFilter) {
			intentFilter = new IntentFilter();
		}

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	}
}
