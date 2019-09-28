package com.demo.shop.utils;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderRecord implements Serializable {

    private Long id;

    private String userId;

    public OrderRecord() {
    }

    public OrderRecord(Long id, String userId) {
        this.id = id;
        this.userId = userId;
    }
}
