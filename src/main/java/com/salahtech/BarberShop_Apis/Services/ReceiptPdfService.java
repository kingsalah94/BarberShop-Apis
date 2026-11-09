package com.salahtech.BarberShop_Apis.Services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import com.salahtech.BarberShop_Apis.repositories.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Génère un reçu PDF (format ticket) pour une prestation de la file. */
@Service
@RequiredArgsConstructor
public class ReceiptPdfService {

    private final QueueEntryRepository queueRepository;

    private static final Font F_TITLE = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font F_SUB = new Font(Font.HELVETICA, 9, Font.NORMAL, new java.awt.Color(120, 120, 120));
    private static final Font F_LABEL = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(90, 90, 90));
    private static final Font F_VALUE = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font F_TOTAL = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font F_FOOT = new Font(Font.HELVETICA, 9, Font.ITALIC, new java.awt.Color(120, 120, 120));

    @Transactional(readOnly = true)
    public byte[] buildForEntry(Long entryId) {
        QueueEntry e = queueRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation introuvable: " + entryId));

        String salonName = e.getSalon() != null ? e.getSalon().getName() : "BarberShop";
        String barber = e.getAssignedBarber() != null ? e.getAssignedBarber().getName()
                : (e.getAssignedEmployee() != null
                        ? (e.getAssignedEmployee().getFirstName() + " " + e.getAssignedEmployee().getLastName())
                        : null);
        LocalDateTime when = e.getEndedAt() != null ? e.getEndedAt() : LocalDateTime.now();
        BigDecimal amount = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
        String method = e.getPaymentMethod() != null ? methodLabel(e.getPaymentMethod().name()) : "—";
        boolean paid = e.getPaymentStatus() != null && e.getPaymentStatus().name().equals("PAID");

        // Page étroite (~80 mm de large).
        Document doc = new Document(new Rectangle(240, 460), 18, 18, 20, 18);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(center(new Paragraph(salonName, F_TITLE)));
        doc.add(center(new Paragraph("Reçu de paiement", F_SUB)));
        doc.add(sep());

        doc.add(row("Ticket", e.getTicketNumber() != null ? e.getTicketNumber() : ("#" + e.getId())));
        doc.add(row("Client", e.getCustomerName() != null ? e.getCustomerName() : "—"));
        doc.add(row("Date", when.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        doc.add(row("Heure", when.format(DateTimeFormatter.ofPattern("HH:mm"))));
        if (barber != null) doc.add(row("Prestataire", barber));
        doc.add(sep());

        doc.add(row("Méthode", method));
        doc.add(row("Statut", paid ? "PAYÉ" : "NON PAYÉ"));

        Paragraph total = new Paragraph();
        total.add(new Phrase("TOTAL", F_TOTAL));
        Paragraph totalVal = center(new Paragraph(amount.stripTrailingZeros().toPlainString() + " FCFA", F_TOTAL));
        totalVal.setSpacingBefore(6);
        doc.add(sep());
        doc.add(totalVal);

        Paragraph foot = center(new Paragraph("Merci de votre visite !", F_FOOT));
        foot.setSpacingBefore(14);
        doc.add(foot);

        doc.close();
        return out.toByteArray();
    }

    private Paragraph center(Paragraph p) { p.setAlignment(Element.ALIGN_CENTER); return p; }

    private Paragraph sep() {
        Paragraph p = new Paragraph("----------------------------------------",
                new Font(Font.COURIER, 8, Font.NORMAL, new java.awt.Color(180, 180, 180)));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        return p;
    }

    /** Ligne « label ......... valeur » sur toute la largeur. */
    private Paragraph row(String label, String value) {
        Paragraph p = new Paragraph();
        p.setSpacingAfter(2);
        Phrase l = new Phrase(label + "  ", F_LABEL);
        Phrase v = new Phrase(value, F_VALUE);
        p.add(l);
        p.add(v);
        return p;
    }

    private String methodLabel(String m) {
        switch (m) {
            case "CASH": return "Espèces";
            case "CREDIT_CARD": return "Carte bancaire";
            case "MOBILE_MONEY": return "Mobile Money";
            default: return m;
        }
    }
}
