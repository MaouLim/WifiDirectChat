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
import bupt.wifidirectchat.services.DevicesService;
import bupt.wifidirectchat.activities.adapters.ListAdapter;
import bupt.wifidirectchat.activities.adapters.Pair;

/*
 * Created by Liu Cong on 2017/7/6.
 */

public class ChatActivity extends AppCompatActivity {

	private DevicesService binder = null;

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			DevicesService.MBinder mBinder = (DevicesService.MBinder) service;
			binder = mBinder.getService();
			binder.setMessagesListener(new DevicesService.Messages() {
				@Override
				public void onMessageArrived(String message) {
					messages.add(new Pair(" ", message));
					recyclerView.post(new Runnable() {
						@Override
						public void run() {
							listAdapter.updateItems(messages);
						}
					});
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// empty
		}
	};

	private RecyclerView recyclerView;
	private ListAdapter listAdapter;
	private EditText inputText;
	private List<Pair> messages = new ArrayList<>();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		initRecycleView();
		bindService(
			new Intent(ChatActivity.this, DevicesService.class),
			serviceConnection,
			Service.BIND_AUTO_CREATE
		);

		initButtonListener();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		binder.stopTCPConnect();
		unbindService(serviceConnection);
	}

	private void initRecycleView() {
		recyclerView = (RecyclerView) findViewById(R.id.chat_list);
		listAdapter = new ListAdapter(this);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		listAdapter.initData(messages);
		recyclerView.setAdapter(listAdapter);
	}

	private void initButtonListener() {
		inputText = (EditText) findViewById(R.id.input_text);
		Button sendButton = (Button) findViewById(R.id.send_button);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String content = inputText.getText().toString();
				messages.add(new Pair("Self", content));
				listAdapter.newItem(new Pair("Self", content));
				binder.sendMessage(content);
				inputText.setText("");
			}
		});
	}

}
