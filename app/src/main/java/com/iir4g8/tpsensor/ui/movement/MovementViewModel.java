package com.iir4g8.tpsensor.ui.movement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MovementViewModel extends ViewModel {
    private final MutableLiveData<String> mText;

    public MovementViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the movement fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}