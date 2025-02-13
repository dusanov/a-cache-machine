package a.cache.machine.engine;

import java.io.Serializable;

public class LFUFrequencyPair<F, S>implements Serializable {
    private static final long serialVersionUID = 321L;
    F first;
    S second;

    LFUFrequencyPair(F first, S second) {
        this.first = first;
        this.second = second;
    }
}