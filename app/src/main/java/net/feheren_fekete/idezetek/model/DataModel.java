package net.feheren_fekete.idezetek.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.util.Log;

import net.feheren_fekete.idezetek.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class DataModel {

    public interface Listener {
        void onQuotesChanged(String bookTitle);
    }

    private static final class BooksChangedEvent {
        public final DataModel sender;
        public BooksChangedEvent(DataModel sender) {
            this.sender = sender;
        }
    }

    private static final class QuotesChangedEvent {
        public final DataModel sender;
        public final String bookTitle;
        public QuotesChangedEvent(DataModel sender, String bookTitle) {
            this.sender = sender;
            this.bookTitle = bookTitle;
        }
    }

    private static final String TAG = DataModel.class.getSimpleName();

    private static final String BOOKS_FILE_NAME = "books.txt";
    private static final String QUOTES_FILE_PREFIX = "quotes_";
    private static final String QUOTES_FILE_POSTFIX = ".txt";

    private Context mContext;
    private String mQuotesDirPath;
    private @Nullable List<Book> mBooks;
    private @Nullable Listener mListener;

    public DataModel(Context context) {
        mContext = context;
        mQuotesDirPath = mContext.getFilesDir().getAbsolutePath();
        mBooks = null;
        EventBus.getDefault().register(this);
    }

    public void close() {
        EventBus.getDefault().unregister(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public List<Book> loadBooks() {
        synchronized (this) {
            return getBooks();
        }
    }

    public List<Quote> loadQuotes(String bookTitle) {
        synchronized (this) {
            List<Quote> result = new ArrayList<>();

            Book book = getBookByTitle(bookTitle);
            if (book == null) {
                return result;
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(mQuotesDirPath + File.separator + book.getFileName())));
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
    }

    public void importQuotes(InputStream quotesStream, String bookTitle) {
        String quotesFileName = QUOTES_FILE_PREFIX
                + String.valueOf(System.currentTimeMillis())
                + QUOTES_FILE_POSTFIX;
        copyQuotes(quotesStream, quotesFileName);
        addOrUpdateBook(new Book(bookTitle, quotesFileName));
        saveBooks();
    }

    public void updateBook(Book book) {
        addOrUpdateBook(book);
        saveBooks();
    }

    public void deleteBook(Book book) {
        List<Book> books = getBooks();
        int existingBookIndex = books.indexOf(book);
        if (existingBookIndex != -1) {
            books.remove(existingBookIndex);
        }
        saveBooks();
    }

    public boolean doesBookExist(String title) {
        for (Book book : mBooks) {
            if (book.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    public void initDefaultQuotes() {
        synchronized (this) {
            List<Book> books = getBooks();
            if (!books.isEmpty()) {
                return;
            }

            AssetManager assetManager = mContext.getAssets();
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open("buddha_quotes.txt");
                importQuotes(inputStream, "Buddha");
                inputStream = assetManager.open("i_quotes.txt");
                importQuotes(inputStream, "I");
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

    public boolean updateQuotes(String bookTitle, List<Quote> quotes) {
        synchronized (this) {
            boolean result = false;

            Book book = getBookByTitle(bookTitle);
            if (book == null) {
                return false;
            }

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(mQuotesDirPath + File.separator + book.getFileName())));
                for (Quote quote : quotes) {
                    writer.write(quote.getAuthor() + ":" + quote.getQuote() + "\n");
                }
                result = true;
            } catch (IOException e) {
                // TODO: Report error.
                Log.e(TAG, "Cannot write quotes to file", e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        // Ignore.
                    }
                }
            }

            EventBus.getDefault().post(new QuotesChangedEvent(this, bookTitle));

            return result;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBooksChangedEvent(DataModel.BooksChangedEvent event) {
        if (event.sender != this) {
            mBooks = reloadBooks();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQuotesChangedEvent(DataModel.QuotesChangedEvent event) {
        if (event.sender != this) {
            if (mListener != null) {
                mListener.onQuotesChanged(event.bookTitle);
            }
        }
    }

    @Nullable
    private Book getBookByTitle(String title) {
        List<Book> books = getBooks();
        for (Book book : books) {
            if (book.getTitle().equals(title)) {
                return book;
            }
        }
        return null;
    }

    private List<Book> getBooks() {
        if (mBooks == null) {
            mBooks = reloadBooks();
        }
        return mBooks;
    }

    private List<Book> reloadBooks() {
        List<Book> result = new ArrayList<>();

        if (!assureBooksFile()) {
            return result;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(mQuotesDirPath + File.separator + BOOKS_FILE_NAME)));
            String line;
            while ((line = reader.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon != -1) {
                    String title = line.substring(0, colon);
                    String fileName = line.substring(colon + 1);
                    result.add(new Book(title, fileName));
                }
            }
        } catch (IOException e) {
            // TODO: Report error.
            Log.e(TAG, "Cannot load books", e);
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

    private void saveBooks() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(mQuotesDirPath + File.separator + BOOKS_FILE_NAME)));
            for (Book book : mBooks) {
                writer.write(book.getTitle() + ":" + book.getFileName());
                writer.newLine();
            }
        } catch (IOException e) {
            // TODO: Report error.
            Log.e(TAG, "Cannot store books", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }

        EventBus.getDefault().post(new BooksChangedEvent(this));
    }

    private boolean assureBooksFile() {
        File booksFile = new File(mQuotesDirPath + File.separator + BOOKS_FILE_NAME);
        if (!booksFile.exists()) {
            try {
                File parentDir = booksFile.getParentFile();
                if (!parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        // TODO: Report error.
                        Log.e(TAG, "Cannot create dir " + parentDir.getAbsolutePath());
                        return false;
                    }
                }
                return booksFile.createNewFile();
            } catch (IOException e) {
                // TODO: Report error.
                Log.e(TAG, "Cannot create books file", e);
                return false;
            }
        }
        return true;
    }

    private void copyQuotes(InputStream quotesStream, String quotesFileName) {
        File quotesFile = new File(mQuotesDirPath + File.separator + quotesFileName);
        FileUtils.copyFile(quotesStream, quotesFile);
    }

    private void addOrUpdateBook(Book book) {
        List<Book> books = getBooks();
        int existingBookIndex = books.indexOf(book);
        if (existingBookIndex != -1) {
            books.get(existingBookIndex).setFileName(book.getFileName());
            books.get(existingBookIndex).setTitle(book.getTitle());
        } else {
            books.add(book);
        }
    }

}
