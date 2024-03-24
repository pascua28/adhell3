package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.tasks.StaticProxyRxTaskFactory;
import com.fusionjack.adhell3.utils.ProxyUtils;
import com.fusionjack.adhell3.utils.dialog.StaticProxyEditDialog;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.function.Consumer;

public class StaticProxyInfoAdapter extends ArrayAdapter<StaticProxy> {
    
    private final Consumer<View> onPositiveButton;

    public StaticProxyInfoAdapter(@NonNull Context context, @NonNull List<StaticProxy> staticProxyInfos, Consumer<View> onPositiveButton) {
        super(context, 0, staticProxyInfos);
        this.onPositiveButton = onPositiveButton;
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // This is for performance reason because onClick lambdas require final view (additional copy)
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_proxy_info, parent, false);
        } else {
            view = convertView;
        }

        StaticProxy staticProxy = getItem(position);
        if (staticProxy == null) {
            return view;
        }

        TextView proxyNameTextView = view.findViewById(R.id.proxyNameTextView);
        TextView proxyConnectionTextView = view.findViewById(R.id.proxyConnectionTextView);
        SwitchMaterial proxySwitch = view.findViewById(R.id.switchDisable);
        ImageView editProxyImageView = view.findViewById(R.id.editProxyImageView);
        ImageView deleteProxyImageView = view.findViewById(R.id.deleteProxyImageView);

        proxyNameTextView.setText(staticProxy.name);
        proxyConnectionTextView.setText(String.format("%s:%d", staticProxy.hostname, staticProxy.port));

        proxySwitch.setChecked(false);
        proxySwitch.setEnabled(true);

        editProxyImageView.setOnClickListener(imageView -> {
            StaticProxyEditDialog.show(view, staticProxy, onPositiveButton);
        });
        deleteProxyImageView.setOnClickListener(imageView -> {
            new QuestionDialogBuilder(view)
                    .setTitle(R.string.delete_proxy_dialog_title)
                    .setQuestion(R.string.delete_proxy_dialog_text)
                    .show(() -> deleteProxy(staticProxy));
        });

        CompoundButton.OnCheckedChangeListener switchListener = (buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }

            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(getContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                }
            };
            if (isChecked) {
                try {
                    ProxyUtils.getInstance().setStaticProxy(staticProxy, handler);
                    disableAllSwitches(parent);
                    buttonView.setChecked(true);
                    editProxyImageView.setClickable(false);
                    editProxyImageView.setVisibility(View.INVISIBLE);
                    deleteProxyImageView.setClickable(false);
                    deleteProxyImageView.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                }
            } else {
                try {
                    ProxyUtils.getInstance().clearProxy(handler);
                    buttonView.setChecked(false);
                    editProxyImageView.setClickable(true);
                    editProxyImageView.setVisibility(View.VISIBLE);
                    deleteProxyImageView.setClickable(true);
                    deleteProxyImageView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(true);
                }
            }
        };

        try {
            LiveData<StaticProxy> current = ProxyUtils.getInstance().getCurrentStaticProxyFromDB();
            if (current != null) {
                Observer<StaticProxy> observer = currentDb -> {
                    if (currentDb == null) {
                        Toast.makeText(getContext(), "Current proxy from Knox is not in DB clearing...", Toast.LENGTH_SHORT).show();

                        Handler handler = new Handler(Looper.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                Toast.makeText(getContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                            }
                        };
                        try {
                            ProxyUtils.getInstance().clearProxy(handler);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else if (currentDb.id == staticProxy.id) {
                        proxySwitch.setChecked(true);
                        editProxyImageView.setClickable(false);
                        editProxyImageView.setVisibility(View.INVISIBLE);
                        deleteProxyImageView.setClickable(false);
                        deleteProxyImageView.setVisibility(View.INVISIBLE);
                    }
                };
                current.observeForever(observer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        proxySwitch.setOnCheckedChangeListener(switchListener);

        return view;
    }

    private void deleteProxy(StaticProxy staticProxy) {
        new RxCompletableIoBuilder().async(StaticProxyRxTaskFactory.deleteProxy(staticProxy));
    }

    private void disableAllSwitches(@NonNull ViewGroup parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            SwitchMaterial proxySwitch = child.findViewById(R.id.switchDisable);
            ImageView editProxyImageView = child.findViewById(R.id.editProxyImageView);
            ImageView deleteProxyImageView = child.findViewById(R.id.deleteProxyImageView);

            proxySwitch.setChecked(false);
            editProxyImageView.setClickable(true);
            editProxyImageView.setVisibility(View.VISIBLE);
            deleteProxyImageView.setClickable(true);
            deleteProxyImageView.setVisibility(View.VISIBLE);
        }
    }
}
