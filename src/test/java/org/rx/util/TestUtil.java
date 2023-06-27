package org.rx.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.rx.core.Linq;
import org.rx.core.StringBuilder;
import org.rx.core.Strings;
import org.rx.io.Files;
import org.rx.io.IOStream;
import org.rx.net.Sockets;
import org.rx.net.http.AuthenticProxy;
import org.rx.net.http.HttpClient;
import org.rx.util.function.Action;
import org.rx.util.pinyin.Pinyin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rx.core.Extends.*;
import static org.rx.core.Sys.fromJson;
import static org.rx.core.Sys.toJsonString;

@Slf4j
public class TestUtil {
    @Test
    public void pinyin() {
        String l = "小王，abc";
        System.out.println(l.length());
        l = "小范";
        String pyn = Pinyin.toPinyin(l, "_");
        System.out.println(pyn);

        String subject = "什么小范小范，吃饭啦！,好的";
        String pySubject = Pinyin.toPinyin(subject, "_");
        System.out.println(pySubject);

        String n = "小范", n1 = "XIAO_FAN";
        System.out.println(getSubject("今天天气如何", n, n1));
        System.out.println(getSubject("小范，今天天气如何", n, n1));
        System.out.println(getSubject("小饭，今天天气如何", n, n1));
        System.out.println(getSubject("总之呐，小范，今天天气如何", n, n1));
        System.out.println(getSubject("总之呐，小饭，今天天气如何", n, n1));
    }

    String getSubject(String line, String name, String pinyinName) {
        if (line.length() <= name.length()) {
            return null;
        }
//        line = new StringBuilder(line).replace("，", "")
//                .replace(",", "")
//                .toString();
        String s = "_";
        String pinyinLine = Pinyin.toPinyin(line, s);
        if (pinyinLine.startsWith(pinyinName)) {
            return line.substring(name.length()).trim();
        }
        int i = pinyinLine.indexOf(s + pinyinName);
        if (i == -1) {
            return null;
        }
        int c = 1;
        for (int j = 0; j < i; j++) {
            if (pinyinLine.charAt(j) == s.charAt(0)) {
                c++;
            }
        }
        log.info("{} -> @{} - {}", pinyinLine, i, c);
        return line.substring(c + name.length()).trim();
    }

    @Test
    public void email() {
        Helper.sendEmail("hw", "abc", "rockywong.chn@qq.com");
    }

    String excelFile = "D:\\数据处理\\免费义诊-预发数据-2023年-6月.xlsx";

