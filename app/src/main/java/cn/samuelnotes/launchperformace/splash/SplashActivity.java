package cn.samuelnotes.launchperformace.splash;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import cn.samuelnotes.launchperformace.R;
import cn.samuelnotes.launchperformace.activity.MainActivity;

/**
 * Created by samuelnotes on 2018/7/13.
 * 方案1， 可以把启动页的主题背景设置成要展示的内容， 可以给用户以迅速启动展现的效果。
 */

public class SplashActivity extends AppCompatActivity {

    private static final long WAIT_TIME_SECONDS = 2000;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        transparentStatusBar(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        enterApp();
    }


    /**
     * 使状态栏透明
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void transparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }


    private void cancelTimerTask() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }


    private void goMainAty() {
        cancelTimerTask();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }


    private void enterApp() {

        countDownTimer = new CountDownTimer(WAIT_TIME_SECONDS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                goMainAty();
            }

        };
        countDownTimer.start();

    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelTimerTask();
    }



}
