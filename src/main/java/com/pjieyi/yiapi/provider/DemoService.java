package com.pjieyi.yiapi.provider;

public interface DemoService {

    //服务接口消费端与服务端的桥梁 interface

    String sayHello(String name);

    String sayBuy(String name);
}