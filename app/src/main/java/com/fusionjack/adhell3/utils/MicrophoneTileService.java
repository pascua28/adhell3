package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.SettingsFragment;


public class MicrophoneTileService extends TileService {

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
    public void onClick() {
        super.onClick();

        boolean isAllow = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean allowQSTilesLockscreen = preferences.getBoolean(SettingsFragment.ALLOW_QSTILES_LOCKSCREEN, true);

        if (this.isSecure() && allowQSTilesLockscreen) isAllow = true;
        else if (!this.isSecure()) isAllow = true;

        if (isAllow) {
            boolean microphoneState = AdhellFactory.getInstance().getMicrophoneState();
            AdhellFactory.getInstance().setMicrophoneState(!microphoneState);
            updateTitle(!microphoneState);
        }
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
