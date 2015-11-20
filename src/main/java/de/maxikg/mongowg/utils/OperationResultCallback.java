package de.maxikg.mongowg.utils;

import com.google.common.base.Preconditions;
import com.mongodb.async.SingleResultCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Useful {@link SingleResultCallback} for most of MongoWG's operations. It checks for a exception and loads them into
 * a {@link AtomicReference} and {@link CountDownLatch#countDown()} a {@link CountDownLatch}.
 */
public class OperationResultCallback<T> implements SingleResultCallback<T> {

    private static final Logger LOGGER = Logger.getLogger(OperationResultCallback.class.getName());

    private final AtomicReference<Throwable> error;
    private final CountDownLatch waiter;
    private final SingleResultCallback<T> chained;

    /**
     * Constructor.
     *
     * @param error The error holder
     * @param waiter The {@link CountDownLatch}
     */
    public OperationResultCallback(AtomicReference<Throwable> error, CountDownLatch waiter) {
        this(error, waiter, null);
    }

    public OperationResultCallback(AtomicReference<Throwable> error, CountDownLatch waiter, SingleResultCallback<T> chained) {
        this.error = Preconditions.checkNotNull(error, "error must be not null.");
        this.waiter = Preconditions.checkNotNull(waiter, "waiter must be not null.");
        this.chained = chained;
    }

    @Override
    public void onResult(T o, Throwable throwable) {
        if (throwable != null)
            error.set(throwable);

        waiter.countDown();

        if (chained != null) {
            try {
                chained.onResult(o, throwable);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "An error occurred while executing chained SingleResultCallback.", e);
            }
        }
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

    public static <T> OperationResultCallback<T> create(AtomicReference<Throwable> error, CountDownLatch waiter, SingleResultCallback<T> chained) {
        return new OperationResultCallback<>(error, waiter, chained);
    }
}
