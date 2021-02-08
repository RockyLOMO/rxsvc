package org.rx.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTransferQueue;
import org.redisson.codec.SerializationCodec;
import org.rx.redis.HybridCache;
import org.rx.io.IOStream;
import org.rx.util.Helper;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

public class UtilTester {
    @SneakyThrows
    @Test
    public void redis() {
        RTransferQueue<IOStream> queue = HybridCache.create("").getTransferQueue("x", new SerializationCodec());
        String p = "E:\\Photo\\养生\\f0.jpg", p2 = "E:\\Photo\\养生\\喝水.png";
        queue.add(IOStream.wrap(p));
        System.out.println(queue.getCodec());
        IOStream poll = queue.take();
        System.out.println(poll);
    }

    @Test
    public void email() {
        Helper.sendEmail("hw", "asd", "rockywong.chn@qq.com");
    }

    @SneakyThrows
    @Test
    public void excel() {
        String path = "D:/data3.xlsx";
        Map<String, List<Object[]>> map = Helper.readExcel(new FileInputStream(path), false);
        for (Map.Entry<String, List<Object[]>> entry : map.entrySet()) {
            System.out.println(entry.getKey());
//            System.out.println(toJsonString(entry.getValue()));
            System.out.println();
        }
    }
}
