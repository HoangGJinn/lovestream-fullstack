package com.hcmute.lovestream.service.admin.transaction;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class AdminReportService {

    public ReportDocument createReport(List<Map<String, Object>> data, String format) {
        ReportVisitableData reportData = new ReportVisitableData(data);
        ReportExportVisitor visitor = ReportExportVisitorFactory.create(format);
        return reportData.accept(visitor);
    }

    public record ReportDocument(ByteArrayOutputStream content, String fileName, String contentType) {}

    interface ReportExportVisitor {
        ReportDocument visit(ReportVisitableData data);
    }

    static class ReportVisitableData {
        private final List<Map<String, Object>> rows;

        ReportVisitableData(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        ReportDocument accept(ReportExportVisitor visitor) {
            return visitor.visit(this);
        }
    }

    static class ReportExportVisitorFactory {
        private ReportExportVisitorFactory() {
        }

        static ReportExportVisitor create(String format) {
            String normalized = format == null ? "pdf" : format.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "excel", "xlsx", "exel" -> new ExcelExportVisitor();
                case "pdf" -> new PdfExportVisitor();
                default -> throw new IllegalArgumentException("Unsupported report format: " + format);
            };
        }
    }

    static class ExcelExportVisitor implements ReportExportVisitor {
        @Override
        public ReportDocument visit(ReportVisitableData data) {
            try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = wb.createSheet("Transactions");
                int rowIdx = 0;

                Row header = sheet.createRow(rowIdx++);
                String[] headers = new String[]{"ID", "User ID", "Plan", "Amount", "Payment Date", "Status"};
                CellStyle headerStyle = wb.createCellStyle();
                Font font = wb.createFont();
                font.setBold(true);
                headerStyle.setFont(font);

                for (int i = 0; i < headers.length; i++) {
                    Cell c = header.createCell(i);
                    c.setCellValue(headers[i]);
                    c.setCellStyle(headerStyle);
                }

                for (Map<String, Object> row : data.getRows()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(String.valueOf(row.getOrDefault("id", "")));
                    r.createCell(1).setCellValue(String.valueOf(row.getOrDefault("userId", "")));
                    r.createCell(2).setCellValue(String.valueOf(row.getOrDefault("planName", "")));
                    Object amount = row.get("amount");
                    if (amount instanceof Number) {
                        r.createCell(3).setCellValue(((Number) amount).doubleValue());
                    } else {
                        r.createCell(3).setCellValue(String.valueOf(amount));
                    }
                    Object pd = row.get("paymentDate");
                    if (pd != null) {
                        r.createCell(4).setCellValue(pd.toString());
                    } else {
                        r.createCell(4).setCellValue("");
                    }
                    r.createCell(5).setCellValue(String.valueOf(row.getOrDefault("status", "")));
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                wb.write(baos);
                return new ReportDocument(
                        baos,
                        "transactions.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                );
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create excel", ex);
            }
        }
    }

    static class PdfExportVisitor implements ReportExportVisitor {
        @Override
        public ReportDocument visit(ReportVisitableData data) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='utf-8'><style>")
                    .append("body{font-family: DejaVu Sans, Arial, sans-serif; font-size:12px;}")
                    .append("table{width:100%;border-collapse:collapse;}th,td{border:1px solid #ddd;padding:6px;}th{background:#f2f2f2;}")
                    .append("</style></head><body>");
            html.append("<h2>Danh sách giao dịch</h2>");
            html.append("<table><thead><tr><th>ID</th><th>User</th><th>Plan</th><th>Amount</th><th>Time</th><th>Status</th></tr></thead><tbody>");
            for (Map<String, Object> r : data.getRows()) {
                html.append("<tr>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("id", "")))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("userId", "")))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("planName", "")))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("amount", "")))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("paymentDate", "")))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(r.getOrDefault("status", "")))).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table></body></html>");

            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(html.toString(), null);
                builder.toStream(os);
                builder.run();
                return new ReportDocument(os, "transactions.pdf", "application/pdf");
            } catch (Exception ex) {
                throw new RuntimeException("PDF creation failed", ex);
            }
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
