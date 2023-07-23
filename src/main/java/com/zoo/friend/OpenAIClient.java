package com.zoo.friend;

import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.zoo.friend.common.ErrorCode;
import com.zoo.friend.constant.OpenAI;
import com.zoo.friend.entity.models.Problem;
import com.zoo.friend.exception.BusinessException;
import com.zoo.friend.factory.FastJsonConverterFactory;
import com.zoo.friend.interfaces.OpenAIGPTInterface;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.util.StringUtils;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * @author 咏鹅
 * @version 1.0
 * @description TODO
 * @date 2023/7/23 13:14
 */
public class OpenAIClient {

    @Getter
    private OpenAIGPTInterface openAIGPTInterface;

    @Getter
    private OkHttpClient okHttpClient;

    @Getter
    private String apikey;

    @Getter
    private String host;

    //是否开启代理
    @Getter
    private Boolean isProxy;

    public static PartyRun Party(){
        return new PartyRun();
    }

    public OpenAIClient(PartyRun run){
        if(StringUtils.containsWhitespace(run.host)){
            host = OpenAI.HOST_URL;
        }else host = run.host;

        host = run.host;
        if(StringUtils.containsWhitespace(run.apikey)){
            throw new BusinessException(ErrorCode.APIKEY_ERROR);
        }


        apikey = run.apikey;

        okHttpClient = run.okHttpClient;

        openAIGPTInterface =  new Retrofit.Builder()
                .baseUrl(host)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(FastJsonConverterFactory.create())
                .build().create(OpenAIGPTInterface.class);
    }

    private OkHttpClient okHttpClient() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",7890));
        return new OkHttpClient
                .Builder()
                .proxy(proxy)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void streamCompletions(Problem pro, EventSourceListener source){

        EventSource.Factory factory = EventSources.createFactory(this.okHttpClient);
        String requestBody = JSONUtil.toJsonStr(pro);
        Request request = new Request.Builder()
                .url(this.host + "v1/chat/completions")
                .post(RequestBody.create(requestBody,MediaType.parse(ContentType.JSON.getValue())))
                .build();
        //创建事件
        EventSource eventSource = factory.newEventSource(request, source);
    }

    public void streamCompletions(String question, EventSourceListener source) {
        Problem pro = Problem.builder()
                .stream(true)
                .description(question)
                .build();
        this.streamCompletions(pro, source);
    }
    public static final class PartyRun{
        public PartyRun(){

        }
        public OpenAIClient partyRun(){
            return new OpenAIClient(this);
        }
        //openAI密钥
        private String apikey;

        private OkHttpClient okHttpClient;

        //请求地址
        private String host;

        public PartyRun apikey(@NonNull String apikey) {
            this.apikey = apikey;
            return this;
        }

        public PartyRun host(String host) {
            this.host = host;
            return this;
        }

        public PartyRun okHttpClient(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
            return this;
        }
    }




}