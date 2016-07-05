package com.orbbec.DepthViewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by Xiebj on 16-6-25.
 */
public class HomeKeyListener {
    static final String TAG = "HomeListener";

    public static final String FINISH = "finish";

    private Context mContext;
    private IntentFilter mHomeKeyFilter;
    private IntentFilter mFinishFilter;
    private OnHomePressedListener mListener;
    private InnerReceiver mReceiver;

    // 回调接口
    public interface OnHomePressedListener {
        public void onHomePressed();

        public void onHomeLongPressed();
    }

    public HomeKeyListener(Context context) {
        mContext = context;
        mHomeKeyFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mFinishFilter = new IntentFilter();
    }

    /**
     * 设置监听
     *
     * @param listener
     */
    public void setOnHomePressedListener(OnHomePressedListener listener) {
        mListener = listener;
        mReceiver = new InnerReceiver();
    }

    /**
     * 开始监听，注册广播
     */
    public void startWatch() {
        if (mReceiver != null) {
            mContext.registerReceiver(mReceiver, mHomeKeyFilter);
        }
    }

    /**
     * 停止监听，注销广播
     */
    public void stopWatch() {
        if (mReceiver != null) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (Exception e) {
            }

        }
    }

    class InnerReceiver extends BroadcastReceiver {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null) {
                    //                    Log.e(TAG, "action:" + action + ",reason:" + reason);
                    if (mListener != null) {
                        //TODO : see FutureBox
                        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                            // 短按home键
                            mListener.onHomePressed();
                        }/* else if (reason
                                .equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                            // 长按home键
                            mListener.onHomeLongPressed();
                        }*/
                    }
                }
            } else if (action.equals(FINISH)) {
                // 短按home键
                mListener.onHomePressed();
            }
        }
    }
}