package net.feheren_fekete.idezetek.quotebooks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.feheren_fekete.idezetek.QuotesActivity;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.Book;
import net.feheren_fekete.idezetek.model.DataModel;

import java.io.FileNotFoundException;
import java.io.InputStream;

import bolts.Task;

public class QuoteBooksActivity extends AppCompatActivity implements QuoteBooksAdapter.Listener {

    private static final String TAG = QuoteBooksActivity.class.getSimpleName();

    private static final int PICK_FILE_REQUEST_CODE = 1;

    private RecyclerView mRecyclerView;
    private DataModel mDataModel;
    private QuoteBooksAdapter mQuoteBooksAdapter;
    private FloatingActionButton mFloatingActionButton;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote_books);

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

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(mFabClickListener);
    }

    private View.OnClickListener mFabClickListener = view -> {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
        Toast.makeText(this, R.string.import_toast_pick_book, Toast.LENGTH_SHORT);
    };

    @Override
    protected void onResume() {
        super.onResume();
        mQuoteBooksAdapter.loadItems();
    }

    @Override
    public void onItemsLoaded() {

    }

    @Override
    public void onItemClicked(Book book) {
        Intent intent = new Intent(this, QuotesActivity.class);
        intent.putExtra(QuotesActivity.EXTRA_BOOK_TITLE, book.getTitle());
        startActivity(intent);
    }

    @Override
    public void onItemLongClicked(Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(book.getTitle());
        builder.setItems(R.array.quote_book_context_menu_items, (dialogInterface, i) -> {
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
        if (requestCode == PICK_FILE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri bookUri = null;
                if (data != null) {
                    bookUri = data.getData();
                    askTitleAndImport(bookUri);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void renameBook(Book book) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(book.getTitle());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.import_dialog_book_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (mDataModel.doesBookExist(newTitle)) {
                Toast.makeText(this, R.string.title_exists, Toast.LENGTH_SHORT).show();
                mHandler.post(() -> renameBook(book));
            } else if (newTitle.isEmpty()) {
                Toast.makeText(this, R.string.title_invalid, Toast.LENGTH_SHORT).show();
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
        builder.setTitle(R.string.import_dialog_book_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (mDataModel.doesBookExist(title)) {
                Toast.makeText(this, R.string.title_exists, Toast.LENGTH_SHORT).show();
                mHandler.post(() -> askTitleAndImport(bookUri));
            } else if (title.isEmpty()) {
                Toast.makeText(this, R.string.title_invalid, Toast.LENGTH_SHORT).show();
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
}