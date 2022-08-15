package org.rx.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTransferQueue;
import org.redisson.codec.SerializationCodec;
import org.rx.io.Files;
import org.rx.io.IOStream;
import org.rx.net.http.HttpClient;
import org.rx.util.Helper;
import org.rx.util.function.Action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rx.core.Extends.eq;

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

    @SneakyThrows
    @Test
    public void excel() {
        String excelFile = "D:\\数据处理\\免费义诊-预发数据-8月15.xlsx";
        String sheet = "Sheet1";
        int maxRow = 100;
        AtomicInteger outIndex = new AtomicInteger();
        Map<String, List<Object[]>> sheets = Helper.readExcel(new FileInputStream(excelFile), false);
        List<Object[]> rows = sheets.get(sheet);
        List<Object[]> copy = new ArrayList<>(maxRow);
        Action fn = () -> {
            copy.add(0, rows.get(0));
            boolean b = false;
            for (int i1 = 1; i1 < copy.size(); i1++) {
                Object[] cells = copy.get(i1);
                if (cells[0] == null) {
                    cells[0] = "free_clinic";
//                        b = true;
//                        break;
                }
//                    cells[5] = i1;
            }
            if (b) {
                return;
            }
            Helper.writeExcel(new FileOutputStream(Files.changeExtension(excelFile, String.format("%s.xlsx", outIndex.incrementAndGet()))),
                    false,
                    Collections.singletonMap(sheet, copy));
            copy.clear();
            System.out.println("dump " + outIndex + " file");
        };
        for (int i = 1; i < rows.size(); i++) {
            copy.add(rows.get(i));
            if (copy.size() == maxRow) {
                fn.invoke();
            }
        }
        if (!copy.isEmpty()) {
            fn.invoke();
        }
    }

    @Test
    public void post() {
        String excelFile = "D:\\数据处理\\免费义诊-预发数据-8月15.xlsx";
        String fn = Files.getName(excelFile);
        HttpClient client = new HttpClient();
        for (File file : Files.listFiles(Files.getFullPath(excelFile), false)) {
            if (eq(file.getName(), fn)) {
                continue;
            }
            String ret = client.post("https://aicenterserver-stage.gaojihealth.cn/api/internal/aicenter/fileImport/heartDayActivity/importActivityDoctorData",
                    Collections.emptyMap(),
                    Collections.singletonMap("file", IOStream.wrap(file))).toString();
            System.out.println(file + "\n" + ret + "\n");
        }
    }
}
