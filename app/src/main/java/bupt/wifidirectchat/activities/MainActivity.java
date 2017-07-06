package bupt.wifidirectchat.activities;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import bupt.wifidirectchat.R;
import bupt.wifidirectchat.activities.service.DevicesService;
import bupt.wifidirectchat.adapter.ListAdapter;
import bupt.wifidirectchat.adapter.pair;

/*
 * Created by Liu Cong on 2017/7/6.
 */

public class MainActivity extends AppCompatActivity {

	DevicesService binder = null;
	private boolean isConnecting = false;

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (DevicesService) service;

			binder.initManager(MainActivity.this, (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE));

			binder.searchDevices();
			binder.setDeviceListener(new DevicesService.DeviceListener() {
				@Override
				public void onDeviceDiscover(List<pair> devices) {
					pairList.addAll(devices);
				}
			});

			binder.setP2PConnectStatus(new DevicesService.P2PConnectStatus() {
				@Override
				public void onConnectStopped() {
					rv.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "p2p连接已中断", Toast.LENGTH_SHORT).show();
							isConnecting = false;
						}
					});
				}

				@Override
				public void onConnected() {
					rv.post(new Runnable() {
						@Override
						public void run() {
							isConnecting = false;
							startActivity(new Intent(MainActivity.this, ChatActivity.class));
							binder.cancelDiscover();
						}
					});
				}
			});

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};

	List<pair> pairList = new ArrayList<>();
	RecyclerView rv;
	ListAdapter la;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService(new Intent(MainActivity.this, DevicesService.class));
		bindService(new Intent(MainActivity.this, DevicesService.class), serviceConnection, Service.BIND_AUTO_CREATE);

		initToolBar();
		initList();
		updateWifiDevices();
	}

	@Override
	protected void onStart() {
		super.onStart();
		binder.searchDevices();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(serviceConnection);
	}

	public void initToolBar() {
		Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);

		toolbar.setTitle("设备");

		setSupportActionBar(toolbar);
	}

	public void initList() {
		rv = (RecyclerView) findViewById(R.id.device_list);

		la = new ListAdapter(this);

		la.initData(pairList);

		rv.setLayoutManager(new LinearLayoutManager(this));

		rv.setAdapter(la);
	}

	public void updateWifiDevices() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (rv != null && la != null) {
					rv.post(new Runnable() {
						@Override
						public void run() {
							la.updateItems(pairList);
						}
					});
				}
			}
		}, 3000);
	}

	public void initButtonListener() {
		la.setItemClick(new ListAdapter.ItemClick() {
			@Override
			public void onItemClick(int position, String content) {
				if (!isConnecting) {
					isConnecting = true;
					binder.connectToDevice(content, new DevicesService.ConnectListener() {
						@Override
						public void onConnectToSuccess(pair _pair) {
						}

						@Override
						public void onConnectToFail(pair _pair) {

						}
					});

				}
				else {
					Toast.makeText(MainActivity.this, "之前连接正在建立，请不要尝试连接其他设备", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

}
