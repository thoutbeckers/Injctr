package houtbecke.rs.injctr.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.internal.util.Stopwatch;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.activity.event.OnActivityResultEvent;
import roboguice.activity.event.OnContentChangedEvent;
import roboguice.activity.event.OnNewIntentEvent;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnRestartEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.activity.event.OnSaveInstanceStateEvent;
import roboguice.activity.event.OnStopEvent;
import roboguice.context.event.OnConfigurationChangedEvent;
import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.context.event.OnStartEvent;
import roboguice.event.EventManager;
import roboguice.inject.ContentViewListener;
import roboguice.inject.RoboInjector;
import roboguice.util.RoboContext;

public class RoboAppCompatActivity extends AppCompatActivity implements RoboContext {

    protected EventManager eventManager;
    protected HashMap<Key<?>, Object> scopedObjects = new HashMap<>();
    @Inject
    ContentViewListener ignored;

    public RoboAppCompatActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Stopwatch stopwatch = new Stopwatch();
        RoboInjector injector = RoboGuice.getInjector(this);
        stopwatch.resetAndLog("RoboActivity creation of injector");
        this.eventManager = (EventManager)injector.getInstance(EventManager.class);
        stopwatch.resetAndLog("RoboActivity creation of eventmanager");
        injector.injectMembersWithoutViews(this);
        stopwatch.resetAndLog("RoboActivity inject members without views");

        super.onCreate(savedInstanceState);

        stopwatch.resetAndLog("RoboActivity super onCreate");
        this.eventManager.fire(new OnCreateEvent(this, savedInstanceState));
        stopwatch.resetAndLog("RoboActivity fire event");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.eventManager.fire(new OnSaveInstanceStateEvent(this, outState));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.eventManager.fire(new OnRestartEvent(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.eventManager.fire(new OnStartEvent(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.eventManager.fire(new OnResumeEvent(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.eventManager.fire(new OnPauseEvent(this));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.eventManager.fire(new OnNewIntentEvent(this));
    }

    @Override
    protected void onStop() {
        try {
            this.eventManager.fire(new OnStopEvent(this));
        } finally {
            super.onStop();
        }

    }

    @Override
    protected void onDestroy() {
        try {
            this.eventManager.fire(new OnDestroyEvent(this));
        } finally {
            try {
                RoboGuice.destroyInjector(this);
            } finally {
                super.onDestroy();
            }
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Configuration currentConfig = this.getResources().getConfiguration();
        super.onConfigurationChanged(newConfig);
        this.eventManager.fire(new OnConfigurationChangedEvent(this, currentConfig, newConfig));
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        RoboGuice.getInjector(this).injectViewMembers(this);
        this.eventManager.fire(new OnContentChangedEvent(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.eventManager.fire(new OnActivityResultEvent(this, requestCode, resultCode, data));
    }

    @Override
    public Map<Key<?>, Object> getScopedObjectMap() {
        return this.scopedObjects;
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return shouldInjectOnCreateView(name)?injectOnCreateView(name, context, attrs):super.onCreateView(name, context, attrs);
    }

    @Override
    @TargetApi(11)
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return shouldInjectOnCreateView(name)?injectOnCreateView(name, context, attrs):super.onCreateView(parent, name, context, attrs);
    }

    protected static boolean shouldInjectOnCreateView(String name) {
        return false;
    }

    protected static View injectOnCreateView(String name, Context context, AttributeSet attrs) {
        try {
            Constructor<?> constructor = Class.forName(name).getConstructor(new Class[]{Context.class, AttributeSet.class});
            View view = (View)constructor.newInstance(new Object[]{context, attrs});
            RoboGuice.getInjector(context).injectMembers(view);
            RoboGuice.getInjector(context).injectViewMembers(view);
            return view;
        } catch (Exception var5) {
            throw new RuntimeException(var5);
        }
    }

}