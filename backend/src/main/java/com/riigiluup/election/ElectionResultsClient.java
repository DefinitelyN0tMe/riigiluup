package com.riigiluup.election;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches and parses the Riigikogu 2023 election RESULTS.xml from the State
 * Electoral Office open-data portal (opendata.valimised.ee, CC BY 4.0).
 *
 * <p>Structure (default namespace, parsed namespace-unaware):
 * {@code OutputReport/data/electionResult/votesAndMandates/party/candidates/candidate},
 * where each party carries {@code <name>/<code>} and each candidate carries
 * {@code <forename>/<surname>/<votes>/<districtNumber>/<registrationNumber>/<elected>}
 * plus {@code <mandateType>} only when elected.
 */
@Component
public class ElectionResultsClient {

    private static final String RESULTS_URL = "https://opendata.valimised.ee/api/RK_2023/RESULTS.xml";

    private final RestClient rest = RestClient.builder().build();

    public List<ElectionCandidateDto> fetchRk2023Results() {
        byte[] xml = rest.get().uri(RESULTS_URL).retrieve().body(byte[].class);
        if (xml == null) throw new IllegalStateException("Empty RESULTS.xml from opendata.valimised.ee");
        return parse(xml);
    }

    static List<ElectionCandidateDto> parse(byte[] xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            // Harden the parser (defence in depth; the feed carries no DOCTYPE/entities).
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

            List<ElectionCandidateDto> out = new ArrayList<>();
            NodeList parties = doc.getElementsByTagName("party");
            for (int i = 0; i < parties.getLength(); i++) {
                Element party = (Element) parties.item(i);
                String partyName = childText(party, "name");
                String partyCode = childText(party, "code");
                NodeList candidates = party.getElementsByTagName("candidate");
                for (int j = 0; j < candidates.getLength(); j++) {
                    Element c = (Element) candidates.item(j);
                    boolean elected = "true".equalsIgnoreCase(childText(c, "elected"));
                    out.add(new ElectionCandidateDto(
                            childText(c, "forename"),
                            childText(c, "surname"),
                            parseInt(childText(c, "votes")),
                            parseIntObj(childText(c, "districtNumber")),
                            childText(c, "mandateType"),
                            partyName,
                            partyCode,
                            parseIntObj(childText(c, "registrationNumber")),
                            elected));
                }
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RK_2023 RESULTS.xml", e);
        }
    }

    /** First descendant element with the given tag; party/candidate tag sets don't overlap. */
    private static String childText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String t = nl.item(0).getTextContent();
        return t == null ? null : t.trim();
    }

    private static int parseInt(String s) {
        try { return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static Integer parseIntObj(String s) {
        try { return s == null || s.isBlank() ? null : Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
