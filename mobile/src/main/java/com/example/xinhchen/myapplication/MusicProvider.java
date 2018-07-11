/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.xinhchen.myapplication;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.example.xinhchen.myapplication.RemoteJSONSource;

import org.json.JSONException;

import static com.example.xinhchen.myapplication.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.xinhchen.myapplication.MediaIDHelper.MEDIA_ID_ROOT;
import static com.example.xinhchen.myapplication.MediaIDHelper.USHER_GARAGE_KEYS;
import static com.example.xinhchen.myapplication.MediaIDHelper.createMediaID;
/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private MusicProviderSource mSource;

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;

    private final Set<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        this(new RemoteJSONSource());
    }
    public MusicProvider(MusicProviderSource source) {
        mSource = source;
        mMusicListByGenre = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */

    public Iterable<String> getKeys() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        Set<String> keys = new HashSet<>();
        keys.add("L1 Enter");
        keys.add("L2 Enter");
        keys.add("L2 Out");
        keys.add("L3 Out");
        return keys;
    }

    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Callback callback) {
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildListsByGenre();
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) throws JSONException{
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if(MEDIA_ID_ROOT.equals(mediaId)){
            for(String key : getKeys()){
                mediaItems.add(createBrowsableMediaItemForKey(key, resources));
            }
        }else if(mediaId.startsWith(USHER_GARAGE_KEYS)){
            Log.i("testtesttest","Garage key :" + mediaId.replace(USHER_GARAGE_KEYS+"/",""));
            switch (mediaId.replace(USHER_GARAGE_KEYS+"/","")){
                case "L2 Enter":{
                    MediaMetadataCompat l2Enter = buildSongHardcode(true,1);
                    mediaItems.add(createMediaItem(l2Enter));
                    break;
                }
                case "L2 Out": {
                    MediaMetadataCompat l2Enter = buildSongHardcode(true,2);
                    mediaItems.add(createMediaItem(l2Enter));
                    break;
                }
                case "L1 Enter": {
                    MediaMetadataCompat l2Enter = buildSongHardcode(true,3);
                    mediaItems.add(createMediaItem(l2Enter));
                    break;
                }
                case "L3 Out": {
                    MediaMetadataCompat l2Enter = buildSongHardcode(true,4);
                    mediaItems.add(createMediaItem(l2Enter));
                    break;
                }
            }


//            if(mediaId.endsWith("Success")){
//
//            }else if(mediaId.endsWith("Fail")){
//
//            }
        }

        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForKey(String key,
                                                                        Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, USHER_GARAGE_KEYS, key))
                .setTitle(key)
                .setSubtitle(resources.getString(
                        R.string.browse_usher_keys, key))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create quithe proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
//        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
//                0);

    }

    private MediaMetadataCompat buildSongHardcode(boolean flag, int key) throws JSONException {
        String title = flag? "Access Granted" : "Access Denied";
        String album = "album";
        String artist = "";
        if(key == 1){
            artist = "HQ P3 Lane 2 Entry";
        }else if(key == 2){
            artist = "HQ P3 Lane 2 Out";
        }else if(key == 3){
            artist = "HQ P3 Lane 1 Entry";
        }else if(key == 4){
            artist = "HQ P3 Lane 3 Out";
        }
        String genre = "Usher";
        String source = "The_Messenger.mp3";
        String iconUrl = "album_art.jpg";
        int trackNumber = 2;
        int totalTrackCount = 6;
        int duration = 132000; // ms

        // Media is stored relative to JSON file
//        if (!source.startsWith("http")) {
//            source = basePath + source;
//        }
//        if (!iconUrl.startsWith("http")) {
//            iconUrl = basePath + iconUrl;
//        }
        // Since we don't have a unique ID in the server, we fake one using the hashcode of
        // the music source. In a real world app, this could come from the server.
        String id = String.valueOf(source.hashCode());

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        //noinspection ResourceType
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .build();
    }

}
