package net.feheren_fekete.idezetek.quotes;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import net.feheren_fekete.idezetek.QuotesPreferences;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.quoteeditor.QuoteEditor;
import net.feheren_fekete.idezetek.utils.UiUtils;
import net.feheren_fekete.idezetek.widget.QuotesWidgetService;

public class QuotesActivity extends AppCompatActivity
        implements QuotesAdapter.Listener, DataModel.Listener {

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

        setContentView(R.layout.quotes_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mBookTitle);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        DataModel dataModel = new DataModel(this);
        dataModel.setListener(this);
        mQuotesAdapter = new QuotesAdapter(dataModel, PreferenceManager.getDefaultSharedPreferences(this));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quotes_menu, menu);
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
            case R.id.action_jump_to_quote: {
                return true;
            }
            case R.id.action_search_quote: {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onQuotesChanged(String bookTitle) {
        if (mBookTitle.equals(bookTitle)) {
            mQuotesAdapter.loadItems(bookTitle);
        }
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
        editQuote(position);
    }

    @Override
    public void onItemLongClicked(int position) {
        int menuResourceId = R.array.quotes_context_menu;
        int deleteQuoteMenuItem = 0;
        int setQuoteMenuItem = -1;
        if (ACTION_SET_WIDGET_QUOTE.equals(mIntentAction)
                && mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            menuResourceId = R.array.quotes_context_menu_for_widget;
            setQuoteMenuItem = 0;
            deleteQuoteMenuItem = 1;
        }

        int finalSetQuoteMenuItem = setQuoteMenuItem;
        int finalDeleteQuoteMenuItem = deleteQuoteMenuItem;
        String quoteText = mQuotesAdapter.getQuote(position).getQuote();
        int truncatedQuoteTextLength = Math.min(quoteText.length(), 20);
        String truncatedQuoteText = quoteText.substring(0, truncatedQuoteTextLength) + "...";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(truncatedQuoteText);
        builder.setItems(menuResourceId, (dialogInterface, i) -> {
            if (finalSetQuoteMenuItem == i) {
                setQuoteOnWidget(position);
            } else if (finalDeleteQuoteMenuItem == i) {
                deleteQuote(position);
            }
        });
        builder.create().show();
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
                UiUtils.showToastAtCenter(this, R.string.quotes_toast_longtap_help, Toast.LENGTH_SHORT);
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

    private void deleteQuote(int position) {
        // TODO
        Toast.makeText(this, R.string.toast_feature_not_implemented, Toast.LENGTH_SHORT).show();
    }

}
