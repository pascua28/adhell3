package com.fusionjack.adhell3.utils.rx;

import io.reactivex.rxjava3.schedulers.Schedulers;

public final class RxSingleComputationBuilder extends RxSingleBuilder {

    public RxSingleComputationBuilder() {
        super(Schedulers.computation());
    }

}
