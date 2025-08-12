package com.zj.xjm.service;

import com.alibaba.fastjson2.JSONObject;
import com.zj.xjm.model.retail.request.RetailBatchRepairRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RetailService {


    public static final String RETAIL_BATCH_REPAIR_URL = "https://gateway.iuctrip.com/retail-admin-service/retail/opr/repair";

    public void batchRepair(@RequestBody RetailBatchRepairRequest req) throws InterruptedException, IOException {
        if (CollectionUtils.isEmpty(req.getOrderList())) {
            return;
        }

        List<Map<String, String>> faileList = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();
        System.out.println("开始");
        for (String orderNo : req.getOrderList()) {
            Thread.sleep(3000);
            String url = RETAIL_BATCH_REPAIR_URL + "?orderNo=" + orderNo;
            // 1. 创建Request对象
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            // 2. 使用client发送请求并获取响应
            Response response = client.newCall(request).execute();
            String string = response.body().string();

            HashMap<String, String> map = new HashMap<>();
            map.put(orderNo, string);
            faileList.add(map);
        }

        System.out.println("结束--------------------------------------------------");
        if (!faileList.isEmpty()) {
            log.error("失败订单及错误信息：{}", JSONObject.toJSONString(faileList));
        }
    }


}
