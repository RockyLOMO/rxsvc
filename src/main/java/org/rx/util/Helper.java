package org.rx.util;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rx.config.MiddlewareConfig;
import org.rx.spring.SpringContext;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static org.rx.core.App.*;

@Slf4j
public class Helper {
    public static void sendEmail(String body) {
        MiddlewareConfig config = SpringContext.getBean(MiddlewareConfig.class);
        Helper.sendEmail(body, config.getSmtpPwd(), config.getSmtpTo());
    }

    public static void sendEmail(String body, @NonNull String password, @NonNull String toEmail) {
        final String fromEmail = "17091916400@163.com";
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.163.com");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.auth", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress(fromEmail, "System"));
            msg.setReplyTo(InternetAddress.parse("no_reply@f-li.cn", false));

            msg.setSubject("Notification", "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            Transport.send(msg);
        } catch (Exception e) {
            log.warn("sendEmail {}", e.getMessage());
        }
    }

    @SneakyThrows
    public static Map<String, List<Object[]>> readExcel(InputStream in, boolean skipColumn) {
        return readExcel(in, skipColumn, false);
    }

    @SneakyThrows
    public static Map<String, List<Object[]>> readExcel(InputStream in, boolean skipColumn, boolean keepNullRow) {
        Map<String, List<Object[]>> data = new LinkedHashMap<>();
        Workbook workbook;
        try {
            workbook = new HSSFWorkbook(in);
        } catch (Exception e) {
            log.warn("readExcel {}", e.getMessage());
            //todo in stream pos
            workbook = new XSSFWorkbook(in);
        }
        try {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                List<Object[]> rows = new ArrayList<>();
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                for (int rowIndex = skipColumn ? 1 : sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        if (keepNullRow) {
                            rows.add(null);
                        }
                        continue;
                    }
                    List<Object> cells = new ArrayList<>();
                    for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                            cells.add(null);
                            continue;
                        }
                        Object value;

                        switch (cell.getCellType()) {
                            case NUMERIC:
                                value = cell.getNumericCellValue();
                                break;
                            case BOOLEAN:
                                value = cell.getBooleanCellValue();
                                break;
                            default:
//                                value = cell.getStringCellValue();
                                if (cell.getCellType() == CellType.ERROR) {
                                    cell.setCellType(CellType.STRING);
                                    log.debug("sheetIndex={} rowIndex={} rowCellLength={} cells={}", sheetIndex, rowIndex, row.getLastCellNum(), toJsonString(cells));
                                }
                                value = cell.toString();
                                break;
                        }
                        cells.add(value);
                    }
                    if (cells.contains(null)) {
                        log.debug("sheetIndex={} rowIndex={} rowCellLength={} cells={}", sheetIndex, rowIndex, row.getLastCellNum(), toJsonString(cells));
                    }
                    rows.add(cells.toArray());
                }
                data.put(sheet.getSheetName(), rows);
            }
        } finally {
            tryClose(workbook);
        }
        return data;
    }

    public static void writeExcel(OutputStream out, Map<String, List<Object[]>> data) {
        writeExcel(out, data, null);
    }

    @SneakyThrows
    public static void writeExcel(OutputStream out, Map<String, List<Object[]>> data, Function<HSSFRow, HSSFRow> onRow) {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            for (Map.Entry<String, List<Object[]>> entry : data.entrySet()) {
                HSSFSheet sheet = workbook.getSheet(entry.getKey());
                if (sheet == null) {
                    sheet = workbook.createSheet(entry.getKey());
                }
                List<Object[]> rows = entry.getValue();
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    HSSFRow row = sheet.getRow(rowIndex);
                    if (row == null) {
                        row = sheet.createRow(rowIndex);
                    }
                    Object[] cells = rows.get(rowIndex);
                    for (int i = 0; i < cells.length; i++) {
                        HSSFCell cell = row.getCell(i);
                        if (cell == null) {
                            cell = row.createCell(i);
                        }
                        Object val = cells[i];
                        if (val == null) {
                            continue;
                        }
                        String value;
                        if (val instanceof Date) {
                            value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(val);
                        } else {
                            value = String.valueOf(val);
                        }
                        cell.setCellValue(value);
                    }
                    if (onRow != null) {
                        if (row.getRowStyle() == null) {
                            row.setRowStyle(workbook.createCellStyle());
                        }
                        onRow.apply(row);
                    }
                }
            }
            workbook.write(out);
        }
    }
}
