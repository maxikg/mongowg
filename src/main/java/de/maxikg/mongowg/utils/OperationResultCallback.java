package de.maxikg.mongowg.utils;

import com.google.common.base.Preconditions;
import com.mongodb.async.SingleResultCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Useful {@link SingleResultCallback} for most of MongoWG's operations. It checks for a exception and loads them into
 * a {@link AtomicReference} and {@link CountDownLatch#countDown()} a {@link CountDownLatch}.
 */
public class OperationResultCallback<T> implements SingleResultCallback<T> {

    private final AtomicReference<Throwable> error;
    private final CountDownLatch waiter;

    /**
     * Constructor.
     *
     * @param error The error holder
     * @param waiter The {@link CountDownLatch}
     */
    public OperationResultCallback(AtomicReference<Throwable> error, CountDownLatch waiter) {
        this.error = Preconditions.checkNotNull(error, "error must be not null.");
        this.waiter = Preconditions.checkNotNull(waiter, "waiter must be not null.");
    }

    @Override
    public void onResult(Object o, Throwable throwable) {
        if (throwable != null)
            error.set(throwable);

        waiter.countDown();
    }

    /**
     * Static factory method for {@code OperationResultCallback}.
     *
     * @param error The error holder
     * @param waiter The {@link CountDownLatch}
     * @param <T> The result type
     * @return The constructed {@code OperationResultCallback}
     */
    public static <T> OperationResultCallback<T> create(AtomicReference<Throwable> error, CountDownLatch waiter) {
        return new OperationResultCallback<>(error, waiter);
    }
}
