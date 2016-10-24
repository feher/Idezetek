package net.feheren_fekete.idezetek.quotes;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import net.feheren_fekete.idezetek.QuotesPreferences;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.quoteeditor.QuoteEditor;
import net.feheren_fekete.idezetek.widget.QuotesWidgetService;

public class QuotesActivity extends AppCompatActivity implements QuotesAdapter.Listener {

    private static final String TAG = QuotesActivity.class.getSimpleName();

    public static final String ACTION_SET_WIDGET_QUOTE =
            QuotesActivity.class.getCanonicalName() + ".ACTION_SET_WIDGET_QUOTE";

    public static final String EXTRA_BOOK_TITLE =
            QuotesActivity.class.getCanonicalName() + ".EXTRA_BOOK_TITLE";

    public static final String EXTRA_WIDGET_ID =
            QuotesActivity.class.getCanonicalName() + ".EXTRA_WIDGET_ID";

    private String mIntentAction;
    private String mBookTitle;
    private int mScrollToQuoteIndex = -1;
    private int mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RecyclerView mRecyclerView;
    private QuotesAdapter mQuotesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set default result code.
        setResult(Activity.RESULT_CANCELED);

        initFromIntent(getIntent());

        setContentView(R.layout.activity_quotes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mBookTitle);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mQuotesAdapter = new QuotesAdapter(new DataModel(this));
        mQuotesAdapter.setListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mQuotesAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Set default result code.
        setResult(Activity.RESULT_CANCELED);

        initFromIntent(intent);

        getSupportActionBar().setTitle(mBookTitle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQuotesAdapter.loadItems(mBookTitle);
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

    @Override
    public void onItemsLoaded() {
        if (mScrollToQuoteIndex != -1) {
            mQuotesAdapter.setSelectedItem(mScrollToQuoteIndex);
            mRecyclerView.scrollToPosition(mScrollToQuoteIndex);
            mScrollToQuoteIndex = -1;
        }
    }

    @Override
    public void onItemClicked(int position) {
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)
                && mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(R.array.quote_context_menu_items, (dialogInterface, i) -> {
                switch (i) {
                    case 0:
                        setQuoteOnWidget(position);
                        break;
                    case 1:
                        editQuote(position);
                        break;
                }
            });
            builder.create().show();
        } else {
            editQuote(position);
        }
    }

    private void initFromIntent(Intent intent) {
        mWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mBookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE);
        mIntentAction = intent.getAction();
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)) {
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                QuotesPreferences preferences = new QuotesPreferences(this);
                String widgetBookTitle = preferences.getWidgetBookTitle(mWidgetId);
                if (widgetBookTitle.equals(mBookTitle)) {
                    mScrollToQuoteIndex = preferences.getWidgetQuoteIndex(mWidgetId);
                }
            }
        } else {
            mBookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE);
        }

        if (TextUtils.isEmpty(mBookTitle)) {
            // TODO: Report error.
            Log.e(TAG, "Missing book title");
            finish();
        }
    }

    private void editQuote(int position) {
        Intent intent = new Intent(this, QuoteEditor.class);
        intent.putExtra(QuoteEditor.EXTRA_BOOK_TITLE, mBookTitle);
        intent.putExtra(QuoteEditor.EXTRA_QUOTE_INDEX, position);
        startActivity(intent);
    }


    private void setQuoteOnWidget(int position) {
        Intent setQuoteIntent = new Intent(this, QuotesWidgetService.class);
        setQuoteIntent.setAction(QuotesWidgetService.ACTION_SET_QUOTE);
        setQuoteIntent.putExtra(QuotesWidgetService.EXTRA_WIDGET_ID, mWidgetId);
        setQuoteIntent.putExtra(QuotesWidgetService.EXTRA_BOOK_TITLE, mBookTitle);
        setQuoteIntent.putExtra(QuotesWidgetService.EXTRA_QUOTE_INDEX, position);
        startService(setQuoteIntent);
        setResult(Activity.RESULT_OK);
        finish();
    }

}
