import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { fetchInitiatives, type InitiativeListItem } from "../api/initiatives";
import SearchInput from "../components/SearchInput";
import { formatDate } from "../lib/formatDate";

const SIGNATURE_THRESHOLD = 1000;

const PHASE_CODES = ["", "edit", "sign", "parliament", "government", "done"] as const;
const DECISION_CODES = [
  "",
  "return",
  "reject",
  "solve-differently",
  "forward",
  "forward-to-government",
  "draft-act-or-national-matter",
] as const;

const PHASE_CLASS: Record<string, string> = {
  edit: "bg-slate-100 text-slate-700",
  sign: "bg-amber-100 text-amber-800",
  parliament: "bg-blue-100 text-blue-800",
  government: "bg-violet-100 text-violet-800",
  done: "bg-emerald-100 text-emerald-800",
};

const DECISION_CLASS: Record<string, string> = {
  return: "bg-slate-100 text-slate-600",
  reject: "bg-red-100 text-red-800",
  "solve-differently": "bg-amber-100 text-amber-800",
  forward: "bg-emerald-100 text-emerald-800",
  "forward-to-government": "bg-emerald-100 text-emerald-800",
  "draft-act-or-national-matter": "bg-emerald-100 text-emerald-800",
};

const FALLBACK_BADGE_CLASS = "bg-slate-100 text-slate-600";

function badgeClass(map: Record<string, string>, key: string | null): string {
  if (!key) return FALLBACK_BADGE_CLASS;
  return map[key] ?? FALLBACK_BADGE_CLASS;
}

/** Signature count against the 1000-signature legal threshold, with a clear reached/not-reached indicator. */
function SignatureMeter({ count }: { count: number | null }) {
  const { t } = useTranslation();
  const c = count ?? 0;
  const reached = c >= SIGNATURE_THRESHOLD;
  const pct = Math.min(100, (c / SIGNATURE_THRESHOLD) * 100);
  return (
    <div
      className="flex items-center gap-2 shrink-0"
      role="img"
      aria-label={t("initiatives.signatureProgress", { count: c, threshold: SIGNATURE_THRESHOLD })}
    >
      <div className="w-20 h-1.5 rounded-full bg-rule overflow-hidden">
        <div className={`h-full ${reached ? "bg-live-deep" : "bg-blue"}`} style={{ width: `${pct}%` }} />
      </div>
      <span
        className={`font-mono text-[12px] font-bold tracking-[0.02em] whitespace-nowrap ${reached ? "text-live-deep" : "text-ink-2"}`}
      >
        {c.toLocaleString()}
        <span className="text-muted font-normal"> / {SIGNATURE_THRESHOLD.toLocaleString()}</span>
      </span>
    </div>
  );
}

function InitiativeCard({ item }: { item: InitiativeListItem }) {
  const { t } = useTranslation();
  return (
    <div className="bg-white border border-rule rounded-[20px] p-4 sm:p-5 hover:border-blue transition-colors">
      <div className="flex justify-between items-start gap-3 mb-2">
        <Link to={`/initiatives/${item.id}`} className="min-w-0 group">
          <h2 className="font-display font-bold text-[16px] sm:text-[18px] leading-[1.2] tracking-[-0.02em] group-hover:underline">
            {item.title ?? `#${item.externalId}`}
          </h2>
        </Link>
        {item.phase && (
          <span className={`shrink-0 px-2 py-0.5 rounded text-xs font-medium ${badgeClass(PHASE_CLASS, item.phase)}`}>
            {t(`initiatives.phase.${item.phase}` as const, { defaultValue: item.phase })}
          </span>
        )}
      </div>

      <p className="font-mono text-[11px] text-muted tracking-[0.04em] mb-3">
        {item.authors ?? "—"}
        {item.sentToParliamentAt && (
          <> · {t("initiatives.sentToParliamentInline", { date: formatDate(item.sentToParliamentAt) })}</>
        )}
      </p>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <SignatureMeter count={item.signatureCount} />
        {item.decision && (
          <span className={`px-2 py-0.5 rounded text-xs font-medium ${badgeClass(DECISION_CLASS, item.decision)}`}>
            {t(`initiatives.decision.${item.decision}` as const, { defaultValue: item.decision })}
          </span>
        )}
      </div>

      {item.committees.length > 0 && (
        <ul className="flex flex-wrap gap-1.5 mt-3 list-none p-0" aria-label={t("sections.committees")}>
          {item.committees.map((c) => (
            <li
              key={c.slug}
              className="font-mono text-[10px] tracking-[0.06em] uppercase border border-rule rounded-full px-2.5 py-1 text-ink-2 bg-off"
            >
              {c.name ?? c.slug}
            </li>
          ))}
        </ul>
      )}

      <div className="flex items-center justify-between mt-3 pt-3 border-t border-rule">
        <Link
          to={`/initiatives/${item.id}`}
          className="font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5"
        >
          {t("initiatives.viewDetails")} →
        </Link>
        <a
          href={item.sourceUrl}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={t("initiatives.openSourceAria", { title: item.title ?? item.externalId })}
          className="font-mono text-[11px] text-muted hover:text-blue tracking-[0.04em]"
        >
          {t("initiatives.openSource")} ↗
        </a>
      </div>
    </div>
  );
}

