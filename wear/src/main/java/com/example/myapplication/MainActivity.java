package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity {


    private TextView textView;
    Button talkButton;
    public static ArrayList<String> todoList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView =  findViewById(R.id.text);
        talkButton =  findViewById(R.id.talkClick);

        /*talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String onClickMessage = button.getText().toString();
                textView.setText(onClickMessage);

//Use the same path//

                String datapath = "/my_path";
                new SendMessage(datapath, onClickMessage).start();

            }
        });*/

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);


        // Enables Always-on
        setAmbientEnabled();
    }

        public void buttonClick(View view) {

            Button button = (Button) view;
            String onClickMessage = button.getText().toString();
            textView.setText(onClickMessage);

//Use the same path//

            String datapath = "/my_path";
            new SendMessage(datapath, onClickMessage).start();
        }

    public void openToDoList(View view){
        Intent intent = new Intent(this, TodoListActivity.class);
        startActivity(intent);
    }


    public class Receiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {

//Display the following when a new message is received//

            //String onMessageReceived = "I just received a message from the handheld " + receivedMessageNumber++;
            String message = intent.getStringExtra("message");
            textView.setText(message);

            todoList.add(message);
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

//Constructor for sending information to the Data Layer//

        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

//Retrieve the connected devices//

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously//

                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {

//Send the message///

                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

                        Integer result = Tasks.await(sendMessageTask);

//Handle the errors//

                    } catch (ExecutionException exception) {

//TO DO//

                    } catch (InterruptedException exception) {

//TO DO//

                    }

                }

            } catch (ExecutionException exception) {

//TO DO//

            } catch (InterruptedException exception) {

//TO DO//

            }
        }
    }
}
