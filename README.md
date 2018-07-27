
提升应用的启动速度与设计
-
> 这一部分主要写一写 应用启动白屏， 启动速度较慢，以及优化方案。本文一共分三个部分。搞定启动速度，让应用飞起来。

**1. 常规的优化方案**

**2. 优化方案探讨**

**3. 总结**

## 1.常规的优化方案

### a.热启动与冷启动
	1）冷启动：当直接从桌面上直接启动，同时后台没有该进程的缓存，这个时候系统就需要
	重新创建一个新的进程并且分配各种资源。
	2）热启动：该app后台有该进程的缓存，这时候启动的进程就属于热启动。

	热启动不需要重新分配进程，也不会Application了，直接走的就是app的入口Activity，这样就速度快很多

### b.如何测量一个应用的启动时间

	使用命令行来启动app，同时进行时间测量。单位：毫秒
	adb shell am start -W [PackageName]/[PackageName.MainActivity]
	adb shell am start -W com.samuelnotes.launchperformace/.splash.SplashActivity

	ThisTime: 165 指当前指定的MainActivity的启动时间
	TotalTime: 165 整个应用的启动时间，Application+Activity的使用的时间。
	WaitTime: 175 包括系统的影响时间---比较上面大。
	
    
### c.热启动与冷启动测试方法
    
连个真机，第一次打开应用之后，

1.切到内存清理界面，直接杀死。 

2.在应用内部，应用的最后一个Activity相应的方法是onbackpressed，或者 finish 。 而不是app.exit  或者 process.kill  . 

经过比较之后就是，直接杀死的应用，再次启动耗时比较长。而经过finish或者back键退出的再次启动几乎耗时在130ms左右。 明显比直接杀死的快了几倍。 
由此可以得出简单结论（这里不涉及源码与理论性探讨，只做经验性总结）， 谷歌工程师千方百计地进行优化，也不及开发者强制杀死来的厉害。
**好车需要好司机才能体现出性能。**

    
## 2.优化方案的探讨
### a.应用启动的流程
	Application从构造方法开始--->attachBaseContext()--->onCreate()
	Activity构造方法--->onCreate()--->设置显示界面布局，设置主题、背景等等属性
	--->onStart()--->onResume()--->显示里面的view（测量、布局、绘制，显示到界面上）
    接下来我们分析一下，时间花在哪里了。

### b.减少应用的启动时间的耗时
	1）、不要在Application的构造方法、attachBaseContext()、onCreate()里面进行初始化耗时操作。
	2）、MainActivity，由于用户只关心最后的显示的这一帧，对我们的布局的层次要求要减少，自定义控件的话测量、布局、绘制的时间。
		不要在onCreate、onStart、onResume当中做耗时操作。
	3）、对于SharedPreference的初始化。
		因为他初始化的时候是需要将数据全部读取出来放到内存当中。
		优化
	    1：可以尽可能减少sp文件数量(IO需要时间)；
		2.像这样的初始化最好放到线程里面；
		3.大的数据缓存到数据库里面。

app启动的耗时主要是在：Application初始化 + MainActivity的界面加载绘制时间。

由于MainActivity的业务和布局复杂度非常高，甚至该界面必须要有一些初始化的数据才能显示。
那么这个时候MainActivity就可能半天都出不来，这就给用户感觉app太卡了。

我们要做的就是给用户干净利落的体验。点击app就立马弹出我们的界面。
于是乎想到使用SplashActivity--非常简单的一个欢迎页面上面都不干就只显示一个图片。

但是SplashActivity启动之后，还是需要跳到MainActivity。MainActivity还是需要从头开始加载布局和数据。
想到SplashActivity里面可以去做一些MainActivity的数据的预加载。然后需要通过意图传到MainActivity。

可不可以再做一些更好的优化呢？
耗时的问题：Application+Activity的启动及资源加载时间；预加载的数据花的时间。

如果我们能让这两个时间重叠在一个时间段内并发地做这两个事情就省时间了。

### c.解决方案：

将SplashActivity和MainActivity合为一个。一进来还是现实的MainActivity，SplashActivity可以变成一个SplashFragment，然后放一个FrameLayout作为根布局直接现实SplashFragment界面。
SplashFragment里面非常之简单，就是现实一个图片，启动非常快。
当SplashFragment显示完毕后再将它remove。同时在splash的2S的友好时间内进行网络数据缓存。
这个时候我们才看到MainActivity，就不必再去等待网络数据返回了。

如果SplashView和ContentView加载放到一起来做了 ，这可能会影响应用的启动时间。

可以使用ViewStub延迟加载MainActivity当中的View来达到减轻这个影响。


### d.如何设计延迟加载DelayLoad
	第一时间想到的就是在onCreate里面调用handler.postDelayed()方法。
	问题：这个延迟时间如何控制？
	不同的机器启动速度不一样。这个时间如何控制？
	假设，需要在splash做一个动画--2S
	
	需要达到的效果：应用已经启动并加载完成，界面已经显示出来了，然后我们再去做其他的事情。

