package org.rx.test;

import org.junit.jupiter.api.Test;
import org.rx.util.Helper;

import static org.rx.core.Extends.quietly;

public class TestUtil {
    @Test
    public void email() {
        Helper.sendEmail("hw", "asd", "rockywong.chn@qq.com");
    }
}
