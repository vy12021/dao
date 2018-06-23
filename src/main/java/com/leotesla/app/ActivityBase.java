package com.leotesla.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public abstract class ActivityBase extends AppCompatActivity {

    private final SuperHandler<ActivityBase> mInternalHandler = new SuperHandler<>(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.mInternalHandler.awake();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            this.mInternalHandler.suspend();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mInternalHandler.exit();
    }

    public Handler getHandler () {
        return this.mInternalHandler;
    }

}