export default function InitiativesPage() {
  const { t } = useTranslation();
  const [sp, setSp] = useSearchParams();

  const q = sp.get("q") ?? "";
  const phase = sp.get("phase") ?? "";
  const decision = sp.get("decision") ?? "";
  const committee = sp.get("committee") ?? "";
  const committeeName = sp.get("committeeName") ?? "";
  const page = sp.get("page") ? Number(sp.get("page")) : 0;

  function updateParams(next: Record<string, string | number | undefined | null>) {
    const nextSp = new URLSearchParams(sp);
    for (const [k, v] of Object.entries(next)) {
      if (v === undefined || v === null || v === "") nextSp.delete(k);
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

  const phaseLabel = (code: string) =>
    code === "" ? t("initiatives.allPhases") : t(`initiatives.phase.${code}` as const, { defaultValue: code });
  const decisionLabel = (code: string) =>
    code === "" ? t("initiatives.allDecisions") : t(`initiatives.decision.${code}` as const, { defaultValue: code });

  const { data, isLoading, error } = useQuery({
    queryKey: ["initiatives", q, phase, decision, committee, page],
    queryFn: () =>
      fetchInitiatives({
        q: q || undefined,
        phase: phase || undefined,
        decision: decision || undefined,
        committee: committee || undefined,
        page,
        size: 20,
      }),
    placeholderData: (prev) => prev,
  });

  const chips = useMemo(() => {
    const out: { key: string; label: string; removeKeys: string[] }[] = [];
    if (phase) out.push({ key: "phase", label: `${t("initiatives.chip.phase")}: ${phaseLabel(phase)}`, removeKeys: ["phase"] });
    if (decision)
      out.push({ key: "decision", label: `${t("initiatives.chip.decision")}: ${decisionLabel(decision)}`, removeKeys: ["decision"] });
    if (committee)
      out.push({
        key: "committee",
        label: `${t("initiatives.chip.committee")}: ${committeeName || committee}`,
        removeKeys: ["committee", "committeeName"],
      });
    return out;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, decision, committee, committeeName, t]);

  return (
    <div className="max-w-[1040px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <h1 className="font-display font-bold text-[34px] sm:text-[44px] tracking-[-0.03em] leading-none mb-2">
        {t("initiatives.title")}
      </h1>
      <p className="font-serif italic text-[15px] text-ink-2 mb-6 max-w-[70ch]">{t("initiatives.lede")}</p>

      <div className="flex flex-wrap gap-3 items-center mb-4">
        <SearchInput
          value={q}
          onChange={(v) => {
            const nextSp = new URLSearchParams(sp);
            if (v) nextSp.set("q", v);
            else nextSp.delete("q");
            nextSp.delete("page");
            setSp(nextSp, { replace: true });
          }}
          placeholder={t("initiatives.searchPlaceholder")}
          ariaLabel={t("initiatives.searchPlaceholder")}
        />
        <label htmlFor="initiatives-phase-filter" className="sr-only">
          {t("a11y.filterByPhase")}
        </label>
        <select
          id="initiatives-phase-filter"
          value={phase}
          onChange={(e) => updateParams({ phase: e.target.value || undefined, page: undefined })}
          className="bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue min-w-[180px]"
        >
          {PHASE_CODES.map((code) => (
            <option key={code} value={code}>
              {phaseLabel(code)}
            </option>
          ))}
        </select>
        <label htmlFor="initiatives-decision-filter" className="sr-only">
          {t("initiatives.filterByDecision")}
        </label>
        <select
          id="initiatives-decision-filter"
          value={decision}
          onChange={(e) => updateParams({ decision: e.target.value || undefined, page: undefined })}
          className="bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue min-w-[200px]"
        >
          {DECISION_CODES.map((code) => (
            <option key={code} value={code}>
              {decisionLabel(code)}
            </option>
          ))}
        </select>
      </div>

      {chips.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4">
          <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted self-center">
            {t("initiatives.chip.filters")}:
          </span>
          {chips.map((c) => (
            <button
              key={c.key}
              type="button"
              onClick={() => removeFilter(c.removeKeys)}
              className="inline-flex items-center gap-2 bg-blue text-white text-[12px] font-medium rounded-full pl-3 pr-2 py-1.5 hover:bg-blue-deep transition-colors"
            >
              {c.label}
              <span className="w-4 h-4 rounded-full bg-white/25 grid place-items-center text-[10px] font-bold">×</span>
            </button>
          ))}
        </div>
      )}

      {data && (
        <p className="font-mono text-[11px] tracking-[0.1em] uppercase text-muted mb-4" aria-live="polite" aria-atomic="true">
          {t("initiatives.results", { count: data.totalElements })}
        </p>
      )}

      {isLoading && !data && (
        <p className="mt-2 text-muted font-mono text-sm" role="status">
          {t("common.loading")}
        </p>
      )}
      {error && (
        <p className="mt-2 text-hot-deep font-mono text-sm" role="alert">
          {t("common.failedToLoad")} {(error as Error).message}
        </p>
      )}

      {data && data.items.length === 0 && <p className="mt-8 text-muted font-serif italic">{t("initiatives.empty")}</p>}

      {data && data.items.length > 0 && (
        <ul className="mt-2 flex flex-col gap-3 sm:gap-4 list-none p-0">
          {data.items.map((i) => (
            <li key={i.id}>
              <InitiativeCard item={i} />
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
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
    </div>
  );
}
