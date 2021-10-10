package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    Button clearButton;
    TextView textview;
    EditText todoListInput;
    protected Handler myHandler;
    private int[] sounds= new int[]{R.raw.fursrodah, R.raw.oof, R.raw.quack, R.raw.rubberduck, R.raw.xpshutdown, R.raw.xpstartup};
    private boolean flash = false;
    public static ArrayList<TodoListItem> todoList = new ArrayList<>();
    // public static Set<String> todoSet = new HashSet<String>(todoList);
    public boolean isListEmpty = true;
    private SharedPreferences sharedPreferences;
    private String saveFile = "cuculo";
    private String STATE_LIST = "lista";
    private String STATE_IS_EMPTY = "ures";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clearButton = findViewById(R.id.clearButton);
        textview = findViewById(R.id.textView);
        todoListInput = findViewById(R.id.todoListInput);
        String path = this.getFilesDir().getPath();
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

        sharedPreferences = getSharedPreferences(saveFile, MODE_PRIVATE);

        isListEmpty = sharedPreferences.getBoolean(STATE_IS_EMPTY,isListEmpty);
        //todoSet = sharedPreferences.getStringSet(STATE_LIST,todoSet);
        //todoList.addAll(todoSet);


        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public void openToDoList(View view){
        Intent intent = new Intent(this, TodoListActivity.class);
        startActivity(intent);
    }

    public void openImageSender(View view) {
        Intent intent = new Intent(this, PhotoSenderActivity.class);
        startActivity(intent);
    }


    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override

        public void onReceive(Context context, Intent intent) {

            //Store the received message
            String message = intent.getStringExtra("message");
            //Display the received message, this is mainly for debugging
            textview.setText("Last Command: " + message);

            //Do the corresponding action for the received message
            if (message.equals("Vibrate")){
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }
            }
            else if(message.equals("Ring")){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //It picks a random sound effect from the ones stored in the /raw folder inside the resources, then plays the sound effect
                Random rand = new Random();
                int thingy = rand.nextInt(sounds.length);
                int myUri = sounds[thingy];
                final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), myUri);
                mp.start();
            }
            else if(message.equals("Flashlight")){
                boolean isFlashAvailable = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                //This is used to find the camera modules on the phone, then it's id is stored
                CameraManager myCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
                String myCameraId = null;
                try {
                    myCameraId = myCameraManager.getCameraIdList()[0];
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                //Toggle the flashlight on the phone
                try {
                    myCameraManager.setTorchMode(myCameraId, !flash);
                    flash = !flash;
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            else if(message.equals("Clear List")){
                todoList.clear();
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

//Adds the entered activity to the to-do list
    public void addToList(View view) {
        String todo = todoListInput.getText().toString();
        if(todo.equals("")){
            textview.setText("Enter a normal activity!");
        }
        else{
            todoListInput.setText("");
            TodoListItem tmpItem = new TodoListItem(todo, false);
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
            new SendMessage("/my_path", todo).start();
            Toast.makeText(this,"Added Activity To The List", Toast.LENGTH_SHORT).show();
        }

    }
//Clears the to-do list and sends the "Clear List" string to the watch telling it to clear it's list too
    public void talkClick(View view) {

        Button button = (Button) view;
        String message = button.getText().toString();
        if(message.equals("Clear List")){
            todoList.clear();
            Toast.makeText(this,"Cleared all activities", Toast.LENGTH_SHORT).show();
        }

//Sending a message can block the main UI thread, so use a new thread

        new SendMessage("/my_path", message).start();
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

//Retrieve the connected devices, known as nodes

            Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

//Block on a task and get the result synchronously

                        Integer result = Tasks.await(sendMessageTask);

                        //if the Task fails, then…..

                    } catch (ExecutionException exception) {

                        //TO DO: Handle the exception

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

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(STATE_IS_EMPTY,isListEmpty);
        //editor.putStringSet(STATE_LIST, todoSet);
        editor.apply();
    }
}
