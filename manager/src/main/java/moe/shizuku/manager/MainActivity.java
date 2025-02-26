package moe.shizuku.manager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import moe.shizuku.api.ShizukuService;
import moe.shizuku.manager.adapter.HomeAdapter;
import moe.shizuku.manager.app.BaseActivity;
import moe.shizuku.manager.viewmodel.AppsViewModel;
import moe.shizuku.manager.viewmodel.HomeViewModel;
import moe.shizuku.manager.viewmodel.SharedViewModelProviders;
import moe.shizuku.support.design.RaisedToolbar;
import moe.shizuku.support.recyclerview.RecyclerViewHelper;

public class MainActivity extends BaseActivity {

    private BroadcastReceiver mBinderReceivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAppsModel.load(context);
            mAdapter.updateData(context);
        }
    };

    private BroadcastReceiver mRequestRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkServerStatus();
        }
    };

    private RaisedToolbar mToolbar;

    private HomeViewModel mHomeModel;
    private AppsViewModel mAppsModel;

    private HomeAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setActionBar(mToolbar = findViewById(R.id.toolbar));

        mHomeModel = ViewModelProviders.of(this).get("home", HomeViewModel.class);
        mHomeModel.observe(this, object -> {
            if (isFinishing())
                return;

            if (object != null && !(object instanceof Throwable)) {
                final Context context = this;
                mAdapter.updateData(context);

                if (mHomeModel.getServiceStatus().isV3Running()) {
                    ShizukuManagerSettings.setLastLaunchMode(mHomeModel.getServiceStatus().getUid() == 0
                            ? ShizukuManagerSettings.LaunchMethod.ROOT : ShizukuManagerSettings.LaunchMethod.ADB);
                }
            }
        });

        mAppsModel = SharedViewModelProviders.of(this).get("apps", AppsViewModel.class);
        mAppsModel.getGrantedCount().observe(this, object -> {
            final Context context = this;
            if (object.data != null) {
                mAdapter.updateData(context);
            }
        });

        if (ShizukuService.pingBinder()) {
            mAppsModel.load(this);
        }

        mAdapter = new HomeAdapter(this, mHomeModel, mAppsModel);

        RecyclerView recyclerView = findViewById(android.R.id.list);
        recyclerView.setAdapter(mAdapter);

        recyclerView.setPaddingRelative(recyclerView.getPaddingStart(), recyclerView.getPaddingTop() + recyclerView.getPaddingBottom(), recyclerView.getPaddingEnd(), recyclerView.getPaddingBottom());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                RecyclerView.LayoutManager _lm = recyclerView.getLayoutManager();
                boolean raised = false;
                if (_lm instanceof LinearLayoutManager) {
                    LinearLayoutManager lm = (LinearLayoutManager) _lm;
                    raised = lm.findFirstCompletelyVisibleItemPosition() != 0;
                } else if (_lm instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager lm = (StaggeredGridLayoutManager) _lm;
                    int spans = lm.getSpanCount();
                    int[] out = new int[spans];
                    lm.findFirstCompletelyVisibleItemPositions(out);
                    raised = false;
                    for (int i = 0; i < spans; i++) {
                        raised |= i != out[i];
                    }
                }
                mToolbar.setRaised(raised);

            }
        });

        RecyclerViewHelper.fixOverScroll(recyclerView);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBinderReceivedReceiver, new IntentFilter(AppConstants.ACTION_BINDER_RECEIVED));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mRequestRefreshReceiver, new IntentFilter(AppConstants.ACTION_REQUEST_REFRESH));
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkServerStatus();
    }

    private void checkServerStatus() {
        mHomeModel.load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBinderReceivedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRequestRefreshReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Dialog dialog = new AlertDialog.Builder(this)
                        .setView(R.layout.dialog_about)
                        .show();

                ((TextView) dialog.findViewById(R.id.source_code)).setMovementMethod(LinkMovementMethod.getInstance());

                ((TextView) dialog.findViewById(R.id.icon_credits)).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) dialog.findViewById(R.id.icon_credits)).setText(Html.fromHtml(getString(R.string.about_icon_credits)));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
