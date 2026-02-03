package cz.muni.fi.distributed_prov_system.utils.prov;

import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class ProvToolboxUtils {

    private ProvToolboxUtils() {
    }

    public static Document parseDocument(String base64Document, String format) {
        try {
            System.out.println("Decoding PROV document from base64.");
            byte[] decoded = Base64.getDecoder().decode(base64Document);
            System.out.println("Input stream creating.");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(decoded);
            System.out.println("Interop framework initializing.");
            InteropFramework interop = new InteropFramework();
            System.out.println("Determining PROV format.");
            String toolboxFormat = normalizeFormat(format);
            System.out.println("Parsing PROV document.");
            Formats.ProvFormat provFormat = interop.getTypeForFormat(toolboxFormat);
            System.out.println("PROV format determined: " + provFormat);
            if (provFormat == null) {
                throw new IllegalArgumentException("Unknown PROV format: " + toolboxFormat);
            }
            return interop.readDocument(inputStream, provFormat);
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalArgumentException("Unable to parse PROV document: " + detail, e);
        }
    }

    public static String serializeDocumentToBase64(Document document, String format) {
        try {
            InteropFramework interop = new InteropFramework();
            String toolboxFormat = normalizeFormat(format);
            Formats.ProvFormat provFormat = interop.getTypeForFormat(toolboxFormat);
            if (provFormat == null) {
                throw new IllegalArgumentException("Unknown PROV format: " + toolboxFormat);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            interop.writeDocument(outputStream, document, provFormat);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize PROV document.", e);
        }
    }

    private static String normalizeFormat(String format) {
        if (format == null) {
            return "json";
        }
        return switch (format.toLowerCase()) {
            case "rdf", "trig" -> "trig";
            case "xml" -> "xml";
            case "provn" -> "provn";
            case "json" -> "json";
            default -> format.toLowerCase();
        };
    }
}
