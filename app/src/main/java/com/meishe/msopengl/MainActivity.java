package com.meishe.msopengl;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.meishe.msopengl.databinding.ActivityMainBinding;
import com.meishe.msopengl.view.MSOpenGLSurfaceView;
import com.meishe.msopengl.view.MSRecordButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'knative-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
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
                Toast.makeText(MainActivity.this, "录制完成！", Toast.LENGTH_SHORT).show();
            }
        });

        mBinding.groupRecordSpeed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rbtn_record_speed_extra_slow: // 极慢
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_EXTRA_SLOW);
                        break;
                    case R.id.rbtn_record_speed_slow:   // 慢
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_SLOW);
                        break;
                    case R.id.rbtn_record_speed_normal: // 正常 标准
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_NORMAL);
                        break;
                    case R.id.rbtn_record_speed_fast:   // 快
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_FAST);
                        break;
                    case R.id.rbtn_record_speed_extra_fast: // 极快
                        mBinding.glSurfaceView.setSpeed(Speed.MODE_EXTRA_FAST);
                        break;
                }
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