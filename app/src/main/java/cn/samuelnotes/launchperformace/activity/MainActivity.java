package cn.samuelnotes.launchperformace.activity;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewStub;

import cn.samuelnotes.launchperformace.R;
import cn.samuelnotes.launchperformace.splash.SplashFragment;

public class MainActivity extends FragmentActivity {

	private Handler mHandler = new Handler();
	private SplashFragment splashFragment;
	private ViewStub viewStub;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		splashFragment = new SplashFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.frame, splashFragment);
		transaction.commit();
		
//		mHandler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				mProgressBar.setVisibility(View.GONE);
//				iv.setVisibility(View.VISIBLE);
//			}
//		}, 2500);
		
		viewStub = (ViewStub)findViewById(R.id.content_viewstub);

		getWindow().getDecorView().post(new Runnable() {
			
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						viewStub.inflate();
					}
				} );
			}
		});
		
		
		getWindow().getDecorView().post(new Runnable() {
			
			@Override
			public void run() {
				mHandler.postDelayed(new DelayRunnable(MainActivity.this, splashFragment) ,2000);
//				mHandler.post(new DelayRunnable(MainActivity.this, splashFragment));
				
			}
		});

		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	static class DelayRunnable implements Runnable{
		private WeakReference<Context> contextRef;
		private WeakReference<SplashFragment> fragmentRef;
		
		public DelayRunnable(Context context, SplashFragment f) {
			contextRef = new WeakReference<Context>(context);
			fragmentRef = new WeakReference<SplashFragment>(f);
		}

		@Override
		public void run() {
			if(contextRef!=null){
				SplashFragment splashFragment = fragmentRef.get();
				if(splashFragment==null){
					return;
				}
				FragmentActivity activity = (FragmentActivity) contextRef.get();
				FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
				transaction.remove(splashFragment);
				transaction.commit();
				
			}
		}
		
	}

}
