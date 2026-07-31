import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { fetchSpeeches } from "../api/speeches";
import SearchInput from "../components/SearchInput";
import LoadFailed from "../components/LoadFailed";
import { formatDateTime } from "../lib/formatDate";

/**
 * Search hits arrive wrapped in [[ ]] delimiters from ts_headline. Splitting keeps the
 * rendering XSS-safe (plain React elements, no innerHTML) even though the underlying
 * stenogram text is untrusted source data.
 */
function Excerpt({ text }: { text: string }) {
  const parts = text.split(/\[\[|\]\]/);
  return (
    <p className="font-serif text-[15px] leading-relaxed text-ink-2">
      {parts.map((p, i) =>
        i % 2 === 1
          ? <mark key={i} className="bg-amber/30 text-ink rounded-sm px-0.5">{p}</mark>
          : <span key={i}>{p}</span>
      )}
      …
    </p>
  );
}

export default function SpeechesPage() {
  const { t } = useTranslation();
  const [sp, setSp] = useSearchParams();

  const q = sp.get("q") ?? "";
  const member = sp.get("member") ?? "";
  const memberName = sp.get("memberName") ?? "";
  const billId = sp.get("billId") ?? "";
  const billCode = sp.get("billCode") ?? "";
  const page = sp.get("page") ? Number(sp.get("page")) : 0;

  function updateParams(next: Record<string, string | number | undefined | null>) {
    const nextSp = new URLSearchParams(sp);
    for (const [k, v] of Object.entries(next)) {
      if (v === undefined || v === null || v === "") nextSp.delete(k);
      else nextSp.set(k, String(v));
    }
    setSp(nextSp, { replace: false });
  }

  const { data, isLoading, error } = useQuery({
    queryKey: ["speeches", q, member, billId, page],
    queryFn: () => fetchSpeeches({
      q: q || undefined,
      member: member || undefined,
      billId: billId || undefined,
      page,
      size: 20,
    }),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="max-w-[1040px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <h1 className="font-display font-bold text-[34px] sm:text-[44px] tracking-[-0.03em] leading-none mb-2">
        {t("speeches.title")}
      </h1>
      <p className="font-serif italic text-[15px] text-ink-2 mb-6 max-w-[70ch]">
        {t("speeches.lede")}
      </p>

      <SearchInput
        value={q}
        onChange={(v) => {
          const nextSp = new URLSearchParams(sp);
          if (v) nextSp.set("q", v); else nextSp.delete("q");
          nextSp.delete("page");
          setSp(nextSp, { replace: true });
        }}
        placeholder={t("speeches.searchPlaceholder")}
        ariaLabel={t("speeches.searchPlaceholder")}
      />

      {(member || billId) && (
        <div className="mt-3 flex items-center gap-2 flex-wrap">
          {member && (
            <span className="inline-flex items-center gap-2 bg-ink text-white rounded-full px-3 py-1 font-mono text-[11px] tracking-[0.08em] uppercase">
              {t("speeches.filterMember")}: {memberName || member}
              <button
                type="button"
                onClick={() => updateParams({ member: undefined, memberName: undefined, page: undefined })}
                aria-label={t("speeches.clearMember")}
                className="font-bold hover:text-hot"
              >×</button>
            </span>
          )}
          {billId && (
            <span className="inline-flex items-center gap-2 bg-blue text-white rounded-full px-3 py-1 font-mono text-[11px] tracking-[0.08em] uppercase">
              {t("speeches.filterBill")}: {billCode || t("speeches.filterBillFallback")}
              <button
                type="button"
                onClick={() => updateParams({ billId: undefined, billCode: undefined, page: undefined })}
                aria-label={t("speeches.clearBill")}
                className="font-bold hover:text-hot"
              >×</button>
            </span>
          )}
        </div>
      )}

      {data && (
        <p className="mt-4 font-mono text-[11px] tracking-[0.1em] uppercase text-muted" aria-live="polite">
          {t("speeches.results", { count: data.totalElements })}
        </p>
      )}

      {isLoading && !data && <p className="mt-6 text-muted font-mono text-sm" role="status">{t("common.loading")}</p>}
      {error && <LoadFailed error={error} className="mt-6 text-hot-deep font-mono text-sm" />}

      {data && data.items.length === 0 && (
        <p className="mt-8 text-muted font-serif italic">{t("speeches.empty")}</p>
      )}

      {data && data.items.length > 0 && (
        <ul className="mt-6 flex flex-col divide-y divide-rule border border-rule rounded-[22px] bg-white overflow-hidden">
          {data.items.map((s) => (
            <li key={s.id} className="p-4 sm:p-5">
              <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1 mb-1.5">
                {s.memberSlug ? (
                  <Link to={`/politicians/${encodeURIComponent(s.memberSlug)}`}
                        className="font-display font-bold text-[16px] tracking-[-0.015em] text-blue hover:underline">
                    {s.memberName ?? s.speakerRaw}
                  </Link>
                ) : (
                  <span className="font-display font-bold text-[16px] tracking-[-0.015em]">{s.speakerRaw}</span>
                )}
                <span className="font-mono text-[11px] text-muted tracking-[0.06em]">
                  {formatDateTime(s.spokenAt)}
                </span>
              </div>
              {s.agendaItemTitle && (
                <div className="font-mono text-[10px] tracking-[0.1em] uppercase text-muted mb-2 line-clamp-1">
                  {s.agendaItemTitle}
                </div>
              )}
              <Excerpt text={s.excerpt} />
              <a href={s.sourceUrl} target="_blank" rel="noreferrer noopener"
                 className="inline-block mt-2 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
                {t("speeches.openStenogram")} ↗
              </a>
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

      <p className="mt-8 font-mono text-[10px] tracking-[0.1em] uppercase text-muted max-w-[80ch]">
        {t("speeches.searchNote")}
      </p>
    </div>
  );
}
