/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.ClientCallStreamObserver;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * FlowableBackpressureOnReadyHandler.
 * @param <T>
 */
public class FlowableBackpressureOnReadyHandler<T> implements Subscriber<T>, Runnable {
    private ClientCallStreamObserver<T> requestStream;
    private Subscription subscription;

    public FlowableBackpressureOnReadyHandler(ClientCallStreamObserver<T> requestStream) {
        this.requestStream = requestStream;
        requestStream.setOnReadyHandler(this);
    }

    @Override
    public void run() {
        // restart the pump
        subscription.request(1);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(T t) {
        requestStream.onNext(t);
        if (requestStream.isReady()) {
            // keep the pump going
            subscription.request(1);
        } else {
            System.out.println("XXX");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        requestStream.onError(throwable);
    }

    @Override
    public void onComplete() {
        requestStream.onCompleted();
    }
}
