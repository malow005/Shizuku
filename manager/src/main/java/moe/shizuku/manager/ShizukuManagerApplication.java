package moe.shizuku.manager;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;

import com.topjohnwu.superuser.Shell;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import me.weishu.reflection.Reflection;
import moe.shizuku.api.ShizukuClientHelper;
import moe.shizuku.manager.authorization.AuthorizationManager;
import moe.shizuku.support.app.DayNightDelegate;
import moe.shizuku.support.app.LocaleDelegate;

import static moe.shizuku.manager.utils.Logger.LOGGER;

public class ShizukuManagerApplication extends Application implements ViewModelStoreOwner {

    static {
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
    }

    public static boolean isUserUnlocked(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserManager um = context.getSystemService(UserManager.class);
            if (um != null && !um.isUserUnlocked()) {
                return um.isUserUnlocked();
            }
        }
        return true;
    }

    public static Context getDeviceProtectedStorageContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createDeviceProtectedStorageContext();
        }
        return context;
    }

    public static Context getDeviceProtectedStorageContextIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!isUserUnlocked(context)) {
                return context.createDeviceProtectedStorageContext();
            }
        }
        return context;
    }

    public static void init(Context context) {
        ShizukuManagerSettings.initialize(context);
        LocaleDelegate.setDefaultLocale(ShizukuManagerSettings.getLocale());
        DayNightDelegate.setDefaultNightMode(ShizukuManagerSettings.getNightMode());
        AuthorizationManager.init(context);
        ServerLauncher.writeFiles(context);

        ShizukuClientHelper.setBinderReceivedListener(() -> {
            LOGGER.i("onBinderReceived");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(AppConstants.ACTION_BINDER_RECEIVED));
        });
    }

    private ViewModelStore mViewModelStore;

    @Override
    public void onCreate() {
        super.onCreate();

        init(this);
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (mViewModelStore == null) {
            mViewModelStore = new ViewModelStore();
        }
        return mViewModelStore;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }
}
