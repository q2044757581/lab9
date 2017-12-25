package com.cyp.lab9;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;


/**
 * Created by Administrator on 2017/12/25.
 */

public interface GithubService {
    @GET("/users/{user}")
    Observable<user>getUser(@Path("user")String user);
    @GET("/users/{user}/repos")
    Observable<List<Repos>>getRepos(@Path("user")String user);
}
