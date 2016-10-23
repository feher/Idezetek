package net.feheren_fekete.idezetek.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    public static void copyFileOrDirectory(String sourcePath, String destinationPath) {

        try {
            File sourceFile = new File(sourcePath);
            File destinationFile = new File(destinationPath, sourceFile.getName());

            if (sourceFile.isDirectory()) {
                String files[] = sourceFile.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(sourceFile, files[i]).getPath());
                    String dst1 = destinationFile.getPath();
                    copyFileOrDirectory(src1, dst1);
                }
            } else {
                copyFile(sourceFile, destinationFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot copy", e);
        }
    }

    public static void copyFile(File sourceFile, File destinationFile) {
        InputStream in = null;

        try {
            in = new FileInputStream(sourceFile);
            copyFile(in, destinationFile);
        } catch (IOException e) {
            Log.e(TAG, "Cannot copy file: " + destinationFile.getAbsolutePath(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

    public static void copyFile(InputStream inputStream, File destinationFile) {
        OutputStream out = null;

        File destinationDir = destinationFile.getParentFile();
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                Log.e(TAG, "Cannot create directory: " + destinationDir.getAbsolutePath());
                return;
            }
        }

        try {
            out = new FileOutputStream(destinationFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot copy file: " + destinationFile.getAbsolutePath(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

}
