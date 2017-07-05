package bupt.wifidirectchat.service.wifi.handler;

/*
 * Created by Maou on 2017/7/5.
 */

import android.net.wifi.p2p.WifiP2pDevice;

public interface DeviceChangedHandler {

	void onDeviceChanged(WifiP2pDevice device, Object sender);
}
