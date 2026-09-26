package com.gpms.backend.gatepass.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.company.application.CompanyProfileService;
import com.gpms.backend.company.domain.CompanyProfile;
import com.gpms.backend.gatepass.domain.GatePassItem;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.gatepass.infrastructure.GatePassItemRepository;
import com.gpms.backend.signature.domain.DigitalSignature;
import com.gpms.backend.user.domain.User;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a gate pass as the printable document the client asked
 * for: company logo and name at the head, the pass and vehicle
 * details, the consignment lines, and three signature blocks at the
 * foot.
 *
 * The signature blocks are the point of the whole document, so they
 * behave carefully:
 *
 *   - The requester's block is filled the moment the pass is raised,
 *     with their signature, their name and the creation date. The
 *     pass is printed and carried away immediately, so that copy has
 *     to be signed for by whoever raised it.
 *
 *   - The manager and admin blocks are left genuinely BLANK - a bare
 *     ruled line under the role name - until that role approves.
 *     Nothing is written there, because the printed copy circulates
 *     while it is still unsigned and a placeholder word could be
 *     mistaken for a state the document is in.
 *
 *   - When they do approve, the block shows the signature image that
 *     was in force on the day they signed, their name, and the date
 *     they signed.
 *
 *   - Those dates come off the approval, never off the pass. A pass
 *     created on 1 September and approved on the 6th prints the 6th.
 */
