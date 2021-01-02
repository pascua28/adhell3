package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.SettingsFragment;


public class CameraTileService extends TileService {

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();

        AdhellFactory.getInstance().setCameraState(true);
        updateTitle(true);
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
            boolean cameraState = AdhellFactory.getInstance().getCameraState();
            AdhellFactory.getInstance().setCameraState(!cameraState);
            updateTitle(!cameraState);
        }
    }

    private void updateTitle(boolean cameraState) {
        Tile tile = getQsTile();

        if (cameraState) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_cam_on));
            tile.setLabel(getString(R.string.qs_camera_on));
            tile.setContentDescription(getString(R.string.qs_camera_on));
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_cam_off));
            tile.setLabel(getString(R.string.qs_camera_off));
            tile.setContentDescription(getString(R.string.qs_camera_off));
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

}
