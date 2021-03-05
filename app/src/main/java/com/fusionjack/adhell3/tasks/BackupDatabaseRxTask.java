package com.fusionjack.adhell3.tasks;

import android.content.Context;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.utils.dialog.DialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;

import java.lang.ref.WeakReference;

import io.reactivex.Completable;
import io.reactivex.functions.Action;

public class BackupDatabaseRxTask implements Runnable {

    private final WeakReference<Context> contextWeakReference;

    public BackupDatabaseRxTask(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    public void run() {
        Context context = contextWeakReference.get();
        if (context == null) {
            return;
        }

        Runnable onCompleteCallback = () -> DialogBuilder.showDialog(R.string.info, "Backup database is finished.", context);
        Action action = () -> DatabaseFactory.getInstance().backupDatabase();

        new RxCompletableIoBuilder()
                .setShowDialog("Backing up database ...", context)
                .async(Completable.fromAction(action), onCompleteCallback);
    }
}
