package com.db2ghsync.dropbox;

import com.db2ghsync.common.FilterUtils;
import com.db2ghsync.common.SyncAction;
import com.db2ghsync.entity.SyncEntry;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.Metadata;

public class SyncEntryFactory {

    /**
     * DropBox公式のメタデータを独自エントリに変換するメソッド
     * 
     * DropboxのMetadata（ファイル・フォルダ・削除情報）をSyncEntryに変換する。
     *
     * @param metadata Dropbox APIのMetadata
     * @return SyncEntryオブジェクト
     */
    public static SyncEntry convertMetadataToSyncEntry(Metadata metadata) {

        String dropboxPath = metadata.getPathLower();
        String subDirectoryKey = FilterUtils.getFirstDir(dropboxPath);
        String name = metadata.getName();

        SyncAction action = null;
        if (metadata instanceof DeletedMetadata) {
            // 削除の場合
            action = SyncAction.DELETE;
        } else {
            // 新規・更新の場合
            action = SyncAction.CREATE_OR_UPDATE;
        }

        return new SyncEntry(
                dropboxPath,
                name,
                subDirectoryKey,
                action);
    }

}
