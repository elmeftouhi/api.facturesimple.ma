package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.company.Company;
import com.elmeftouhi.facturesimple.company.CompanyRepository;
import com.elmeftouhi.facturesimple.customer.Customer;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentResponse;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY = new DecimalFormat("0.00");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;
    private final InvoiceDiscountRepository invoiceDiscountRepository;

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long invoiceId, Long tenantId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        List<InvoiceLineItemResponse> lineItems = lineItemRepository.findByInvoiceIdOrderByIdAsc(invoice.getId())
                .stream()
                .map(lineItem -> new InvoiceLineItemResponse(
                        lineItem.getId(),
                        lineItem.getItemReference(),
                        lineItem.getItemDescription(),
                        lineItem.getQuantity(),
                        lineItem.getUnitPrice(),
                        lineItem.getQuantity().multiply(lineItem.getUnitPrice())
                ))
                .toList();

        List<InvoicePaymentResponse> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .map(payment -> new InvoicePaymentResponse(
                        payment.getId(),
                        payment.getPaymentMethod(),
                        payment.getPaymentReference(),
                        payment.getPaymentDate(),
                        payment.getPaidAmount(),
                        payment.getBankName()
                ))
                .toList();

        List<InvoiceDiscount> discounts = invoiceDiscountRepository.findByInvoiceIdOrderByIdAsc(invoice.getId());

        Company company = companyRepository.findByTenantId(tenantId).orElse(null);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.OVERWRITE, true, true)) {
                renderDocument(content, invoice, lineItems, payments, discounts, company);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate invoice PDF", ex);
        }
    }

    private void renderDocument(PDPageContentStream content, Invoice invoice, List<InvoiceLineItemResponse> lineItems, List<InvoicePaymentResponse> payments, List<InvoiceDiscount> discounts, Company company) throws IOException {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, 595, 842);
        content.fill();
        // Reset text color after painting white page background.
        content.setNonStrokingColor(Color.BLACK);
        content.setStrokingColor(Color.BLACK);

        float margin = 40f;
        float y = 800f;
        float contentWidth = 515f;
        float rightColumnX = 305f;
        float sectionWidth = 250f;

        drawTitle(content, margin, y, company != null ? company.getName() : "Invoice");
        y -= 24;
        drawSubtitle(content, margin, y, "Invoice " + safe(invoice.getFormattedNumber()) + " | Template: " + resolveTemplateLabel(invoice));

        y -= 30;

        List<String> companyLines = company == null ? List.of("No company configured") : List.of(
                safe(company.getName()),
                "Email: " + safe(company.getEmail()),
                "Phone: " + safe(company.getPhone()),
                "Address: " + safe(company.getAddress()),
                "VAT/ICE: " + safe(company.getTaxId())
        );

        Customer customer = invoice.getCustomer();
        List<String> customerLines = List.of(
                safe(customer != null ? customer.getName() : null),
                "Email: " + safe(customer != null ? customer.getEmail() : null),
                "Phone: " + safe(customer != null ? customer.getPhone() : null),
                "Address: " + safe(customer != null ? customer.getAddress() : null),
                "Tax ID: " + safe(customer != null ? customer.getTaxId() : null)
        );

        float companyHeight = computeBoxHeight(companyLines, sectionWidth - 14f);
        float customerHeight = computeBoxHeight(customerLines, sectionWidth - 14f);
        float infoBoxHeight = Math.max(companyHeight, customerHeight);
        ensureSpace(y, infoBoxHeight + 14f);

        drawInfoBox(content, margin, y, sectionWidth, infoBoxHeight, "Company", companyLines);
        drawInfoBox(content, rightColumnX, y, sectionWidth, infoBoxHeight, "Client", customerLines);

        y = y - infoBoxHeight - 18f;

        drawHeader(content, margin, y, "Line items");
        y -= 14f;

        float[] colWidths = new float[] {85f, 220f, 50f, 80f, 80f};
        float tableX = margin;
        float headerHeight = 18f;

        ensureSpace(y, headerHeight + 20f);
        drawTableRowFrame(content, tableX, y, colWidths, headerHeight, new Color(240, 240, 240));
        drawCellText(content, tableX, y, colWidths, 0, "Ref", PDType1Font.HELVETICA_BOLD, 9f);
        drawCellText(content, tableX, y, colWidths, 1, "Description", PDType1Font.HELVETICA_BOLD, 9f);
        drawCellText(content, tableX, y, colWidths, 2, "Qty", PDType1Font.HELVETICA_BOLD, 9f);
        drawCellText(content, tableX, y, colWidths, 3, "Unit", PDType1Font.HELVETICA_BOLD, 9f);
        drawCellText(content, tableX, y, colWidths, 4, "Total", PDType1Font.HELVETICA_BOLD, 9f);
        y -= headerHeight;

        if (lineItems.isEmpty()) {
            float emptyHeight = 20f;
            ensureSpace(y, emptyHeight + 10f);
            drawTableRowFrame(content, tableX, y, colWidths, emptyHeight, null);
            drawCellText(content, tableX, y, colWidths, 1, "No items", PDType1Font.HELVETICA, 9f);
            y -= emptyHeight;
        } else {
            for (InvoiceLineItemResponse item : lineItems) {
                List<String> refLines = wrapText(PDType1Font.HELVETICA, 9, colWidths[0] - 8f, safe(item.itemReference()));
                List<String> descLines = wrapText(PDType1Font.HELVETICA, 9, colWidths[1] - 8f, safe(item.itemDescription()));
                List<String> qtyLines = wrapText(PDType1Font.HELVETICA, 9, colWidths[2] - 8f, formatNumber(item.quantity()));
                List<String> unitLines = wrapText(PDType1Font.HELVETICA, 9, colWidths[3] - 8f, formatMoney(item.unitPrice()));
                List<String> totalLines = wrapText(PDType1Font.HELVETICA, 9, colWidths[4] - 8f, formatMoney(item.lineTotal()));

                int linesCount = Math.max(refLines.size(), Math.max(descLines.size(), Math.max(qtyLines.size(), Math.max(unitLines.size(), totalLines.size()))));
                float rowHeight = linesCount * 12f + 8f;
                ensureSpace(y, rowHeight + 16f);

                drawTableRowFrame(content, tableX, y, colWidths, rowHeight, null);
                drawCellLines(content, tableX, y, colWidths, 0, refLines);
                drawCellLines(content, tableX, y, colWidths, 1, descLines);
                drawCellLines(content, tableX, y, colWidths, 2, qtyLines);
                drawCellLines(content, tableX, y, colWidths, 3, unitLines);
                drawCellLines(content, tableX, y, colWidths, 4, totalLines);
                y -= rowHeight;
            }
        }

        y -= 12f;
        BigDecimal totalGrossAmount = invoice.getAmount();
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        List<String> discountLines = new java.util.ArrayList<>();
        for (InvoiceDiscount d : discounts) {
            BigDecimal dVal = BigDecimal.ZERO;
            if (d.getDiscountType() == DiscountType.PERCENTAGE) {
                dVal = totalGrossAmount.multiply(d.getDiscountValue()).divide(new BigDecimal("100.00"), 2, java.math.RoundingMode.HALF_UP);
                discountLines.add("  " + d.getName() + " (" + formatMoney(d.getDiscountValue()) + "%): -" + formatMoney(dVal));
            } else {
                dVal = d.getDiscountValue();
                discountLines.add("  " + d.getName() + ": -" + formatMoney(dVal));
            }
            totalDiscountAmount = totalDiscountAmount.add(dVal);
        }
        totalDiscountAmount = totalDiscountAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalNetAmount = totalGrossAmount.subtract(totalDiscountAmount).max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal vatAmount = totalNetAmount.multiply(invoice.getVatRate()).divide(new BigDecimal("100.00"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = totalNetAmount.add(vatAmount).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal paidAmount = payments.stream().map(InvoicePaymentResponse::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);

        float summaryWidth = 220f;
        float summaryX = margin + contentWidth - summaryWidth;

        List<String> summaryParts = new ArrayList<>();
        summaryParts.add("Total Gross HT: " + formatMoney(totalGrossAmount));
        if (!discountLines.isEmpty()) {
            summaryParts.addAll(discountLines);
            summaryParts.add("Total Discounts: -" + formatMoney(totalDiscountAmount));
            summaryParts.add("Net Commercial HT: " + formatMoney(totalNetAmount));
        }
        summaryParts.add("VAT (" + formatMoney(invoice.getVatRate()) + "%): " + formatMoney(vatAmount));
        summaryParts.add("Total TTC: " + formatMoney(totalAmount));
        summaryParts.add("Paid: " + formatMoney(paidAmount));
        summaryParts.add("Remaining: " + formatMoney(remainingAmount));
        summaryParts.add("Status: " + resolvePaymentStatus(totalAmount, remainingAmount));

        List<String> summary = summaryParts;
        float summaryHeight = computeBoxHeight(summary, summaryWidth - 14f);
        ensureSpace(y, summaryHeight + 14f);
        float summaryEndY = drawInfoBox(content, summaryX, y, summaryWidth, summaryHeight, "Summary", summary);

        y = summaryEndY - 14f;

        drawHeader(content, margin, y, "Payments");
        y -= 18;
        if (payments.isEmpty()) {
            drawText(content, margin, y, "No payments recorded");
        } else {
            for (InvoicePaymentResponse payment : payments) {
                List<String> paymentLines = wrapText(
                        PDType1Font.HELVETICA,
                        9,
                        contentWidth,
                        safeDate(payment.paymentDate()) + " | " + payment.paymentMethod() + " | " + formatMoney(payment.paidAmount()) + " | " + safe(payment.paymentReference())
                );
                ensureSpace(y, paymentLines.size() * 12f + 4f);
                for (String line : paymentLines) {
                    drawText(content, margin, y, line);
                    y -= 12;
                }
                y -= 4;
            }
        }
    }

    private float drawInfoBox(PDPageContentStream content, float x, float topY, float width, float height, String title, List<String> lines) throws IOException {
        drawTableRowFrame(content, x, topY, new float[] {width}, height, null);
        drawHeader(content, x + 6f, topY - 14f, title);
        float textY = topY - 28f;
        for (String line : lines) {
            List<String> wrapped = wrapText(PDType1Font.HELVETICA, 9, width - 14f, line);
            for (String wrappedLine : wrapped) {
                drawText(content, x + 6f, textY, wrappedLine);
                textY -= 12f;
            }
        }
        return topY - height;
    }

    private float computeBoxHeight(List<String> lines, float contentWidth) throws IOException {
        int totalLineCount = 0;
        for (String line : lines) {
            totalLineCount += wrapText(PDType1Font.HELVETICA, 9, contentWidth, line).size();
        }
        return 28f + totalLineCount * 12f + 8f;
    }

    private void drawTableRowFrame(PDPageContentStream content, float x, float topY, float[] colWidths, float rowHeight, Color fillColor) throws IOException {
        float totalWidth = 0f;
        for (float w : colWidths) {
            totalWidth += w;
        }

        if (fillColor != null) {
            content.setNonStrokingColor(fillColor);
            content.addRect(x, topY - rowHeight, totalWidth, rowHeight);
            content.fill();
            content.setNonStrokingColor(Color.BLACK);
        }

        content.addRect(x, topY - rowHeight, totalWidth, rowHeight);
        content.stroke();

        float cursorX = x;
        for (int i = 0; i < colWidths.length - 1; i++) {
            cursorX += colWidths[i];
            content.moveTo(cursorX, topY);
            content.lineTo(cursorX, topY - rowHeight);
            content.stroke();
        }
    }

    private void drawCellText(PDPageContentStream content, float tableX, float rowTopY, float[] colWidths, int colIndex, String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize) throws IOException {
        float x = tableX;
        for (int i = 0; i < colIndex; i++) {
            x += colWidths[i];
        }
        writeTextAt(content, font, fontSize, x + 4f, rowTopY - 12f, text);
    }

    private void drawCellLines(PDPageContentStream content, float tableX, float rowTopY, float[] colWidths, int colIndex, List<String> lines) throws IOException {
        float x = tableX;
        for (int i = 0; i < colIndex; i++) {
            x += colWidths[i];
        }
        float textY = rowTopY - 12f;
        for (String line : lines) {
            drawText(content, x + 4f, textY, line);
            textY -= 12f;
        }
    }

    private float writeSection(PDPageContentStream content, float x, float y, float maxWidth, String title, List<String> lines) throws IOException {
        drawHeader(content, x, y, title);
        float currentY = y - 18;
        for (String line : lines) {
            List<String> wrapped = wrapText(PDType1Font.HELVETICA, 9, maxWidth, safe(line));
            for (String wrappedLine : wrapped) {
                drawText(content, x, currentY, wrappedLine);
                currentY -= 12;
            }
        }
        return currentY;
    }

    private void ensureSpace(float y, float neededHeight) {
        if (y - neededHeight < 80f) {
            throw new IllegalStateException("PDF pagination not yet implemented for long invoices");
        }
    }

    private void drawTitle(PDPageContentStream content, float x, float y, String title) throws IOException {
        writeTextAt(content, PDType1Font.HELVETICA_BOLD, 20, x, y, title);
    }

    private void drawSubtitle(PDPageContentStream content, float x, float y, String text) throws IOException {
        writeTextAt(content, PDType1Font.HELVETICA, 11, x, y, text);
    }

    private void drawHeader(PDPageContentStream content, float x, float y, String text) throws IOException {
        writeTextAt(content, PDType1Font.HELVETICA_BOLD, 12, x, y, text);
    }

    private void drawText(PDPageContentStream content, float x, float y, String text) throws IOException {
        writeTextAt(content, PDType1Font.HELVETICA, 9, x, y, text);
    }

    private void writeTextAt(PDPageContentStream content, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float x, float y, String text) throws IOException {
        boolean textStarted = false;
        try {
            content.beginText();
            textStarted = true;
            content.setNonStrokingColor(Color.BLACK);
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(sanitizeForPdf(safe(text)));
        } finally {
            if (textStarted) {
                content.endText();
            }
        }
    }

    private String sanitizeForPdf(String value) {
        String normalized = value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            // Keep printable ASCII + latin-1 supplement, replace others for Helvetica compatibility.
            if ((ch >= 32 && ch <= 126) || (ch >= 160 && ch <= 255)) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private String resolveTemplateLabel(Invoice invoice) {
        return invoice.getTemplateUsed() != null ? invoice.getTemplateUsed().name() : InvoiceTemplate.CLASSIC.name();
    }

    private List<String> wrapText(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float maxWidth, String text) throws IOException {
        String normalized = sanitizeForPdf(safe(text));
        String[] words = normalized.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (textWidth(font, fontSize, word) <= maxWidth) {
                current.append(word);
            } else {
                lines.addAll(breakLongWord(font, fontSize, maxWidth, word));
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines.isEmpty() ? List.of("-") : lines;
    }

    private List<String> breakLongWord(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float maxWidth, String word) throws IOException {
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            chunk.append(word.charAt(i));
            if (textWidth(font, fontSize, chunk.toString()) > maxWidth) {
                if (chunk.length() == 1) {
                    chunks.add(chunk.toString());
                    chunk.setLength(0);
                } else {
                    String accepted = chunk.substring(0, chunk.length() - 1);
                    chunks.add(accepted);
                    String overflow = chunk.substring(chunk.length() - 1);
                    chunk.setLength(0);
                    chunk.append(overflow);
                }
            }
        }
        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private float textWidth(org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(sanitizeForPdf(text)) / 1000f * fontSize;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeDate(java.time.LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : MONEY.format(value);
    }

    private String formatNumber(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private InvoicePaymentStatus resolvePaymentStatus(BigDecimal invoiceAmount, BigDecimal remainingAmount) {
        if (remainingAmount.compareTo(invoiceAmount) == 0) {
            return InvoicePaymentStatus.UNPAID;
        }
        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoicePaymentStatus.PAID;
        }
        return InvoicePaymentStatus.PARTIAL;
    }
}

