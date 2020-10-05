package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    Button talkbutton;
    TextView textview;
    EditText todoListInput;
    protected Handler myHandler;
    private int[] sounds= new int[]{R.raw.fursrodah, R.raw.oof, R.raw.quack, R.raw.rubberduck, R.raw.xpshutdown, R.raw.xpstartup};
    private boolean flash = false;
    public static ArrayList<String> todoList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        talkbutton = findViewById(R.id.talkButton);
        textview = findViewById(R.id.textView);
        todoListInput = findViewById(R.id.todoListInput);



        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public void openToDoList(View view){
        Intent intent = new Intent(this, TodoListActivity.class);
        startActivity(intent);
    }


    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override

        public void onReceive(Context context, Intent intent) {

//Upon receiving each message from the wearable, display the following text//

            //String message = "I just received a message from the wearable " + receivedMessageNumber++;
            String message = intent.getStringExtra("message");
            textview.setText("Last Command: " + message);

            if (message.equals("Vibrate")){
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }
            }
            else if(message.equals("Ring")){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Random rand = new Random();
                int thingy = rand.nextInt(sounds.length);
                int myUri = sounds[thingy];
                final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), myUri);
                mp.start();
            }
            else if(message.equals("Flashlight")){
                boolean isFlashAvailable = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                CameraManager myCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
                String myCameraId = null;
                try {
                    myCameraId = myCameraManager.getCameraIdList()[0];
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                try {
                    myCameraManager.setTorchMode(myCameraId, !flash);
                    flash = !flash;
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            textview.append("\n" + newinfo);
        }
    }

    public void addToList(View view) {
        String todo = todoListInput.getText().toString();
        if(todo.equals("")){
            textview.setText("Enter a normal activity!");
        }
        else{
            todoList.add(todo);
            new NewThread("/my_path", todo).start();
        }

    }

    public void talkClick(View view) {

        Button button = (Button) view;
        String message = button.getText().toString();
        //textview.setText(message);

//Sending a message can block the main UI thread, so use a new thread//

        new NewThread("/my_path", message).start();
    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }

    class NewThread extends Thread {
        String path;
        String message;

//Constructor for sending information to the Data Layer//

        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

//Retrieve the connected devices, known as nodes//

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =

//Send the message//

                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

//Block on a task and get the result synchronously//

                        Integer result = Tasks.await(sendMessageTask);
                        //sendmessage("I just sent the wearable a message " + sentMessageNumber++);

                        //if the Task fails, then…..//

                    } catch (ExecutionException exception) {

                        //TO DO: Handle the exception//

                    } catch (InterruptedException exception) {

                        //TO DO: Handle the exception//

                    }

                }

            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }

        }
    }
}
