import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class q1a implements q1.ConcurrentArray{
    public volatile Object[] lockingArray;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition resizeCompleteCondition = lock.newCondition();
    private volatile boolean resizeComplete = true;
    private final int INITIAL_LENGTH = 20;
    private final int INCREMENT = 10;

    public q1a () {
        lockingArray = new Object[INITIAL_LENGTH];
    }

    public Object get(int i) {
        // Non-blocking
        if (i < lockingArray.length) {
            return lockingArray[i];
        }

        // Blocking: Only one thread can resize at a time
        try {
            lock.lock();
            // Spurious wakeup prevention
            while (!resizeComplete) {
                try {
                    resizeCompleteCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // if resize needed
            if (i >= lockingArray.length) {
                resizeArray();
            }
            return lockingArray[i];
        } finally {
            lock.unlock();
        }

    }

    public void set(int i, Object o) {
        // Non-blocking
        if (i < lockingArray.length) {
            lockingArray[i] = o;
            return;
        }

        // Blocking: Only one thread can resize at a time
        try {
            lock.lock();
            // Spurious wakeup prevention
            while (!resizeComplete) {
                try {
                    resizeCompleteCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // if resize needed
            if (i >= lockingArray.length) {
                resizeArray();
            }
            lockingArray[i] = 0;
        } finally {
            lock.unlock();
        }
    }

    public int length() {
        return lockingArray.length;
    }

    private synchronized void resizeArray() {
        // Set signal
        resizeComplete = false;

        // Transfer old elements to new array
        int newLength = lockingArray.length + INCREMENT;
        Object[] newArray = new Object[newLength];
        System.arraycopy(lockingArray, 0, newArray, 0, lockingArray.length);
        lockingArray = newArray;

        // Signal completion
        resizeComplete = true;
        resizeCompleteCondition.signalAll();
    }
}
