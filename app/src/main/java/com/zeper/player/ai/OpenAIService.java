package com.zeper.player.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("v1/chat/completions")
    Call<ChatResponse> chat(
            @Header("Authorization") String auth,
            @Body ChatRequest request
    );
}
