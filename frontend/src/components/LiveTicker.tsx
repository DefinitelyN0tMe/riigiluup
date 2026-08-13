import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../api/politicians";
import { fetchVotes } from "../api/votes";
import type { VoteListItem } from "../types";

type TickerItem = { id: string; title: string; result: string; up: boolean };

function toTickerItem(v: VoteListItem): TickerItem {
  const id = v.votingNumber != null ? `#${v.votingNumber}` : `#${v.externalId}`;
  // for / against / abstained — use the real abstentions (resultNeutral); resultAbstained is an
  // overlapping source total (did-not-vote + absent), see lib/voteTally.
  const result = `${v.resultInFavor} / ${v.resultAgainst} / ${v.resultNeutral}`;
  return {
    id,
    title: v.billTitle ?? v.description ?? "—",
    result,
    up: v.resultInFavor >= v.resultAgainst,
  };
}

function Item({ it }: { it: TickerItem }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <b className="text-blue-glow font-bold">{it.id}</b>
      <span className="text-white/85">{it.title}</span>
      <b className={it.up ? "text-live" : "text-hot"}>{it.up ? "↑" : "↓"}</b>
      <span className="text-white/70">{it.result}</span>
    </span>
  );
}

export default function LiveTicker() {
  const { t, i18n } = useTranslation();
  const { data: status } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const { data: votes } = useQuery({ queryKey: ["votes-ticker"], queryFn: () => fetchVotes({ size: 8 }) });

  const first = status?.[0];
  const syncTime = first?.lastRunAt
    ? new Date(first.lastRunAt).toLocaleTimeString(i18n.resolvedLanguage, { hour: "2-digit", minute: "2-digit" })
    : "—";
  const records = first?.lastRunRecords ?? "—";

  const realItems = (votes?.items ?? []).map(toTickerItem);
  // Doubled for a seamless marquee loop. Empty while loading / no data — no fabricated fallback.
  const items = realItems.length ? [...realItems, ...realItems] : [];

  return (
    <div className="bg-ink text-white h-[38px] grid grid-cols-[1fr] md:grid-cols-[240px_1fr_280px] items-center font-mono text-[11px] tracking-[0.06em] border-b border-white/[0.08] overflow-hidden">
      {/* left */}
      <div className="hidden md:flex items-center gap-2.5 px-5 h-full border-r border-white/[0.08]">
        <span className="w-2 h-2 rounded-full bg-live shadow-[0_0_12px_theme(colors.live)] animate-pulse-dot" />
        <span>{t("chrome.ticker.systemLive")}</span>
      </div>
      {/* marquee (always visible) */}
      <div className="relative overflow-hidden h-full mask-fade-x">
        {items.length > 0 ? (
          <div className="ticker-row animate-scroll-x">
            {items.map((it, i) => <Item key={i} it={it} />)}
          </div>
        ) : (
          <div className="ticker-row"><span className="text-white/40">—</span></div>
        )}
      </div>
      {/* right */}
      <div className="hidden md:flex justify-end gap-5 px-5 h-full items-center text-white/70 border-l border-white/[0.08]">
        <span>{t("chrome.ticker.sync")} <b className="text-white font-bold">{syncTime}</b></span>
        <span>{t("chrome.ticker.records")} <b className="text-white font-bold">{records}</b></span>
      </div>
    </div>
  );
}
