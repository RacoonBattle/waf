package info.yangguo.waf.request;

import io.atomix.core.map.ConsistentMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:32
 * <p>
 * Description:
 * <p>
 * 拦截器链
 */
public class HttpRequestFilterChain {
    public List<HttpRequestFilter> filters = new ArrayList<>();

    public HttpRequestFilterChain() {
        //要注意顺序，是从上向下执行的
        filters.add(new WIpHttpRequestFilter());
        filters.add(new IpHttpRequestFilter());
        filters.add(new CCHttpRequestFilter());
        filters.add(new ScannerHttpRequestFilter());
        filters.add(new WUrlHttpRequestFilter());
        filters.add(new UaHttpRequestFilter());
        filters.add(new UrlHttpRequestFilter());
        filters.add(new ArgsHttpRequestFilter());
        filters.add(new CookieHttpRequestFilter());
        filters.add(new PostHttpRequestFilter());
        filters.add(new FileHttpRequestFilter());
    }

    public ImmutablePair<Boolean, HttpRequestFilter> doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, ConsistentMap<String, Map> configs) {
        for (HttpRequestFilter filter : filters) {
            Map<String, Object> config = configs.asJavaMap().get(filter.getClass().getName());
            if ((boolean) config.get("isStart")) {
                boolean result = filter.doFilter(originalRequest, httpObject, channelHandlerContext, (Map<String, Boolean>) config.get("pattern"));
                if (result && filter.isBlacklist()) {
                    return new ImmutablePair<>(filter.isBlacklist(), filter);
                } else if (result && !filter.isBlacklist()) {
                    break;
                }
            }
        }
        return new ImmutablePair<>(false, null);
    }
}
