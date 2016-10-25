package net.feheren_fekete.idezetek.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.Book;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.quotebooks.QuoteBooksAdapter;

public class QuotesWidgetConfigActivity extends AppCompatActivity implements QuoteBooksAdapter.Listener {

    private static final String TAG = QuotesWidgetConfigActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private DataModel mDataModel;
    private QuoteBooksAdapter mQuoteBooksAdapter;
    private int mWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quotes_widget_config_activity);

        setTitle(R.string.quote_books_activity_title);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mDataModel = new DataModel(this);
        mQuoteBooksAdapter = new QuoteBooksAdapter(mDataModel);
        mQuoteBooksAdapter.setListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mQuoteBooksAdapter);

        mWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        Toast.makeText(this, "Pick a quote book for the widget", Toast.LENGTH_SHORT).show();

        // Set default result.
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQuoteBooksAdapter.loadItems();
    }

    @Override
    public void onItemsLoaded() {
        // Nothing.
    }

    @Override
    public void onItemClicked(Book book) {
        Intent addWidgetIntent = new Intent(this, QuotesWidgetService.class);
        addWidgetIntent.setAction(QuotesWidgetService.ACTION_ADD_WIDGET);
        addWidgetIntent.putExtra(QuotesWidgetService.EXTRA_WIDGET_ID, mWidgetId);
        addWidgetIntent.putExtra(QuotesWidgetService.EXTRA_BOOK_TITLE, book.getTitle());
        startService(addWidgetIntent);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
        setResult(Activity.RESULT_OK, resultIntent);

        finish();
    }

    @Override
    public void onItemLongClicked(Book book) {
        // Nothing.
    }

}
