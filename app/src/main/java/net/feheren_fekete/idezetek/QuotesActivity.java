package net.feheren_fekete.idezetek;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import net.feheren_fekete.idezetek.model.DataModel;

import java.util.List;

public class QuotesActivity extends AppCompatActivity implements QuotesAdapter.Listener {

    private String mQuotesTag;
    private RecyclerView mRecyclerView;
    private QuotesAdapter mQuotesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quotes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Loading...");

        mQuotesTag = "buddha";

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mQuotesAdapter = new QuotesAdapter(new DataModel(this));
        mQuotesAdapter.setListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mQuotesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQuotesAdapter.loadItems(mQuotesTag);
    }

    @Override
    public void onItemsLoaded() {
        List<String> authors = mQuotesAdapter.getAuthors();
        StringBuilder authorsBuilder = new StringBuilder();
        for (String author : authors) {
            authorsBuilder.append(author).append(", ");
        }
        String authorsString = authorsBuilder.toString();
        authorsString = authorsString.substring(0, authorsString.length() - 2);
        getSupportActionBar().setTitle(authorsString);
    }
}
