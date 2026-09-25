package com.aoo.bcg.common.room;

import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoritativeRandomSeatAllocatorTest {
    private final AuthoritativeRandomSeatAllocator allocator = new AuthoritativeRandomSeatAllocator();

    @Test void selectsByStableVacantSeatOrderAndNeverReturnsAnOccupiedSeat() {
        Set<Integer> occupied = Set.of(0, 2, 5, 7);
        FixedIndexRandom random = new FixedIndexRandom(2);

        int selected = allocator.allocate(8, occupied, random);

        assertEquals(4, selected);
        assertEquals(4, random.lastBound);
        assertTrue(!occupied.contains(selected));
    }

    @Test void supportsEightAndTenSeatBoundaries() {
        assertEquals(7, allocator.allocate(8, Set.of(0, 1, 2, 3, 4, 5, 6), new FixedIndexRandom(0)));
        assertEquals(9, allocator.allocate(10, Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8), new FixedIndexRandom(0)));
    }

    @Test void sameSeedReplaysTheSameSelection() {
        Set<Integer> occupied = Set.of(1, 3, 6);

        int first = allocator.allocate(10, occupied, new SeededGameRandomSource(20260925L));
        int replay = allocator.allocate(10, occupied, new SeededGameRandomSource(20260925L));

        assertEquals(first, replay);
    }

    @Test void rejectsFullRoomAndInvalidAuthorityInputs() {
        assertThrows(IllegalStateException.class,
                () -> allocator.allocate(3, Set.of(0, 1, 2), new FixedIndexRandom(0)));
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(0, Set.of(), new FixedIndexRandom(0)));
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(3, Set.of(3), new FixedIndexRandom(0)));
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(3, null, new FixedIndexRandom(0)));
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(3, Set.of(), null));
    }

    private static final class FixedIndexRandom implements GameRandomSource {
        private final int index;
        private int lastBound = -1;

        private FixedIndexRandom(int index) { this.index = index; }

        @Override public int nextInt(int bound) {
            lastBound = bound;
            if (index < 0 || index >= bound) throw new IllegalArgumentException("fixed index outside bound");
            return index;
        }

        @Override public boolean nextBoolean() { throw new UnsupportedOperationException(); }
        @Override public void shuffle(List<?> values) { throw new UnsupportedOperationException(); }
        @Override public long seed() { return 0; }
    }
}
