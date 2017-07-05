package bupt.wifidirectchat.service.wifi.handler;

/*
 * Created by Maou on 2017/7/5.
 */

import android.net.NetworkInfo;

public interface ConnectionChangedHandler {

	void onConnectionChanged(NetworkInfo info, Object sender);
}
