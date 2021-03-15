package com.fusionjack.adhell3.utils.rx;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxCompletableComputationBuilder extends RxCompletableBuilder {

    public RxCompletableComputationBuilder() {
        super(Schedulers.computation());
    }
    
}
