import { useEffect, useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import {
  AdminUnauthorizedError,
  createAffiliation, deleteAffiliation, listAffiliations, updateAffiliation,
  type AdminAffiliation, type AdminAffiliationUpsert,
} from "../../api/admin";
import { fetchPoliticians } from "../../api/politicians";
import type { Politician } from "../../types";

const KINDS = ["PARTY", "MOVEMENT", "FRACTION_ORIGINAL", "INDEPENDENT"] as const;

const TODAY = new Date().toISOString().slice(0, 10);

const EMPTY: AdminAffiliationUpsert = {
  memberSlug: "",
  organization: "",
  orgKind: "PARTY",
  role: "",
  validFrom: TODAY,
  validTo: null,
  sourceUrl: "",
  sourceLabel: "",
  verifiedBy: "",
  verifiedAt: TODAY,
  note: "",
};

export default function AdminAffiliationsSection({
  onUnauthorized,
}: { onUnauthorized: () => void }) {
  const { t } = useTranslation();
  const [items, setItems] = useState<AdminAffiliation[]>([]);
  const [filterSlug, setFilterSlug] = useState("");
  const [form, setForm] = useState<AdminAffiliationUpsert>(EMPTY);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [mpSuggest, setMpSuggest] = useState<Politician[]>([]);
  const [mpQuery, setMpQuery] = useState("");

  const load = useCallback(async () => {
    setErr(null);
    try {
      const rows = await listAffiliations(filterSlug || undefined);
      setItems(rows);
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    }
  }, [filterSlug, onUnauthorized]);

  useEffect(() => { load(); }, [load]);

  // MP picker autocomplete (public API, works without admin auth)
  useEffect(() => {
    if (mpQuery.length < 2) { setMpSuggest([]); return; }
    let cancelled = false;
    const t = setTimeout(async () => {
      try {
        const page = await fetchPoliticians({ q: mpQuery, activeOnly: true, page: 0, size: 8 });
        if (!cancelled) setMpSuggest(page.items);
      } catch { /* ignore */ }
    }, 200);
    return () => { cancelled = true; clearTimeout(t); };
  }, [mpQuery]);

  function startEdit(row: AdminAffiliation) {
    setEditingId(row.id);
    setForm({
      memberSlug: row.memberSlug,
      organization: row.organization,
      orgKind: row.orgKind,
      role: row.role ?? "",
      validFrom: row.validFrom,
      validTo: row.validTo,
      sourceUrl: row.sourceUrl,
      sourceLabel: row.sourceLabel,
      verifiedBy: row.verifiedBy,
      verifiedAt: row.verifiedAt,
      note: row.note ?? "",
    });
    setMpQuery(row.memberSlug);
    setMpSuggest([]);
  }
  function cancelEdit() {
    setEditingId(null);
    setForm(EMPTY);
    setMpQuery("");
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      if (editingId) await updateAffiliation(editingId, form);
      else await createAffiliation(form);
      cancelEdit();
      await load();
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    } finally { setBusy(false); }
  }

  async function remove(id: string) {
    if (!confirm(t("admin.affiliations.confirmDelete"))) return;
    setBusy(true);
    setErr(null);
    try {
      await deleteAffiliation(id);
      await load();
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    } finally { setBusy(false); }
  }

  const inputCls = "w-full bg-white border border-rule rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue";

  return (
    <section aria-label="External affiliations" className="border border-rule rounded-[20px] bg-off p-5 sm:p-6">
      <div className="flex items-baseline justify-between flex-wrap gap-2 mb-5">
        <div>
          <h2 className="font-display font-bold text-[22px] tracking-[-0.02em]">{t("admin.affiliations.title")}</h2>
          <p className="text-sm text-muted mt-1">{t("admin.affiliations.subtitle")}</p>
        </div>
        <a href="/politicians" className="font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
          {t("admin.affiliations.openPublic")} ↗
        </a>
      </div>

      {/* FORM */}
      <form onSubmit={submit} className="bg-white border border-rule rounded-[16px] p-4 sm:p-5 mb-6">
        <h3 className="font-display font-bold text-[15px] tracking-[-0.015em] mb-3">
          {editingId ? t("admin.affiliations.editingTitle") : t("admin.affiliations.newTitle")}
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {/* MP picker */}
          <div className="md:col-span-2 relative">
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.mp")}</label>
            <input value={mpQuery}
                   onChange={(e) => { setMpQuery(e.target.value); setForm((f) => ({ ...f, memberSlug: e.target.value.toLowerCase().replace(/\s+/g, "-") })); }}
                   placeholder={t("admin.affiliations.f.mpPlaceholder")}
                   className={inputCls} required />
            {mpSuggest.length > 0 && (
              <ul className="absolute z-10 mt-1 w-full bg-white border border-rule rounded-md shadow-lg max-h-60 overflow-y-auto">
                {mpSuggest.map((mp) => (
                  <li key={mp.id}>
                    <button type="button"
                            className="w-full text-left px-3 py-2 text-sm hover:bg-off"
                            onClick={() => { setForm((f) => ({ ...f, memberSlug: mp.slug })); setMpQuery(mp.fullName); setMpSuggest([]); }}>
                      <span className="font-medium">{mp.fullName}</span>
                      <span className="text-muted text-xs ml-2">{mp.factionName ?? "—"} · <code>{mp.slug}</code></span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-1 font-mono text-[10px] text-muted">slug: <b className="text-ink">{form.memberSlug || "—"}</b></div>
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.organization")}</label>
            <input value={form.organization} onChange={(e) => setForm((f) => ({ ...f, organization: e.target.value }))}
                   className={inputCls} required placeholder="Koos Erakond" />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.orgKind")}</label>
            <select value={form.orgKind} onChange={(e) => setForm((f) => ({ ...f, orgKind: e.target.value }))}
                    className={inputCls} required>
              {KINDS.map((k) => <option key={k} value={k}>{t(`affiliation.kind.${k}`, { defaultValue: k })}</option>)}
            </select>
          </div>
          <div className="md:col-span-2">
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.role")}</label>
            <input value={form.role ?? ""} onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
                   className={inputCls} placeholder={t("admin.affiliations.f.rolePlaceholder")} />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.validFrom")}</label>
            <input type="date" value={form.validFrom} onChange={(e) => setForm((f) => ({ ...f, validFrom: e.target.value }))}
                   className={inputCls} required />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">
              {t("admin.affiliations.f.validTo")} <span className="normal-case tracking-normal font-sans text-[10px]">({t("admin.affiliations.f.validToHint")})</span>
            </label>
            <input type="date" value={form.validTo ?? ""} onChange={(e) => setForm((f) => ({ ...f, validTo: e.target.value || null }))}
                   className={inputCls} />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.sourceUrl")}</label>
            <input type="url" value={form.sourceUrl} onChange={(e) => setForm((f) => ({ ...f, sourceUrl: e.target.value }))}
                   className={inputCls} required placeholder="https://…" />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.sourceLabel")}</label>
            <input value={form.sourceLabel} onChange={(e) => setForm((f) => ({ ...f, sourceLabel: e.target.value }))}
                   className={inputCls} required placeholder="ERR, 2024-08-01" />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">
              {t("admin.affiliations.f.verifiedBy")} <span className="normal-case tracking-normal font-sans text-[10px]">({t("admin.affiliations.f.verifiedByHint")})</span>
            </label>
            <input value={form.verifiedBy ?? ""} onChange={(e) => setForm((f) => ({ ...f, verifiedBy: e.target.value }))}
                   className={inputCls} placeholder="auto" />
          </div>
          <div>
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.verifiedAt")}</label>
            <input type="date" value={form.verifiedAt ?? ""} onChange={(e) => setForm((f) => ({ ...f, verifiedAt: e.target.value }))}
                   className={inputCls} />
          </div>
          <div className="md:col-span-2">
            <label className="block text-[11px] font-mono tracking-[0.12em] uppercase text-muted mb-1">{t("admin.affiliations.f.note")}</label>
            <textarea value={form.note ?? ""} onChange={(e) => setForm((f) => ({ ...f, note: e.target.value }))}
                      rows={2} className={inputCls} />
          </div>
        </div>
        <div className="flex gap-3 mt-4">
          <button type="submit" disabled={busy}
                  className="bg-ink text-white font-bold text-sm rounded-full px-5 py-2.5 disabled:opacity-40 hover:bg-blue transition-colors">
            {busy ? "…" : editingId ? t("admin.affiliations.save") : t("admin.affiliations.create")}
          </button>
          {editingId && (
            <button type="button" onClick={cancelEdit}
                    className="text-ink border border-rule rounded-full px-5 py-2.5 text-sm font-semibold hover:bg-off">
              {t("admin.affiliations.cancel")}
            </button>
          )}
        </div>
        {err && <p className="mt-3 text-hot text-sm" role="alert">{err}</p>}
      </form>

      {/* FILTER */}
      <div className="flex items-center gap-3 mb-3">
        <label className="text-[11px] font-mono tracking-[0.12em] uppercase text-muted">{t("admin.affiliations.filterSlug")}</label>
        <input value={filterSlug} onChange={(e) => setFilterSlug(e.target.value)}
               placeholder="varro-vooglaid" className="bg-white border border-rule rounded-full px-3 py-1 text-sm w-64" />
      </div>

      {/* LIST */}
      {items.length === 0 ? (
        <p className="text-muted text-sm py-6 text-center">{t("admin.affiliations.empty")}</p>
      ) : (
        <ul className="divide-y divide-rule border border-rule rounded-lg bg-white">
          {items.map((it) => (
            <li key={it.id} className="p-3 sm:p-4">
              <div className="flex justify-between items-start gap-3 flex-wrap">
                <div className="min-w-0">
                  <div className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                    {it.memberSlug} · {t(`affiliation.kind.${it.orgKind}`, { defaultValue: it.orgKind })}
                  </div>
                  <div className="font-display font-bold text-[16px] tracking-[-0.015em] mt-0.5">{it.organization}</div>
                  {it.role && <div className="text-[13px] text-ink-2">{it.role}</div>}
                  <div className="text-[11px] text-muted mt-1">
                    {it.validFrom} → {it.validTo ?? t("affiliation.present")}
                    <span className="mx-2">·</span>
                    <a href={it.sourceUrl} target="_blank" rel="noreferrer" className="text-blue underline">{it.sourceLabel}</a>
                    <span className="mx-2">·</span>
                    <span>by {it.verifiedBy}</span>
                  </div>
                </div>
                <div className="flex gap-2 shrink-0">
                  <button onClick={() => startEdit(it)} className="text-blue text-sm font-semibold hover:underline">
                    {t("admin.affiliations.edit")}
                  </button>
                  <button onClick={() => remove(it.id)} className="text-hot text-sm font-semibold hover:underline">
                    {t("admin.affiliations.delete")}
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
