package com.h2o_execution.smart_order_router.core;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamTools {
    public static <T> boolean areIdentical(Stream<T> streamOne, Stream<T> streamTwo) {
        if (streamOne == null && streamTwo == null) {
            return true;
        } else if (streamOne == null || streamTwo == null) {
            return false;
        }
        return checkIdenticalNonNullStreams(streamOne, streamTwo);
    }

    private static <T> boolean checkIdenticalNonNullStreams(Stream<T> streamOne, Stream<T> streamTwo) {
        Iterator<T> streamOneIterator = streamOne.iterator();
        Iterator<T> streamTwoIterator = streamTwo.iterator();
        while (streamsTillConsumable(streamOneIterator, streamTwoIterator)) {
            if (streamElementsNotEqual(streamOneIterator, streamTwoIterator)) {
                return false;
            }
        }
        return streamsAreEmpty(streamOneIterator, streamTwoIterator);
    }

    private static <T> boolean streamsAreEmpty(Iterator<T> streamOneIterator, Iterator<T> streamTwoIterator) {
        return !streamOneIterator.hasNext() && !streamTwoIterator.hasNext();
    }

    private static <T> boolean streamElementsNotEqual(Iterator<T> streamOneIterator, Iterator<T> streamTwoIterator) {
        return !streamOneIterator.next().equals(streamTwoIterator.next());
    }

    private static <T> boolean streamsTillConsumable(Iterator<T> streamOneIterator, Iterator<T> streamTwoIterator) {
        return streamOneIterator.hasNext() && streamTwoIterator.hasNext();
    }

    public static <T> Tuple<Stream<T>> duplicate(Stream<T> stream) {
        List<T> collect = stream.collect(Collectors.toList());
        return new Tuple<>(collect.stream(), collect.stream());
    }
}
