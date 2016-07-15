package net.feheren_fekete.idezetek.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import net.feheren_fekete.idezetek.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DataModel {

    private static final String TAG = DataModel.class.getSimpleName();

    private static final String QUOTES_DIR = "quotes";
    private static final String QUOTES_FILE_POSTFIX = "_quotes.txt";

    private Context mContext;
    private String mQuotesDirPath;

    public DataModel(Context context) {
        mContext = context;
        mQuotesDirPath = mContext.getFilesDir().getAbsolutePath() + File.separator + QUOTES_DIR;
    }

    public List<Quote> loadQuotes(String tag) {
        List<Quote> result = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(mQuotesDirPath + File.separator + tag + QUOTES_FILE_POSTFIX)));
            String line;
            while ((line = reader.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon != -1) {
                    String author = line.substring(0, colon);
                    String quote = line.substring(colon + 1);
                    result.add(new Quote(author, quote));
                }
            }
        } catch (IOException e) {
            // TODO: Report error.
            Log.e(TAG, "Cannot load quotes", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }

        return result;
    }

    public void initDefaultQuotes() {
        File destinationFile = new File(mQuotesDirPath + File.separator + "buddha" + QUOTES_FILE_POSTFIX);
        if (destinationFile.exists()) {
            return;
        }

        AssetManager assetManager = mContext.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("buddha" + QUOTES_FILE_POSTFIX);
            FileUtils.copyFile(inputStream, destinationFile);
        } catch (IOException e) {
            // TODO: Report error.
            Log.e(TAG, "Cannot init default quotes", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

}
