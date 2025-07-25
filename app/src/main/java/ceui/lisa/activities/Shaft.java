package ceui.lisa.activities;

import static ceui.lisa.utils.Local.LOCAL_DATA;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.hjq.toast.ToastUtils;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.tencent.mmkv.MMKV;

import org.jetbrains.annotations.NotNull;

import ceui.lisa.R;
import ceui.lisa.database.AppDatabase;
import ceui.lisa.feature.HostManager;
import ceui.lisa.helper.ShortcutHelper;
import ceui.lisa.helper.ThemeHelper;
import ceui.lisa.models.UserModel;
import ceui.lisa.notification.NetWorkStateReceiver;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.utils.Local;
import ceui.lisa.utils.Settings;
import ceui.lisa.view.MyDeliveryHeader;
import ceui.lisa.viewmodel.AppLevelViewModel;
import ceui.loxia.ServicesProvider;
import ceui.pixiv.db.EntityWrapper;
import ceui.pixiv.session.SessionManager;
import ceui.pixiv.ui.background.AppBackground;
import ceui.pixiv.ui.task.TaskPool;
import ceui.pixiv.utils.NetworkStateManager;
import me.jessyan.progressmanager.ProgressManager;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Where the app code starts.
 */
public class Shaft extends Application implements ServicesProvider {

    public static UserModel sUserModel;
    public static Settings sSettings;
    public static Gson sGson;
    public static SharedPreferences sPreferences;
    public static AppLevelViewModel appViewModel;
    /**
     * 状态栏高度，初始化
     */
    public static int statusHeight = 0, toolbarHeight = 0;
    private static MMKV mmkv;
    /**
     * 全局context
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = null;

    static {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((context, layout) -> {
            return new ClassicsHeader(context);//.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header(BezierRadarHeader)
        });

        SmartRefreshLayout.setDefaultRefreshFooterCreator((context, layout) ->
                new ClassicsFooter(context).setDrawableSize(20));
    }

    protected NetWorkStateReceiver netWorkStateReceiver;
    private NetworkStateManager networkStateManager;
    private OkHttpClient mOkHttpClient;
    private EntityWrapper entityWrapper;
    private AppBackground appBackground;
    private TaskPool taskPool;

    public static Context getContext() {
        return sContext;
    }

    public static String getThemeColor() {
        int current = Shaft.sSettings.getThemeIndex();
        return switch (current) {
            case 0 -> "#686bdd";
            case 1 -> "#56baec";
            case 2 -> "#008BF3";
            case 3 -> "#03d0bf";
            case 4 -> "#fee65e";
            case 5 -> "#fe83a2";
            case 6 -> "#F44336";
            case 7 -> "#673AB7";
            case 8 -> "#4CAF50";
            case 9 -> "#E91E63";
            default -> "#686bdd";
        };
    }

    public static MMKV getMMKV() {
        if (mmkv == null) {
            mmkv = MMKV.defaultMMKV();
        }
        return mmkv;
    }

    /**
     * Initialize the whole application.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        //初始化context
        sContext = this;
        sGson = new Gson();
        //0.0127254

        sPreferences = getSharedPreferences(LOCAL_DATA, Context.MODE_PRIVATE);

        Timber.plant(new Timber.DebugTree());

        MMKV.initialize(this);


        networkStateManager = new NetworkStateManager(this);
        appBackground = new AppBackground();
        taskPool = new TaskPool();
        sUserModel = Local.getUser();

        sSettings = Local.getSettings();

        entityWrapper = new EntityWrapper(AppDatabase.getAppDatabase(this));
        entityWrapper.initialize();

        SessionManager.INSTANCE.initialize();

        updateTheme();

        ThemeHelper.applyTheme(null, sSettings.getThemeType());

        this.mOkHttpClient = ProgressManager.getInstance().with(new OkHttpClient.Builder()).build();

        //计算状态栏高度并赋值
        statusHeight = 0;
        int resourceId = sContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusHeight = sContext.getResources().getDimensionPixelSize(resourceId);
        }
        toolbarHeight = DensityUtil.dp2px(56.0f);

        //Init the network
        if (netWorkStateReceiver == null) {
            netWorkStateReceiver = new NetWorkStateReceiver();
        }

        HostManager.get().init();

        //Init Toast utils
        ToastUtils.init(this);
        ToastUtils.setGravity(Gravity.BOTTOM, 0, 0);

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(
                sSettings.isFirebaseEnable()
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, filter);

        ShortcutHelper.addAppShortcuts();

        appViewModel = new AppLevelViewModel(this);
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    /**
     * Update the theme according to the setting.
     */
    private void updateTheme() {
        int current = Shaft.sSettings.getThemeIndex();
        switch (current) {
            case 0:
                setTheme(R.style.AppTheme_Index0);
                break;
            case 1:
                setTheme(R.style.AppTheme_Index1);
                break;
            case 2:
                setTheme(R.style.AppTheme_Index2);
                break;
            case 3:
                setTheme(R.style.AppTheme_Index3);
                break;
            case 4:
                setTheme(R.style.AppTheme_Index4);
                break;
            case 5:
                setTheme(R.style.AppTheme_Index5);
                break;
            case 6:
                setTheme(R.style.AppTheme_Index6);
                break;
            case 7:
                setTheme(R.style.AppTheme_Index7);
                break;
            case 8:
                setTheme(R.style.AppTheme_Index8);
                break;
            case 9:
                setTheme(R.style.AppTheme_Index9);
                break;
            default:
                setTheme(R.style.AppTheme_Default);
                break;
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        try {
            super.unbindService(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_YES:
                MyDeliveryHeader.changeCloudColor(getContext());
                break;
        }
    }

    @Override
    public @NotNull MMKV getPrefStore() {
        return getMMKV();
    }

    @Override
    public @NotNull NetworkStateManager getNetworkStateManager() {
        return networkStateManager;
    }

    @Override
    public @NotNull EntityWrapper getEntityWrapper() {
        return entityWrapper;
    }

    @Override
    public @NotNull AppBackground getAppBackground() {
        return appBackground;
    }

    @Override
    public @NotNull TaskPool getTaskPool() {
        return taskPool;
    }
}
