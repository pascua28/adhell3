package com.fusionjack.adhell3.utils.rx;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxCompletableIoBuilder extends RxCompletableBuilder {

    public RxCompletableIoBuilder() {
        super(Schedulers.io());
    }

}
