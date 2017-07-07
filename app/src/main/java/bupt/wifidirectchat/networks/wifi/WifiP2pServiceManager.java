package bupt.wifidirectchat.networks.wifi;

/*
 * Created by Maou on 2017/7/5.
 */

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import bupt.wifidirectchat.networks.wifi.handler.ResultListener;
import bupt.wifidirectchat.networks.wifi.handler.SameServiceFoundHandler;

public class WifiP2pServiceManager {

	public static final String TAG              = "WifiP2pServiceManager";
	public static final String TXT_RECORD_PROP  = "available";
	public static final String SERVICE_INSTANCE = "_my_wifi_p2p_app";
	public static final String SERVICE_REG_TYPE = "_presence._tcp";

	private String                     instanceName =  SERVICE_INSTANCE;
	private WifiP2pManager             manager      = null;
	private WifiP2pManager.Channel     channel      = null;
	private WifiP2pDnsSdServiceRequest request      = null;

	public WifiP2pServiceManager(Context context,
	                             WifiP2pManager manager,
	                             String instanceName) {
		this.manager = manager;
		this.channel = manager.initialize(context, context.getMainLooper(), null);
		this.instanceName = instanceName;
	}

	public WifiP2pServiceManager(Context context, WifiP2pManager manager) {
		this(context, manager, SERVICE_INSTANCE);
	}

	public void registerService(final ResultListener listener) {
		/* register self */
		Map<String, String> record = new HashMap<String, String>();
		record.put(TXT_RECORD_PROP, "visible");

		WifiP2pDnsSdServiceInfo info = WifiP2pDnsSdServiceInfo.newInstance(
			SERVICE_INSTANCE, SERVICE_REG_TYPE, record
		);

		manager.addLocalService(channel, info, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				listener.onSuccess(WifiP2pServiceManager.this);
			}

			@Override
			public void onFailure(int reason) {
				listener.onFailure(reason, WifiP2pServiceManager.this);
			}
		});
	}

	public void discoverServices(final SameServiceFoundHandler handler) {
		manager.setDnsSdResponseListeners(
				channel,
				new WifiP2pManager.DnsSdServiceResponseListener() {
					@Override
					public void onDnsSdServiceAvailable(String instanceName,
					                                    String registrationType,
					                                    WifiP2pDevice srcDevice) {
						if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
							handler.onFound(registrationType, srcDevice, WifiP2pServiceManager.this);
						}
					}
				},
				new WifiP2pManager.DnsSdTxtRecordListener() {
					@Override
					public void onDnsSdTxtRecordAvailable(String fullDomainName,
					                                      Map<String, String> txtRecordMap,
					                                      WifiP2pDevice srcDevice) {
						Log.d(TAG, srcDevice.deviceName + ": " + txtRecordMap.get(TXT_RECORD_PROP));
					}
				}
		);

		request = WifiP2pDnsSdServiceRequest.newInstance();
		manager.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "in discover: addServiceRequest success.");
			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "in discover: addServiceRequest failed, errorCode: " + reason);
			}
		});
		manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "in discover: discoverServices start.");
			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "in discover: discoverServices failed to start.");
			}
		});
	}

	public void initDiscover() {
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "in initDiscover: initDiscover success.");
//				manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
//					@Override
//					public void onPeersAvailable(WifiP2pDeviceList peers) {
//						Log.e(TAG, "PeersAvailable" + peers.getDeviceList().size());
//					}
//				});

			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "in initDiscover: initDiscover failed, errorCode: " + reason);
			}
		});
	}

	public void requestPeerList(final WifiP2pManager.PeerListListener listener) {
		manager.requestPeers(channel, listener);
	}

	public void connect(final WifiP2pDevice device, final ResultListener listener) {

		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;

		if (null != request) {
			manager.removeServiceRequest(
				channel,
				request,
				new WifiP2pManager.ActionListener() {
					@Override
					public void onSuccess() {
						Log.e(TAG, "in connect: removeServiceRequest success.");
					}

					@Override
					public void onFailure(int reason) {
						Log.e(TAG, "in connect: removeServiceRequest failed, errorCode: " + reason);
					}
				}
			);

			request = null;
		}

		manager.connect(
			channel,
			config,
			new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				listener.onSuccess(WifiP2pServiceManager.this);
			}

			@Override
			public void onFailure(int reason) {
				listener.onFailure(reason, WifiP2pServiceManager.this);
			}
		}
		);
	}

	public void disconnect() {
		manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "in disconnect: cancelConnect success.");
			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "in disconnect: cancelConnect failed, errorCode: " + reason);
			}
		});
	}

	public void requestConnectionInfo(final WifiP2pManager.ConnectionInfoListener listener) {
		manager.requestConnectionInfo(channel, listener);
	}

	public void cancelDiscover() {
		manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "in cancelDiscover: stopPeerDiscovery success.");
			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "in cancelDiscover: stopPeerDiscovery failed, errorCode: " + reason);
			}
		});
	}

	public String getInstanceName() {
		return instanceName;
	}

	public WifiP2pManager getManager() {
		return manager;
	}

	public WifiP2pManager.Channel getChannel() {
		return channel;
	}
}
