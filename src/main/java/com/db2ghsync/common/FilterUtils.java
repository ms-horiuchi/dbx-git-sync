
package com.db2ghsync.common;

import java.util.List;

/**
 * ファイル名やパスのフィルタリング処理を提供するユーティリティクラス。
 */
public class FilterUtils {

    /**
     * ファイル名が指定拡張子リストのいずれかで終わるか判定する。
     * 
     * @param fileName   判定対象のファイル名
     * @param extensions 拡張子リスト（例: .txt, .pdf）
     * @return 拡張子が一致すればtrue
     */
    public static boolean matchExtension(String fileName, List<String> extensions) {
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * パス文字列の一番目のディレクトリ名を取得する。
     * 例: /foo/bar.txt -> foo
     * 
     * @param path パス文字列（先頭が/で始まること）
     * @return 一番目のディレクトリ名。該当しない場合は空文字
     */
    public static String getFirstDir(String path) {
        if (path != null && path.startsWith("/")) {
            String[] parts = path.split("/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "";
    }
}