import { describe, expect, it } from "vitest";
import { factionShortName, isCoalitionFaction, localizeFactionShort } from "./factionName";

describe("factionName", () => {
  it("maps the source's genitive faction names to party names", () => {
    expect(factionShortName("Eesti Reformierakonna fraktsioon")).toBe("Reformierakond");
    expect(factionShortName("Eesti Keskerakonna fraktsioon")).toBe("Keskerakond");
    expect(factionShortName("Eesti Konservatiivse Rahvaerakonna fraktsioon")).toBe("EKRE");
    expect(factionShortName("Sotsiaaldemokraatliku Erakonna fraktsioon")).toBe("SDE");
    expect(factionShortName("Isamaa fraktsioon")).toBe("Isamaa");
    expect(factionShortName("Eesti 200 fraktsioon")).toBe("Eesti 200");
  });

  it("puts Reform and Eesti 200 in the coalition (the home-page bug of September 2026)", () => {
    expect(isCoalitionFaction("Eesti Reformierakonna fraktsioon")).toBe(true);
    expect(isCoalitionFaction("Eesti 200 fraktsioon")).toBe(true);
    expect(isCoalitionFaction("Sotsiaaldemokraatliku Erakonna fraktsioon")).toBe(false);
    expect(isCoalitionFaction("Fraktsiooni mittekuuluvad Riigikogu liikmed")).toBe(false);
  });

  it("translates the api's non-attached label and keeps party names", () => {
    const t = (k: string) => (k === "common.unaffiliated" ? "Вне фракции" : k);
    expect(localizeFactionShort("Sõltumatud", t)).toBe("Вне фракции");
    expect(localizeFactionShort("EKRE", t)).toBe("EKRE");
    expect(localizeFactionShort(null, t)).toBe("—");
  });
});
