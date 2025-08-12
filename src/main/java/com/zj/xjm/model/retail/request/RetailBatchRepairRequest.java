package com.zj.xjm.model.retail.request;

import lombok.Data;

import java.util.List;


@Data
public class RetailBatchRepairRequest {


    private List<String> orderList;
}
