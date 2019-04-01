package com.jscheng.hotfixapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created By Chengjunsen on 2019/4/1
 */
public class SplashActivity extends AppCompatActivity{
    private static final int PERMISSION_CODE = 1;
    private Button button;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
        });
        if (requestPermission()) {
            FixDexUtil.hotfix(this);
        }
    }

    private boolean requestPermission() {
        return PermissionUtil.checkPermissionsAndRequest(this, PermissionUtil.STORAGE, PERMISSION_CODE, "请求访问SD卡权限被拒绝");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            FixDexUtil.hotfix(this);
        }
    }
}
