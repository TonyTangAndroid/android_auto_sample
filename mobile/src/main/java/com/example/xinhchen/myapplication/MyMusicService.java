package com.example.xinhchen.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static com.example.xinhchen.myapplication.CarHelper.MEDIA_CONNECTION_STATUS;
import static com.example.xinhchen.myapplication.MediaIDHelper.MEDIA_ID_EMPTY_ROOT;
import static com.example.xinhchen.myapplication.MediaIDHelper.MEDIA_ID_ROOT;


/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 * <p>
 * <ul>
 * <p>
 * <li> Extend {@link MediaBrowserServiceCompat}, implementing the media browsing
 * related methods {@link MediaBrowserServiceCompat#onGetRoot} and
 * {@link MediaBrowserServiceCompat#onLoadChildren};
 * <li> In onCreate, start a new {@link MediaSessionCompat} and notify its parent
 * with the session's token {@link MediaBrowserServiceCompat#setSessionToken};
 * <p>
 * <li> Set a callback on the {@link MediaSessionCompat#setCallback(MediaSessionCompat.Callback)}.
 * The callback will receive all the user's actions, like play, pause, etc;
 * <p>
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 * {@link android.media.MediaPlayer})
 * <p>
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * {@link MediaSessionCompat#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat)}
 * {@link MediaSessionCompat#setMetadata(android.support.v4.media.MediaMetadataCompat)} and
 * {@link MediaSessionCompat#setQueue(java.util.List)})
 * <p>
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 * <p>
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 * <p>
 * <ul>
 * <p>
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 * <p>
 * </ul>
 */
public class MyMusicService extends MediaBrowserServiceCompat {

    private MediaSessionCompat mSession;
    private MusicProvider mMusicProvider;
    private PackageValidator mPackageValidator;
    private BroadcastReceiver mCarConnectionReceiver;
    private BroadcastReceiver autoKeyReceiver;
    private boolean mIsConnectedToCar;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicProvider = new MusicProvider();

        // To make the app more responsive, fetch and cache catalog information now.
        // This can help improve the response time in the method
        // {@link #onLoadChildren(String, Result<List<MediaItem>>) onLoadChildren()}.

        mMusicProvider.retrieveMediaAsync(null /* Callback */);

        mPackageValidator = new PackageValidator(this);

        mSession = new MediaSessionCompat(this, "MyMusicService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        registerCarConnectionReceiver();
//        registerAutoKeyConnectionReceiver();
    }

    @Override
    public void onDestroy() {
        mSession.release();
        unregisterCarConnectionReceiver();
//        unregisterReceiver(autoKeyReceiver);
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
//        return new BrowserRoot("root", null);
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty browser root.
            // If you return null, then the media browser will not be able to connect and
            // no further calls will be made to other media browsing methods.
            return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }

        //noinspection StatementWithEmptyBody
        if (CarHelper.isValidCarPackage(clientPackageName)) {
            // Optional: if your app needs to adapt the music library to show a different subset
            // when connected to the car, this is where you should handle it.
            // If you want to adapt other runtime behaviors, like tweak ads or change some behavior
            // that should be different on cars, you should instead use the boolean flag
            // set by the BroadcastReceiver mCarConnectionReceiver (mIsConnectedToCar).
        }
        //noinspection StatementWithEmptyBody


        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
//        result.sendResult(new ArrayList<MediaItem>());
        Log.i("testtesttest","LoadChildren :" );
        if (MEDIA_ID_EMPTY_ROOT.equals(parentMediaId)) {
            result.sendResult(new ArrayList<MediaItem>());
        } else if (mMusicProvider.isInitialized()) {
            // if music library is ready, return immediately
//            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
            if(parentMediaId.equals(MEDIA_ID_ROOT)){
                mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                    @Override
                    public void onMusicCatalogReady(boolean success) {
                        try{
                            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });
            }else {
                mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                    @Override
                    public void onMusicCatalogReady(boolean success) {
                        try{
                            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                            Log.i("testtesttest","parentId " + parentMediaId);
                            Log.i("testtesttest", "3333333333333");
                            String realKey = parentMediaId.replace("__BY_KEYS__/","");
                            sendApiCall(realKey);
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
//                        if (realKey.equals("L2 Enter")) {
//                            new AsyncTask<Void, Void, Void>() {
//                                @Override
//                                protected Void doInBackground(Void... params) {
//                                    HttpURLConnection conn = null;
//                                    byte[] requestBody = null;
//                                    byte[] responseBody = null;
//                                    try{
//                                        URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/5533248");
////                                URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/"+autoKeys);
////                                Log.i("testtesttest","urls" + );
//                                        conn = (HttpURLConnection) url.openConnection();
//                                        conn.setDoOutput(true);
//
//                                        conn.setRequestProperty("Authorization", "Bearer qxPGfKI9glRDP_MeVtu0jxFUPmxlweI9_TF5dwUm40QjBvC9F6riZZMtFcWog2h4XvA");
//                                        conn.setRequestProperty("badge_id","95239");
//
//                                        OutputStream os = conn.getOutputStream();
//
////                                requestBody = new String("resource_id=f50dc1bca395ddffff2810bfaf89273aae7608576d1e6a1f52d6c0267a940a3d").getBytes("UTF-8");
//
////                                os.write(requestBody);
//
//                                        os.flush();
//
//                                        os.close();
//
//                                        InputStream is = conn.getInputStream();
//
//                                        responseBody = getBytesByInputStream(is);
//                                        Log.i("testtesttest", "response : "+ responseBody);
//                                    }catch (Exception e){
//                                        e.printStackTrace();
//                                    }finally {
//                                        if(conn !=null){
//                                            conn.disconnect();
//                                        }
//                                    }
//                                    return null;
//                                }
//
//                                @Override
//                                protected void onPostExecute(Void result) {
//
//                                }
//                            }.execute();
//                        }
//                        Context context = getApplicationContext();
//                        Intent intent1 = new Intent("Auto");
////                        intent1.putExtra("Keys","5533708");
//                        intent1.putExtra("Keys","5533248");
//                        Log.i("testtesttest","broadcast one step before : ");
//                        context.sendBroadcast(intent1);
//                        Log.i("testtesttest","broadcast one step after : " + intent1.toString());
                    }
                });
            }
        } else {
            // otherwise, only return results when the music library is retrieved
            result.detach();
            if(parentMediaId.equals(MEDIA_ID_ROOT)){
                mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                    @Override
                    public void onMusicCatalogReady(boolean success) {
                        try{
                            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });
            }else {
                mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                    @Override
                    public void onMusicCatalogReady(boolean success) {
                        try{
                            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                            Log.i("testtesttest","parentId " + parentMediaId);
                            Log.i("testtesttest", "3333333333333");
                            String realKey = parentMediaId.replace("__BY_KEYS__/","");
                            sendApiCall(realKey);
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
//                        String realKey = parentMediaId.replace("__BY_KEYS__/","");
//                        Log.i("testtesttest","parentId " + realKey);
//                        Context context = getApplicationContext();
//                        Intent intent1 = new Intent("Auto");
////                        intent1.putExtra("Keys","5533708");
//                        intent1.putExtra("Keys","5533248");
//                        Log.i("testtesttest","broadcast one step before : ");
//                        context.sendBroadcast(intent1);
                    }
                });
            }
        }
    }

    private void registerCarConnectionReceiver() {
        IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
        mCarConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("testtesttest","44444444444444444" + intent.getStringExtra("CarHelper.MEDIA_CONNECTION_STATUS"));
                String connectionEvent = intent.getStringExtra(MEDIA_CONNECTION_STATUS);
                mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                Log.i("testtesttest","Connection event to Android Auto: " + connectionEvent +
                        " isConnectedToCar=" + mIsConnectedToCar);
            }
        };
        registerReceiver(mCarConnectionReceiver, filter);
    }

//    private void registerAutoKeyConnectionReceiver() {
//        Log.i("testtesttest","Keys Receiver created");
//        IntentFilter keyFilter = new IntentFilter("Auto");
//        autoKeyReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.i("testtesttest","44444444444444444" + intent.getStringExtra("Keys"));
//                final String autoKeys = intent.getStringExtra("Keys");
//                try{
//                    Log.i("testtesttest","entered FetchKey");
////                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
////                    fetchKey(url,conn);
//                    new AsyncTask<Void, Void, Void>() {
//                        @Override
//                        protected Void doInBackground(Void... params) {
//                            HttpURLConnection conn = null;
//                            byte[] requestBody = null;
//                            byte[] responseBody = null;
//                            try{
//                                URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/"+autoKeys);
////                                URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/"+autoKeys);
////                                Log.i("testtesttest","urls" + );
//                                conn = (HttpURLConnection) url.openConnection();
//                                conn.setDoOutput(true);
//
//                                conn.setRequestProperty("Authorization", "Bearer qxPGfKI9glRDP_MeVtu0jxFUPmxlweI9_TF5dwUm40QjBvC9F6riZZMtFcWog2h4XvA");
//                                conn.setRequestProperty("badge_id","95239");
//
//                                OutputStream os = conn.getOutputStream();
//
////                                requestBody = new String("resource_id=f50dc1bca395ddffff2810bfaf89273aae7608576d1e6a1f52d6c0267a940a3d").getBytes("UTF-8");
//
////                                os.write(requestBody);
//
//                                os.flush();
//
//                                os.close();
//
//                                InputStream is = conn.getInputStream();
//
//                                responseBody = getBytesByInputStream(is);
//                                Log.i("testtesttest", "response : "+ responseBody);
//                            }catch (Exception e){
//                                e.printStackTrace();
//                            }finally {
//                                if(conn !=null){
//                                    conn.disconnect();
//                                }
//                            }
//                            return null;
//                        }
//
//                        @Override
//                        protected void onPostExecute(Void result) {
//
//                        }
//                    }.execute();
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//
//            }
//        };
//        registerReceiver(autoKeyReceiver, keyFilter);
//    }

    private void unregisterCarConnectionReceiver() {
        unregisterReceiver(mCarConnectionReceiver);
    }

    private void sendApiCall(String key){
        String resource_id;
        if(key.equals("L2 Enter")){
            resource_id = "f50dc1bca395ddffff2810bfaf89273aae7608576d1e6a1f52d6c0267a940a3d";
            Log.i("testtesttest","get resource : " + resource_id);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                HttpURLConnection conn = null;
                byte[] requestBody = null;
                byte[] responseBody = null;
                try{
                    URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/5533248");
//                                URL url = new URL("https://mstr-1w.customer.cloud.microstrategy.com/usekey/"+autoKeys);
//                                Log.i("testtesttest","urls" + );
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);

                    conn.setRequestProperty("Authorization", "Bearer qxPGfKI9glRDP_MeVtu0jxFUPmxlweI9_TF5dwUm40QjBvC9F6riZZMtFcWog2h4XvA");
                    conn.setRequestProperty("badge_id","95239");

                    OutputStream os = conn.getOutputStream();

//                                requestBody = new String("resource_id=f50dc1bca395ddffff2810bfaf89273aae7608576d1e6a1f52d6c0267a940a3d").getBytes("UTF-8");

//                                os.write(requestBody);

                    os.flush();

                    os.close();

                    InputStream is = conn.getInputStream();

                    responseBody = getBytesByInputStream(is);
                    Log.i("testtesttest", "response : "+ responseBody);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(conn !=null){
                        conn.disconnect();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {

            }
        }.execute();
    }

    private byte[] getBytesByInputStream(InputStream is) {
        byte[] bytes = null;
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        byte[] buffer = new byte[1024 * 8];
        int length = 0;
        try {
            while ((length = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            bos.flush();
            bytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }



    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
        }

        @Override
        public void onSeekTo(long position) {
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onSkipToNext() {
        }

        @Override
        public void onSkipToPrevious() {
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
        }
    }
}
