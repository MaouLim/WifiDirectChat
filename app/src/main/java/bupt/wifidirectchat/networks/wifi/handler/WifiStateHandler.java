package bupt.wifidirectchat.networks.wifi.handler;

/*
 * Created by Maou on 2017/7/5.
 */

public interface WifiStateHandler {

	void onStateChangeTo(boolean enable, Object sender);
}
