package com.db2ghsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.db2ghsync.DropboxGitSyncApplication.CommandLineOptions;
import com.db2ghsync.common.SyncDirection;

class DropboxGitSyncApplicationTest {

    @Test
    void testParseValidArguments() {
        CommandLineOptions options = CommandLineOptions.parse(
                new String[] { "--config", "C:\\\\config.properties", "--direction", "dbx-to-git" });

        assertEquals("C:\\\\config.properties", options.configPath());
        assertEquals(SyncDirection.DBX_TO_GIT, options.direction());
    }

    @Test
    void testParseMissingDirectionThrows() {
        assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse(
                new String[] { "--config", "config.properties" }));
    }

    @Test
    void testParseUnknownArgumentThrows() {
        assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse(
                new String[] { "--config", "cfg", "--direction", "dbx-to-git", "--unknown" }));
    }

    @Test
    void testParseMissingValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse(
                new String[] { "--config" }));
    }
}

