package bupt.wifidirectchat.activities;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import bupt.wifidirectchat.R;
import bupt.wifidirectchat.activities.service.DevicesService;
import bupt.wifidirectchat.adapter.ListAdapter;
import bupt.wifidirectchat.adapter.pair;
import bupt.wifidirectchat.service.wifi.WifiP2pStateListener;

/**
 * Created by Maou on 2017/7/6.
 */

public class ChatActivity extends AppCompatActivity {

	DevicesService binder = null;

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (DevicesService) service;
			binder.setMessagesListener(new DevicesService.Messages() {
				@Override
				public void onMessageArrived(String message) {
					messages.add(new pair(" ", message));
					la.newItem(new pair(" ", message));
				}
			});

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};

	RecyclerView recyclerView;
	ListAdapter la;


	EditText inputText;
	Button sendButton;
	List<pair> messages = new ArrayList<>();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		initRecycleView();
		bindService(new Intent(ChatActivity.this, DevicesService.class), serviceConnection, Service.BIND_AUTO_CREATE);


	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		binder.stopTCPConnect();
		unbindService(serviceConnection);


	}

	private void initRecycleView(){
		recyclerView = (RecyclerView) findViewById(R.id.chat_list);

		la = new ListAdapter(this);


		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		la.initData(messages);

		recyclerView.setAdapter(la);
	}

	private void initButtonListener(){
		inputText = (EditText) findViewById(R.id.input_text);
		sendButton = (Button) findViewById(R.id.send_button);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String m = inputText.getText().toString();
				binder.sendMessage(m);
				messages.add(new pair("Self", m));
				la.newItem(new pair("Self", m));
			}
		});

	}




}
