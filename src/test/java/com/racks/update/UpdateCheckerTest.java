package com.racks.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Version ordering for the update notice.
 *
 * <p>Worth pinning down because the failure is silent and one-directional: get it wrong and either
 * every operator is told about an update that does not exist, or nobody is ever told about one that
 * does. Segments are compared as numbers rather than text, which is the whole point, {@code 1.0.10}
 * really is newer than {@code 1.0.9} even though it sorts earlier as a string.
 */
class UpdateCheckerTest {

    @Test
    void aHigherVersionIsNewer() {
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"));
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.9"));
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"));
    }

    @Test
    void theSameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"));
    }

    @Test
    void anOlderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.1"));
        assertFalse(UpdateChecker.isNewer("1.9.9", "2.0.0"));
    }

    @Test
    void segmentsCompareAsNumbersNotText() {
        assertTrue(UpdateChecker.isNewer("1.0.10", "1.0.9"));
        assertFalse(UpdateChecker.isNewer("1.0.9", "1.0.10"));
    }

    @Test
    void aMissingSegmentCountsAsZero() {
        assertTrue(UpdateChecker.isNewer("1.1", "1.0.9"));
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.0"));
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0"));
    }

    @Test
    void preReleaseSuffixesAreIgnored() {
        // The running build is what carries -SNAPSHOT; a release must still register as newer.
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0-SNAPSHOT"));
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0-SNAPSHOT"));
    }
}