@Service
public class GatePassPdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /* The logo's navy, so the document matches the app. */
    private static final Color NAVY = new Color(0x10, 0x22, 0x39);
    private static final Color RULE = new Color(0xDD, 0xE4, 0xEC);
    private static final Color MUTED = new Color(0x5A, 0x6B, 0x84);

    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font VALUE = FontFactory.getFont(FontFactory.HELVETICA, 10, NAVY);
    private static final Font VALUE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    /*
     * Stands in for a scanned signature until real images are
     * uploaded. Italic serif rather than the body face, so a typed
     * name in the signature area reads as a mark rather than as one
     * more line of data.
     */
    private static final Font SIGNATURE_NAME =
            FontFactory.getFont(FontFactory.TIMES_ITALIC, 16, NAVY);

    private final GatePassItemRepository gatePassItemRepository;
    private final CompanyProfileService companyProfileService;

    public GatePassPdfService(
            GatePassItemRepository gatePassItemRepository,
            CompanyProfileService companyProfileService
    ) {
        this.gatePassItemRepository = gatePassItemRepository;
        this.companyProfileService = companyProfileService;
    }

    @Transactional(readOnly = true)
    public byte[] render(GatePassRequest pass) {

        CompanyProfile company = companyProfileService.require();

        List<GatePassItem> items = gatePassItemRepository
                .findAllByGatePassRequestIdAndDeletedFalseOrderByLineNumberAsc(pass.getId());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addLetterhead(document, company);
            addTitleBar(document, pass);
            addDetails(document, pass);
            addItems(document, items);
            addRemarks(document, pass);
            addSignatures(document, pass);
            addFooter(document);

            document.close();

        } catch (Exception exception) {
            /*
             * Wrapped rather than propagated: OpenPDF throws checked
             * DocumentException from nearly every call, and a caller
             * of "give me the PDF" can do nothing useful with it.
             */
            throw new BusinessException("Could not generate the gate pass PDF");
        }

        return out.toByteArray();
    }

    // ---------------------------------------------------------------
    // HEADER
    // ---------------------------------------------------------------

    private void addLetterhead(Document document, CompanyProfile company) throws Exception {

        PdfPTable head = new PdfPTable(new float[]{1f, 3f});
        head.setWidthPercentage(100);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (company.getLogoData() != null && company.getLogoData().length > 0) {
            Image logo = Image.getInstance(company.getLogoData());
            logo.scaleToFit(70, 70);
            logoCell.addElement(logo);
        }
        head.addCell(logoCell);

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.addElement(new Paragraph(nullToDash(company.getName()), H1));

        String address = joinNonBlank(", ",
                company.getAddressLine1(), company.getAddressLine2(),
                company.getCity(), company.getState(), company.getPostalCode());
        if (!address.isBlank()) {
            nameCell.addElement(new Paragraph(address, SMALL));
        }

        String contact = joinNonBlank("   |   ",
                blankToNull(company.getPhoneNumber()),
                blankToNull(company.getEmail()),
                company.getGstNumber() == null || company.getGstNumber().isBlank()
                        ? null : "GSTIN " + company.getGstNumber());
        if (!contact.isBlank()) {
            nameCell.addElement(new Paragraph(contact, SMALL));
        }

        head.addCell(nameCell);
        document.add(head);

        document.add(rule(12));
    }

    private void addTitleBar(Document document, GatePassRequest pass) throws Exception {

        PdfPTable bar = new PdfPTable(new float[]{2f, 1f});
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(6);

        PdfPCell title = new PdfPCell(new Phrase("GATE PASS", H1));
        title.setBorder(Rectangle.NO_BORDER);
        title.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(title);

        PdfPCell number = new PdfPCell();
        number.setBorder(Rectangle.NO_BORDER);
        number.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph gp = new Paragraph(nullToDash(pass.getGatePassNumber()), H2);
        gp.setAlignment(Element.ALIGN_RIGHT);
        number.addElement(gp);

        Paragraph req = new Paragraph("Request " + nullToDash(pass.getRequestNumber()), SMALL);
        req.setAlignment(Element.ALIGN_RIGHT);
        number.addElement(req);

        Paragraph status = new Paragraph(
                "Status: " + (pass.getStatus() == null ? "-" : pass.getStatus().name()), SMALL);
        status.setAlignment(Element.ALIGN_RIGHT);
        number.addElement(status);

        bar.addCell(number);
        document.add(bar);
        document.add(rule(10));
    }

    // ---------------------------------------------------------------
    // DETAILS
    // ---------------------------------------------------------------

    private void addDetails(Document document, GatePassRequest pass) throws Exception {

        document.add(sectionHeading("Vehicle and dispatch"));

        PdfPTable grid = new PdfPTable(4);
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(4);

        field(grid, "Vehicle number", pass.getVehicle() == null
                ? null : pass.getVehicle().getRegistrationNumber());
        field(grid, "Vehicle type", pass.getVehicle() == null
                ? null : pass.getVehicle().getVehicleType());
        field(grid, "Driver name", pass.getDriver() == null
                ? null : pass.getDriver().getName());
        field(grid, "Driver mobile", pass.getDriver() == null
                ? null : pass.getDriver().getMobileNumber());
        field(grid, "Transport company", pass.getVendor() == null
                ? null : pass.getVendor().getName());
        field(grid, "Destination", pass.getDestination());
        field(grid, "Dispatch date", formatDate(pass.getDispatchDate()));
        field(grid, "Warehouse", pass.getWarehouse() == null
                ? null : pass.getWarehouse().getName());

        document.add(grid);
    }

    private void addItems(Document document, List<GatePassItem> items) throws Exception {

        document.add(sectionHeading("Consignment"));

        PdfPTable table = new PdfPTable(new float[]{0.7f, 5f, 1.3f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        headerCell(table, "#");
        headerCell(table, "Description");
        headerCell(table, "Qty");
        headerCell(table, "Unit");

        if (items.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No consignment lines recorded.", VALUE));
            empty.setColspan(4);
            empty.setPadding(6);
            empty.setBorderColor(RULE);
            table.addCell(empty);
        } else {
            for (GatePassItem item : items) {
                bodyCell(table, String.valueOf(item.getLineNumber()), Element.ALIGN_CENTER);
                bodyCell(table, nullToDash(item.getItemDescription()), Element.ALIGN_LEFT);
                bodyCell(table, item.getQuantity() == null
                        ? "-" : String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
                bodyCell(table, nullToDash(item.getUnitOfMeasure()), Element.ALIGN_CENTER);
            }
        }

        document.add(table);
    }

    private void addRemarks(Document document, GatePassRequest pass) throws Exception {

        if (pass.getRemarks() == null || pass.getRemarks().isBlank()) {
            return;
        }
        document.add(sectionHeading("Remarks"));
        Paragraph remarks = new Paragraph(pass.getRemarks(), VALUE);
        remarks.setSpacingBefore(4);
        document.add(remarks);
    }

    // ---------------------------------------------------------------
    // SIGNATURES
    // ---------------------------------------------------------------

    private void addSignatures(Document document, GatePassRequest pass) throws Exception {

        document.add(rule(18));
        document.add(sectionHeading("Authorisation"));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);

        /*
         * The employee block, not "requested by".
         *
         * Any of the three roles can raise a pass, so the requester
         * is not necessarily the employee - and on a manager-raised
         * pass this block stays blank until an employee signs it,
         * which is exactly what the printed document should show.
         */
        table.addCell(signatureCell(
                "Employee",
                pass.getEmployeeApprovedBy(),
                pass.getEmployeeSignature(),
                pass.getEmployeeApprovedAt()));

        table.addCell(signatureCell(
                "Manager",
                pass.getManagerApprovedBy(),
                pass.getManagerSignature(),
                pass.getManagerApprovedAt()));

        table.addCell(signatureCell(
                "Admin",
                pass.getAdminApprovedBy(),
                pass.getAdminSignature(),
                pass.getAdminApprovedAt()));

        document.add(table);
    }

    /**
     * One signature block.
     *
     * [signedAt] null means this role has not acted. The block then
     * prints as a bare ruled line with only the role name under it -
     * no name, no date, no placeholder text. That blank is the
     * audit trail saying "not authorised yet", and it fills in
     * whenever the approver gets to it, days later if need be.
     */
    private PdfPCell signatureCell(
            String role,
            User person,
            DigitalSignature signature,
            Instant signedAt
    ) throws Exception {

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setMinimumHeight(86);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);

        boolean signed = signedAt != null;

        boolean hasImage = signature != null
                && signature.getImageData() != null
                && signature.getImageData().length > 0;

        if (signed && hasImage) {

            Image image = Image.getInstance(signature.getImageData());
            image.scaleToFit(120, 38);
            Paragraph holder = new Paragraph();
            holder.add(new com.lowagie.text.Chunk(image, 0, 0));
            cell.addElement(holder);

        } else if (signed) {

            /*
             * Signed, but no image on file yet.
             *
             * The typed name goes in the signature area instead of a
             * blank space, so the document still shows WHO signed it
             * rather than looking unsigned. Once signatures are
             * uploaded the branch above takes over and nothing else
             * about this block changes.
             */
            Paragraph typed = new Paragraph(
                    person == null ? " " : person.getFullName(), SIGNATURE_NAME);
            typed.setSpacingAfter(6);
            cell.addElement(typed);

        } else {
            /*
             * Not signed. Left deliberately empty - the printed copy
             * circulates while it is still unauthorised, and the
             * blank IS the statement.
             *
             * The spacer keeps the block the same height as a signed
             * one, so the three columns stay level on the page.
             */
            Paragraph spacer = new Paragraph(" ", VALUE);
            spacer.setSpacingAfter(22);
            cell.addElement(spacer);
        }

        cell.addElement(rule(2));

        /*
         * A single space, not an empty string: OpenPDF drops a
         * zero-length paragraph entirely, which would shorten the
         * unsigned blocks and leave the three columns sitting at
         * different heights.
         */
        Paragraph name = new Paragraph(
                signed ? (person == null ? " " : person.getFullName()) : " ",
                VALUE_BOLD);
        name.setSpacingBefore(3);
        cell.addElement(name);

        cell.addElement(new Paragraph(role, LABEL));

        cell.addElement(new Paragraph(signed ? formatInstant(signedAt) : " ", SMALL));

        return cell;
    }

    private void addFooter(Document document) throws Exception {
        document.add(rule(16));
        Paragraph note = new Paragraph(
                "Computer generated gate pass. Valid only with the authorisations shown above.",
                SMALL);
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);
    }

    // ---------------------------------------------------------------
    // BUILDING BLOCKS
    // ---------------------------------------------------------------

    private Paragraph sectionHeading(String text) {
        Paragraph heading = new Paragraph(text.toUpperCase(), H2);
        heading.setSpacingBefore(14);
        return heading;
    }

    private PdfPTable rule(float spacingBefore) {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(spacingBefore);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(1f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(RULE);
        line.addCell(cell);
        return line;
    }

    private void field(PdfPTable grid, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.addElement(new Paragraph(label.toUpperCase(), LABEL));
        cell.addElement(new Paragraph(nullToDash(value), VALUE));
        grid.addCell(cell);
    }

    private void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), LABEL));
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(0xF4, 0xF7, 0xFB));
        cell.setBorderColor(RULE);
        table.addCell(cell);
    }

    private void bodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, VALUE));
        cell.setPadding(6);
        cell.setBorderColor(RULE);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    private String formatInstant(Instant instant) {
        return instant == null
                ? "-"
                : DATE_TIME.format(instant.atZone(ZoneId.systemDefault()));
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String joinNonBlank(String separator, String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }
}
