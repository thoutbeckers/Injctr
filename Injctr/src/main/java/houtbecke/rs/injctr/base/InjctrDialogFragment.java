package houtbecke.rs.injctr.base;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import houtbecke.rs.injctr.InjctrUtil;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class InjctrDialogFragment extends DialogFragment {

    @Inject
    protected InjctrUtil injctrUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (injctrUtil == null)
            injctrUtil = new InjctrUtil(getActivity().getApplicationContext(), getActivity().getResources());

        int viewId = injctrUtil.getFragmentLayout(this);

        View view;
        if (viewId != 0)
            view = inflater.inflate(viewId, container, false);
        else
            view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        injctrUtil.injctrFragment(this);
    }
}
