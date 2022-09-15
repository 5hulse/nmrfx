package org.nmrfx.processor.datasets;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

public class DatasetCompare {

    public static long compare(File refFile, File testFile) throws IOException {
        long refLen = refFile.length();
        long testLen = testFile.length();
        long result;

        if (refLen != testLen) {
            result = Math.min(refLen, testLen);
        } else {
            result = Files.mismatch(refFile.toPath(), testFile.toPath());
        }
        return result;
    }

     static long[] compareParts(File refFile, File testFile) throws IOException {
        long refLen = refFile.length();
        long testLen = testFile.length();
        long fileSizeError = -1;
        long headerErrorPosition = -1;
        long dataErrorPosition = -1;

        if (refLen != testLen) {
            fileSizeError = Math.min(refLen, testLen);
        } else {
            RandomAccessFile refRAFile = new RandomAccessFile(refFile, "r");
            RandomAccessFile testRAFile = new RandomAccessFile(testFile, "r");

            MappedByteBuffer refBuffer = refRAFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, refLen);
            MappedByteBuffer testBuffer = testRAFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, testLen);
            if (refBuffer.compareTo(testBuffer) != 0) {
                int headerSize = refFile.getName().equals("data.dat") ? 0 :
                        refFile.getName().endsWith(".nv") ? Dataset.NV_HEADER_SIZE : Dataset.UCSF_HEADER_SIZE;
                refBuffer.position(0);
                testBuffer.position(0);
                for (int i = 0; i < headerSize; i++) {
                    if (refBuffer.get(i) != testBuffer.get(i)) {
                        headerErrorPosition = i;
                        break;
                    }
                }
                for (int i = headerSize; i < refLen; i++) {
                    if (refBuffer.get(i) != testBuffer.get(i)) {
                        dataErrorPosition = i;
                        break;
                    }
                }
            }
        }
        return new long[]{fileSizeError, dataErrorPosition, headerErrorPosition};
    }

    public static long[] compareDetailed(File refFile, File testFile) throws IOException {
        long refLen = refFile.length();
        long testLen = testFile.length();
        long[] result = {-1, -1, -1};

        if (refLen != testLen) {
            result[0] = Math.min(refLen, testLen);
        } else {
            long matchScore = Files.mismatch(refFile.toPath(), testFile.toPath());
            if (matchScore != -1) {
                result = compareParts(refFile, testFile);
            }
        }
        return result;
    }

    public static long compareFloat(File refFile, File testFile) throws IOException {
        long refLen = refFile.length();
        long testLen = testFile.length();
        long result = -1;

        if (refLen != testLen) {
            result = Math.min(refLen, testLen);
        } else {
            try (DataInputStream refStream = new DataInputStream(new FileInputStream(refFile));
                 DataInputStream testStream = new DataInputStream(new FileInputStream(testFile))) {
                int i = 0;
                while (refStream.available() > 0) {
                    if (Float.compare(refStream.readFloat(), testStream.readFloat()) != 0) {
                        result = i == 0? -1 : i;
                        break;
                    }
                    i++;
                }
            }
        }
        return result;
    }
}
