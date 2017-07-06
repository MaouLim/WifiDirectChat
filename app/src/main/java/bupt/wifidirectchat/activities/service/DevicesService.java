package bupt.wifidirectchat.activities.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

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

/**
 * Created by Maou on 2017/7/6.
 */

public class DevicesService extends Service {


	IBinder binder = new MBinder();

	public class MBinder extends Binder {

		DevicesService getService(){
			return DevicesService.this;
		}

	}

	Context context;

	List<pair> pairs = new ArrayList<>();
	Collection<WifiP2pDevice> devices;

	Communicator communicator = null;



	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
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

	public void setDeviceListener(DeviceListener dl){
		this.dl = dl;
	}

	WifiP2pServiceManager wpsm;

	public void initManager(Context context, WifiP2pManager wpm) {
		this.context = context;
		wpsm = new WifiP2pServiceManager(context, wpm);
	}

	public void searchDevices() {
		pairs.clear();
		devices.clear();
		wpsm.initDiscover();

	}

	public interface ConnectListener{
		void onConnectToSuccess(pair _pair);
		void onConnectToFail(pair _pair);
	}
	ConnectListener connectListener;





	public void conectToDevice(final String deviceAddress, ConnectListener _cl) {
		connectListener = _cl;

		assert wpsm != null;
		WifiP2pDevice wifiP2pDevice = null;
		for (WifiP2pDevice wpd : devices) {
			if (wpd.deviceAddress.equals(deviceAddress)) {
				wifiP2pDevice = wpd;
				break;
			}
		}

		final pair _pair = new pair(wifiP2pDevice.deviceName, wifiP2pDevice.deviceAddress);
		wpsm.connect(wifiP2pDevice, new ResultListener() {
			@Override
			public void onSuccess(Object sender) {
				if(connectListener != null) {
					connectListener.onConnectToSuccess(_pair);
				}


			}

			@Override
			public void onFailure(int errorCode, Object sender) {
				if(connectListener != null) {
					connectListener.onConnectToFail(_pair);
				}
			}
		});
	}


	public interface Messages{
		void onMessageArrived(String message);
	}
	Messages messagesListener;

	public void setMessagesListener(Messages messagesListener){
		this.messagesListener = messagesListener;
	}


	public void startTCPConnect(InetAddress ip){
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

					}
				};
			}
		});
	}

	public void startTCPListener(){
		TCPHelper.startListener(new Listener(1) {
			@Override
			public void handleConnectionEstablished(Socket socket, Object sender) {
				communicator = new Communicator(socket) {
					@Override
					public void handleMessageArrived(String content, Object sender) {
						messagesListener.onMessageArrived(content);
					}

					@Override
					public void handleConnectionReset(Throwable throwable, Object sender) {

					}
				};
			}
		});
	}

	public void stopTCPConnect(){
		communicator.close();
	}

	public void sendMessage(String content){
		communicator.send(content);
	}


	public interface P2PConnectStatu{
		void onConnectStopped();
		void onConnected();
	}
	P2PConnectStatu p2PConnectStatu;

	public void setP2PConnectStatu(P2PConnectStatu p2PConnectStatu){
		this.p2PConnectStatu = p2PConnectStatu;
	}


	public class P2PListener extends WifiP2pStateListener{

		@Override
		public void onStateChangeTo(boolean enable, Object sender) {

		}

		@Override
		public void onPeersListChanged(Object sender) {
			wpsm.requestPeerList(new WifiP2pManager.PeerListListener() {
				@Override
				public void onPeersAvailable(WifiP2pDeviceList peers) {
					devices.clear();
					pairs.clear();
					devices = peers.getDeviceList();
					for(WifiP2pDevice wd: devices){
						pairs.add(new pair(wd.deviceName, wd.deviceAddress));
					}
					dl.onDeviceDiscover(pairs);
				}
			});

		}

		@Override
		public void onConnectionChanged(NetworkInfo info, Object sender) {
			if(info.isConnected()){
				p2PConnectStatu.onConnected();
				wpsm.requestConnectionInfo(new WifiP2pManager.ConnectionInfoListener() {
					@Override
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						InetAddress ip = info.groupOwnerAddress;
						if(info.isGroupOwner) {
							startTCPConnect(ip);
						}else {
							startTCPListener();
						}

					}
				});
			}else {
				p2PConnectStatu.onConnectStopped();
			}

		}

		@Override
		public void onDeviceChanged(WifiP2pDevice device, Object sender) {

		}
	}

	public void cancleDiscover(){
		wpsm.cancelDiscover();
	}



}
