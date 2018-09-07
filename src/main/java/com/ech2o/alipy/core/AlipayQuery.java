package com.ech2o.alipy.core;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Created by KAI on 2018/9/6.
 * ectest@foxmail.com
 */
@Configuration
@EnableWebSocketMessageBroker
public class AlipayQuery extends AbstractWebSocketMessageBrokerConfigurer {


    @Resource
    private SimpMessagingTemplate messagingTemplate;


    @Value("${url}")
    String url;
    // 账单开始日期
    @Value("${startTime}")
    String startTime;
    // 账单截止日期
    @Value("${endTime}")
    String endTime;
    // 支付宝 id
    @Value("${billUserId}")
    String billUserId;
    // 账单类型
    @Value("${status}")
    String status;
    @Value("${cookie}")
    String cookie;
    @Value("${referer}")
    String referer;


    private static BasicThreadFactory factory = new BasicThreadFactory.Builder()
            .namingPattern("MY ALIPAY QUERY")
            .daemon(true)
            .priority(Thread.MAX_PRIORITY)
            .build();


    private static ExecutorService executorService = new ThreadPoolExecutor(5, 200, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory, new ThreadPoolExecutor.AbortPolicy());


    private boolean notError = true;

    private int errNum = 0;


    /**
     * 调用 支付宝 web 版接口
     *
     * @return
     * @throws IOException
     */
    public String query() throws IOException {
        String rt;
        rt = Request.Post(url).bodyForm(Form.form()
                .add("startTime", startTime)
                .add("endTime", endTime)
                .add("billUserId", billUserId)
                .add("status", status).build())
                .addHeader("Cookie", cookie)
                .addHeader("Referer", referer)
                .connectTimeout(3000)
                .execute()
                .returnContent().asString();
        return rt;
    }

    /**
     * 线程轮询并使用 websocket 推送
     */
    @PostConstruct
    public void socketPush() {
        System.out.println("Just do it ~");

        Runnable payLog = () -> {
            while (notError) {
                try {
                    String log = query();
                    if (StringUtils.isNotEmpty(log)) {
                        System.out.println(log);
                        JSONObject logObj = JSONObject.parseObject(log);
                        // 当 cookie 失效或其他错误，使用 server酱（http://sc.ftqq.com） 报警通知
                        if ("succeed".equals(logObj.getString("status"))) {
                            // 此处可将 log 自行入库筛选保存再精简推送
                            if (messagingTemplate != null) {
                                messagingTemplate.convertAndSend("/paylog", log);
                                sleep(1500);
                            }

                        } else {
                            waring("账单轮询失败", String.valueOf(System.currentTimeMillis()));
                            sleep(1500);
                            if (errNum++ == 3) {
                                waring("轮询线程已关闭", "请重新登录支付宝web版，替换cookie");
                                notError = false;
                            }
                        }


                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };

        executorService.submit(payLog);
    }


    /**
     * server酱通知
     * http://sc.ftqq.com
     */
    public void waring(String text, String desp) {
        String url = "https://sc.ftqq.com/SCU1385T395a4fdb3b786249f3dc287f3a32bee45b91fcdd09ab7.send?text=" + text + "&desp=" + desp;
        try {
            String rt = Request
                    .Get(url)
                    .execute().returnContent().asString();
            System.out.println(rt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/push").withSockJS();
    }
}