如果我们这样：

```
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setVisibility(View.GONE);
				iv.setVisibility(View.VISIBLE);
			}
		}, 2500);
```

是没法做到等应用已经启动并加载完成，界面已经显示出来了，然后我们再去做其他的事情。

那么什么时候应用已经启动并加载完成，界面已经显示出来了。？

onResume执行完了之后才显示完毕，所以说不行。 可以根据这么几种进行监听，优化。onWindowFocusChange  ViewTreeObserver  DecorView Handler消息队列处理后的处理


### e.ViewStub 的简单分析
曾看过一篇文章说的是ViewStub 就是为了解决加载 耗时，占用资源过多的问题。 比如有一个页面内的一个小功能只有在很少的情况下使用，而且其布局很复杂，inflate的过程很耗时。 这时候就需要一个viewStub来完成替身的作用。下边来简单看看其实现。 

ViewStub 继承自View ， 里边的主要就这方法， inflate， replace . 

```
 /**
     * Inflates the layout resource identified by {@link #getLayoutResource()}
     * and replaces this StubbedView in its parent by the inflated layout resource.
     *
     * @return The inflated layout resource.
     * 我们把inflate的耗时方法 ，放在了具体的业务场景之中，这也就减少了具体直接的load时间。 
     * 以下代码主要方法就是inflate, loadxml ,replace .三个部分，都一一注释。
     *
     */
    public View inflate() {
        final ViewParent viewParent = getParent();

        if (viewParent != null && viewParent instanceof ViewGroup) {
            if (mLayoutResource != 0) {
                final ViewGroup parent = (ViewGroup) viewParent;
                final View view = inflateViewNoAdd(parent);
                replaceSelfWithView(view, parent);

                mInflatedViewRef = new WeakReference<>(view);
                if (mInflateListener != null) {
                    mInflateListener.onInflate(this, view);
                }

                return view;
            } else {
                throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
            }
        } else {
            throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        }
    }
    
```

```
    /**
    拉取布局，赋值给view 。 设置Id
    **/
  private View inflateViewNoAdd(ViewGroup parent) {
        final LayoutInflater factory;
        if (mInflater != null) {
            factory = mInflater;
        } else {
            factory = LayoutInflater.from(mContext);
        }
        final View view = factory.inflate(mLayoutResource, parent, false);

        if (mInflatedId != NO_ID) {
            view.setId(mInflatedId);
        }
        return view;
    }
    //// 把替身删除，把自己添加到替身的位置。从而完成了替换过程。
    private void replaceSelfWithView(View view, ViewGroup parent) {
        final int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);

        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            parent.addView(view, index, layoutParams);
        } else {
            parent.addView(view, index);
        }
    }
    
```
### f. 变更主题法
如果觉得上边的这种方法太繁琐。还可以试一试另外一种比较简单快捷的方法。 当然需要美工的帮助。 
我们知道了应用的启动 
Application从构造方法开始--->attachBaseContext --->onCreate() /// 这时候是只显示一个白色的默认背景	Activity构造方法--->onCreate()--->设置显示界面布局，设置主题、背景等等属性 这时候是只显示一个白色的默认背景。其实这个白色的背景背后就是页面在加载解析 布局。 

我们就从这个背景入手，当然是改一下主题。 嗯嗯， 解释不如看代码

```

    <!-- Base application theme. -->
    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <!-- Customize your theme here. -->
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>

        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>

    </style>



    <style name="splash_bg_style" parent="AppTheme">
        <item name="android:windowBackground">@drawable/splash_layer_layout</item>
    </style>


```
再看manifest配置

```
<application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">

        <activity
            android:name=".splash.SplashActivity"
            android:theme="@style/splash_bg_style">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>


        <activity
            android:name=".activity.MainActivity"
            >
         
        </activity>
        
```
一试，我靠居然快了， 但是看am -w 时间 其实是一模一样的。 只是视觉上的。因为application主题背景设置成透明的，而splash页的主题背景设置的本来就是contentview的背景。就这么简单地骗过了眼睛。 

以上两种方法都已经放在了git上,欢迎fork， star [https://github.com/samuelhehe/LaunchPerformance](https://github.com/samuelhehe/LaunchPerformance)

## 3.总结

不知不觉拉了这么多， 其实说起性能优化，脱离了具体的业务场景，都是在耍流氓。 也就是实践出真知，也许就是这种方案适合自己。也许另一种更适合业务场景，性能有一定的损耗。 

    我们常常在选择的时候都会面临这样的选择，选择的内容也许不是性能或者专业领域最好的，但往往是最有效的。大家可以根据以上方案自行适配自己的方案。 

如果有更好的方案，或者有不对的地方，欢迎批评指正。 
