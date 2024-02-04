package com.pjieyi.yiapigatewayapplication;

import com.pjieyi.yiapiclientsdk.client.YiApiClient;
import com.pjieyi.yiapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全局过滤
 * @author pjieyi
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    //白名单
    public static final List<String> IP_WHITE_LIST= Arrays.asList("127.0.0.1");
    public static final Long FIVE_MINUTE= 5*60L;
    /**
     * 全局过滤
     * @param exchange 路由交换机
     * @param chain 责任链模式
     * @return Mono是响应式编程的一种对象 异步操作
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.请求日志
        ServerHttpRequest request = exchange.getRequest();
        log.info("请求唯一标识："+request.getId());
        log.info("请求方法："+request.getMethod());
        log.info("请求路径："+request.getPath());
        log.info("请求参数："+request.getQueryParams());
        String sourceAddress = request.getRemoteAddress().getHostString();
        log.info("请求来源地址："+sourceAddress);
        //拿到响应对象
        ServerHttpResponse response = exchange.getResponse();
        //2.访问控制-添加黑白名单
        if (!IP_WHITE_LIST.contains(sourceAddress)){
            return handleNoAuth(response);
        }
        //3.用户鉴权（ak,sk是否正确）
        //todo 从数据库中获取ak sk
        //3.1从请求头中获取信息
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        //随机数
        String nonce = headers.getFirst("nonce");
        //时间
        String timestamp = headers.getFirst("timestamp");
        //签名
        String sign = headers.getFirst("sign");
        //用户信息
        String userName = headers.getFirst("userName");

        try {
            userName=new String(headers.getFirst("userName").getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        //3.2验证信息
        if (!"4ad918cf5ef7a74d9a71dbe836a3bc81".equals(accessKey)){
            return handleNoAuth(response);
        }
        if (nonce.length()>5){
            return handleNoAuth(response);
        }
        //时间和当前时间不能超过5分钟
        //当前时间 秒
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime-Long.parseLong(timestamp)>=FIVE_MINUTE){
            return handleNoAuth(response);
        }
        //验证签名
        String genSign = SignUtils.genSign(userName, "c23e79e9432490b53af052e207f9a8495235b9e0");
        if (!sign.equals(genSign)){
            return handleNoAuth(response);
        }
        //4.请求的模拟接口是否存在
        //todo 从数据库中查询接口是否存在，以及请求方法是否匹配(还可以检验请求参数)
        //5.请求转发，调用模拟接口


        //Spring Cloud Gateway 的处理逻辑是等待所有过滤器都执行完毕后
        // 才会继续向下走，直到最终调用被代理的服务

        //预期是等模拟接口调用完成，才记录响应日志、统计调用次数。
        //但现实是 chain.filter 方法立刻返回了，直到 filter 过滤器 return 后才调用了模拟接口。
        //原因是：chain.filter 是个异步操作，理解为前端的 promise
        //调用成功返回响应状态码
        log.info("响应,"+response.getStatusCode());
        //6.响应日志
        return handleResponse(exchange,chain);
        //if (response.getStatusCode()==HttpStatus.OK){
        //
        //}else{
        //    //8。调用失败，返回一个规范的错误码
        //    return handleInvokeError(response);
        //}
    }

    /**
     * 定义执行顺序
     * @return
     */
    @Override
    public int getOrder() {
        return -1;
    }


    /**
     * 处理响应
     * 装饰器模式
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange,GatewayFilterChain chain){
        try {
            // 获取原始的响应对象
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 获取数据缓冲工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 获取响应的状态码
            HttpStatus statusCode = originalResponse.getStatusCode();

            // 判断状态码是否为200 OK(按道理来说,现在没有调用,是拿不到响应码的,对这个保持怀疑 )
            if(statusCode == HttpStatus.OK) {
                // 创建一个装饰后的响应对象(开始穿装备，增强能力)装饰者模式
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

                    // 重写writeWith方法，用于处理响应体的数据
                    // 这段方法就是只要当我们的模拟接口调用完成之后,等它返回结果，
                    // 就会调用writeWith方法,我们就能根据响应结果做一些自己的处理
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        // 判断响应体是否是Flux类型
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // (这里就理解为它在拼接字符串,它把缓冲区的数据取出来，一点一点拼接好)
                            // 拿到返回值之后才会处理
                            return super.writeWith(fluxBody.map(dataBuffer -> {
                                //todo 7.调用成功，接口调用次数+1 invokeCount
                                //读取响应体的内容并转换为字节数组
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);//释放掉内存
                                // 构建日志
                                StringBuilder sb2 = new StringBuilder(200);
                                List<Object> rspArgs = new ArrayList<>();
                                rspArgs.add(originalResponse.getStatusCode());
                                //rspArgs.add(requestUrl);
                                String data = new String(content, StandardCharsets.UTF_8);//data
                                sb2.append(data);
                                log.info("响应结果："+data);
                                // 将处理后的内容重新包装成DataBuffer并返回
                                return bufferFactory.wrap(content);
                            }));
                        } else {
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 对于200 OK的请求,将装饰后的响应对象传递给下一个过滤器链,并继续处理(设置repsonse对象为装饰过的)
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            // 对于非200 OK的请求，直接返回，进行降级处理
            return chain.filter(exchange);
        }catch (Exception e){
            // 处理异常情况，记录错误日志
            log.error("网关处理响应异常:"+e);
            return chain.filter(exchange);
        }
    }


    /**
     * 没有权限
     * @param response
     * @return
     */
    public Mono<Void> handleNoAuth(ServerHttpResponse response){
        //设置响应状态码为403forbidden 禁止访问
        response.setStatusCode(HttpStatus.FORBIDDEN);
        //返回处理完成的响应 相当于我们告诉程序，请求处理完成了，不需要再执行其他操作
        return response.setComplete();
    }

    public Mono<Void> handleInvokeError(ServerHttpResponse response){
        //返回错误码500调用失败
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }

}