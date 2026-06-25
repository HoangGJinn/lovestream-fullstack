package com.hcmute.lovestream.util;

import com.hcmute.lovestream.entity.VideoContent;
import java.util.Iterator;

public class AgeFilterIterable<T extends VideoContent> implements Iterable<T> {
    private final Iterable<T> originalIterable;
    private final int userAge;

    public AgeFilterIterable(Iterable<T> originalIterable, int userAge) {
        this.originalIterable = originalIterable;
        this.userAge = userAge;
    }

    @Override
    public Iterator<T> iterator() {
        return new AgeRestrictedIterator<>(originalIterable.iterator(), userAge);
    }
}
