package com.fusionjack.adhell3.utils;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.fusionjack.adhell3.R;


public class CameraTileService extends TileService {

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

        AdhellFactory.getInstance().setCameraState(true);
        updateTitle(true);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        updateTitle(AdhellFactory.getInstance().getCameraState());
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        boolean cameraState = AdhellFactory.getInstance().getCameraState();
        AdhellFactory.getInstance().setCameraState(!cameraState);
        updateTitle(!cameraState);
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
