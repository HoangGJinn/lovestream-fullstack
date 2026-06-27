package com.hcmute.lovestream.util;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.AgeRating;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class AgeRestrictedIterator<T extends VideoContent> implements Iterator<T> {
    private final Iterator<T> innerIterator;
    private final int userAge;
    private T nextElement;

    public AgeRestrictedIterator(Iterator<T> innerIterator, int userAge) {
        this.innerIterator = innerIterator;
        this.userAge = userAge;
        advance();
    }

    private void advance() {
        nextElement = null;
        while (innerIterator.hasNext()) {
            T element = innerIterator.next();
            if (isEligible(element)) {
                nextElement = element;
                break;
            }
        }
    }

    private boolean isEligible(T element) {
        AgeRating rating = element.getAgeRating();
        if (rating == null) {
            return true;
        }

        int minAge = switch (rating) {
            case G -> 0;
            case PG_13 -> 13;
            case R_16 -> 16;
            case R_18 -> 18;
        };
        return this.userAge >= minAge;
    }

    @Override
    public boolean hasNext() {
        return nextElement != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more content matching age requirements");
        }
        T current = nextElement;
        advance();
        return current;
    }
}
