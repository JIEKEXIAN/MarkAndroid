/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intlime.mark.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileTool {
    public static long sizeOf(File file) {
        if (!file.exists()) {
            return 0;
        }
        if (file.isDirectory()) {
            return sizeOfDirectory(file);
        } else {
            return file.length();
        }
    }

    public static long sizeOfDirectory(File directory) {
        if (!checkDirectory(directory)){
            return 0;
        }

        final File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            return 0L;
        }
        long size = 0;

        for (final File file : files) {
            try {
                if (!isSymlink(file)) {
                    size += sizeOf(file);
                    if (size < 0) {
                        break;
                    }
                }
            } catch (IOException ioe) {
                // Ignore exceptions caught when asking if a File is a symlink.
            }
        }
        return size;
    }

    private static boolean checkDirectory(File directory) {
        if (!directory.exists()) {
            return false;
        }
        if (!directory.isDirectory()) {
            return false;
        }
        return true;
    }

    private static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        if (File.separatorChar == '\\') {
            return false;
        }
        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
            return false;
        } else {
            return true;
        }
    }

    public static void cleanDirectory(File directory) {
        try {
            if (!directory.exists()) {
                String message = directory + " does not exist";
                throw new IllegalArgumentException(message);
            }

            if (!directory.isDirectory()) {
                String message = directory + " is not a directory";
                throw new IllegalArgumentException(message);
            }

            File[] files = directory.listFiles();
            if (files == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + directory);
            }

            for (File file : files) {
                try {
                    forceDelete(file);
                } catch (IOException ioe) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent){
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message =
                        "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }
        if (!directory.delete()) {
            String message =
                    "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }
}
