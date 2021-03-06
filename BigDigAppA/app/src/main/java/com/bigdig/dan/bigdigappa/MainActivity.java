package com.bigdig.dan.bigdigappa;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String BROADCAST_DB_UPDATE = "com.bigdig.dan.actin.DB_UPDATED";
    private static final String ACTION_APPB = "com.bigdig.dan.IMAGE";

    private static final String INTENT_URL = "String";
    private static final String INTENT_STATUS = "Status";
    private static final String INTENT_ID = "Id";

    private EditText editTextUrl;

    private SQLiteDatabase mDatabase;

    private List<Link> mLinks;
    private HistoryAdapter mHistoryAdapter;
    private MenuItem mMenuItemSortTime;
    private MenuItem mMenuItemSortStatus;
    private int mSortWas;
    private DbUpdateReceiver mDbUpdateReceiver;
    private final String tab1Id = "tab1";
    private final String tab2Id = "tab2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        initTabs();
        initUI();
    }

    private void initTabs() {
        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec(tab1Id);
        spec.setContent(R.id.tab1);
        spec.setIndicator(getString(R.string.tab_1_name));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec(tab2Id);
        spec.setContent(R.id.tab2);
        spec.setIndicator(getString(R.string.tab_2_name));
        tabHost.addTab(spec);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                switch (tabId) {
                    case tab1Id:
                        hideMenu();
                        break;
                    case tab2Id:
                        showMenu();
                        break;
                }
            }
        });
    }

    private void initDatabase() {
        DbOpenHelper dbOpenHelper = new DbOpenHelper(this);
        mDatabase = dbOpenHelper.getReadableDatabase();
        mDbUpdateReceiver = new DbUpdateReceiver(this);
    }

    private void initUI() {
        editTextUrl = (EditText) findViewById(R.id.image_url);
        Button btnOpenImage = (Button) findViewById(R.id.btn_open_image);
        btnOpenImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeLoadPhotoIntent(editTextUrl.getText().toString(), -1, -1);
            }
        });
        RecyclerView hisoryRecycler = (RecyclerView) findViewById(R.id.tab2);
        hisoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        mLinks = new ArrayList<>();
        mHistoryAdapter = new HistoryAdapter(mLinks);
        hisoryRecycler.setAdapter(mHistoryAdapter);
        updateLinks();
        mSortWas = R.id.action_sort_time;
    }

    private void makeLoadPhotoIntent(String url, int status, int id) {
        if (!url.equals("")) {
            Intent i = new Intent(ACTION_APPB);
            i.putExtra(INTENT_URL, url);
            i.putExtra(INTENT_STATUS, status);
            i.putExtra(INTENT_ID, id);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(i.resolveActivity(getPackageManager())!=null)
                startActivity(i);
            else
                Toast.makeText(this, R.string.app_b_not_installed_alert,Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.url_not_installed_alert, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateLinks() {
        Cursor cursor = mDatabase
                .query(
                        DbOpenHelper.TABLE_NAME,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        mLinks.clear();
        mLinks.addAll(Link.getLinksFromCursor(cursor));
        cursor.close();
        if (mSortWas == R.id.action_sort_time)
            Collections.sort(mLinks, Link.COMPARE_BY_DATE);
        else
            Collections.sort(mLinks, Link.COMPARE_BY_STATUS);
        mHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        mMenuItemSortTime = menu.findItem(R.id.action_sort_time);
        mMenuItemSortStatus = menu.findItem(R.id.action_sort_status);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_time:
                timeSortClick();
                break;
            case R.id.action_sort_status:
                statusSortClick();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void timeSortClick() {
        Collections.sort(mLinks, Link.COMPARE_BY_DATE);
        mHistoryAdapter.notifyDataSetChanged();
        mMenuItemSortTime.setVisible(false);
        mMenuItemSortStatus.setVisible(true);
    }

    private void statusSortClick() {
        Collections.sort(mLinks, Link.COMPARE_BY_STATUS);
        mHistoryAdapter.notifyDataSetChanged();
        mMenuItemSortTime.setVisible(true);
        mMenuItemSortStatus.setVisible(false);
    }

    private void hideMenu() {
        if (mMenuItemSortStatus.isVisible()) {
            mMenuItemSortStatus.setVisible(false);
            mSortWas = R.id.action_sort_time;
        } else {
            mMenuItemSortTime.setVisible(false);
            mSortWas = R.id.action_sort_status;
        }
    }

    private void showMenu() {
        if (mSortWas == R.id.action_sort_status)
            mMenuItemSortTime.setVisible(true);
        else
            mMenuItemSortStatus.setVisible(true);
    }

    @Override
    protected void onResume() {
        updateLinks();
        this.registerReceiver(mDbUpdateReceiver, new IntentFilter(BROADCAST_DB_UPDATE));
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mDbUpdateReceiver);
    }

    private class LinkHolder extends RecyclerView.ViewHolder {
        TextView mUrlTextView;
        TextView mTimeTextView;
        View cardView;

        public LinkHolder(View itemView) {
            super(itemView);
            cardView = itemView;
            mUrlTextView = itemView.findViewById(R.id.url);
            mTimeTextView = itemView.findViewById(R.id.time);
        }

        public void bindLink(final Link link) {
            mUrlTextView.setText(link.getUrl());
            Date linkDate = link.getDate();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM HH:mm:ss");
            String time = simpleDateFormat.format(linkDate);
            mTimeTextView.setText(String.valueOf(time));
            switch (link.getStatus()) {
                case 1:
                    cardView.setBackgroundColor(Color.GREEN);
                    break;
                case 2:
                    cardView.setBackgroundColor(Color.RED);
                    break;
                case 3:
                    cardView.setBackgroundColor(Color.GRAY);
                    break;
            }
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makeLoadPhotoIntent(link.getUrl(), link.getStatus(), link.getID());
                }
            });
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<LinkHolder> {
        private List<Link> mLinkList;

        public HistoryAdapter(List<Link> links) {
            mLinkList = links;
        }

        @Override
        public LinkHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.link_item, parent, false);
            return new LinkHolder(v);
        }

        @Override
        public void onBindViewHolder(LinkHolder holder, int position) {
            holder.bindLink(mLinkList.get(position));
        }

        @Override
        public int getItemCount() {
            return mLinkList.size();
        }
    }
}
