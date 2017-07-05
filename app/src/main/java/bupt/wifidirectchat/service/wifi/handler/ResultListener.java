package bupt.wifidirectchat.service.wifi.handler;

/*
 * Created by Maou on 2017/7/5.
 */

public interface ResultListener {

	void onSuccess(Object sender);

	void onFailure(int errorCode, Object sender);
}
