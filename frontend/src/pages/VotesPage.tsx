import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { fetchVotes } from "../api/votes";
import VoteRow from "../components/VoteRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";
import LoadFailed from "../components/LoadFailed";

const TYPE_CODES = ["", "OPEN", "ATTENDANCE_CHECK", "SECRET", "OTHER"] as const;
const DOW_LABELS_KEY = ["viz.timing.d1","viz.timing.d2","viz.timing.d3","viz.timing.d4","viz.timing.d5","viz.timing.d6","viz.timing.d7"];

export default function VotesPage() {
  const { t } = useTranslation();
  const [sp, setSp] = useSearchParams();

  const type = sp.get("type") ?? "";
  const hour = sp.get("hour") ? Number(sp.get("hour")) : undefined;
  const dow = sp.get("dow") ? Number(sp.get("dow")) : undefined;
  const onlyWeekend = sp.get("onlyWeekend") === "true";
  const nightOnly = sp.get("nightOnly") === "true";
  const lateOnly = sp.get("lateOnly") === "true";
  const factionA = sp.get("factionA") ?? undefined;
  const factionB = sp.get("factionB") ?? undefined;
  const factionAName = sp.get("factionAName") ?? undefined;
  const factionBName = sp.get("factionBName") ?? undefined;
  const page = sp.get("page") ? Number(sp.get("page")) : 0;

  function updateParams(next: Record<string, string | number | undefined | null>) {
    const nextSp = new URLSearchParams(sp);
    for (const [k, v] of Object.entries(next)) {
      if (v === undefined || v === null || v === "" ) nextSp.delete(k);
      else nextSp.set(k, String(v));
    }
    setSp(nextSp, { replace: false });
  }
  const removeFilter = (keys: string[]) => {
    const nextSp = new URLSearchParams(sp);
    keys.forEach((k) => nextSp.delete(k));
    nextSp.delete("page");
    setSp(nextSp);
  };

  const typeLabel = (code: string) =>
    code === "" ? t("voteType.allTypes") : t(`voteType.${code}` as const, { defaultValue: code });

  const { data, isLoading, error } = useQuery({
    queryKey: ["votes", type, hour, dow, onlyWeekend, nightOnly, lateOnly, factionA, factionB, page],
    queryFn: () => fetchVotes({
      type: type || undefined,
      hour,
      dow,
      onlyWeekend: onlyWeekend || undefined,
      nightOnly: nightOnly || undefined,
      lateOnly: lateOnly || undefined,
      factionA,
      factionB,
      page,
      size: 50,
    }),
    placeholderData: (prev) => prev,
  });

  const chips = useMemo(() => {
    const out: { key: string; label: string; removeKeys: string[] }[] = [];
    if (hour !== undefined) out.push({
      key: "hour",
      label: `${t("votes.chip.hour")}: ${String(hour).padStart(2, "0")}:00`,
      removeKeys: ["hour"],
    });
    if (dow !== undefined) out.push({
      key: "dow",
      label: `${t("votes.chip.dow")}: ${t(DOW_LABELS_KEY[dow] ?? "viz.timing.d1")}`,
      removeKeys: ["dow"],
    });
    if (onlyWeekend) out.push({
      key: "weekend",
      label: t("votes.chip.weekend"),
      removeKeys: ["onlyWeekend"],
    });
    if (nightOnly) out.push({
      key: "night",
      label: t("votes.chip.night", "Night (before 08:00 or after 22:00)"),
      removeKeys: ["nightOnly"],
    });
    if (lateOnly) out.push({
      key: "late",
      label: t("votes.chip.late"),
      removeKeys: ["lateOnly"],
    });
    if (factionA && factionB) out.push({
      key: "pair",
      label: `${t("votes.chip.disagreement")}: ${factionAName ?? factionA.slice(0, 6)} ↔ ${factionBName ?? factionB.slice(0, 6)}`,
      removeKeys: ["factionA", "factionB", "factionAName", "factionBName"],
    });
    return out;
  }, [hour, dow, onlyWeekend, nightOnly, lateOnly, factionA, factionB, factionAName, factionBName, t]);

  return (
    <div className="px-5 sm:px-8 md:px-10 py-10 sm:py-14 md:py-16 max-w-[1440px] mx-auto w-full">
      <div className="mb-8 md:mb-12">
        <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold flex items-center gap-2.5 mb-4">
          <span className="bg-blue text-white px-2 py-0.5 rounded font-bold tracking-[0.14em]">I.</span>
          {t("pages.votes.kicker")}
        </div>
        <div className="flex items-end justify-between flex-wrap gap-4">
          <h1 className="font-display font-bold h-display-lg">
            {t("votes.title")}
          </h1>
          <DataFreshnessBadge job="votes.window-refresh" />
        </div>
      </div>

      <div className="flex flex-wrap gap-3 items-center mb-4 sm:mb-5">
        <label htmlFor="votes-type-filter" className="sr-only">{t("a11y.filterByType")}</label>
        <select
          id="votes-type-filter"
          value={type}
          onChange={(e) => updateParams({ type: e.target.value || undefined, page: undefined })}
          className="bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue min-w-[220px]"
        >
          {TYPE_CODES.map((c) => <option key={c} value={c}>{typeLabel(c)}</option>)}
        </select>
      </div>

      {chips.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4 sm:mb-5">
          <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted self-center">
            {t("votes.chip.filters")}:
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

      {isLoading && <p className="text-muted font-mono text-sm tracking-[0.06em]" role="status">{t("common.loading")}</p>}
      {error && <LoadFailed error={error} className="text-hot-deep font-mono text-sm" />}

      {data && (
        <>
          <p className="font-mono text-[11px] tracking-[0.1em] uppercase text-muted mb-4 sm:mb-5" aria-live="polite" aria-atomic="true">
            {t("votes.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          {data.items.length === 0 && (
            <p className="font-serif italic text-[14px] text-ink-2 py-4">{t("common.noResults")}</p>
          )}
          <ul className="grid grid-cols-1 gap-3 sm:gap-4 list-none p-0">
            {data.items.map((v) => (
              <li key={v.id}>
                <VoteRow v={v} />
              </li>
            ))}
          </ul>
          {data.totalPages > 1 && (
            <nav className="flex gap-3 items-center pt-8 justify-center" aria-label={t("a11y.pagination")}>
              <button
                onClick={() => updateParams({ page: Math.max(0, page - 1) })}
                disabled={page === 0}
                aria-label={t("a11y.prevPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >{t("common.prev")}</button>
              <span className="font-mono text-[11px] tracking-[0.1em] text-muted" aria-live="polite">
                {t("common.pageOf", { page: data.page + 1, total: data.totalPages })}
              </span>
              <button
                onClick={() => updateParams({ page: page + 1 < data.totalPages ? page + 1 : page })}
                disabled={page + 1 >= data.totalPages}
                aria-label={t("a11y.nextPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >{t("common.next")}</button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
