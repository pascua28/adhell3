package com.fusionjack.adhell3.utils.rx;

import io.reactivex.schedulers.Schedulers;

public class RxCompletableIoBuilder extends RxCompletableBuilder {

    public RxCompletableIoBuilder() {
        super(Schedulers.io());
    }

}
