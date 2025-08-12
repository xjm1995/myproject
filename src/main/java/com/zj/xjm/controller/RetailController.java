package com.zj.xjm.controller;

import com.zj.xjm.model.retail.request.RetailBatchRepairRequest;
import com.zj.xjm.service.RetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/retail")
@RequiredArgsConstructor
public class RetailController {

    private final RetailService retailService;

    @PostMapping("/batchRepair")
    public Objects batchRepair(@RequestBody RetailBatchRepairRequest request) throws InterruptedException, IOException {
        retailService.batchRepair(request);
        return null;
    }
}
