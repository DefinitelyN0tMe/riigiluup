import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { MpSimilarPeers, PeerAgreement } from "../../api/analytics";
import { formatPercent } from "../../lib/formatNumber";

export default function SimilarPeers({ data, currentSlug }: { data: MpSimilarPeers; currentSlug: string }) {
  const { t } = useTranslation();
  if (data.mostSimilar.length === 0 && data.mostOpposite.length === 0) {
    return <div className="text-muted font-mono text-xs tracking-[0.14em] uppercase py-4">{t("viz.similarPeers.empty")}</div>;
  }
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <PeerList
        title={t("viz.similarPeers.mostSimilar")}
        subtitle={t("viz.similarPeers.mostSimilarNote")}
        items={data.mostSimilar}
        currentSlug={currentSlug}
        tone="blue"
      />
      <PeerList
        title={t("viz.similarPeers.mostOpposite")}
        subtitle={t("viz.similarPeers.mostOppositeNote")}
        items={data.mostOpposite}
        currentSlug={currentSlug}
        tone="hot"
      />
    </div>
  );
}

function PeerList({
  title, subtitle, items, currentSlug, tone,
}: {
  title: string; subtitle: string; items: PeerAgreement[]; currentSlug: string;
  tone: "blue" | "hot";
}) {
  const { t } = useTranslation();
  const barColor = tone === "hot" ? "bg-hot" : "bg-blue";
  const rankColor = tone === "hot" ? "text-hot-deep" : "text-blue";
  return (
    <div>
      <div className="font-mono text-[10px] tracking-[0.2em] uppercase text-muted mb-1">{title}</div>
      <p className="font-serif italic text-[13px] text-ink-2 mb-3 leading-snug">{subtitle}</p>
      <ol className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden bg-white">
        {items.map((p, i) => (
          <li key={p.slug}>
            <Link to={`/compare?left=${encodeURIComponent(currentSlug)}&right=${encodeURIComponent(p.slug)}`}
              className="grid grid-cols-[28px_1fr_auto] items-center gap-3 px-3 py-2.5 hover:bg-off transition-colors">
              <div className={`font-serif italic font-light text-[20px] leading-none ${rankColor}`}>
                {String(i + 1).padStart(2, "0")}
              </div>
              <div className="min-w-0">
                <div className="font-display font-bold text-[14px] tracking-[-0.015em] truncate">{p.name}</div>
                <div className="flex items-center gap-1.5 font-mono text-[10px] tracking-[0.12em] uppercase text-muted mt-0.5">
                  <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: p.factionColorHex ?? "#0072CE" }} />
                  {p.factionShortName ?? "—"} · {p.overlap} {t("viz.similarPeers.overlap")}
                </div>
              </div>
              <div className="flex flex-col items-end gap-1 min-w-[80px]">
                <div className={`font-display font-bold text-[18px] leading-none tracking-[-0.03em] ${rankColor}`}>
                  {formatPercent(p.agreementRate, 0)}
                </div>
                <div className="w-20 h-1 rounded-full bg-off overflow-hidden">
                  <div className={`h-full ${barColor}`} style={{ width: `${p.agreementRate * 100}%` }} />
                </div>
              </div>
            </Link>
          </li>
        ))}
      </ol>
    </div>
  );
}
