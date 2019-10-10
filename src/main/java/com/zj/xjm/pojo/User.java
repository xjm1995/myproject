package com.zj.xjm.pojo;

public class User {
    private Integer id;
    private String name;
    private String token;
    private String accountId;
    private long gmtcreate;
    private  long gmtmodified;

    public void setId(Integer id) {
        this.id = id;
    }

    public long getGmtmodified() {
        return gmtmodified;
    }

    public void setGmtmodified(long gmtmodified) {
        this.gmtmodified = gmtmodified;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getGmtcreate() {
        return gmtcreate;
    }

    public void setGmtcreate(long gmtcreate) {
        this.gmtcreate = gmtcreate;
    }
}
