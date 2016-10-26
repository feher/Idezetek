package net.feheren_fekete.idezetek.quoteeditor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.model.Quote;

import java.util.Collections;
import java.util.List;

import bolts.Task;

public class QuoteEditor extends AppCompatActivity {

    private static final String TAG = QuoteEditor.class.getSimpleName();

    public static final String EXTRA_BOOK_TITLE =
            QuoteEditor.class.getCanonicalName() + ".EXTRA_BOOK_TITLE";

    public static final String EXTRA_QUOTE_INDEX =
            QuoteEditor.class.getCanonicalName() + ".EXTRA_QUOTE_INDEX";

    private String mBookTitle;
    private int mQuoteIndex;
    private DataModel mDataModel;
    private List<Quote> mQuotes = Collections.emptyList();
    private EditText mQuoteAuthorEditText;
    private EditText mQuoteEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.quote_editor_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.quote_editor_activity_title);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        mDataModel = new DataModel(this);
        mQuoteAuthorEditText = (EditText) findViewById(R.id.quote_author);
        mQuoteEditText = (EditText) findViewById(R.id.quote_text);

        initFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initFromIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Task.callInBackground(() -> {
            return mDataModel.loadQuotes(mBookTitle);
        }).continueWith((task) -> {
            mQuotes = task.getResult();
            mQuoteAuthorEditText.setText(mQuotes.get(mQuoteIndex).getAuthor());
            mQuoteEditText.setText(mQuotes.get(mQuoteIndex).getQuote());
            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quote_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                // Respond to the action bar's Up/Home button
                finish();
                return true;
            }
            case R.id.action_done: {
                if (saveQuote()) {
                    finish();
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }


    private void initFromIntent(Intent intent) {
        mBookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE);
        if (TextUtils.isEmpty(mBookTitle)) {
            // TODO: Report error.
            Log.e(TAG, "Missing book title");
            finish();
            return;
        }

        mQuoteIndex = intent.getIntExtra(EXTRA_QUOTE_INDEX, -1);
        if (mQuoteIndex == -1) {
            // TODO: Report error.
            Log.e(TAG, "Missing quote index");
            finish();
            return;
        }
    }

    private boolean saveQuote() {
        String quoteAuthor = mQuoteAuthorEditText.getText().toString().trim();
        String quoteText = mQuoteEditText.getText().toString().trim();
        if (quoteAuthor.isEmpty()) {
            Toast.makeText(this, "Author cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        } else if (quoteText.isEmpty()) {
            Toast.makeText(this, "Quote cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Quote quote = mQuotes.get(mQuoteIndex);
            quote.setAuthor(quoteAuthor);
            quote.setQuote(quoteText);
            Task.callInBackground(() -> {
                return mDataModel.updateQuotes(mBookTitle, mQuotes);
            }).continueWith((task) -> {
                boolean isOk = task.getResult();
                if (!isOk) {
                    Toast.makeText(this, "Cannot save quote", Toast.LENGTH_SHORT).show();
                }
                return null;
            }, Task.UI_THREAD_EXECUTOR);
            return true;
        }
    }

}
