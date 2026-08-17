// Single source of truth for faction -> chart colour, shared by the homepage charts and MP cards
// (previously duplicated in both files, at risk of silent drift).
//
// NOTE: these are legibility-tuned display hues for small dots/bars on a WHITE surface, not the
// parties' official brand colours. Reform's brand yellow (~#F9C812), for one, is near-invisible on
// white, so it is shown as a legible blue here. The unaffiliated group is deliberately neutral grey
// (it is not a party). If you ever source colours from the API/DB colorHex instead, re-check
// on-white contrast first.
export const PARTY_COLOR: Record<string, string> = {
  "Eesti Reformierakonna fraktsioon": "#0072CE",
  "Eesti Keskerakonna fraktsioon": "#003E7E",
  "Eesti Konservatiivse Rahvaerakonna fraktsioon": "#0A0A0A",
  "Isamaa fraktsioon": "#FFB020",
  "Sotsiaaldemokraatliku Erakonna fraktsioon": "#FF4B3E",
  "Eesti 200 fraktsioon": "#1EA98A",
  "Fraktsiooni mittekuuluvad Riigikogu liikmed": "#94a3b8",
};

/** Neutral grey fallback so an unmapped/new faction never masquerades as another party's colour. */
export const PARTY_COLOR_FALLBACK = "#94a3b8";

export function partyColor(name?: string | null): string {
  if (!name) return PARTY_COLOR_FALLBACK;
  return PARTY_COLOR[name] ?? PARTY_COLOR_FALLBACK;
}
