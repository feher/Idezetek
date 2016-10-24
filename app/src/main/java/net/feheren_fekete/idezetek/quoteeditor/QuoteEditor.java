package net.feheren_fekete.idezetek.quoteeditor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

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

        setContentView(R.layout.activity_quote_editor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.quote_editor_activity_title);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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

    private void saveQuote() {
        // TODO
    }

}
