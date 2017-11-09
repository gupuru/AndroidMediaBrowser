package com.gupuru.mediabrowsersample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;


public class MediaListFragment extends Fragment {

    private BrowseAdapter mBrowserAdapter;


    private static class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        public BrowseAdapter(Activity context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaBrowserCompat.MediaItem>());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            MediaBrowserCompat.MediaItem item = getItem(position);
            return MediaItemViewHolder.setupListView((Activity) getContext(), convertView, parent,
                    item);
        }
    }

    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    super.onMetadataChanged(metadata);
                    if (metadata == null) {
                        return;
                    }
                    mBrowserAdapter.notifyDataSetChanged();
                }

                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    super.onPlaybackStateChanged(state);
                    LogHelper.d(TAG, "Received state change: ", state);
                    checkForUserVisibleErrors(false);
                    mBrowserAdapter.notifyDataSetChanged();
                }
            };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback
            = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());
            if (mediaController != null) {
                mediaController.unregisterCallback(mediaControllerCallback);
                MediaControllerCompat.setMediaController(getActivity(), null);
            }
        }

        @Override
        public void onConnected() {
            super.onConnected();
            handleConnected();
        }
    };

    public MediaListFragment() {

    }

    public static MediaListFragment newInstance() {
        return new MediaListFragment();
    }

    private void handleConnected() {
        if (mediaId == null) {
            mediaId = mediaBrowserCompat.getRoot();
        }
        mediaBrowserCompat.subscribe(mediaId, subscriptionCallback);

        try {
            MediaControllerCompat mediaController =
                    new MediaControllerCompat(getActivity(),
                            mediaBrowserCompat.getSessionToken());
            MediaControllerCompat.setMediaController(getActivity(), mediaController);

            // Register a Callback to stay in sync
            mediaController.registerCallback(mediaControllerCallback);
        } catch (RemoteException e) {
            Log.e("ログ", "Failed to connect to MediaController", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_list, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview);
        mediaAdapter = new MediaAdapter(getActivity());
        listView.setAdapter(mediaAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaBrowserCompat.MediaItem item = mediaAdapter.getItem(position);
                FragmentDataHelper dataHelper = (FragmentDataHelper) getActivity();
                dataHelper.onMediaItemSelected(item, false);
            }
        });

        mediaBrowserCompat = new MediaBrowserCompat(
                getActivity(),
                new ComponentName(getActivity(), MusicService.class), connectionCallback, null);
        return rootView;
    }

    public interface FragmentDataHelper {
        void onMediaItemSelected(MediaBrowserCompat.MediaItem item, boolean isPlaying);
    }

    public class MediaAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        public MediaAdapter(@NonNull Context context) {
            super(context, R.layout.item_media_info, new ArrayList<MediaBrowserCompat.MediaItem>());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_media_info, parent, false);
            }

            TextView author = (TextView) convertView.findViewById(R.id.text_view_media_author);
            author.setText(getItem(position).getDescription().getSubtitle());
            TextView mediaName = (TextView) convertView.findViewById(R.id.text_view_media_name);
            mediaName.setText(getItem(position).getDescription().getTitle());

            return convertView;
        }
    }

}
