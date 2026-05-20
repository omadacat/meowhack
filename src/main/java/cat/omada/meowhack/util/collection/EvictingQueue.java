package cat.omada.meowhack.util.collection;

import java.util.ArrayDeque;

public class EvictingQueue<E> extends ArrayDeque<E> {
    private final int maxSize;

    public EvictingQueue(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E e) {
        if (size() >= maxSize) pollFirst();
        return super.add(e);
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }
}
