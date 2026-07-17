import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  AdminUnauthorizedError,
  linkInitiativeToBill,
  unlinkInitiativeFromBill,
} from "../../api/admin";
import { fetchInitiative, fetchInitiatives, type InitiativeDetail } from "../../api/initiatives";
import { fetchLegislation } from "../../api/legislation";
import type { LegislationListItem } from "../../types";
import { formatDate, formatDateTime } from "../../lib/formatDate";

const DECISION = "draft-act-or-national-matter";

/** Bill search box for one row — reuses the public /api/v1/legislation search, same shape as the MP picker. */
function BillPicker({
  onPick,
  disabled,
}: {
  onPick: (bill: LegislationListItem) => void;
  disabled: boolean;
}) {
  const { t } = useTranslation();
  const [query, setQuery] = useState("");
  const [suggest, setSuggest] = useState<LegislationListItem[]>([]);

  useEffect(() => {
    if (query.trim().length < 2) {
      setSuggest([]);
      return;
    }
    let cancelled = false;
    const timer = setTimeout(async () => {
      try {
        const page = await fetchLegislation({ q: query, size: 8 });
        if (!cancelled) setSuggest(page.items);
      } catch {
        /* ignore */
      }
    }, 200);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query]);

  return (
    <div className="relative">
      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        disabled={disabled}
        placeholder={t("admin.initiativeLinks.searchPlaceholder")}
        className="w-full bg-white border border-rule rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue disabled:opacity-50"
      />
      {suggest.length > 0 && (
        <ul className="absolute z-10 mt-1 w-full bg-white border border-rule rounded-md shadow-lg max-h-60 overflow-y-auto">
          {suggest.map((bill) => (
            <li key={bill.id}>
              <button
                type="button"
                className="w-full text-left px-3 py-2 text-sm hover:bg-off"
                onClick={() => {
                  onPick(bill);
                  setQuery(bill.title);
                  setSuggest([]);
                }}
              >
                <span className="font-medium">{bill.title}</span>
                <span className="text-muted text-xs ml-2">
                  {bill.initiatedDate ? formatDate(bill.initiatedDate) : "—"} · <code>{bill.id}</code>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function AdminInitiativeLinksSection({
  onUnauthorized,
}: {
  onUnauthorized: () => void;
}) {
  const { t } = useTranslation();
  const [details, setDetails] = useState<InitiativeDetail[]>([]);
  const [selectedBill, setSelectedBill] = useState<Record<number, LegislationListItem | null>>({});
  const [busyId, setBusyId] = useState<number | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(async () => {
    setErr(null);
    try {
      const page = await fetchInitiatives({ decision: DECISION, size: 20 });
      // The list endpoint doesn't carry linkedBill — fetch each candidate's detail so the
      // section can show current link state (at most a handful of rows by construction).
      const full = await Promise.all(page.items.map((it) => fetchInitiative(String(it.id))));
      setDetails(full);
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    }
  }, [onUnauthorized]);

  useEffect(() => {
    load();
  }, [load]);

  async function link(id: number) {
    const bill = selectedBill[id];
    if (!bill) return;
    setBusyId(id);
    setErr(null);
    try {
      await linkInitiativeToBill(id, bill.id);
      setSelectedBill((s) => ({ ...s, [id]: null }));
      await load();
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    } finally {
      setBusyId(null);
    }
  }

  async function unlink(id: number) {
    if (!confirm(t("admin.initiativeLinks.confirmUnlink"))) return;
    setBusyId(id);
    setErr(null);
    try {
      await unlinkInitiativeFromBill(id);
      await load();
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section aria-label="Initiative to bill links" className="border border-rule rounded-[20px] bg-off p-5 sm:p-6">
      <div className="flex items-baseline justify-between flex-wrap gap-2 mb-5">
        <div>
          <h2 className="font-display font-bold text-[22px] tracking-[-0.02em]">{t("admin.initiativeLinks.title")}</h2>
          <p className="text-sm text-muted mt-1">{t("admin.initiativeLinks.subtitle")}</p>
        </div>
        <a href="/initiatives" className="font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
          {t("admin.initiativeLinks.openPublic")} ↗
        </a>
      </div>

      {err && <p className="mb-3 text-hot-deep text-sm" role="alert">{err}</p>}

      {details.length === 0 ? (
        <p className="text-muted text-sm py-6 text-center">{t("admin.initiativeLinks.empty")}</p>
      ) : (
        <ul className="divide-y divide-rule border border-rule rounded-lg bg-white">
          {details.map((it) => (
            <li key={it.id} className="p-3 sm:p-4">
              <div className="min-w-0 mb-2">
                <div className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                  #{it.externalId} · {t("initiatives.signaturesShort", { count: it.signatureCount ?? 0 })}
                </div>
                <a
                  href={it.sourceUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="font-display font-bold text-[16px] tracking-[-0.015em] mt-0.5 hover:underline block"
                >
                  {it.title ?? `#${it.externalId}`}
                </a>
                <div className="text-[11px] text-muted mt-1">
                  {it.sentToParliamentAt && (
                    <>
                      {t("initiatives.timeline.sentToParliament")}: {formatDate(it.sentToParliamentAt)}
                    </>
                  )}
                  {it.finishedInParliamentAt && (
                    <span className="ml-2">
                      · {t("initiatives.timeline.finished")}: {formatDate(it.finishedInParliamentAt)}
                    </span>
                  )}
                </div>
              </div>

              {it.linkedBill ? (
                <div className="flex items-center justify-between gap-3 flex-wrap bg-off border border-rule rounded-md p-3">
                  <div className="min-w-0">
                    <div className="font-mono text-[10px] tracking-[0.1em] uppercase text-muted">
                      {t("initiatives.timeline.linkedBill")}
                    </div>
                    <Link to={`/legislation/${it.linkedBill.id}`} className="font-semibold text-[14px] hover:underline">
                      {it.linkedBill.title}
                    </Link>
                    <div className="text-[11px] text-muted mt-0.5">
                      {t("initiatives.timeline.linkedByAt", {
                        who: it.linkedBill.linkedBy ?? "—",
                        date: it.linkedBill.linkedAt ? formatDateTime(it.linkedBill.linkedAt) : "—",
                      })}
                    </div>
                  </div>
                  <button
                    onClick={() => unlink(it.id)}
                    disabled={busyId === it.id}
                    className="text-hot-deep text-sm font-semibold hover:underline shrink-0 disabled:opacity-50"
                  >
                    {busyId === it.id ? "…" : t("admin.initiativeLinks.unlink")}
                  </button>
                </div>
              ) : (
                <div className="flex items-start gap-3 flex-wrap">
                  <div className="flex-1 min-w-[220px]">
                    <BillPicker
                      disabled={busyId === it.id}
                      onPick={(bill) => setSelectedBill((s) => ({ ...s, [it.id]: bill }))}
                    />
                  </div>
                  <button
                    onClick={() => link(it.id)}
                    disabled={busyId === it.id || !selectedBill[it.id]}
                    className="bg-ink text-white font-bold text-sm rounded-full px-5 py-2.5 disabled:opacity-40 hover:bg-blue transition-colors shrink-0"
                  >
                    {busyId === it.id ? "…" : t("admin.initiativeLinks.link")}
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
