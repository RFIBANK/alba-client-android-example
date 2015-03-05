package ru.rficb.albaexample;


import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class AlbaResultReceiver extends ResultReceiver {

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle data);
    }

    private Receiver mReceiver;

    public AlbaResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