    @SneakyThrows
    @Test
    public void excelPrepare() {
        String sheet = "Sheet2";
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
            Helper.writeExcel(new FileOutputStream(Files.changeExtension(excelFile, String.format("%s.xlsx", outIndex.incrementAndGet()))), false, Collections.singletonMap(sheet, copy));
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
    public void excelPost() {
        HttpClient client = new HttpClient();
        client.setEnableLog(true);

        String fn = Files.getName(excelFile);
        for (File file : Files.listFiles(Files.getFullPath(excelFile), false)) {
            if (eq(file.getName(), fn)) {
                continue;
            }
            quietly(() -> {
                String ret = client.post("https://aicenterserver-stage.gaojihealth.cn/api/internal/aicenter/fileImport/heartDayActivity/importActivityDoctorData", Collections.emptyMap(), Collections.singletonMap("file", IOStream.wrap(file))).toString();
                System.out.println(file + "\n" + ret + "\n");
            }, 3);
        }

//        String s;
//        Map<String, Object> d = Collections.singletonMap("prescriptionPicUrl", "https://gjscrm-1256038144.cos.ap-beijing.myqcloud.com/scrm/1681713003030_王大.jpg");
////        client.setProxy(new AuthenticProxy(Proxy.Type.HTTP, Sockets.parseEndpoint("127.0.0.1:8888"), null, null));
////        client.getRequestHeaders().add("X-XSRF-TOKEN","fb630e0c-0305-4ba7-bfb6-1dcdbfd77365");
//        client.getRequestHeaders().add("x-xsrf-token", "4b017276-44ff-428d-be5b-aa42c21cdeab");
//        client.getRequestHeaders().add("cookie", "login=; access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlvbklkIjpudWxsLCJ1c2VyX25hbWUiOiIxNTAwMDAwMDAwMiIsImxvZ2luQ3JlYXRlZERhdGUiOiIyMDIwLTA3LTI5IDE2OjExOjMxIiwicm9sZXMiOiJbXCJMU1lTXCIsXCJEWlwiLFwiTUREWlwiLFwiTFNZRERHTFlcIl0iLCJidXNpbmVzc0lkIjoiOTk5OTkiLCJ0eXBlIjoiMSIsInVzZXJJZCI6IjE2NTkxNTY5MTY5OTk5OSIsImF1dGhvcml0aWVzIjpbIk1ERFoiLCJMU1lTIiwiRFoiLCJMU1lEREdMWSJdLCJjbGllbnRfaWQiOiJ3ZWJfYXBwIiwicGhvbmUiOiIxNTAwMDAwMDAwMiIsInNjb3BlIjpbIm9wZW5pZCJdLCJsb2dpbk5hbWUiOiIxNTAwMDAwMDAwMiIsIm5hbWUiOiLov57plIHoja_luIgyIiwiZXhwIjoxNjgxMzIxMTE2LCJqdGkiOiI0MmYxYThhYy03ZmMxLTQxZjctYTExNi05NDZiZDM4MmZjZGYiLCJzdGF0dXMiOiIxIn0.kGQqD_4ynT9ICRoED5mILLj75hMEMNj1SvMslbmwRWWju_te4a5HzowOInkPL2nbG5HVHVfHd8HxmEuegVIvwuR4bGJYcT7_Wi0s6dmusLQkFk_xfXagbwfaQ2FReRV5LhITkxiQiPxjlZnS7W75ur3-xZhffaAd64DheTDxFbIbI-yVpdso9_R2jUZgcsFNO8N-TWGJ7wUClA905uMFDYkxPPEi0607hEeWpOPh_Xn04MXt6m0zHC86Uu-prRialcQmYoVbGOxKVSbZVrC8xyXQj2DgKmKFkNGnTzO1-b9LSg0RvGABUNfjtKN2y3LoTAcNNZSBCq4aea8h-ToD1g; refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlvbklkIjpudWxsLCJ1c2VyX25hbWUiOiIxNTAwMDAwMDAwMiIsImxvZ2luQ3JlYXRlZERhdGUiOiIyMDIwLTA3LTI5IDE2OjExOjMxIiwicm9sZXMiOiJbXCJMU1lTXCIsXCJEWlwiLFwiTUREWlwiLFwiTFNZRERHTFlcIl0iLCJidXNpbmVzc0lkIjoiOTk5OTkiLCJ0eXBlIjoiMSIsInVzZXJJZCI6IjE2NTkxNTY5MTY5OTk5OSIsImF1dGhvcml0aWVzIjpbIk1ERFoiLCJMU1lTIiwiRFoiLCJMU1lEREdMWSJdLCJjbGllbnRfaWQiOiJ3ZWJfYXBwIiwicGhvbmUiOiIxNTAwMDAwMDAwMiIsInNjb3BlIjpbIm9wZW5pZCJdLCJhdGkiOiI0MmYxYThhYy03ZmMxLTQxZjctYTExNi05NDZiZDM4MmZjZGYiLCJsb2dpbk5hbWUiOiIxNTAwMDAwMDAwMiIsIm5hbWUiOiLov57plIHoja_luIgyIiwiZXhwIjoxNjgzODU4NzIwLCJqdGkiOiI4MjdjNDZmNC1kNjdjLTQ1NTEtYTA2Zi1lNmE0OTQ0NDU1ZWMiLCJzdGF0dXMiOiIxIn0.hOJfKXOYV7JbWaAATEA18FdEhka7eBazlQWTBaZyJcdFtqVBT0WTkK7BIcVXR7hsPrrYK9JipQdiDoMwZ_zeab8b5JQekZ81eKML04z_aBVgrt8GKXWJTgVf4WWMILvp3GPWc1U4KQ-SUp1K3nHUPEX4xLHYaiem3NMyrKixGDHS-iy6gEA0e_t2EUZpMLRTtSAtV_n4rFVQS0_TN0aveIfJ9SxZng1C5hL0_q--Hr8etyYksij0_bhof0_1zFBj87dlQQR1QVeTtUK6vAs89LM8qhDo_C9B-ffasWmQzEbX0HNDs6NyqFJJtILi31kTPy-7RjZNmgh6M_oqk4MwTQ; XSRF-TOKEN=4b017276-44ff-428d-be5b-aa42c21cdeab");
////        String s = client.post("https://api-store-test.gaojihealth.cn/mm-digitalstore/api/bdp/api/ocr/prescription",
////                d).toString();
////        String s = client.postJson("https://api-test-internal.gaojihealth.cn/mm-digitalstore/api/prescription/ocr", d).toString();
////        log.info("dtp {}", s);
//        s = client.post("http://10.2.33.7:8303/api/ocr/prescription", d).toString();
//        log.info("bdp {}", s);

//        String b = "{\n" +
//                "  \"bean\": \"com.cowell.medicalmall.ai.rest.impl.DoctorSuffererRelInfoServiceImpl\",\n" +
//                "  \"method\": \"addDoctorSuffererRelInfo\",\n" +
//                "  \"args\": [{\"createTime\":1684810811,\"event\":\"SCAN\",\"eventKey\":\"{\\\"OP\\\":\\\"20000116178415600002540600010039\\\",\\\"ASID\\\":\\\"962172\\\",\\\"T\\\":\\\"GJDA\\\"}\",\"fromUserName\":\"oecitwo5Zn5r9_iatKLGL04OT8CA\",\"msgType\":\"event\",\"toUserName\":\"gh_0d989d46444a\",\"url\":\"https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=gQHZ7zwAAAAAAAAAAS5odHRwOi8vd2VpeGluLnFxLmNvbS9xLzAyaTJ0TlFlVjhlSjIxRVJWanhBY0UAAgQ1LGxkAwQAjScA\",\"userDataReqDTO\":{\"city\":\"\",\"country\":\"\",\"groupid\":\"0\",\"headimgurl\":\"https://ai-prod-1300948849.cos.ap-beijing.myqcloud.com/patient_default_headImg/Bitmap%20Copy%E5%A4%87%E4%BB%BD%2010%403x.png\",\"language\":\"zh_CN\",\"nickname\":\"未注册患者q3yzaI\",\"openid\":\"oecitwo5Zn5r9_iatKLGL04OT8CA\",\"province\":\"\",\"qrScene\":\"0\",\"qrSceneStr\":\"9cc3087247a04935ae3cf1316dc0f1deRE\",\"remark\":\"\",\"sex\":\"0\",\"subscribe\":1,\"subscribeScene\":\"ADD_SCENE_QR_CODE\",\"subscribeTime\":\"1635327657\",\"tagidList\":[],\"unionid\":\"oWMs41A43N9Qwm91fJMNZCq3yzaI\"}}]\n" +
//                "}";
//        String u = HttpClient.buildUrl("https://api.gaojihealth.cn/aiinfo/api/noauth/call", Collections.singletonMap("body", b));
//        HttpClient.ResponseContent res = client.get(u);
//        System.out.println(res.getHeaders().toString() + "\n");
//        System.out.println(res.toString());
    }

    @SneakyThrows
    @Test
    public void importTask() {
        String f = "D:\\专科中心入组医生明细 0420V1.xlsx";
        Map<String, Object> j = fromJson("{\n" +
                "  \"DiseaseCenter:11\": \"754\",\n" +
                "  \"DiseaseCenter:7\": \"753\",\n" +
                "  \"DiseaseCenter:6\": \"751\",\n" +
                "  \"DiseaseCenter:5\": \"752\",\n" +
                "  \"DiseaseCenter:4\": \"750\",\n" +
                "  \"DiseaseCenter:1\": \"744\",\n" +
                "  \"KetangTest:cc8c6ffd-ce1d-4124-a8d1-9d3e7f46c6df\": \"749\"\n" +
                "}", Map.class);
        Map<Integer, String> j2 = Linq.from(j.entrySet()).toMap(p -> Integer.parseInt(p.getValue().toString()), p -> p.getKey().substring(p.getKey().lastIndexOf(":") + 1));
        System.out.println(j2);
        Map<String, List<Object[]>> map = Helper.readExcel(new FileInputStream(f), false, true, true);
        HttpClient client = new HttpClient();
        for (List<Object[]> dt : map.values()) {
            eachQuietly(dt, row -> {
                //                System.out.println("row: " + toJsonString(row));
                String opId = row[1].toString().trim();
                if (Strings.isEmpty(opId)) {
                    return;
                }

                int taskId = Integer.parseInt(row[2].toString().trim());
                int centerCode = Integer.parseInt(j2.get(taskId));
                String u = String.format("https://api-stage-internal.gaojihealth.cn/aicenter/api/noauth/joinDiseaseCenterCallback?userId=%s&centerCode=%s", opId, centerCode);
                String t = client.get(u).toString();
                log.info("{} -> {}", u, t);
            });
        }
    }

//    @SneakyThrows
//    @Test
//    public void lusu() {
//        String path = "D:\\监管-lusu-熙康字段映射.xlsx";
//        Map<String, List<Object[]>> sheets = Helper.readExcel(new FileInputStream(path), false, true, false);
//        StringBuilder buf = new StringBuilder();
//        for (Map.Entry<String, List<Object[]>> entry : sheets.entrySet()) {
//            String fn = "D:\\var\\" + entry.getKey() + ".log";
//            buf.setLength(0);
//            buf.appendLine("{\"gjRequestId\":\"\",\"_token\":\"\",%s}", Linq.from(entry.getValue()).where(p -> p.length > 0).toJoinString(",", p -> String.format("\"%s\":\"\"", p[0]))).appendLine();
//            buf.appendLine("{\"gjRequestId\":\"\",\"_token\":\"\",%s}", Linq.from(entry.getValue()).where(p -> p.length > 0).toJoinString(",", p -> String.format("\"%s\":\"\"", p[1]))).appendLine();
//            IOStream.writeString(new FileOutputStream(fn), buf.toString(), StandardCharsets.UTF_8);
//        }
//    }
}
