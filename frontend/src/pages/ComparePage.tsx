import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { fetchFactions, fetchPoliticians } from "../api/politicians";
import { fetchComparison, fetchFactionComparison } from "../api/comparisons";
import { formatDate } from "../lib/formatDate";
import LoadFailed from "../components/LoadFailed";

function pct(rate: number | null | undefined): string {
  return rate == null ? "—" : `${(rate * 100).toFixed(1)}%`;
}

type Disagreement = {
  voteEventId: string;
  description: string | null;
  startedAt: string | null;
  leftChoice: string;
  rightChoice: string;
  billId: string | null;
  billTitle: string | null;
  billMark: string | null;
};

/** Shared result panel: agreement headline + overlap + recent disagreements. */
function AgreementResult({
  leftName, rightName, rate, same, overlap, disagreements,
}: {
  leftName: string; rightName: string;
  rate: number | null; same: number; overlap: number; disagreements: Disagreement[];
}) {
  const { t } = useTranslation();
  return (
    <div className="mt-6 border border-rule rounded-[20px] p-5 sm:p-6 bg-white">
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <div className="min-w-0">
          <div className="font-display font-bold text-[15px] leading-tight text-ink">
            {leftName} <span className="text-muted font-normal">·</span> {rightName}
          </div>
          <div className="font-mono text-[11px] text-muted mt-1">
            {t("compare.overlap", { same, total: overlap })}
          </div>
        </div>
        <div className="text-right">
          <div className="font-display font-extrabold text-[40px] leading-none tracking-[-0.03em] text-blue">{pct(rate)}</div>
          <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1">{t("compare.agreement")}</div>
        </div>
      </div>
      {overlap === 0 ? (
        <p className="mt-4 font-serif italic text-[14px] text-ink-2">{t("compare.noOverlap")}</p>
      ) : disagreements.length > 0 && (
        <div className="mt-5">
          <h3 className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mb-2">{t("compare.disagreements")}</h3>
          <ul className="divide-y divide-rule border border-rule rounded-[14px]">
            {disagreements.map((d) => (
              <li key={d.voteEventId} className="p-3 text-sm flex justify-between items-start gap-3">
                <span className="min-w-0">
                  {d.billTitle ? (
                    <Link to={`/legislation/${encodeURIComponent(d.billId ?? "")}`} className="text-ink font-medium hover:underline">
                      {d.billTitle}
                    </Link>
                  ) : (
                    <Link to={`/votes/${d.voteEventId}`} className="text-ink hover:underline">
                      {d.description ?? t("common.noDescription")}
                    </Link>
                  )}
                  <span className="block font-mono text-[11px] text-muted">
                    {d.billTitle && (
                      <Link to={`/votes/${d.voteEventId}`} className="hover:underline">
                        {d.billMark ? `${d.billMark} · ` : ""}{d.description ?? t("common.noDescription")}
                      </Link>
                    )}
                    {d.billTitle && d.startedAt ? " · " : ""}{d.startedAt ? formatDate(d.startedAt) : ""}
                  </span>
                </span>
                <span className="shrink-0 text-right text-xs">
                  <span className="text-blue font-medium">{t(`choice.${d.leftChoice}`, { defaultValue: d.leftChoice })}</span>
                  <span className="text-muted mx-1">/</span>
                  <span className="text-hot-deep font-medium">{t(`choice.${d.rightChoice}`, { defaultValue: d.rightChoice })}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

const SELECT_CLS =
  "w-full border border-rule rounded-full px-4 py-2.5 text-[14px] bg-white focus:outline-none focus:border-blue";

function PartyCompare() {
  const { t } = useTranslation();
  const factions = useQuery({ queryKey: ["factions"], queryFn: fetchFactions });
  const opts = (factions.data ?? []).filter((f) => !/mittekuuluv/i.test(f.name));
  const [left, setLeft] = useState("");
  const [right, setRight] = useState("");
  const cmp = useQuery({
    queryKey: ["faction-cmp", left, right],
    queryFn: () => fetchFactionComparison(left, right),
    enabled: !!left && !!right && left !== right,
  });
  return (
    <div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <select className={SELECT_CLS} value={left} onChange={(e) => setLeft(e.target.value)} aria-label={t("compare.pickLeft")}>
          <option value="">{t("compare.pickParty")}</option>
          {opts.map((f) => <option key={f.externalId} value={f.externalId} disabled={f.externalId === right}>{f.name.replace(/fraktsioon/i, "").trim()}</option>)}
        </select>
        <select className={SELECT_CLS} value={right} onChange={(e) => setRight(e.target.value)} aria-label={t("compare.pickRight")}>
          <option value="">{t("compare.pickParty")}</option>
          {opts.map((f) => <option key={f.externalId} value={f.externalId} disabled={f.externalId === left}>{f.name.replace(/fraktsioon/i, "").trim()}</option>)}
        </select>
      </div>
      {cmp.isLoading && <p className="mt-6 text-muted font-mono text-sm">{t("common.loading")}</p>}
      {cmp.error && <LoadFailed error={cmp.error} className="mt-6" />}
      {cmp.data && (
        <AgreementResult
          leftName={cmp.data.left.name.replace(/fraktsioon/i, "").trim()}
          rightName={cmp.data.right.name.replace(/fraktsioon/i, "").trim()}
          rate={cmp.data.agreementRate}
          same={cmp.data.sameCount}
          overlap={cmp.data.totalOverlap}
          disagreements={cmp.data.recentDisagreements}
        />
      )}
    </div>
  );
}

function PoliticianCompare() {
  const { t } = useTranslation();
  const list = useQuery({ queryKey: ["mp-all"], queryFn: () => fetchPoliticians({ status: "current", size: 200 }) });
  const mps = list.data?.items ?? [];
  const [left, setLeft] = useState("");
  const [right, setRight] = useState("");
  const cmp = useQuery({
    queryKey: ["mp-cmp", left, right],
    queryFn: () => fetchComparison(left, right),
    enabled: !!left && !!right && left !== right,
  });
  return (
    <div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <select className={SELECT_CLS} value={left} onChange={(e) => setLeft(e.target.value)} aria-label={t("compare.pickLeft")}>
          <option value="">{t("compare.pickMp")}</option>
          {mps.map((m) => <option key={m.slug} value={m.slug} disabled={m.slug === right}>{m.fullName}</option>)}
        </select>
        <select className={SELECT_CLS} value={right} onChange={(e) => setRight(e.target.value)} aria-label={t("compare.pickRight")}>
          <option value="">{t("compare.pickMp")}</option>
          {mps.map((m) => <option key={m.slug} value={m.slug} disabled={m.slug === left}>{m.fullName}</option>)}
        </select>
      </div>
      {cmp.isLoading && <p className="mt-6 text-muted font-mono text-sm">{t("common.loading")}</p>}
      {cmp.error && <LoadFailed error={cmp.error} className="mt-6" />}
      {cmp.data && (
        <AgreementResult
          leftName={cmp.data.left.fullName}
          rightName={cmp.data.right.fullName}
          rate={cmp.data.agreement.agreementRate}
          same={cmp.data.agreement.sameCount}
          overlap={cmp.data.agreement.totalOverlap}
          disagreements={cmp.data.recentDisagreements.map((d) => ({
            voteEventId: d.voteEventId,
            description: d.voteEventDescription,
            startedAt: d.startedAt,
            leftChoice: d.leftChoice,
            rightChoice: d.rightChoice,
            billId: d.billId,
            billTitle: d.billTitle,
            billMark: d.billMark,
          }))}
        />
      )}
    </div>
  );
}

export default function ComparePage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<"parties" | "politicians">("parties");
  return (
    <div className="max-w-[860px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <h1 className="font-display font-bold text-[34px] sm:text-[44px] tracking-[-0.03em] leading-none mb-2">
        {t("compare.title")}
      </h1>
      <p className="font-serif italic text-[15px] text-ink-2 mb-6 max-w-[70ch]">{t("compare.lede")}</p>

      <div className="inline-flex rounded-full border border-rule p-1 mb-6 bg-white">
        {(["parties", "politicians"] as const).map((k) => (
          <button
            key={k}
            type="button"
            onClick={() => setTab(k)}
            className={`px-4 py-1.5 rounded-full text-[13px] font-medium transition-colors ${
              tab === k ? "bg-blue text-white" : "text-ink-2 hover:text-ink"
            }`}
          >
            {t(`compare.tab.${k}`)}
          </button>
        ))}
      </div>

      {tab === "parties" ? <PartyCompare /> : <PoliticianCompare />}
    </div>
  );
}
