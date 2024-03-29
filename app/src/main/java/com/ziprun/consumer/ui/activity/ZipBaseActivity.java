package com.ziprun.consumer.ui.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.ziprun.consumer.ZipRunApp;
import com.ziprun.consumer.data.ZipRunSession;
import com.ziprun.consumer.utils.AndroidBus;
import com.ziprun.consumer.utils.Utils;

import javax.inject.Inject;

import dagger.ObjectGraph;

public abstract class ZipBaseActivity extends ActionBarActivity {

    private ObjectGraph activityGraph;

    @Inject
    Utils utils;

    @Inject
    ZipRunSession zipRunSession;

    @Inject
    AndroidBus bus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ZipRunApp application = (ZipRunApp) getApplication();
        activityGraph = application.getApplicationGraph().plus(
                getModules());
        activityGraph.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    /**
     * A list of modules to use for the individual activity graph. Subclasses
     * can override this method to provide additional modules provided they call
     * and include the modules returned by calling {@code super.getModules()}.
     */
    protected Object[] getModules() {
        return new Object[]{new ActivityModule(this)};
    }

    
    /**
     * Inject the supplied {@code object} using the com.voxapp.sdk.activity-specific graph.
     */
    public void inject(Object object) {
        activityGraph.inject(object);
    }

    public ObjectGraph getActivityGraph() {
        return activityGraph;
    }


}
