package com.example.myapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableRecyclerView;

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
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PhotoReceiverActivity extends Activity
        implements DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener {

    private static final String TAG = "MainActivity";


    ImageView imageContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_receiver_layout);
        imageContainer = findViewById(R.id.received_image_view);

    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEvents) {
        Log.d("PhotoReceiver", "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if ("/image".equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photoAsset = dataMapItem.getDataMap().getAsset("photo");
                    // Loads image on background thread.
                    new LoadBitmapAsyncTask().execute(photoAsset);
                }
            }
        }

    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
    }


    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {
            if (params.length > 0) {
                Asset asset = params[0];
                Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask =
                        Wearable.getDataClient(getApplicationContext()).getFdForAsset(asset);
                try {
                    DataClient.GetFdForAssetResponse getFdForAssetResponse =
                            Tasks.await(getFdForAssetResponseTask);
                    InputStream assetInputStream = getFdForAssetResponse.getInputStream();
                    if (assetInputStream != null) {
                        return BitmapFactory.decodeStream(assetInputStream);
                    } else {
                        Log.w(TAG, "Requested an unknown Asset.");
                        return null;
                    }
                } catch (ExecutionException exception) {
                    Log.e(TAG, "Failed retrieving asset, Task failed: " + exception);
                    return null;
                } catch (InterruptedException exception) {
                    Log.e(TAG, "Failed retrieving asset, interrupt occurred: " + exception);
                    return null;
                }
            } else {
                Log.e(TAG, "Asset must be non-null");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                Log.d("PhotoReceiver", "Setting background image on second page..");
                imageContainer.setImageBitmap(bitmap);

            }
        }
    }
}
