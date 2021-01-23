package com.fusionjack.adhell3.utils.rx;

import io.reactivex.schedulers.Schedulers;

public final class RxSingleComputationBuilder extends RxSingleBuilder {

    public RxSingleComputationBuilder() {
        super(Schedulers.computation());
    }

}
