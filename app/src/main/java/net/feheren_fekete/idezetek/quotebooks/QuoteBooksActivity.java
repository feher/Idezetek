package net.feheren_fekete.idezetek.quotebooks;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import net.feheren_fekete.idezetek.quotes.QuotesActivity;
import net.feheren_fekete.idezetek.QuotesPreferences;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.Book;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.utils.UiUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;

import bolts.Task;

public class QuoteBooksActivity extends AppCompatActivity implements QuoteBooksAdapter.Listener {

    private static final String TAG = QuoteBooksActivity.class.getSimpleName();

    public static final String ACTION_SET_WIDGET_QUOTE =
            QuoteBooksActivity.class.getCanonicalName() + ".ACTION_SET_WIDGET_QUOTE";

    public static final String EXTRA_WIDGET_ID =
            QuoteBooksActivity.class.getCanonicalName() + ".EXTRA_WIDGET_ID";

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final int REQUEST_CODE_SET_WIDGET_QUOTE = 2;

    private String mIntentAction;
    private int mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Handler mHandler = new Handler();
    private RecyclerView mRecyclerView;
    private DataModel mDataModel;
    private QuoteBooksAdapter mQuoteBooksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_books_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.quote_books_activity_title);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mDataModel = new DataModel(this);
        mQuoteBooksAdapter = new QuoteBooksAdapter(mDataModel);
        mQuoteBooksAdapter.setListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mQuoteBooksAdapter);

        mIntentAction = getIntent().getAction();
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)) {
            mWidgetId = getIntent().getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            QuotesPreferences preferences = new QuotesPreferences(this);
            String widgetBookTitle = preferences.getWidgetBookTitle(mWidgetId);
            startQuotesActivityForResult(widgetBookTitle);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQuoteBooksAdapter.loadItems();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mIntentAction = getIntent().getAction();
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)) {
            mWidgetId = getIntent().getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            QuotesPreferences preferences = new QuotesPreferences(this);
            String widgetBookTitle = preferences.getWidgetBookTitle(mWidgetId);
            startQuotesActivityForResult(widgetBookTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quote_books_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import: {
                importBook();
                return true;
            }
            case R.id.action_add: {
                createBook();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onItemsLoaded() {
        // Nothing.
    }

    @Override
    public void onItemClicked(Book book) {
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)) {
            startQuotesActivityForResult(book.getTitle());
        } else {
            Intent intent = new Intent(this, QuotesActivity.class);
            intent.putExtra(QuotesActivity.EXTRA_BOOK_TITLE, book.getTitle());
            startActivity(intent);
        }
    }

    @Override
    public void onItemLongClicked(Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(book.getTitle());
        builder.setItems(R.array.quote_books_context_menu, (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    renameBook(book);
                    break;
                case 1:
                    deleteBook(book);
                    break;
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri bookUri = null;
                if (data != null) {
                    bookUri = data.getData();
                    askTitleAndImport(bookUri);
                }
            }
        } else if (requestCode == REQUEST_CODE_SET_WIDGET_QUOTE) {
            if (resultCode == Activity.RESULT_OK) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void importBook() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        UiUtils.showToastAtCenter(this, R.string.import_toast_pick_book, Toast.LENGTH_SHORT);
    }

    private void createBook() {
        // TODO
        Toast.makeText(this, R.string.toast_feature_not_implemented, Toast.LENGTH_SHORT).show();
    }

    private void renameBook(Book book) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(book.getTitle());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.import_dialog_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (mDataModel.doesBookExist(newTitle)) {
                UiUtils.showToastAtCenter(this, R.string.title_exists, Toast.LENGTH_SHORT);
                mHandler.post(() -> renameBook(book));
            } else if (newTitle.isEmpty()) {
                UiUtils.showToastAtCenter(this, R.string.title_invalid, Toast.LENGTH_SHORT);
                mHandler.post(() -> renameBook(book));
            } else {
                book.setTitle(newTitle);
                Task.callInBackground(() -> {
                    mDataModel.updateBook(book);
                    return null;
                }).continueWith((task) -> {
                    mQuoteBooksAdapter.loadItems();
                    return null;
                }, Task.UI_THREAD_EXECUTOR);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void deleteBook(Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.book_delete_dialog_title);
        builder.setMessage(String.format(getResources().getString(R.string.book_delete_dialog_message), book.getTitle()));
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Task.callInBackground(() -> {
                mDataModel.deleteBook(book);
                return null;
            }).continueWith((task) -> {
                mQuoteBooksAdapter.loadItems();
                return null;
            }, Task.UI_THREAD_EXECUTOR);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();

    }

    private void askTitleAndImport(Uri bookUri) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.import_dialog_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (mDataModel.doesBookExist(title)) {
                UiUtils.showToastAtCenter(this, R.string.title_exists, Toast.LENGTH_SHORT);
                mHandler.post(() -> askTitleAndImport(bookUri));
            } else if (title.isEmpty()) {
                UiUtils.showToastAtCenter(this, R.string.title_invalid, Toast.LENGTH_SHORT);
                mHandler.post(() -> askTitleAndImport(bookUri));
            } else {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(bookUri);
                    Task.callInBackground(() -> {
                        mDataModel.importQuotes(inputStream, title);
                        return null;
                    }).continueWith((task) -> {
                        mQuoteBooksAdapter.loadItems();
                        return null;
                    }, Task.UI_THREAD_EXECUTOR);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show();
                    // TODO: Report error.
                    Log.e(TAG, "Cannot import quotes", e);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void startQuotesActivityForResult(String bookTitle) {
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Intent intent = new Intent(this, QuotesActivity.class);
            intent.setAction(QuotesActivity.ACTION_SET_WIDGET_QUOTE);
            intent.putExtra(QuotesActivity.EXTRA_BOOK_TITLE, bookTitle);
            intent.putExtra(QuotesActivity.EXTRA_WIDGET_ID, mWidgetId);
            startActivityForResult(intent, REQUEST_CODE_SET_WIDGET_QUOTE);
        } else {
            // TODO: Report error.
            throw new RuntimeException("Widget ID is invalid");
        }
    }
}
