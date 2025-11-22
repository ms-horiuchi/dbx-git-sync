package com.db2ghsync.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SyncDirectionTest {

    @Test
    void testFromArgumentParsesDbxToGit() {
        assertEquals(SyncDirection.DBX_TO_GIT, SyncDirection.fromArgument("dbx-to-git"));
    }

    @Test
    void testFromArgumentParsesGitToDbxCaseInsensitive() {
        assertEquals(SyncDirection.GIT_TO_DBX, SyncDirection.fromArgument("GIT-TO-DBX"));
    }

    @Test
    void testFromArgumentThrowsOnUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> SyncDirection.fromArgument("invalid"));
    }
}

