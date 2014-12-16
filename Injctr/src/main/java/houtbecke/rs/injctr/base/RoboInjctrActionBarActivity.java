package houtbecke.rs.injctr.base;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import houtbecke.rs.injctr.InjctrUtil;
import roboguice.activity.RoboActionBarActivity;

public class RoboInjctrActionBarActivity extends RoboActionBarActivity {

    @Inject
    protected InjctrUtil injctrUtil;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterContentView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterContentView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        afterContentView();
    }

    protected void afterContentView() {
        if (injctrUtil == null)
            injctrUtil = new InjctrUtil(this, getResources());

        injctrUtil.injctrActivity(this);
    }
}
