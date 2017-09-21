package houtbecke.rs.injctr.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import houtbecke.rs.injctr.InjctrUtil;

public class InjctrActivity extends Activity {

    @Inject
    protected InjctrUtil injctrUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layout = injctrUtil.getLayout(this, "activity");
        if (layout > 0)
            setContentView(layout);
    }

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
            injctrUtil = new InjctrUtil(getApplicationContext(), getResources());

        injctrUtil.injctrActivity(this);
    }
}
