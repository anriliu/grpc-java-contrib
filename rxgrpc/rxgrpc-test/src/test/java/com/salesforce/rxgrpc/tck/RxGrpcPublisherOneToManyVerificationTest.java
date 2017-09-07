/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.tck;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.reactivex.Single;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * Publisher tests from the Reactive Streams Technology Compatibility Kit.
 * https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck
 */
public class RxGrpcPublisherOneToManyVerificationTest extends PublisherVerification<Message> {
    public static final long DEFAULT_TIMEOUT_MILLIS = 500L;
    public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 500L;

    public RxGrpcPublisherOneToManyVerificationTest() {
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
    }

    private Server server;
    private ManagedChannel channel;

    @BeforeMethod
    public void setup(Method method) throws Exception {
        System.out.println("SETUP " + method.getName());
        super.setUp();

        server = InProcessServerBuilder.forName("RxGrpcPublisherOneToManyVerificationTest").addService(new TckService()).build().start();
        channel = InProcessChannelBuilder.forName("RxGrpcPublisherOneToManyVerificationTest").usePlaintext(true).build();
    }

    @AfterMethod
    public void tearDown(Method method) throws Exception {
        System.out.println("TEAR DOWN " + method.getName());
        server.shutdown();
        server.awaitTermination();
        channel.shutdown();
    }

    @Override
    public Publisher<Message> createPublisher(long elements) {
        RxTckGrpc.RxTckStub stub = RxTckGrpc.newRxStub(channel);
        Single<Message> request = Single.just(toMessage((int) elements));
        Publisher<Message> publisher = stub.oneToMany(request);

        return publisher;
    }

    @Override
    public Publisher<Message> createFailedPublisher() {
        RxTckGrpc.RxTckStub stub = RxTckGrpc.newRxStub(channel);
        Single<Message> request = Single.just(toMessage(TckService.KABOOM));
        Publisher<Message> publisher = stub.oneToMany(request);

        return publisher;
    }

    private Message toMessage(int i) {
        return Message.newBuilder().setNumber(i).build();
    }
}