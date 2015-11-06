package de.maxikg.mongowg.utils;

import java.util.concurrent.CountDownLatch;

/**
 * Utilities to deal with concurrent recurring things.
 */
public class ConcurrentUtils {

    private ConcurrentUtils() {
    }

    /**
     * Calls {@link CountDownLatch#await()} and ignores {@link InterruptedException}'s.
     *
     * @param waiter The waiter which should await
     */
    public static void safeAwait(CountDownLatch waiter) {
        try {
            waiter.await();
        } catch (InterruptedException ignore) {
        }
    }
}
