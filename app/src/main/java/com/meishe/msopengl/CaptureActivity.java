package com.meishe.msopengl;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.meishe.msopengl.databinding.ActivityCaptureBinding;
import com.meishe.msopengl.view.MSRecordButton;

import java.util.List;

public class CaptureActivity extends AppCompatActivity {

    // Used to load the 'knative-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityCaptureBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityCaptureBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        requestPermission();
        initListener();
    }

    private void initListener() {
        mBinding.btnRecord.setOnRecordListener(new MSRecordButton.OnRecordListener() {
            @Override
            public void onStartRecording() {
                mBinding.glSurfaceView.startRecording();
            }

            @Override
            public void onStopRecording() {
                mBinding.glSurfaceView.stopRecording();
                Toast.makeText(CaptureActivity.this, "录制完成！", Toast.LENGTH_SHORT).show();
            }
        });

        mBinding.groupRecordSpeed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    /*极慢*/
                    case R.id.rbtn_record_speed_extra_slow:
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_EXTRA_SLOW);
                        break;
                        /*慢*/
                    case R.id.rbtn_record_speed_slow:
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_SLOW);
                        break;
                        /*正常 标准*/
                    case R.id.rbtn_record_speed_normal:
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_NORMAL);
                        break;
                        /* 快*/
                    case R.id.rbtn_record_speed_fast:
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_FAST);
                        break;
                        /*极快*/
                    case R.id.rbtn_record_speed_extra_fast:
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_EXTRA_FAST);
                        break;
                }
            }
        });

        mBinding.chkBigeye.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBinding.glSurfaceView.enableBigEye(isChecked);
            }
        });
    }


    /**
     * 获取授权
     */
    private void requestPermission() {
        XXPermissions.with(this).permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {

                        } else {
                            finish();
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {

                    }
                });
    }


    public native String stringFromJNI();
}