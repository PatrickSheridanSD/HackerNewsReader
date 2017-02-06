package com.example.patri.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

   static Map<Integer, String> articleURLs = new HashMap<Integer, String>();
   static Map<Integer, String> articleTitles = new HashMap<Integer, String>();
   static ArrayList<Integer> articleIDs = new ArrayList<Integer>();

    SQLiteDatabase articlesDB;
    ArrayList <String> titles = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    ArrayList <String> urls = new ArrayList<String>();
    ArrayList <String> content = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //START OF MY CODE

        ListView listView = (ListView)findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Intent i = new Intent(getApplicationContext(),ArticleActivity.class);
                i.putExtra("articleUrl",urls.get(position));
                i.putExtra("content",content.get(position));
                startActivity(i);


            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER (10), url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();



        try {
           task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {



        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public void updateListView(){

        try {

            Log.i("UI updated", "Done");

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);

            int contentIdIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            c.moveToFirst();

            titles.clear();
            urls.clear();

            while (c != null) {

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                content.add(c.getString(contentIdIndex));

                c.moveToNext();

            }

            arrayAdapter.notifyDataSetChanged();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();

                }


                JSONArray jsonArray = new JSONArray(result);

                articlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < 20; i++) {

                    String articleID = jsonArray.getString(i);

                    url = new  URL("https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1){

                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }



                    JSONObject jsonObject = new JSONObject(articleInfo);



                    String articleTitle = jsonObject.getString("title");

                    String articleUrl = jsonObject.getString("url");

                    String articleContent = "";


                      /*

                    url = new  URL(articleUrl);

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                    reader = new InputStreamReader(in);

                    data = reader.read();

                    //String articleContent = "";

                    while (data != -1){

                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }
                    */

                    articleIDs.add(Integer.valueOf(articleID));

                    articleTitles.put(Integer.valueOf(articleID), articleTitle);

                    articleURLs.put(Integer.valueOf(articleID), articleUrl);

                    String sql = "INSERT INTO articles (articleId, url, title,content) VALUES (? , ? , ? , ?)";

                    SQLiteStatement statement = articlesDB.compileStatement(sql);

                    statement.bindString(1, articleID);
                    statement.bindString(2, articleUrl);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleContent);

                    statement.execute();

                }


            }catch (Exception e){

                e.printStackTrace();

            }


            return result;
        }

        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);

            updateListView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


