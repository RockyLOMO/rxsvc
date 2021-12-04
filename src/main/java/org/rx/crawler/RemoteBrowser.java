package org.rx.crawler;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rx.bean.FlagsEnum;
import org.rx.core.*;
import org.rx.exception.InvalidException;
import org.rx.net.Sockets;
import org.rx.net.rpc.Remoting;
import org.rx.net.rpc.RpcClientConfig;
import org.rx.spring.SpringContext;
import org.rx.util.function.*;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.rx.core.App.*;

@Slf4j
public abstract class RemoteBrowser implements Browser {
    private static final ConcurrentHashMap<String, BrowserPoolListener> FACADE = new ConcurrentHashMap<>();

    public static Future<?> invokeAsync(String url, TripleAction<RemoteBrowser, String> callback) {
        return invokeAsync(url, callback, Thread.NORM_PRIORITY);
    }

    public static Future<?> invokeAsync(String url, TripleAction<RemoteBrowser, String> callback, int priority) {
        BrowserAsyncTopic asyncTopic = SpringContext.getBean(BrowserAsyncTopic.class);

        UUID asyncId = UUID.randomUUID();
        Future<?> future = asyncTopic.listen(asyncId, callback);
        asyncTopic.add(new BrowserAsyncRequest(asyncId, priority, url));
        return future;
    }

    public static <T> Future<T> invokeAsync(String url, TripleFunc<RemoteBrowser, String, T> callback) {
        return invokeAsync(url, callback, Thread.NORM_PRIORITY);
    }

    public static <T> Future<T> invokeAsync(String url, TripleFunc<RemoteBrowser, String, T> callback, int priority) {
        BrowserAsyncTopic asyncTopic = SpringContext.getBean(BrowserAsyncTopic.class);

        UUID asyncId = UUID.randomUUID();
        Future<T> future = asyncTopic.listen(asyncId, callback);
        asyncTopic.add(new BrowserAsyncRequest(asyncId, priority, url));
        return future;
    }

    public static String buildCookieRegion(@NonNull String name, @NonNull FlagsEnum<RegionFlags> flags) {
        return String.format("%s_%s", name, flags.getValue());
    }

    public static void invoke(BiAction<RemoteBrowser> consumer) {
        invoke(consumer, null);
    }

    public static void invoke(BiAction<RemoteBrowser> consumer, String cookieRegion) {
        invoke(consumer, cookieRegion, BrowserType.CHROME);
    }

    @SneakyThrows
    public static void invoke(@NonNull BiAction<RemoteBrowser> consumer, String cookieRegion, BrowserType type) {
        try (RemoteBrowser browser = create(type)) {
            browser.setCookieRegion(cookieRegion);
            consumer.invoke(browser);
        }
    }

    public static <T> T invoke(BiFunc<RemoteBrowser, T> consumer) {
        return invoke(consumer, null);
    }

    public static <T> T invoke(BiFunc<RemoteBrowser, T> consumer, String cookieRegion) {
        return invoke(consumer, cookieRegion, BrowserType.CHROME);
    }

    @SneakyThrows
    public static <T> T invoke(@NonNull BiFunc<RemoteBrowser, T> consumer, String cookieRegion, BrowserType type) {
        //Tasks的CompletableFuture timeout时内容会继续执行会出现不一致情况，
        //故暂时RemoteBrowser控制timeout
        try (RemoteBrowser browser = create(type)) {
            browser.setCookieRegion(cookieRegion);
            return consumer.invoke(browser);
        }
    }

    public static synchronized RemoteBrowser create(@NonNull BrowserType type) {
        MiddlewareConfig config = SpringContext.getBean(MiddlewareConfig.class);
        String endpoint = config.getCrawlerEndpoint();
        BrowserPoolListener listener = FACADE.computeIfAbsent(endpoint, k -> Remoting.create(BrowserPoolListener.class, RpcClientConfig.statefulMode(endpoint, 0)));
        int port = listener.nextIdleId(type);
        InetSocketAddress newEndpoint = Sockets.newEndpoint(endpoint, port);
        log.info("RBrowser connect {} -> {}[{}]", type, newEndpoint, endpoint);
        RemoteBrowser browser = wrap(newEndpoint);
        //重连后id返回不正确
//        if (browser.getType() != type) {
//            log.info("RBrowser FIX {}", browser.getType());
//            tryClose(listener);
//            facade.remove(endpoint);
//            sleep(1000);
//            return create(type);
//        }
        return browser;
    }

    static RemoteBrowser wrap(InetSocketAddress endpoint) {
        RpcClientConfig clientConfig = RpcClientConfig.statefulMode(endpoint, 0);
        clientConfig.setEnableReconnect(false);
        Browser browser = Remoting.create(Browser.class, clientConfig);
        return proxy(RemoteBrowser.class, (m, p) -> {
            if (Reflects.isCloseMethod(m)) {
                log.debug("RBrowser release {}", browser.getId());
                browser.close();
                return null;
            }
            if (NQuery.of("createWait", "navigateBlank", "invoke", "invokeMaximize", "waitScriptComplete", "waitClickComplete").contains(m.getName())) {
                return p.fastInvokeSuper();
            }
            return p.fastInvoke(browser);
        });
    }

    @SneakyThrows
    public void invokeMaximize(Action consumer) {
        maximize();
        try {
            consumer.invoke();
        } finally {
            normalize();
        }
    }

    @SneakyThrows
    public <T> T invokeMaximize(Func<T> consumer) {
        maximize();
        try {
            return consumer.invoke();
        } finally {
            normalize();
        }
    }

    //element 不可见时用click()
    public <T> T waitScriptComplete(int timeoutSeconds, @NonNull String checkCompleteScript, String callbackScript) {
        executeScript(String.format("_rx.waitComplete(%s, function () { %s });", timeoutSeconds, checkCompleteScript));

        int waitMillis = getWaitMillis();
        int count = 0, loopCount = Math.round(timeoutSeconds * 1000f / waitMillis);
        do {
            String isOk = executeScript("return _rx.ok;");
            if ("1".equals(isOk)) {
                break;
            }
            sleep(waitMillis);
        }
        while (count++ < loopCount);
        return executeScript(callbackScript);
    }

    public void waitClickComplete(int timeoutSeconds, @NonNull Predicate<Integer> checkComplete, @NonNull String btnSelector, int reClickEachSeconds, boolean skipFirstClick) throws TimeoutException {
        require(timeoutSeconds, timeoutSeconds <= 60);
        require(reClickEachSeconds, reClickEachSeconds > 0);

        createWait(timeoutSeconds).retryMillis(reClickEachSeconds * 1000L).retryCallFirst(!skipFirstClick).until(s -> checkComplete.test(s.getInvokedCount()), s -> {
            try {
                elementClick(btnSelector, true);
                log.debug("waitClickComplete {} click ok", btnSelector);
            } catch (InvalidException e) {
                log.info(e.getMessage());
            }
            return true;
        });
    }
}
