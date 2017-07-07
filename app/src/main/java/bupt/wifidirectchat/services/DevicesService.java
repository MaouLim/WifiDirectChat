package bupt.wifidirectchat.services;

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

import bupt.wifidirectchat.activities.adapters.Pair;
import bupt.wifidirectchat.networks.tcp.Communicator;
import bupt.wifidirectchat.networks.tcp.Connector;
import bupt.wifidirectchat.networks.tcp.Listener;
import bupt.wifidirectchat.networks.tcp.TCPHelper;
import bupt.wifidirectchat.networks.wifi.WifiP2pServiceManager;
import bupt.wifidirectchat.networks.wifi.WifiP2pStateListener;
import bupt.wifidirectchat.networks.wifi.handler.ResultListener;

/*
 * Created by Liu Cong on 2017/7/6.
 */

public class DevicesService extends Service {

	public static String TAG = "DevicesService";

	private IBinder binder = new MBinder();
	private IntentFilter intentFilter = null;

	public class MBinder extends Binder {

		public DevicesService getService() {
			return DevicesService.this;
		}
	}

	private Context context;
	private Collection<WifiP2pDevice> devices = new ArrayList<>();
	private List<Pair> pairs = new ArrayList<>();
	private Communicator communicator = null;

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
		void onDeviceDiscover(List<Pair> devices);
	}

	private DeviceListener deviceListener;

	public void setDeviceListener(DeviceListener deviceListener) {
		this.deviceListener = deviceListener;
	}

	private WifiP2pServiceManager serviceManager;

	public void initManager(Context context, WifiP2pManager wpm) {
		this.context = context;
		this.serviceManager = new WifiP2pServiceManager(context, wpm);
	}

	public void searchDevices() {
		pairs.clear();
		devices.clear();
		serviceManager.initDiscover();
		Log.e(TAG, TAG + " searchDevices");
	}

	public interface ConnectListener {

		void onConnectToSuccess(Pair pair);
		void onConnectToFail(Pair pair);
	}

	ConnectListener connectListener;

	public void connectToDevice(final String deviceAddress, ConnectListener listener) {
		connectListener = listener;
		Log.e("ConnectDevice", " Before Loop");
		assert serviceManager != null;
		WifiP2pDevice wifiP2pDevice = null;
		for (WifiP2pDevice wpd : devices) {
			if (wpd.deviceAddress.equals(deviceAddress)) {
				wifiP2pDevice = wpd;
				break;
			}
		}

		final Pair pair = new Pair(wifiP2pDevice.deviceName, wifiP2pDevice.deviceAddress);
		Log.e("ConnectDevice", " Begin Connect to" + wifiP2pDevice.deviceAddress);

		serviceManager.connect(wifiP2pDevice, new ResultListener() {
			@Override
			public void onSuccess(Object sender) {
				Log.e(TAG, TAG + " serviceManager.connect success " + connectListener);

				if (connectListener != null) {
					connectListener.onConnectToSuccess(pair);
				}
			}

			@Override
			public void onFailure(int errorCode, Object sender) {
				Log.e(TAG, TAG + " serviceManager.connect failed");

				if (connectListener != null) {
					connectListener.onConnectToFail(pair);
				}
			}
		});
	}

	public interface Messages {
		void onMessageArrived(String message);
	}

	private Messages messagesListener;

	public void setMessagesListener(Messages messagesListener) {
		this.messagesListener = messagesListener;
	}

	public void startTCPConnect(InetAddress address) {
		TCPHelper.startConnector(new Connector(address) {

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
			serviceManager.requestPeerList(new WifiP2pManager.PeerListListener() {
				@Override
				public void onPeersAvailable(WifiP2pDeviceList peers) {
					pairs.clear();
					devices = peers.getDeviceList();

					Log.e(TAG, TAG + " onPeersAvailable " + devices.size());

					for (WifiP2pDevice wd : devices) {
						pairs.add(new Pair(wd.deviceName, wd.deviceAddress));
					}
					deviceListener.onDeviceDiscover(pairs);
					Log.e(TAG, pairs.size() + " ");
				}
			});
		}

		@Override
		public void onConnectionChanged(NetworkInfo info, Object sender) {
			Log.e("Listener", "ConnectionChanged " + info.isConnected());

			if (info.isConnected()) {
				p2PConnectStatus.onConnected();
				serviceManager.requestConnectionInfo(new WifiP2pManager.ConnectionInfoListener() {
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
		serviceManager.cancelDiscover();
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
