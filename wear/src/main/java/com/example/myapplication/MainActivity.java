package com.example.myapplication;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {


    private TextView textView;
    Button talkButton;
    public static ArrayList<TodoListItem> todoList = new ArrayList<TodoListItem>();
    public Receiver messageReceiver = new Receiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView =  findViewById(R.id.text);
        talkButton =  findViewById(R.id.talkClick);

        File mFolder = new File(getFilesDir() + "/files");
        File listFile = new File(mFolder.getAbsolutePath() + "/list.tmp");
        if (!mFolder.exists()) {
            mFolder.mkdir();
        }
        if (!listFile.exists()) {
            try {
                listFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileInputStream fileIn = new FileInputStream(getFilesDir() + "/files/list.tmp");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            todoList = (ArrayList<TodoListItem>) in.readObject();
            in.close();
            System.out.println("BEOLVAS");
            fileIn.close();
        } catch (IOException | ClassNotFoundException i) {
            i.printStackTrace();
        }

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

        View.OnKeyListener wrist = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT || keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS){
                    new SendMessage("/command", "Flashlight").start();
                    return true;
                }
                return false;
            }
        };

        findViewById(R.id.layout).setOnKeyListener(wrist);


        // Enables Always-on
        //setAmbientEnabled();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String datapath = "/command";
        if (keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT) {
            new SendMessage(datapath, "Flashlight").start();
        }
        return super.onKeyDown(keyCode, event);
    }

    //This sends the text displayed on the button, the phone will execute the corresponding action
        public void buttonClick(View view) {

            Button button = (Button) view;
            String onClickMessage = button.getText().toString();
            if(onClickMessage.equals("Clear List")){
                todoList.clear();
                try {
                    FileOutputStream fileOut = new FileOutputStream( getFilesDir() + "/files/list.tmp");
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(todoList);
                    out.close();
                    fileOut.close();
                    Log.d("SUCCESS","Serialized data is saved in /files/list.tmp");
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
            textView.setText(onClickMessage);

            String datapath = "/command";
            new SendMessage(datapath, onClickMessage).start();
        }

    public void openToDoList(View view){
        Intent intent = new Intent(this, TodoListActivity.class);
        startActivity(intent);
    }

    public void openImageReceiver(View view) {
        Intent intent = new Intent(this, PhotoReceiverActivity.class);
        startActivity(intent);
    }





    public class Receiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            textView.setText(message);
            if(message.equals("Clear List")){
                todoList.clear();
            }
            else {
                Log.d("MSG:", message);
                TodoListItem tmpItem = new TodoListItem(message, false);
                todoList.add(tmpItem);
                try {
                    FileOutputStream fileOut = new FileOutputStream( getFilesDir() + "/files/list.tmp");
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(todoList);
                    out.close();
                    fileOut.close();
                    Log.d("SUCCESS","Serialized data is saved in /tmp/list.tmp");
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

//Constructor for sending information to the Data Layer

        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

//Retrieve the connected devices

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously

                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {

//Send the message

                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

                        Integer result = Tasks.await(sendMessageTask);

//Handle the errors

                    } catch (ExecutionException exception) {

                    } catch (InterruptedException exception) {

                    }

                }

            } catch (ExecutionException exception) {

            } catch (InterruptedException exception) {

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }
}
