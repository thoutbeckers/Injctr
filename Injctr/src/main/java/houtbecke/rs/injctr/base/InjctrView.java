package houtbecke.rs.injctr.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import javax.inject.Inject;

import houtbecke.rs.injctr.InjctrUtil;

public abstract class InjctrView extends RelativeLayout {
    @Inject
    protected InjctrUtil injctrUtil;

    protected Context context;

    public InjctrView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InjctrView(Context context, AttributeSet attrs, LayoutInflater inflater) {
        super(context, attrs);
        init(context, attrs);
    }

    public InjctrView(Context context, AttributeSet attrs, int defStyle, LayoutInflater inflater) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    /**
     * This is called after the context is set but before anything else.
     *
     * Override this to do things like Roboguice initialization:
     *
     * RoboGuice.getInjector(context).injectMembersWithoutViews(this);
     */
    protected void beforeInit() {}

    /**
     * This is called right after the layout of this view is loaded.
     */
    protected void afterLayout() {}

    public final void init(Context context, AttributeSet attrs) {
        this.context = context;
        beforeInit();

        int layout;
        if (injctrUtil == null)
            injctrUtil = new InjctrUtil(context, context.getResources());

        layout = injctrUtil.getViewLayout(this);
        LayoutInflater.from(context).inflate(layout, this);

        afterLayout();

        if (!isInEditMode())
            injctrUtil.injctrView(this, context, attrs);

        afterInit();
    }

    public void afterInit() {}

}
