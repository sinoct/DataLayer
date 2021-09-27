package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PhotoSenderActivity extends AppCompatActivity implements DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {


    private static final String TAG = "MainActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";

    Button takePhotoButton;
    Button sendPhotoButton;
    ImageView imageContainer;
    private Bitmap mImageBitmap;

    private DataItemAdapter mDataItemListAdapter;

    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_sender_layout);
        takePhotoButton = findViewById(R.id.take_photo_button);
        sendPhotoButton = findViewById(R.id.send_photo_button);
        imageContainer = findViewById(R.id.image_container);

        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
        // mDataItemList.setAdapter(mDataItemListAdapter);

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataItemGeneratorFuture =
                mGeneratorExecutor.scheduleWithFixedDelay(
                        new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    public void takePhotoClick(View view) {
        startTakePictureIntent();
    }

    public void sendPhotoClick(View view) {
        if(mImageBitmap != null) {
            sendPhoto(toAsset(mImageBitmap));
        }
    }

    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);

        dataItemTask.addOnSuccessListener(
                new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.d("MainActivity", "onSuccess: " + dataItem);
                    }
                }
        );
    }

    private void startTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            mImageBitmap = (Bitmap) extras.get("data");
            imageContainer.setImageBitmap(mImageBitmap);
        }
    }

    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                mDataItemListAdapter.add(
                        new Event("DataItem Changed", event.getDataItem().toString()));
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                mDataItemListAdapter.add(
                        new Event("DataItem Deleted", event.getDataItem().toString()));
            }
        }
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);

        mDataItemListAdapter.add(new Event("onCapabilityChanged", capabilityInfo.toString()));
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(
                TAG,
                "onMessageReceived() A message from watch was received:"
                        + messageEvent.getRequestId()
                        + " "
                        + messageEvent.getPath());

        mDataItemListAdapter.add(new Event("Message from watch", messageEvent.toString()));
    }

    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    private class DataItemGenerator implements Runnable {
        private int count = 0;
        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.d(TAG, "Generating DataItem: " + request);

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItem dataItem = Tasks.await(dataItemTask);

                Log.d(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }
}
