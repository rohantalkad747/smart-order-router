package com.h2o_execution.smart_order_router.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamToolsTest {

    @Test
    void WHEN_askedWhetherTwoIdenticalStreamsAreEqual_THEN_itShouldReturnTrue() {
        // Given
        Stream<Integer> integerStreamOne = Stream.of(1, 2, 3, 4);
        Stream<Integer> integerStreamTwo = Stream.of(1, 2, 3, 4);

        // When
        boolean identical = StreamTools.areIdentical(integerStreamOne, integerStreamTwo);

        // Then
        assertTrue(identical);
    }

    @Test
    void WHEN_askedWhetherTwoIdenticalEmptyStreamsAreEqual_THEN_itShouldReturnTrue() {
        // Given
        Stream<Integer> integerStreamOne = Stream.of();
        Stream<Integer> integerStreamTwo = Stream.of();

        // When
        boolean identical = StreamTools.areIdentical(integerStreamOne, integerStreamTwo);

        // Then
        assertTrue(identical);
    }

    @Test
    void WHEN_askedWhetherTwoUnidenticalStreamsAreEqual_THEN_itShouldReturnFalse() {
        // Given
        Stream<Integer> integerStreamOne = Stream.of(1, 2, 3);
        Stream<Integer> integerStreamTwo = Stream.of(1, 2, 3, 4);

        // When
        boolean identical = StreamTools.areIdentical(integerStreamOne, integerStreamTwo);

        // Then
        assertFalse(identical);
    }

    @Test
    void WHEN_askedToDuplicateAStream_THEN_itShouldReturnATupleOfDuplicateStreams() {
        // Given
        Stream<Integer> integerStream = Stream.of(1, 2, 3, 4);

        // When
        Tuple<Stream<Integer>> duplicated = StreamTools.duplicate(integerStream);

        // Then
        Stream<Integer> dupOne = duplicated.getFirst();
        Stream<Integer> dupTwo = duplicated.getSecond();

        List<Integer> dupOneList = dupOne.collect(Collectors.toList());
    }
}
