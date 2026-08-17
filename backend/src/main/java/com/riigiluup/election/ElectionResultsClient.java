package com.riigiluup.election;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches and parses an election RESULTS.xml (RK / EP / KOV) from the State
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

    private static final String RESULTS_URL_TEMPLATE =
            "https://opendata.valimised.ee/api/%s/RESULTS.xml";

    // Bounded timeouts like every other HTTP client here: without them a hung socket to
    // opendata.valimised.ee would park the startup-backfill daemon thread indefinitely.
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private final RestClient rest = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }

    /** Candidates of any published election (RK / EP / KOV) by its code, e.g. "EP_2024". */
    public List<ElectionCandidateDto> fetchResults(String electionCode) {
        String url = String.format(RESULTS_URL_TEMPLATE, electionCode);
        byte[] xml = rest.get().uri(url).retrieve().body(byte[].class);
        if (xml == null) {
            throw new IllegalStateException(
                    "Empty RESULTS.xml from opendata.valimised.ee for " + electionCode);
        }
        return parse(xml, electionCode);
    }

    public List<ElectionCandidateDto> fetchRk2023Results() {
        return fetchResults("RK_2023");
    }

    static List<ElectionCandidateDto> parse(byte[] xml, String electionCode) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            // Harden the parser (defence in depth; the feed carries no DOCTYPE/entities).
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

            List<ElectionCandidateDto> out = new ArrayList<>();
            // Iterate every candidate directly so party-list and independent candidates are both
            // covered; the party (if any) is resolved from the candidate's ancestor chain.
            NodeList candidates = doc.getElementsByTagName("candidate");
            for (int i = 0; i < candidates.getLength(); i++) {
                Element c = (Element) candidates.item(i);
                Element party = enclosingParty(c);
                boolean elected = "true".equalsIgnoreCase(childText(c, "elected"));
                out.add(new ElectionCandidateDto(
                        childText(c, "forename"),
                        childText(c, "surname"),
                        parseInt(childText(c, "votes")),
                        parseIntObj(childText(c, "districtNumber")),
                        childText(c, "mandateType"),
                        party == null ? null : childText(party, "name"),
                        party == null ? null : childText(party, "code"),
                        parseIntObj(childText(c, "registrationNumber")),
                        elected));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RESULTS.xml for " + electionCode, e);
        }
    }

    /** Nearest {@code <party>} ancestor of a candidate, or null (e.g. an independent candidate). */
    private static Element enclosingParty(Element candidate) {
        for (var n = candidate.getParentNode(); n instanceof Element e; n = n.getParentNode()) {
            String tag = e.getTagName();
            if ("party".equals(tag)) return e;
            if ("independentCandidate".equals(tag)) return null;
        }
        return null;
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
