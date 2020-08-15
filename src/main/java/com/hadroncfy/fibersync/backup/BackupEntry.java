package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.util.FileUtil;
import com.hadroncfy.fibersync.util.copy.FileCopier;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

public class BackupEntry implements Comparable<BackupEntry> {
    private static final String WORLDDIR = "world";
    private static final String INFO_JSON = "info.json";

    private final BackupInfo info;
    private final Path dir;

    public BackupEntry(Path dir, BackupInfo info) {
        this.info = info;
        this.dir = dir;
    }

    public BackupInfo getInfo() {
        return info;
    }

    public void writeInfo() throws IOException {
        File infoFile = dir.resolve(INFO_JSON).toFile();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(infoFile), StandardCharsets.UTF_8)) {
            writer.write(BackupInfo.GSON.toJson(info));
        }
    }

    private boolean checkWorldDir() {
        Path d = dir.resolve(WORLDDIR);
        return d.toFile().isDirectory() && d.resolve("level.dat").toFile().exists() && d.resolve("region").toFile().isDirectory();
    }

    public boolean exists() {
        return dir.resolve(INFO_JSON).toFile().exists() && checkWorldDir();
    }

    public void doBackup(Path worldDir, FileOperationProgressListener listener) throws IOException,
            NoSuchAlgorithmException {
        File dirFile = dir.toFile();
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        Path backupDir = dir.resolve(WORLDDIR);
        File f = backupDir.toFile();
        if (!f.exists()) {
            f.mkdirs();
        }

        info.size = FileCopier.copy(worldDir, backupDir, FibersyncMod.getConfig().excludes, listener);
        writeInfo();
    }

    public void delete(FileOperationProgressListener listener) throws IOException {
        FileCopier.deleteFileTree(dir, listener);
    }

    public void overwriteTo(BackupEntry entry) throws IOException {
        File f1 = entry.dir.toFile();
        File dest = entry.dir.resolveSibling(info.name).toFile();
        if (!f1.renameTo(dest)){
            throw new IOException("Failed to rename entry " + f1.toString() + " to " + dest.toString());
        }
    }

    public void back(Path worldDir, FileOperationProgressListener listener) throws NoSuchAlgorithmException, IOException {
        FileCopier.copy(dir.resolve(WORLDDIR), worldDir, FibersyncMod.getConfig().excludes, listener);
    }

    @Override
    public int compareTo(BackupEntry o) {
        return o.info.date.compareTo(info.date);
    }

    public boolean collides(BackupEntry other){
        return dir.equals(other.dir);
    }

    public void copyTo(BackupEntry other, FileOperationProgressListener listener)
            throws NoSuchAlgorithmException, IOException {
        other.doBackup(dir.resolve(WORLDDIR), listener);
    }

    public BackupEntry createAtNewDir(Path dir){
        return new BackupEntry(dir, info);
    }

    public long totalSize() throws IOException {
        if (info.size == 0){
            info.size = FileUtil.totalSize(dir);
            writeInfo();
        }
        return info.size;
    }
}