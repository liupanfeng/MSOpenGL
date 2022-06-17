package com.meishe.msopengl;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.meishe.msopengl.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'knative-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestPermission();

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

                        }else{
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