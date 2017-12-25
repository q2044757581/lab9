package com.cyp.lab9;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.cyp.lab9.MainActivity.createRetrofit;

public class ReposActivity extends AppCompatActivity {
    String login = "";
    private GithubService githubService;
    ProgressBar Progress;
    RecyclerView ReposList;
    List<Map<String, Object>> Repos = new ArrayList<>();
    CommonAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repos);
        Progress = (ProgressBar)findViewById(R.id.detailProgress);
        ReposList = (RecyclerView)findViewById(R.id.detailList);
        Retrofit GithubRetrofit = createRetrofit("https://api.github.com/");
        githubService = GithubRetrofit.create(GithubService.class);
        Bundle extras = getIntent().getExtras();
        if(extras != null)
        {
            login = extras.getString("name");
        }
        ReposList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CommonAdapter(this, R.layout.item2, Repos) {
            @Override
            public void convert(ViewHolder holder, Map<String, Object> s) {
                TextView name = holder.getView(R.id.repos_name);
                name.setText(s.get("name").toString());
                TextView language = holder.getView(R.id.repos_lang);
                language.setText(s.get("language").toString());
                TextView description = holder.getView(R.id.repos_describe);
                description.setText(s.get("description").toString());
            }
        };
        ReposList.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new CommonAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse(mAdapter.getData(position, "html_url"));
                intent.setData(content_url);
                startActivity(intent);
            }
            @Override
            public boolean onLongClick(int position) {
                return false;
            }
        });
        githubService.getRepos(login)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Repos>>() {
                    @Override
                    public void onCompleted() {
                        Progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(ReposActivity.this, e.getMessage()+"确认你搜索的用户存在", Toast.LENGTH_LONG).show();
                        Progress.setVisibility(View.GONE);
                    }
                    @Override
                    public void onNext(List<Repos> details) {
                        for(int i = 0; i < details.size(); i++)
                        {
                            mAdapter.addData(details.get(i));
                        }
                    }
                });
    }
}
