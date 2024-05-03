package com.pjieyi.yiapigatewayapplication;

import com.pjieyi.yiapi.provider.DemoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@SpringBootApplication(exclude = {
        //启动项目不加载数据库相关
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@EnableDubbo
@Service
public class YiApiGatewayApplication {



    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(YiApiGatewayApplication.class, args);
        //YiApiGatewayApplication application = context.getBean(YiApiGatewayApplication.class);
        //String s = application.sayHello("pjieyi");
        //System.out.println(s);
    }


    //String sayHello(String name){
    //    return demoService.sayHello(name);
    //}
    //
    //String sayBuy(String name){
    //    return demoService.sayBuy(name);
    //}

    //@Bean
    //public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    //    return builder.routes()
    //            .route("path_route", r -> r.path("/get")
    //                    .uri("http://httpbin.org"))
    //            .route("host_route", r -> r.host("*.myhost.org")
    //                    .uri("http://httpbin.org"))
    //            .build();
    //}

}
