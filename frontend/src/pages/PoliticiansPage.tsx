import { useMemo, useRef, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { fetchFactions, fetchPoliticians, fetchProfile } from "../api/politicians";
import PoliticianCard from "../components/PoliticianCard";
import SearchInput from "../components/SearchInput";
import DataFreshnessBadge from "../components/DataFreshnessBadge";
import ComparisonPanel from "../components/ComparisonPanel";
import LoadFailed from "../components/LoadFailed";

export default function PoliticiansPage() {
  const { t } = useTranslation();
  const [sp, setSp] = useSearchParams();

  const q = sp.get("q") ?? "";
  const faction = sp.get("faction") ?? "";
  const page = sp.get("page") ? Number(sp.get("page")) : 0;
  const compareLeft = sp.get("compareLeft");
  const compareRight = sp.get("compareRight");

  function updateParams(next: Record<string, string | number | undefined | null>) {
    const nextSp = new URLSearchParams(sp);
    for (const [k, v] of Object.entries(next)) {
      if (v === undefined || v === null || v === "") nextSp.delete(k);
      else nextSp.set(k, String(v));
    }
    setSp(nextSp);
  }
  const removeFilter = (keys: string[]) => {
    const nextSp = new URLSearchParams(sp);
    keys.forEach((k) => nextSp.delete(k));
    nextSp.delete("page");
    setSp(nextSp);
  };

  // Compare-tray logic: max 2 slots, fill left first then right, third click replaces the older one.
  function toggleCompare(slug: string) {
    if (compareLeft === slug) {
      updateParams({ compareLeft: compareRight, compareRight: undefined });
      return;
    }
    if (compareRight === slug) {
      updateParams({ compareRight: undefined });
      return;
    }
    if (!compareLeft) updateParams({ compareLeft: slug });
    else if (!compareRight) updateParams({ compareRight: slug });
    else updateParams({ compareLeft: compareRight, compareRight: slug });
  }
  function clearCompare() {
    updateParams({ compareLeft: undefined, compareRight: undefined });
  }

  // Scroll into view when both slots fill so the panel doesn't stay hidden below fold.
  const comparePanelRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (compareLeft && compareRight) {
      comparePanelRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [compareLeft, compareRight]);

  const factions = useQuery({ queryKey: ["factions"], queryFn: fetchFactions });

  const { data, isLoading, error } = useQuery({
    queryKey: ["politicians", q, faction, page],
    queryFn: () =>
      fetchPoliticians({
        q: q || undefined,
        faction: faction || undefined,
        page,
        size: 52,
        activeOnly: true,
      }),
    placeholderData: (previous) => previous,
  });

  const factionName = useMemo(() => {
    if (!faction) return null;
    return factions.data?.find((f) => f.externalId === faction)?.name ?? faction;
  }, [faction, factions.data]);

  const chips = useMemo(() => {
    const out: { key: string; label: string; removeKeys: string[] }[] = [];
    if (faction) out.push({
      key: "faction",
      label: `${t("politicians.chip.faction")}: ${factionName ?? faction}`,
      removeKeys: ["faction"],
    });
    return out;
  }, [faction, factionName, t]);

  return (
    <div className="px-5 sm:px-8 md:px-10 py-10 sm:py-14 md:py-16 max-w-[1440px] mx-auto w-full">
      <div className="mb-8 md:mb-12">
        <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold flex items-center gap-2.5 mb-4">
          <span className="bg-blue text-white px-2 py-0.5 rounded font-bold tracking-[0.14em]">II.</span>
          {t("pages.politicians.kicker")}
        </div>
        <div className="flex items-end justify-between flex-wrap gap-4">
          <h1 className="font-display font-bold h-display-lg">
            {t("politicians.title")}
          </h1>
          <DataFreshnessBadge />
        </div>
      </div>

      <div className="flex flex-wrap gap-3 items-center mb-4 sm:mb-5">
        <SearchInput
          value={q}
          onChange={(v) => updateParams({ q: v || undefined, page: undefined })}
          placeholder={t("politicians.searchPlaceholder")}
          ariaLabel={t("a11y.searchMps")}
        />
        <label htmlFor="politicians-faction-filter" className="sr-only">{t("a11y.filterByFaction")}</label>
        <select
          id="politicians-faction-filter"
          value={faction}
          onChange={(e) => updateParams({ faction: e.target.value || undefined, page: undefined })}
          className="bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue min-w-[220px]"
        >
          <option value="">{t("politicians.allFactions")}</option>
          {factions.data?.map((f) => (
            <option key={f.externalId} value={f.externalId}>
              {t("politicians.factionOption", { name: f.name, count: f.memberCount })}
            </option>
          ))}
        </select>
      </div>

      {chips.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4 sm:mb-5">
          <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted self-center">
            {t("politicians.chip.filters")}:
          </span>
          {chips.map((c) => (
            <button key={c.key} type="button" onClick={() => removeFilter(c.removeKeys)}
                    className="inline-flex items-center gap-2 bg-blue text-white text-[12px] font-medium rounded-full pl-3 pr-2 py-1.5 hover:bg-blue-deep transition-colors">
              {c.label}
              <span className="w-4 h-4 rounded-full bg-white/25 grid place-items-center text-[10px] font-bold">×</span>
            </button>
          ))}
        </div>
      )}

      <CompareTray
        leftSlug={compareLeft}
        rightSlug={compareRight}
        onRemove={(slug) => toggleCompare(slug)}
        onClear={clearCompare}
      />

      {compareLeft && compareRight && (
        <section
          ref={comparePanelRef}
          aria-label={t("compare.panelAriaLabel")}
          className="mb-8 sm:mb-10 bg-white/60 border border-rule rounded-2xl p-5 sm:p-6"
        >
          <div className="flex items-center justify-between mb-4 gap-2 flex-wrap">
            <h2 className="font-display font-bold text-[22px] tracking-[-0.02em]">
              {t("compare.inlineTitle")}
            </h2>
            <button
              type="button"
              onClick={clearCompare}
              className="font-mono text-[11px] tracking-[0.14em] uppercase text-muted hover:text-hot-deep"
            >
              {t("compare.clear")}
            </button>
          </div>
          <ComparisonPanel leftSlug={compareLeft} rightSlug={compareRight} />
        </section>
      )}

      {isLoading && !data && <p className="text-muted font-mono text-sm tracking-[0.06em]" role="status">{t("common.loading")}</p>}
      {error && <LoadFailed error={error} className="text-hot-deep font-mono text-sm" />}

      {data && (
        <>
          <p className="font-mono text-[11px] tracking-[0.1em] uppercase text-muted mb-4 sm:mb-5" aria-live="polite" aria-atomic="true">
            {t("politicians.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5 list-none p-0">
            {data.items.map((p) => {
              const isActive = p.slug === compareLeft || p.slug === compareRight;
              const bothFilled = !!compareLeft && !!compareRight;
              return (
                <li key={p.id}>
                  <PoliticianCard
                    p={p}
                    compareActive={isActive}
                    compareDisabled={bothFilled && !isActive}
                    onToggleCompare={toggleCompare}
                  />
                </li>
              );
            })}
          </ul>
          {data.totalPages > 1 && (
            <nav className="flex gap-3 items-center pt-8 justify-center" aria-label={t("a11y.pagination")}>
              <button
                onClick={() => updateParams({ page: Math.max(0, page - 1) })}
                disabled={page === 0}
                aria-label={t("a11y.prevPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >
                {t("common.prev")}
              </button>
              <span className="font-mono text-[11px] tracking-[0.1em] text-muted" aria-live="polite">
                {t("common.pageOf", { page: data.page + 1, total: data.totalPages })}
              </span>
              <button
                onClick={() => updateParams({ page: page + 1 < data.totalPages ? page + 1 : page })}
                disabled={page + 1 >= data.totalPages}
                aria-label={t("a11y.nextPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >
                {t("common.next")}
              </button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

/**
 * Compare tray — always-visible chip strip that shows the 0/1/2 MPs picked
 * for comparison. Sits between the filters and the grid. Names are looked up
 * per-slug (react-query caches, so it's ~free).
 */
function CompareTray({
  leftSlug,
  rightSlug,
  onRemove,
  onClear,
}: {
  leftSlug: string | null;
  rightSlug: string | null;
  onRemove: (slug: string) => void;
  onClear: () => void;
}) {
  const { t } = useTranslation();
  const count = (leftSlug ? 1 : 0) + (rightSlug ? 1 : 0);
  return (
    <div
      className="mb-4 sm:mb-5 flex items-center gap-3 flex-wrap bg-white/60 border border-rule rounded-full px-4 py-2"
      aria-label={t("compare.trayAriaLabel")}
    >
      <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-blue font-bold">
        {t("compare.trayLabel", { count })}
      </span>
      {count === 0 && (
        <span className="text-[12px] text-muted">{t("compare.trayHint")}</span>
      )}
      {leftSlug && <CompareChip slug={leftSlug} onRemove={onRemove} />}
      {rightSlug && <CompareChip slug={rightSlug} onRemove={onRemove} />}
      {count > 0 && (
        <button
          type="button"
          onClick={onClear}
          className="ml-auto font-mono text-[10px] tracking-[0.14em] uppercase text-muted hover:text-hot-deep"
        >
          {t("compare.clear")}
        </button>
      )}
    </div>
  );
}

function CompareChip({ slug, onRemove }: { slug: string; onRemove: (slug: string) => void }) {
  const { t } = useTranslation();
  const { data } = useQuery({
    queryKey: ["profile", slug],
    queryFn: () => fetchProfile(slug),
  });
  const label = data?.fullName ?? slug;
  return (
    <span className="inline-flex items-center gap-2 bg-blue text-white text-[12px] font-medium rounded-full pl-3 pr-2 py-1">
      {label}
      <button
        type="button"
        aria-label={t("compare.removeChip", { name: label })}
        onClick={() => onRemove(slug)}
        className="w-4 h-4 rounded-full bg-white/25 grid place-items-center text-[10px] font-bold hover:bg-white/40"
      >
        ×
      </button>
    </span>
  );
}
