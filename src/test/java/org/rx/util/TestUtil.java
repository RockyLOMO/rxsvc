package org.rx.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.rx.io.Files;
import org.rx.io.IOStream;
import org.rx.net.http.HttpClient;
import org.rx.util.function.Action;
import org.rx.util.pinyin.Pinyin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rx.core.Extends.eq;
import static org.rx.core.Extends.quietly;

public class TestUtil {
    @Test
    public void pinyin() {
        String s = "小王小王，吃饭啦！";
        String py = Pinyin.toPinyin(s, " ");
        System.out.println(py);
    }

    @Test
    public void email() {
        Helper.sendEmail("hw", "abc", "rockywong.chn@qq.com");
    }
}
