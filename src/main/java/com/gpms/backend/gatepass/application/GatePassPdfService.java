package com.gpms.backend.gatepass.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.gatepass.domain.GatePassItem;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.gatepass.infrastructure.GatePassItemRepository;
import com.gpms.backend.signature.domain.DigitalSignature;
import com.gpms.backend.user.domain.User;
import com.lowagie.text.Chunk;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a gate pass as the document that leaves the gate with the
 * truck.
 *
 * The order on the page is the order it gets read in at the gate:
 *
 *   1. The company name, so the guard at the far end knows whose pass
 *      this is before anything else.
 *   2. Gate pass number on the left, invoice date on the right - the
 *      two things anybody quotes over the phone about this document.
 *   3. Vehicle and dispatch: who is driving what, for whom, to where.
 *   4. The consignment, by name only. A gate pass is not an invoice;
 *      quantities and prices belong on the invoice it travels with,
 *      and repeating them here only creates two numbers that can
 *      disagree.
 *   5. Three signatures, and nothing beside them. No dates: the
 *      document says who authorised it, and the database keeps when.
 *      A printed date next to a signature is the thing people argue
 *      about, and it is not what the signature is for.
 *   6. The computer-generated notice and the company's contact line.
 *
 * Signature blocks of roles that have not acted stay genuinely blank -
 * a ruled line with the role under it. The printed copy circulates
 * while it is still unsigned, and that blank is the statement.
 */
