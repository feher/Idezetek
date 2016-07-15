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

    public static void copyFileOrDirectory(String srcDir, String dstDir) {

        try {
            File src = new File(srcDir);
            File dst = new File(dstDir, src.getName());

            if (src.isDirectory()) {
                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = dst.getPath();
                    copyFileOrDirectory(src1, dst1);
                }
            } else {
                copyFile(src, dst);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot copy", e);
        }
    }

    public static void copyFile(File src, File dst) {
        InputStream in = null;

        try {
            in = new FileInputStream(src);
            copyFile(in, dst);
        } catch (IOException e) {
            Log.e(TAG, "Cannot copy file: " + dst.getAbsolutePath(), e);
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

    public static void copyFile(InputStream in, File dst) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot copy file: " + dst.getAbsolutePath(), e);
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
