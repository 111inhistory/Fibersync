package com.hadroncfy.fibersync.util.copy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

public class SimpleFileVisitor implements FileVisitor<Path> {
    private final Function<Path, FileVisitResult> consumer;
    private final boolean rev;
    public long size = 0;
    
    public SimpleFileVisitor(Function<Path, FileVisitResult> p, boolean rev){
        consumer = p;
        this.rev = rev;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (rev){
            return consumer.apply(dir);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!rev){
            return consumer.apply(dir);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        consumer.apply(file);
        size += file.toFile().length();
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
    
}