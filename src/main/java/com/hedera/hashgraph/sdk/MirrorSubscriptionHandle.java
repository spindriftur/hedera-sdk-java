package com.hedera.hashgraph.sdk;

public class MirrorSubscriptionHandle {
    private final Runnable onUnsubscribe;

    MirrorSubscriptionHandle(Runnable onUnsubscribe) {
        this.onUnsubscribe = onUnsubscribe;
    }

    public void unsubscribe() {
        this.onUnsubscribe.run();
    }
}
