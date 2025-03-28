import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class q1b implements q1.ConcurrentArray{
    private volatile AtomicReference<Object[]> atomicArray;
    private final int INITIAL_LENGTH = 20;
    private final int INCREMENT = 10;

    public q1b () {
        Object[] array = new Object[INITIAL_LENGTH];
        atomicArray = new AtomicReference<>(array);
    }

    public Object get(int i) {
        if (i >= atomicArray.get().length) {
            resizeArray(i);
        }
        return atomicArray.get()[i];
    }

    public void set(int i, Object o) {
        if (i >= atomicArray.get().length) {
            resizeArray(i);
        }
        Object[] currentArray;
        Object[] newArray;
        // Ensure only one thread can update at a time
        do {
            currentArray = atomicArray.get();
            newArray = currentArray.clone();
            newArray[i] = o;
        } while (!atomicArray.compareAndSet(currentArray, newArray));
    }

    public int length() {
        return atomicArray.get().length;
    }

    private void resizeArray(int attemptAccessIndex) {
        while (attemptAccessIndex >= atomicArray.get().length) {
            Object[] oldArray = atomicArray.get();
            Object[] newArray = new Object[oldArray.length + INCREMENT];

            // Copy
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);

            // Atomic CAS update ensures only works if no set() happened during resizing
            if (atomicArray.compareAndSet(oldArray, newArray)) {
                return;
            }
            // If failed to resize, either other array resized or set. Retry
        }
    }
}
