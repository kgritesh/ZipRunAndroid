package com.ziprun.consumer.ui.fragment;

import com.ziprun.consumer.presenter.LocationPickerPresenter;

import dagger.Module;

@Module(injects = {
        LocationPickerFragment.class,
        InstructionFragment.class,
        ConfirmationFragment.class,
        LocationPickerPresenter.class,
    }, complete = false,
    library = true)
public class FragmentModule {
    private static final String TAG = FragmentModule.class.getCanonicalName();
    private ZipBaseFragment zipFragment;

    public FragmentModule(ZipBaseFragment zipFragment) {
        this.zipFragment = zipFragment;
    }

}