@Service
public class GatePassPdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    /*
     * The palette, deliberately dark.
     *
     * The first version of this page was printed on warehouse laser
     * printers and came out washed out - grey rules on white, labels so
     * pale the guard at the gate could not read them in daylight. Every
     * ink here is a step darker than looks right on a bright screen,
     * because the screen is not where this document is read.
     *
     * INK is near black for everything that must be read at arm's
     * length. BAND is the dark navy behind the title and the table
     * header, which is what makes the sheet read as a document rather
     * than a printout. LABEL_INK is dark enough to survive a toner-low
     * printer, where the old grey simply vanished.
     */
    private static final Color INK = new Color(0x0B, 0x0F, 0x14);
    private static final Color BAND = new Color(0x15, 0x22, 0x33);
    private static final Color BAND_TEXT = new Color(0xFF, 0xFF, 0xFF);
    private static final Color RULE = new Color(0x15, 0x22, 0x33);
    private static final Color HAIR = new Color(0x8D, 0x98, 0xA6);
    private static final Color MUTED = new Color(0x3C, 0x45, 0x52);
    private static final Color WASH = new Color(0xE7, 0xEC, 0xF2);

    private static final Font COMPANY =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INK);
    private static final Font TITLE =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BAND_TEXT);
    private static final Font SECTION =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INK);
    private static final Font LABEL =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, MUTED);
    private static final Font VALUE =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INK);
    private static final Font BODY =
            FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
    private static final Font SMALL =
            FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font TABLE_HEAD =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BAND_TEXT);
    private static final Font NOTICE =
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED);

    /*
     * Stands in for a scanned signature until one is uploaded. Italic
     * serif rather than the body face, so a typed name in the
     * signature area reads as a mark rather than one more data field.
     */
    private static final Font SIGNATURE_NAME =
            FontFactory.getFont(FontFactory.TIMES_ITALIC, 15, INK);

    private final GatePassItemRepository gatePassItemRepository;

    public GatePassPdfService(GatePassItemRepository gatePassItemRepository) {
        this.gatePassItemRepository = gatePassItemRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(GatePassRequest pass) {

        /*
         * The company is taken from the pass itself, through its
         * warehouse - not from whoever is printing it. A pass belongs to
         * the client that issued it, so re-printing an old one in a
         * multi-company deployment still carries the right letterhead
         * even if the account doing the printing changed.
         */
        Company company = pass.getWarehouse() == null
                ? null
                : pass.getWarehouse().getCompany();

        if (company == null) {
            throw new BusinessException(
                    "This gate pass is not attached to a company");
        }

        List<GatePassItem> items = gatePassItemRepository
                .findAllByGatePassRequestIdAndDeletedFalseOrderByLineNumberAsc(pass.getId());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addCompanyName(document, company);
            addPassLine(document, pass);
            addVehicleAndDispatch(document, pass);
            addConsignment(document, items);
            addAuthorisation(document, pass);
            addFooter(document, company);

            document.close();

        } catch (Exception exception) {
            /*
             * Wrapped rather than propagated: OpenPDF throws a checked
             * DocumentException from nearly every call, and a caller of
             * "give me the PDF" can do nothing useful with it.
             */
            throw new BusinessException("Could not generate the gate pass PDF");
        }

        return out.toByteArray();
    }

    // ---------------------------------------------------------------
    // 1. COMPANY NAME
    // ---------------------------------------------------------------

    private void addCompanyName(Document document, Company company) throws Exception {

        if (company.getLogoData() != null && company.getLogoData().length > 0) {
            Image logo = Image.getInstance(company.getLogoData());
            logo.scaleToFit(54, 54);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        }

        Paragraph name = new Paragraph(nullToDash(company.getName()), COMPANY);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(2);
        document.add(name);

        /*
         * The title sits in a solid dark band rather than as another
         * line of text. It is the one piece of the page that has to be
         * recognisable from across a loading bay, and a band survives a
         * fax, a photocopy and a bad printer in a way grey text does
         * not.
         */
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        band.setSpacingBefore(10);

        PdfPCell bandCell = new PdfPCell(new Phrase("GATE PASS", TITLE));
        bandCell.setBackgroundColor(BAND);
        bandCell.setBorder(Rectangle.NO_BORDER);
        bandCell.setPadding(7);
        bandCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        band.addCell(bandCell);

        document.add(band);
    }

    // ---------------------------------------------------------------
    // 2. GATE PASS NUMBER  |  DISPATCH DATE
    // ---------------------------------------------------------------

    private void addPassLine(Document document, GatePassRequest pass) throws Exception {

        PdfPTable row = new PdfPTable(new float[]{1f, 1f});
        row.setWidthPercentage(100);
        row.setSpacingBefore(8);

        /*
         * The gate pass number and nothing else on the left. This is the
         * number the guard reads out and the number every other record
         * of this journey is filed under; an invoice number beside it
         * only invites quoting the wrong one.
         */
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(inline("Gate pass : ", nullToDash(pass.getGatePassNumber()),
                Element.ALIGN_LEFT));
        row.addCell(left);

        /*
         * The dispatch date, not the invoice date.
         *
         * The invoice date belongs to a document this pass travels
         * with and is often not the day the truck leaves - which is the
         * only date a gate cares about. Printing the invoice date here
         * had people reading it as the dispatch date, so the pass now
         * states the date it is actually for.
         */
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.addElement(inline("Dispatch date : ", formatDate(pass.getDispatchDate()),
                Element.ALIGN_RIGHT));
        row.addCell(right);

        document.add(row);
        document.add(rule(8, RULE));
    }

    /** "Label : value" on one line, the label grey and the value black. */
    private Paragraph inline(String label, String value, int alignment) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label, LABEL));
        paragraph.add(new Chunk(value, VALUE));
        paragraph.setAlignment(alignment);
        paragraph.setSpacingBefore(2);
        return paragraph;
    }

    // ---------------------------------------------------------------
    // 3. VEHICLE AND DISPATCH
    // ---------------------------------------------------------------

    private void addVehicleAndDispatch(Document document, GatePassRequest pass) throws Exception {

        document.add(sectionHeading("Vehicle and dispatch"));

        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(4);

        field(grid, "Vehicle number", pass.getVehicle() == null
                ? null : pass.getVehicle().getRegistrationNumber());
        field(grid, "Driver name", pass.getDriver() == null
                ? null : pass.getDriver().getName());
        field(grid, "Driver mobile number", pass.getDriver() == null
                ? null : pass.getDriver().getMobileNumber());

        /*
         * The typed transport name wins over the linked vendor. A pass
         * raised for a company with no vendor record still has to print
         * who is carrying the load.
         */
        field(grid, "Transport company", firstNonBlank(
                pass.getTransportName(),
                pass.getVendor() == null ? null : pass.getVendor().getName()));

        field(grid, "Destination", pass.getDestination());
        field(grid, "Warehouse name", pass.getWarehouse() == null
                ? null : pass.getWarehouse().getName());

        /*
         * No dispatch date here any more - it is on the header line
         * beside the gate pass number. One date, printed once, cannot
         * be read two ways.
         *
         * That leaves exactly six fields, which fills two rows of this
         * three-column grid: an incomplete last row is dropped by
         * OpenPDF along with the field in it, so the count matters.
         */
        document.add(grid);
    }

    // ---------------------------------------------------------------
    // 4. CONSIGNMENT - NAMES ONLY
    // ---------------------------------------------------------------

    private void addConsignment(Document document, List<GatePassItem> items) throws Exception {

        document.add(sectionHeading("Consignment"));

        PdfPTable table = new PdfPTable(new float[]{0.7f, 9.3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        headerCell(table, "#");
        headerCell(table, "Consignment");

        if (items.isEmpty()) {

            /*
             * A pass with nothing listed is allowed, and says so
             * plainly. An empty vehicle leaving the yard is a real
             * movement that a gate still has to authorise, so this is
             * a normal pass rather than an incomplete one.
             */
            PdfPCell empty = new PdfPCell(
                    new Phrase("No consignment listed on this pass.", BODY));
            empty.setColspan(2);
            empty.setPadding(7);
            empty.setBorder(Rectangle.BOTTOM);
            empty.setBorderColorBottom(HAIR);
            empty.setBorderWidthBottom(0.5f);
            table.addCell(empty);

        } else {

            int index = 1;

            for (GatePassItem item : items) {
                boolean shaded = index % 2 == 0;
                bodyCell(table, String.valueOf(index), Element.ALIGN_CENTER, shaded);
                bodyCell(table, nullToDash(item.getItemDescription()),
                        Element.ALIGN_LEFT, shaded);
                index++;
            }
        }

        document.add(table);
    }

    // ---------------------------------------------------------------
    // 5. AUTHORISATION - THREE SIGNATURES, NO DATES
    // ---------------------------------------------------------------

    private void addAuthorisation(Document document, GatePassRequest pass) throws Exception {

        document.add(sectionHeading("Authorisation"));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        /*
         * The employee block, not "requested by": any of the three
         * roles can raise a pass, so on a manager-raised one this block
         * stays blank until an employee signs it - which is exactly
         * what the printed document should show.
         */
        table.addCell(signatureCell(
                "Employee",
                pass.getEmployeeApprovedBy(),
                pass.getEmployeeSignature(),
                pass.getEmployeeApprovedAt() != null));

        table.addCell(signatureCell(
                "Manager",
                pass.getManagerApprovedBy(),
                pass.getManagerSignature(),
                pass.getManagerApprovedAt() != null));

        table.addCell(signatureCell(
                "Admin",
                pass.getAdminApprovedBy(),
                pass.getAdminSignature(),
                pass.getAdminApprovedAt() != null));

        document.add(table);
    }

    private PdfPCell signatureCell(
            String role,
            User person,
            DigitalSignature signature,
            boolean signed
    ) throws Exception {

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setMinimumHeight(80);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);

        boolean hasImage = signature != null
                && signature.getImageData() != null
                && signature.getImageData().length > 0;

        if (signed && hasImage) {

            Image image = Image.getInstance(signature.getImageData());
            image.scaleToFit(115, 36);
            Paragraph holder = new Paragraph();
            holder.add(new Chunk(image, 0, 0));
            cell.addElement(holder);

        } else if (signed) {

            /*
             * Signed, but no image on file. The typed name goes in the
             * signature area so the document still shows who signed
             * rather than looking unsigned.
             */
            Paragraph typed = new Paragraph(
                    person == null ? " " : person.getFullName(), SIGNATURE_NAME);
            typed.setSpacingAfter(4);
            cell.addElement(typed);

        } else {

            /*
             * The spacer keeps an unsigned block the same height as a
             * signed one, so the three columns stay level.
             */
            Paragraph spacer = new Paragraph(" ", BODY);
            spacer.setSpacingAfter(20);
            cell.addElement(spacer);
        }

        cell.addElement(rule(2, RULE, 1.2f));

        /*
         * A single space rather than an empty string: OpenPDF drops a
         * zero-length paragraph entirely, which would shorten the
         * unsigned blocks and leave the columns at different heights.
         */
        Paragraph name = new Paragraph(
                signed && person != null ? person.getFullName() : " ", VALUE);
        name.setSpacingBefore(3);
        cell.addElement(name);

        cell.addElement(new Paragraph(role, LABEL));

        return cell;
    }

    // ---------------------------------------------------------------
    // 6. NOTICE AND CONTACT LINE
    // ---------------------------------------------------------------

    private void addFooter(Document document, Company company) throws Exception {

        document.add(rule(20, RULE, 1.6f));

        Paragraph notice = new Paragraph(
                "This is a computer generated gate pass.", NOTICE);
        notice.setAlignment(Element.ALIGN_CENTER);
        notice.setSpacingBefore(6);
        document.add(notice);

        /*
         * The company's own details, boxed, at the left foot of the page
         * - where a letterhead puts them. Half-width on purpose: a box
         * stretched across the sheet reads as another section of the
         * pass rather than as the sender's address.
         */
        PdfPTable row = new PdfPTable(new float[]{1.3f, 1f});
        row.setWidthPercentage(100);
        row.setSpacingBefore(14);
        row.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell box = new PdfPCell();
        box.setBorder(Rectangle.BOX);
        box.setBorderColor(RULE);
        box.setBorderWidth(0.8f);
        box.setPadding(10);

        String address = joinNonBlank(", ",
                company.getAddressLine1(), company.getAddressLine2(),
                company.getCity(), company.getState(), company.getPostalCode());

        if (!address.isBlank()) {
            box.addElement(new Paragraph(address, SMALL));
        }

        if (blankToNull(company.getPhoneNumber()) != null) {
            box.addElement(new Paragraph("Phone : " + company.getPhoneNumber(), SMALL));
        }

        if (blankToNull(company.getEmail()) != null) {
            box.addElement(new Paragraph("Email : " + company.getEmail(), SMALL));
        }

        if (blankToNull(company.getWebsite()) != null) {
            box.addElement(new Paragraph(company.getWebsite(), SMALL));
        }

        /*
         * OpenPDF collapses a cell with nothing in it and the box loses
         * its borders, so an unfilled company profile still gets one
         * line.
         */
        if (address.isBlank()
                && blankToNull(company.getPhoneNumber()) == null
                && blankToNull(company.getEmail()) == null
                && blankToNull(company.getWebsite()) == null) {
            box.addElement(new Paragraph(nullToDash(company.getName()), SMALL));
        }

        row.addCell(box);

        PdfPCell filler = new PdfPCell();
        filler.setBorder(Rectangle.NO_BORDER);
        filler.addElement(new Paragraph(" ", SMALL));
        row.addCell(filler);

        document.add(row);
    }

    // ---------------------------------------------------------------
    // BUILDING BLOCKS
    // ---------------------------------------------------------------

    /*
     * A section heading, with its own dark underline.
     *
     * Returned as a one-cell table rather than a paragraph so the
     * heading and the line beneath it cannot be split across a page
     * break, which used to leave a stray rule at the top of page two.
     */
    private PdfPTable sectionHeading(String text) {

        PdfPTable holder = new PdfPTable(1);
        holder.setWidthPercentage(100);
        holder.setSpacingBefore(16);

        PdfPCell cell = new PdfPCell(new Phrase(spaced(text), SECTION));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(RULE);
        cell.setBorderWidthBottom(1.2f);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(0);
        holder.addCell(cell);

        return holder;
    }

    /*
     * Letter-spaced capitals. A heading of five characters reads as a
     * label rather than a word when it is set tight, and OpenPDF has no
     * tracking on a Phrase, so the spacing is put in by hand.
     */
    private String spaced(String text) {

        StringBuilder builder = new StringBuilder();

        for (char character : text.toUpperCase().toCharArray()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(character);
        }

        return builder.toString();
    }

    private PdfPTable rule(float spacingBefore, Color color) {
        return rule(spacingBefore, color, 1f);
    }

    /*
     * A horizontal line. The width is explicit because the default
     * hairline all but disappears on a warehouse printer, and these
     * rules are what divide the document into readable blocks.
     */
    private PdfPTable rule(float spacingBefore, Color color, float width) {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(spacingBefore);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(1f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(color);
        cell.setBorderWidthBottom(width);
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
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), TABLE_HEAD));
        cell.setPadding(7);
        cell.setBackgroundColor(BAND);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    /*
     * A row of the consignment table. Every other row carries a pale
     * wash, which is what keeps the eye on one line across a page when
     * a truck is carrying a dozen consignments.
     */
    private void bodyCell(PdfPTable table, String text, int alignment, boolean shaded) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(HAIR);
        cell.setBorderWidthBottom(0.5f);
        cell.setHorizontalAlignment(alignment);

        if (shaded) {
            cell.setBackgroundColor(WASH);
        }

        table.addCell(cell);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
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
