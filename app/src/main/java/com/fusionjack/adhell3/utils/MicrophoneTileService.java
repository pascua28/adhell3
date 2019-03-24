package com.fusionjack.adhell3.utils;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.fusionjack.adhell3.R;


public class MicrophoneTileService extends TileService {

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();

        AdhellFactory.getInstance().setMicrophoneState(true);
        updateTitle(true);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        updateTitle(AdhellFactory.getInstance().getMicrophoneState());
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        boolean microphoneState = AdhellFactory.getInstance().getMicrophoneState();
        AdhellFactory.getInstance().setMicrophoneState(!microphoneState);
        updateTitle(!microphoneState);
    }

    private void updateTitle(boolean microphoneState) {
        Tile tile = getQsTile();

        if (microphoneState) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_mic_on));
            tile.setLabel(getString(R.string.qs_microphone_on));
            tile.setContentDescription(getString(R.string.qs_microphone_on));
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_mic_off));
            tile.setLabel(getString(R.string.qs_microphone_off));
            tile.setContentDescription(getString(R.string.qs_microphone_off));
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

}
