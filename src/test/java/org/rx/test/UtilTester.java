package org.rx.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.rx.util.Helper;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

public class UtilTester {
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
