package com.fusionjack.adhell3.utils.rx;

import io.reactivex.schedulers.Schedulers;

public class RxCompletableComputationBuilder extends RxCompletableBuilder {

    public RxCompletableComputationBuilder() {
        super(Schedulers.computation());
    }
    
}
