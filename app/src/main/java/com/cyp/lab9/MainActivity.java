package com.cyp.lab9;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    public static int MODE = MODE_PRIVATE;
    public static String PREFERENCE_NAME = "SaveLogin";
    List<Map<String, Object>> GithubList = new ArrayList<>();
    CommonAdapter GithubAdapter;
    private int cntLogin = 0;
    private GithubService githubservice;
    private ProgressBar waitPrograss;
    public static Retrofit createRetrofit(String baseUrl){//构造Retrofit对象实现网络访问
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createOkHttp())
                .build();
    }

    public static OkHttpClient createOkHttp(){//Retrofit也是基于OKHttp的封装，所以配置相应的OKHttp对象
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)//连接超时
                .readTimeout(30, TimeUnit.SECONDS)//读超时
                .writeTimeout(10, TimeUnit.SECONDS)//写超时
                .build();
        return okHttpClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences(PREFERENCE_NAME, MODE);
        if(sharedPreferences.getString("cntLogin","").equals("")){//初始化为0
            SharedPreferences.Editor SPEdit = sharedPreferences.edit();
            SPEdit.putString("cntLogin", Integer.toString(0));
            SPEdit.commit();
        }else{
            cntLogin = Integer.parseInt(sharedPreferences.getString("cntLogin", "0"));
        }
        Button clearBtn = (Button)findViewById(R.id.clearBtn);
        Button fetchBtn = (Button)findViewById(R.id.fetchBtn);
        RecyclerView contentList = (RecyclerView)findViewById(R.id.recycler_view);
        waitPrograss = (ProgressBar)findViewById(R.id.mian_prograss);
        Retrofit GithubRetrofit =createRetrofit("https://api.github.com/");
        githubservice = GithubRetrofit.create(GithubService.class);
        contentList.setLayoutManager(new LinearLayoutManager(this));
        GithubAdapter = new CommonAdapter(this, R.layout.item, GithubList) {//填充adapter
            @Override
            public void convert(ViewHolder holder, Map<String, Object> s) {
                TextView name = holder.getView(R.id.item_name);
                name.setText(s.get("name").toString());
                TextView id = holder.getView(R.id.item_id);
                id.setText(s.get("id").toString());
                TextView blog = holder.getView(R.id.item_blog);
                blog.setText(s.get("blog").toString());
            }
        };
        contentList.setAdapter(GithubAdapter);
        GithubAdapter.setOnItemClickListener(new CommonAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {//点击跳到个人
                Intent intent = new Intent(MainActivity.this, ReposActivity.class);
                intent.putExtra("name", GithubAdapter.getData(position, "name"));
                startActivity(intent);
            }

            @Override
            public boolean onLongClick(int position) {//长按删除
                GithubAdapter.removeData(position);
                return true;
            }
        });
        //初始化
        for(int i = 0; i < cntLogin; i++){
            String User = sharedPreferences.getString(Integer.toString(i), "");
            waitPrograss.setVisibility(View.VISIBLE);
            githubservice.getUser(User)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<user>() {
                        @Override
                        public void onCompleted() {//完成传输
                            waitPrograss.setVisibility(View.GONE);
                        }
                        @Override
                        public void onError(Throwable e) {//请求出现错误
                            Toast.makeText(MainActivity.this, e.getMessage()+"请确认你搜索的用户存在", Toast.LENGTH_LONG).show();
                            waitPrograss.setVisibility(View.GONE);
                        }
                        @Override
                        public void onNext(user temp) {//每次收到数据
                            GithubAdapter.addData(temp);
                        }
                    });
        }
        clearBtn.setOnClickListener(new View.OnClickListener() {//清空列表
            @Override
            public void onClick(View v) {
                GithubAdapter.clearData();
                SharedPreferences.Editor SPEdit = sharedPreferences.edit();
                cntLogin = 0;
                SPEdit.clear();
                SPEdit.commit();
            }
        });
        fetchBtn.setOnClickListener(new View.OnClickListener() {//搜索
            @Override
            public void onClick(View v) {
                EditText search = (EditText)findViewById(R.id.searchEdit);
                String user = search.getText().toString();
                waitPrograss.setVisibility(View.VISIBLE);
                githubservice.getUser(user)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<user>() {
                            @Override
                            public void onCompleted() {
                                waitPrograss.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(MainActivity.this, e.getMessage()+"请确认你搜索的用户存在", Toast.LENGTH_LONG).show();
                                waitPrograss.setVisibility(View.GONE);
                            }

                            @Override
                            public void onNext(user github) {
                                GithubAdapter.addData(github);
                                SharedPreferences.Editor SPEdit = sharedPreferences.edit();
                                SPEdit.putString(Integer.toString(cntLogin), github.getLogin());
                                cntLogin = cntLogin + 1;
                                SPEdit.putString("cntLogin", Integer.toString(cntLogin));
                                SPEdit.commit();
                            }
                        });
            }
        });
    }
}
