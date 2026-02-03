package cz.muni.fi.distributed_prov_system.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class TokenUtils {

    private TokenUtils() {
    }

    public static Map<String, Object> buildTokenPayload(StoreGraphRequestDTO body,
                                                        String organizationId,
                                                        String documentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("document", body.getDocument());
        payload.put("documentFormat", body.getDocumentFormat());
        if (body.getSignature() != null) {
            payload.put("signature", body.getSignature());
        }
        if (body.getCreatedOn() != null) {
            payload.put("createdOn", body.getCreatedOn());
        }
        payload.put("organizationId", organizationId);
        payload.put("type", "graph");
        payload.put("graphId", documentId);
        return payload;
    }

    public static Map<String, Object> createDummyToken(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("originatorId", organizationId);
        data.put("authorityId", "TrustedParty");
        data.put("tokenTimestamp", 0);
        data.put("documentCreationTimestamp", 0);
        data.put("documentDigest", "17fd7484d7cac628cfa43c348fe05a009a81d18c8a778e6488b707954addf2a3");

        Map<String, Object> token = new HashMap<>();
        token.put("data", data);
        token.put("signature",
                "bdysXEy2/sOSTN+Lh+v3x7cTdocMcndwuW5OT2wHpQOU/LM4os9Bow0sn4HTln9hRqFdCMukV6Cr6Nn8XvD96jlgEw9KqJj9I+cfBL81x9iqUJX/Wder3lkuIZXYUSeGsOOqUPdlqJAhapgr0V+vibAvPGoiRKqulNi/Xn0jn21lln1HEbHPsnOtM5Ca5wwXuTITJsiXCj+04y9V/XM9Uy9Ib4LLA1VYLCdifjg0ZuxJBcpS/HszlwW9B29rrkUGUsSrV9YU0ViYkeIMcS2bMXsur3EHi3/zSZ5IepUNOBDTu3BDUr33dbrgMOVraI8RU5DTZKmUOx8hzgtApZNotg==");
        return token;
    }

    public static Map<String, Object> parseTokenResponse(String tokenJson) {
        try {
            if (tokenJson == null || tokenJson.isBlank()) {
                throw new IllegalArgumentException("Invalid token response JSON: empty body.");
            }
            String trimmed = tokenJson.trim();
            if (trimmed.startsWith("<")) {
                return parseXmlToken(trimmed);
            }
            ObjectMapper mapper = new ObjectMapper();
            Object parsed = mapper.readValue(tokenJson, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                //noinspection unchecked
                return (Map<String, Object>) map;
            }
            if (parsed instanceof java.util.List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> map) {
                    //noinspection unchecked
                    return (Map<String, Object>) map;
                }
            }
            throw new IllegalArgumentException("Invalid token response JSON: unsupported structure.");
        } catch (IOException e) {
            String snippet = tokenJson == null ? "<null>"
                    : tokenJson.substring(0, Math.min(200, tokenJson.length()));
            throw new IllegalArgumentException("Invalid token response JSON: " + snippet, e);
        }
    }

    private static Map<String, Object> parseXmlToken(String xml) {
        try {
            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            var builder = dbf.newDocumentBuilder();
            var doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) {
                throw new IllegalArgumentException("Invalid token response XML: no item element.");
            }
            Element item = (Element) items.item(0);

            Map<String, Object> token = new HashMap<>();

            Element dataEl = firstChildElement(item, "data");
            if (dataEl != null) {
                Map<String, Object> data = new HashMap<>();
                putIfPresent(data, "originatorId", textOfChild(dataEl, "originatorId"));
                putIfPresent(data, "authorityId", textOfChild(dataEl, "authorityId"));
                putIfPresent(data, "tokenTimestamp", textOfChild(dataEl, "tokenTimestamp"));
                putIfPresent(data, "documentCreationTimestamp", textOfChild(dataEl, "documentCreationTimestamp"));
                putIfPresent(data, "documentDigest", textOfChild(dataEl, "documentDigest"));
                putIfPresent(data, "messageTimestamp", textOfChild(dataEl, "messageTimestamp"));

                Element addEl = firstChildElement(dataEl, "additionalData");
                if (addEl != null) {
                    Map<String, Object> additional = new HashMap<>();
                    NodeList children = addEl.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node node = children.item(i);
                        if (node instanceof Element el) {
                            String key = el.getTagName();
                            String val = el.getTextContent();
                            if (val != null && !val.isBlank()) {
                                additional.put(key, val);
                            }
                        }
                    }
                    if (!additional.isEmpty()) {
                        data.put("additionalData", additional);
                    }
                }
                token.put("data", data);
            }

            String signature = textOfChild(item, "signature");
            if (signature != null && !signature.isBlank()) {
                token.put("signature", signature);
            }

            return token;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token response XML.", e);
        }
    }

    private static Element firstChildElement(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private static String textOfChild(Element parent, String tag) {
        Element el = firstChildElement(parent, tag);
        return el == null ? null : el.getTextContent();
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeTokenData(Map<String, Object> tokenEnvelope) {
        Object dataObj = tokenEnvelope.get("data");
        Map<String, Object> data = dataObj instanceof Map ? (Map<String, Object>) dataObj : new HashMap<>();
        Object signature = tokenEnvelope.get("signature");
        if (signature != null) {
            data.put("signature", signature);
        }
        return data;
    }
}