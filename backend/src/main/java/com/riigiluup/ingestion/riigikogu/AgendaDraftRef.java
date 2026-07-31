package com.riigiluup.ingestion.riigikogu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A draft reference parsed from a plenary agenda-item title, e.g. "(644 SE)" -> mark 644,
 * type "SE". Riigikogu agenda titles name the draft's number + type code in parentheses for
 * every bill reading; procedural and debate items (Istungi rakendamine, Infotund,
 * interpellations, overviews) carry none, so they correctly yield no reference. A combined
 * reading names several drafts, hence a list. Resolving a parsed (mark, type) to a bill
 * happens later, on (mark, type, membership), so an over-parsed code that maps to no bill is
 * simply inert — this is why the parser is deliberately conservative rather than clever.
 */
public record AgendaDraftRef(int mark, String typeCode) {

    // "(644 SE)", "(45 OE)", "(644SE)" — 1-4 digits, optional space, a 2-3 letter uppercase code.
    // The parentheses requirement is what keeps false positives (years, abbreviations) out.
    private static final Pattern CODE = Pattern.compile("\\((\\d{1,4})\\s*([A-Z]{2,3})\\)");

    /** All distinct draft codes referenced by an agenda-item title (plain text, no HTML). */
    public static List<AgendaDraftRef> parse(String agendaItemTitle) {
        List<AgendaDraftRef> out = new ArrayList<>();
        if (agendaItemTitle == null || agendaItemTitle.isBlank()) return out;
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = CODE.matcher(agendaItemTitle);
        while (m.find()) {
            int mark = Integer.parseInt(m.group(1));
            String type = m.group(2);
            if (seen.add(mark + "|" + type)) out.add(new AgendaDraftRef(mark, type));
        }
        return out;
    }
}
