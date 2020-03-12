package com.test.screenlive;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.test.screenlive.constants.Constant;
import com.test.screenlive.data.DataUtil;
import com.test.screenlive.data.H264DataCollecter;
import com.test.screenlive.media.H264Data;
import com.test.screenlive.record.ScreenRecordThread;
import com.test.screenlive.rtsp.RtspServer;
import com.test.screenlive.util.RunState;
import com.test.screenlive.util.ToastUtils;

public class MainActivity extends AppCompatActivity implements H264DataCollecter {

    private TextView mTvAddress;
    private Button mBtnStart;

    public static final int REQUEST_CODE = 1001;
    private RtspServer mRtspServer;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecordThread mScreenRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initServer();
    }

    private void initView() {
        mTvAddress = findViewById(R.id.tvAddress);
        mBtnStart = findViewById(R.id.btnStart);
    }

    private void initServer() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public void onStart(View view) {
        if (!RunState.getInstance().isRun()) {
            RunState.getInstance().setRun(true);
            Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE);
            bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            RunState.getInstance().setRun(false);
            if (mScreenRecord != null) mScreenRecord.release();
            if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);
            unbindService(mRtspServiceConnection);
            changeView();
        }
    }


    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRtspServer = ((RtspServer.LocalBinder) service).getService();
            mRtspServer.addCallbackListener(mRtspCallbackListener);
            mRtspServer.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

        @Override
        public void onError(RtspServer server, Exception e, int error) {
            // We alert the user that the port is already used by another app.
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("端口被占用用")
                        .setMessage("你需要选择另外一个端口")
                        .show();
            }
        }

        @Override
        public void onMessage(RtspServer server, int message) {
            if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        ToastUtils.showShort(MainActivity.this, "用户接入，推流开始");
                    }
                });
            } else if (message == RtspServer.MESSAGE_STREAMING_STOPPED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        ToastUtils.showShort(MainActivity.this, "推流结束");
                    }
                });
            }
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                ToastUtils.showShort(MainActivity.this, "程序发生错误:MediaProjection@1");
                RunState.getInstance().setRun(false);
                return;
            }
            changeView();
            mScreenRecord = new ScreenRecordThread(this, mediaProjection, this);
            mScreenRecord.start();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void changeView() {
        if (RunState.getInstance().isRun()) {
            String playUrl = Constant.displayIpAddress(this);
            mTvAddress.setText(playUrl);
            mBtnStart.setText("停止");
        } else {
            mTvAddress.setText("");
            mBtnStart.setText("开始");
        }
    }

    @Override
    public void collect(H264Data data) {
        DataUtil.getInstance().putData(data);
    }
}
