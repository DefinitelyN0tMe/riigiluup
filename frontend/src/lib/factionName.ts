/**
 * Recognisable party name (Estonian nominative) for a faction. The source names factions in the
 * genitive ("Eesti Reformierakonna fraktsioon"), so a substring test on the nominative party name
 * ("Reformierakond") silently fails; always go through this helper. Mirrors the backend's
 * AnalyticsService.shortenFactionName.
 */
export function factionShortName(name: string): string {
  const s = name.replace(/\s*fraktsioon\s*$/i, "").trim();
  if (/mittekuuluv/i.test(s)) return s;
  if (/reform/i.test(s)) return "Reformierakond";
  if (/kesk/i.test(s)) return "Keskerakond";
  if (/konservatiiv|ekre/i.test(s)) return "EKRE";
  if (/sotsiaal/i.test(s)) return "SDE";
  if (/isamaa/i.test(s)) return "Isamaa";
  if (/eesti 200|e200/i.test(s)) return "Eesti 200";
  return s;
}

/**
 * Governing coalition since March 2025: Reformierakond + Eesti 200 (SDE left the government).
 * Coalition status is not in the source data, so it is kept here; revisit on any change of
 * government (and keep riigiluup.analytics.coalition-factions in the backend in step).
 */
export const COALITION_PARTIES = ["Reformierakond", "Eesti 200"];

export function isCoalitionFaction(name: string): boolean {
  return COALITION_PARTIES.includes(factionShortName(name));
}
