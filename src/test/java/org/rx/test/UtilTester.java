package org.rx.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTransferQueue;
import org.redisson.codec.SerializationCodec;
import org.rx.core.Linq;
import org.rx.core.StringBuilder;
import org.rx.io.Files;
import org.rx.io.IOStream;
import org.rx.net.http.HttpClient;
import org.rx.util.Helper;
import org.rx.util.function.Action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rx.core.Extends.eq;
import static org.rx.core.Extends.quietly;

public class UtilTester {
//    @SneakyThrows
//    @Test
//    public void redis() {
//        RTransferQueue<IOStream> queue = HybridCache.create("").getTransferQueue("x", new SerializationCodec());
//        String p = "E:\\Photo\\养生\\f0.jpg", p2 = "E:\\Photo\\养生\\喝水.png";
//        queue.add(IOStream.wrap(p));
//        System.out.println(queue.getCodec());
//        IOStream poll = queue.take();
//        System.out.println(poll);
//    }

    @Test
    public void email() {
        Helper.sendEmail("hw", "asd", "rockywong.chn@qq.com");
    }
}
