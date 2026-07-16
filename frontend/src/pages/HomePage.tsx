import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { fetchPoliticians, fetchDataStatus, fetchFactions } from "../api/politicians";
import { fetchVotes } from "../api/votes";
import { fetchLegislation } from "../api/legislation";
import { resolveMediaUrl } from "../api/client";
import { formatDateTime } from "../lib/formatDate";
import Sparkline from "../components/Sparkline";
import PartyDonut from "../components/PartyDonut";
import SectionHead from "../components/SectionHead";
import MarqueeStrip from "../components/MarqueeStrip";
import type { Politician, VoteListItem } from "../types";

const PARTY_COLOR: Record<string, string> = {
  "Eesti Reformierakonna fraktsioon": "#0072CE",
  "Eesti Keskerakonna fraktsioon": "#003E7E",
  "Eesti Konservatiivse Rahvaerakonna fraktsioon": "#0A0A0A",
  "Isamaa fraktsioon": "#FFB020",
  "Sotsiaaldemokraatliku Erakonna fraktsioon": "#FF4B3E",
  "Eesti 200 fraktsioon": "#1EA98A",
};
function partyColor(name?: string | null) {
  if (!name) return "#0072CE";
  return PARTY_COLOR[name] ?? "#0072CE";
}
function initials(p?: { firstName?: string; lastName?: string }) {
  return `${p?.firstName?.[0] ?? ""}${p?.lastName?.[0] ?? ""}`.toUpperCase();
}

/* ============ HERO ============ */
function Hero({ syncedAt }: { syncedAt: string | null }) {
  const { t } = useTranslation();
  return (
    <section className="relative bg-blue text-white overflow-hidden isolate px-5 sm:px-8 md:px-10 pt-10 sm:pt-14 pb-16 sm:pb-20 md:pb-24">
      <div className="grain" />
      <div aria-hidden className="hidden sm:block absolute right-[-40px] sm:right-[-60px] top-4 font-serif italic font-light leading-[0.8] tracking-[-0.05em] text-white/[0.05] pointer-events-none select-none z-0"
           style={{ fontSize: "clamp(280px, 40vw, 620px)" }}>
        104
      </div>
      <div aria-hidden className="hidden md:block absolute right-[-160px] bottom-[-260px] w-[720px] h-[720px] border border-white/16 rounded-full z-[1] pointer-events-none">
        <div className="absolute inset-[60px] border border-dashed border-white/12 rounded-full" />
      </div>

      <img src="/logo.png" alt="" aria-hidden
           className="hidden md:block absolute left-[-70px] bottom-[-90px] w-[340px] h-[340px] opacity-[0.07] pointer-events-none select-none z-0" />

      <div className="relative z-[3] pt-6 sm:pt-10 md:pt-16 grid grid-cols-1 md:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)] gap-10 md:gap-16 items-end">
        <div>
          <div className="inline-flex items-center gap-3.5 font-mono text-[10px] xs:text-[11px] sm:text-[12px] tracking-[0.18em] uppercase mb-6 sm:mb-8">
            <img src="/logo.png" alt="Riigiluup" className="w-8 h-8 rounded-full ring-1 ring-white/30 bg-white/95 object-contain p-0.5" />
            <span className="w-8 sm:w-10 h-px bg-white" />
            <span>{t("homePage.hero.eyebrow")}</span>
            <span className="bg-black/28 px-2.5 py-0.5 rounded-full inline-flex items-center gap-1.5 tracking-[0.12em]">
              <span className="w-1.5 h-1.5 rounded-full bg-live shadow-[0_0_8px_theme(colors.live)] animate-pulse-dot" /> {t("homePage.hero.live")}
            </span>
          </div>
          <h1 className="font-display font-bold h-display-mega">
            <span className="block">{t("homePage.hero.line1")}</span>
            <span className="block">{t("homePage.hero.line2")}</span>
            <span className="block font-serif italic font-light text-stroke-white">{t("homePage.hero.line3")}</span>
          </h1>
        </div>

        <div>
          <p className="text-base sm:text-[17px] leading-[1.5] max-w-[42ch] opacity-95 mb-6 sm:mb-8">
            {t("homePage.hero.lede")}
          </p>
          <div className="flex flex-wrap gap-3 items-center mb-7">
            <Link to="/politicians"
              className="inline-flex items-center gap-2.5 bg-white text-blue px-5 py-3 sm:px-6 sm:py-3.5 font-bold text-[15px] rounded-full tracking-[-0.01em]">
              {t("homePage.hero.ctaBrowse")}
              <span className="w-6 h-6 rounded-full bg-blue text-white grid place-items-center font-mono rotate-[-45deg]">↑</span>
            </Link>
            <Link to="/methodology"
              className="inline-flex items-center px-5 py-3 sm:px-6 sm:py-3.5 text-white border-[1.5px] border-white/40 rounded-full font-semibold text-[15px]">
              {t("homePage.hero.ctaMethodology")}
            </Link>
          </div>
        </div>
      </div>

      <div className="relative z-[3] pt-8 sm:pt-12 flex flex-col xs:flex-row justify-between items-start xs:items-end gap-3 text-white/70 font-mono text-[10px] sm:text-[11px] tracking-[0.06em]">
        <div className="flex gap-3"><span>001 / 007</span><span>·</span><b className="text-white font-bold">{t("homePage.hero.footIntroduction")}</b></div>
        <div className="text-right">
          {syncedAt && <>{t("chrome.ticker.sync")} <b className="text-white font-bold">{syncedAt}</b> · </>}{t("homePage.hero.compositionLine")}
        </div>
      </div>
    </section>
  );
}

/* ============ STATS ============ */
function StatTile({
  to, label, hint, value, delta, deltaTone = "live", spark, sparkColor = "#6BB4F0", sparkFilled = false,
}: {
  to: string; label: string; hint: string; value: React.ReactNode;
  delta: string; deltaTone?: "live" | "hot" | "muted";
  spark?: number[]; sparkColor?: string; sparkFilled?: boolean;
}) {
  const deltaCls = deltaTone === "hot" ? "text-hot" : deltaTone === "muted" ? "opacity-70" : "text-live";
  return (
    <Link to={to} className="flex flex-col gap-3 group focus:outline-none focus:ring-2 focus:ring-blue-glow rounded p-1 -m-1 hover:bg-white/[0.04] transition-colors">
      <div className="font-mono text-[10px] tracking-[0.2em] uppercase opacity-55 flex items-center gap-2 group-hover:opacity-100 transition-opacity">
        <span className="bg-white/8 text-blue-glow px-2 py-0.5 rounded tracking-[0.14em]">{label}</span>
        <span className="hidden xs:inline">{hint}</span>
      </div>
      <div className="font-display font-extrabold text-[54px] sm:text-[68px] md:text-[84px] leading-[0.95] tracking-[-0.05em]">
        {value}
      </div>
      {spark && <div className="h-8 opacity-85"><Sparkline points={spark} color={sparkColor} filled={sparkFilled} height={32} /></div>}
      <div className={`font-mono text-[10px] sm:text-[11px] tracking-[0.06em] ${deltaCls}`}>{delta}</div>
    </Link>
  );
}
function StatsStrip({ mpTotal, voteTotal, billTotal }: { mpTotal?: number; voteTotal?: number; billTotal?: number }) {
  const { t } = useTranslation();
  const spark1 = [24,20,26,18,22,14,20,10,15,8,14,6,12,4,10,6,3,7];
  const spark2 = [12,15,10,18,14,20,16,22,18,14,20,12,16,10,14,12];
  const spark3 = [20,14,22,10,18,12,20,6,18,4,20,10,14,6,10,4,8];
  return (
    <section className="bg-ink text-white px-5 sm:px-8 md:px-10 py-8 sm:py-10 md:py-11 border-b border-white/[0.06] relative overflow-hidden">
      <div aria-hidden className="absolute inset-0 pointer-events-none bg-[linear-gradient(to_right,rgba(255,255,255,0.05)_1px,transparent_1px)] bg-[length:25%_100%] hidden sm:block" />
      <div className="relative grid grid-cols-2 md:grid-cols-4 gap-6 sm:gap-8">
        <StatTile to="/politicians" label={t("homePage.stats.mpsLabel")} hint={t("homePage.stats.mpsHint")}
                  value={mpTotal ?? "—"} delta={t("homePage.stats.delta1")} />
        <StatTile to="/votes" label={t("homePage.stats.votesLabel")} hint={t("homePage.stats.votesHint")}
                  value={voteTotal ?? "—"}
                  spark={spark1} sparkFilled delta={t("homePage.stats.delta2")} />
        <StatTile to="/legislation" label={t("homePage.stats.billsLabel")} hint={t("homePage.stats.billsHint")}
                  value={billTotal ?? "—"} spark={spark2} delta={t("homePage.stats.delta3")} deltaTone="hot" />
        <StatTile to="/data-status" label={t("homePage.stats.syncLabel")} hint={t("homePage.stats.syncHint")}
                  value={<><span className="text-blue-glow font-bold not-italic">6</span>
                          <span className="font-serif italic font-light text-blue-glow text-[36px] sm:text-[44px] ml-1">h</span></>}
                  spark={spark3} delta={t("homePage.stats.delta4")} deltaTone="muted" />
      </div>
    </section>
  );
}

/* ============ VOTE CARD ============ */
function VoteRowCard({ v, live = false }: { v: VoteListItem; live?: boolean }) {
  const { t } = useTranslation();
  const total = v.resultInFavor + v.resultAgainst + v.resultAbstained + v.resultPresent + v.resultAbsent;
  const pct = (n: number) => (total ? (n / total) * 100 : 0);
  const when = v.startedAt ? formatDateTime(v.startedAt, { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }) : "";
  const typeLabel = live
    ? "Live"
    : v.type === "OPEN" ? t("voteType.OPEN")
    : v.type === "ATTENDANCE_CHECK" ? t("voteType.ATTENDANCE_SHORT")
    : v.type === "SECRET" ? t("voteType.SECRET") : t("voteType.OTHER");
  return (
    <Link to={`/votes/${encodeURIComponent(v.id)}`}
      className={`grid grid-cols-1 md:grid-cols-[90px_1fr_320px_130px] items-center bg-white border rounded-[22px] p-5 sm:p-6 md:p-7 gap-4 md:gap-7 hover:-translate-y-0.5 hover:border-blue transition-all ${live ? "border-hot shadow-[0_0_0_3px_rgba(255,75,62,0.08)]" : "border-rule"}`}>
      <div className="flex md:flex-col items-center md:items-start gap-3 md:gap-2 md:border-r md:border-rule md:pr-5">
        <div className="font-mono font-bold text-[14px] sm:text-[15px] text-blue tracking-[0.02em]">#{v.votingNumber ?? "—"}</div>
        <span className={`font-mono text-[9px] tracking-[0.16em] uppercase px-1.5 py-0.5 rounded font-bold text-white ${live ? "bg-hot animate-pulse-dot" : v.type === "ATTENDANCE_CHECK" ? "bg-blue" : "bg-ink"}`}>
          {typeLabel}
        </span>
      </div>
      <div className="min-w-0">
        <h3 className="font-display font-bold text-[18px] sm:text-[20px] md:text-[22px] leading-[1.18] tracking-[-0.02em] mb-1.5 truncate md:whitespace-normal md:overflow-visible">
          {v.description ?? "—"}
        </h3>
        <div className="font-mono text-[10px] sm:text-[11px] text-muted tracking-[0.04em] flex flex-wrap gap-2 sm:gap-3">
          <span>{when}</span>
          {v.sittingTitle && <span>· {v.sittingTitle}</span>}
        </div>
      </div>
      <div className="flex flex-col gap-2">
        <div className="relative flex h-3.5 rounded-lg overflow-hidden bg-[#F1F0EA]">
          {v.resultInFavor > 0 && <div className="bg-blue" style={{ width: `${pct(v.resultInFavor)}%` }} />}
          {v.resultAgainst > 0 && <div className="bg-hot" style={{ width: `${pct(v.resultAgainst)}%` }} />}
          {v.resultAbstained > 0 && <div className="bg-ink" style={{ width: `${pct(v.resultAbstained)}%` }} />}
          {v.resultPresent > 0 && <div className="bg-blue-deep" style={{ width: `${pct(v.resultPresent)}%` }} />}
          {v.resultAbsent > 0 && <div className="bg-[#D8D6CB]" style={{ width: `${pct(v.resultAbsent)}%` }} />}
        </div>
        <div className="flex flex-wrap gap-2 sm:gap-3 justify-between font-mono text-[10px] tracking-[0.06em] text-muted">
          {v.resultInFavor > 0 && <span><b className="text-ink font-bold mr-1">{v.resultInFavor}</b>{t("viz.highlights.for")}</span>}
          {v.resultAgainst > 0 && <span><b className="text-ink font-bold mr-1">{v.resultAgainst}</b>{t("viz.highlights.against")}</span>}
          {v.resultAbstained > 0 && <span><b className="text-ink font-bold mr-1">{v.resultAbstained}</b>{t("choice.ABSTAINED").toLowerCase()}</span>}
          {v.resultPresent > 0 && <span><b className="text-ink font-bold mr-1">{v.resultPresent}</b>{t("choice.PRESENT").toLowerCase()}</span>}
          {v.resultAbsent > 0 && <span><b className="text-ink font-bold mr-1">{v.resultAbsent}</b>{t("choice.ABSENT").toLowerCase()}</span>}
        </div>
      </div>
      <div className="md:justify-self-end">
        <span className="inline-flex items-center gap-2 bg-white border-[1.5px] border-ink rounded-full px-3.5 py-2.5 font-mono text-[11px] font-bold text-ink tracking-[0.06em]">
          {t("common.sourceLink").replace(" ↗", "")} <span className="w-4 h-4 rounded-full bg-ink text-white grid place-items-center rotate-[-45deg]">↑</span>
        </span>
      </div>
    </Link>
  );
}

/* ============ MP CARDS ============ */
function MpFeatureCard({ p }: { p: Politician | null }) {
  const { t } = useTranslation();
  const factionUpper = (p?.factionName ?? "REFORMIERAKOND").toUpperCase();
  return (
    <Link to={p ? `/politicians/${encodeURIComponent(p.slug)}` : "/politicians"}
      className="md:col-span-2 md:row-span-2 relative overflow-hidden bg-blue text-white rounded-[22px] p-6 sm:p-8 flex flex-col justify-between hover:-translate-y-0.5 transition-transform">
      <div aria-hidden className="hidden md:block absolute right-[-20px] top-[30%] origin-top-right rotate-[-90deg] font-display font-extrabold text-[160px] xl:text-[200px] tracking-[-0.055em] leading-none text-white/[0.06] pointer-events-none select-none">
        {factionUpper}
      </div>
      <div className="flex justify-between items-start relative">
        <div className="inline-flex items-center gap-2 bg-white/14 px-3 py-1.5 rounded-full font-mono text-[10px] sm:text-[11px] tracking-[0.14em] uppercase font-semibold">
          <span className="w-1.5 h-1.5 rounded-full bg-live shadow-[0_0_8px_theme(colors.live)] animate-pulse-dot" /> {t("homePage.bento.featureTag")}
        </div>
        <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-full bg-white text-blue grid place-items-center font-serif font-semibold text-[26px] sm:text-[28px] tracking-[-0.02em]">
          {initials(p ?? undefined) || "KK"}
        </div>
      </div>
      <div className="relative mt-6">
        <h3 className="font-display font-bold text-[42px] sm:text-[54px] md:text-[68px] leading-[0.9] tracking-[-0.045em]">
          {p?.firstName ?? "Kaja"}<br />
          <span className="font-serif italic font-light text-stroke-white">{p?.lastName ?? "Kallas"}.</span>
        </h3>
        <div className="text-white/85 mt-2 text-sm sm:text-base">{p?.factionName ?? "Reformierakond"}</div>
        <div className="mt-8 pt-5 border-t border-white/18 font-mono text-[10px] tracking-[0.18em] uppercase opacity-70">
          {t("homePage.bento.featureCta")}
        </div>
      </div>
    </Link>
  );
}

function MpMiniCard({ p, invert = false }: { p: Politician; invert?: boolean }) {
  const { t } = useTranslation();
  const photoSrc = resolveMediaUrl(p.photoUrl);
  const color = partyColor(p.factionName);
  const partyShort = (p.factionName?.replace(/fraktsioon/i, "").trim() ?? "—").slice(0, 22);
  return (
    <Link to={`/politicians/${encodeURIComponent(p.slug)}`}
      className={`rounded-[22px] p-5 sm:p-6 border relative overflow-hidden hover:-translate-y-0.5 transition-all ${
        invert ? "bg-ink text-white border-transparent" : "bg-white text-ink border-rule hover:border-blue"
      }`}>
      <div className="flex justify-between items-start mb-7 sm:mb-8">
        <div className={`font-mono text-[10px] tracking-[0.14em] uppercase ${invert ? "text-blue-glow" : "text-muted"}`}>
          {partyShort}
        </div>
        {photoSrc ? (
          <img src={photoSrc} alt="" loading="lazy" className="w-11 h-11 rounded-full object-cover shrink-0" />
        ) : (
          <div className="w-11 h-11 rounded-full grid place-items-center font-bold text-[15px] shrink-0 text-white"
               style={{ backgroundColor: color }}>
            {initials(p)}
          </div>
        )}
      </div>
      <h3 className={`font-display font-bold text-[22px] sm:text-[24px] leading-[1.05] tracking-[-0.025em] ${invert ? "text-white" : "text-ink"}`}>
        {p.fullName}
      </h3>
      <div className={`text-[12px] mt-1 ${invert ? "text-white/70" : "text-muted"}`}>{p.factionName ?? t("common.noFaction")}</div>
    </Link>
  );
}

/* ============ SPLIT ============ */
function SplitLegit() {
  const { t } = useTranslation();
  const steps = [
    { n: "01.", h: t("homePage.split.step1H"), p: t("homePage.split.step1P"), link: t("homePage.split.step1L") },
    { n: "02.", h: t("homePage.split.step2H"), p: t("homePage.split.step2P"), link: t("homePage.split.step2L") },
    { n: "03.", h: t("homePage.split.step3H"), p: t("homePage.split.step3P"), link: t("homePage.split.step3L") },
    { n: "04.", h: t("homePage.split.step4H"), p: t("homePage.split.step4P"), link: t("homePage.split.step4L") },
  ];
  return (
    <section className="bg-paper border-b border-rule px-5 sm:px-8 md:px-10 py-14 sm:py-20 md:py-24 grid grid-cols-1 md:grid-cols-[1fr_1.4fr] gap-10 md:gap-16">
      <div>
        <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold flex items-center gap-2 mb-5">
          <span className="bg-blue text-white px-2 py-0.5 rounded font-bold tracking-[0.14em]">III.</span>
          {t("homePage.split.kicker")}
        </div>
        <h2 className="font-display font-bold h-display-lg mb-5">
          {t("homePage.split.titlePre")}<br />
          <span className="font-serif italic font-light text-blue">{t("homePage.split.titleEm")}</span>
        </h2>
        <p className="font-serif font-light text-[18px] sm:text-[20px] md:text-[22px] leading-[1.4] text-ink-2 max-w-[40ch] mb-7">
          {t("homePage.split.body")}
        </p>
        <blockquote className="font-serif italic text-[20px] sm:text-[22px] md:text-[26px] leading-[1.35] text-blue-deep py-5 pl-6 border-l-4 border-blue max-w-[40ch]">
          {t("homePage.split.quote")}
        </blockquote>
        <div className="font-mono text-[11px] tracking-[0.14em] uppercase text-muted mt-4 pl-6">{t("homePage.split.quoteAttr")}</div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-5">
        {steps.map((s) => (
          <div key={s.n} className="bg-white border border-rule rounded-[20px] p-6">
            <div className="font-serif italic font-light text-[38px] sm:text-[46px] leading-none tracking-[-0.03em] text-blue mb-3.5">{s.n}</div>
            <h3 className="font-display font-bold text-[18px] sm:text-[20px] tracking-[-0.015em] mb-1.5">{s.h}</h3>
            <p className="text-sm leading-[1.5] text-ink-2">{s.p}</p>
            <Link to="/methodology" className="inline-block mt-3 font-mono text-[11px] text-blue tracking-[0.08em] border-b border-blue pb-0.5">{s.link} →</Link>
          </div>
        ))}
      </div>
    </section>
  );
}

/* ============ HOMEPAGE ============ */
export default function HomePage() {
  const { t } = useTranslation();
  const status = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const politicians = useQuery({
    queryKey: ["politicians-home"],
    queryFn: () => fetchPoliticians({ activeOnly: true, page: 0, size: 6 }),
  });
  const votes = useQuery({
    queryKey: ["votes-home"],
    queryFn: () => fetchVotes({ page: 0, size: 4 }),
  });
  const legislation = useQuery({
    queryKey: ["legislation-home"],
    queryFn: () => fetchLegislation({ page: 0, size: 1 }),
  });
  const factions = useQuery({ queryKey: ["factions"], queryFn: fetchFactions });

  const first = status.data?.[0];
  const syncedAt = first?.lastRunAt
    ? formatDateTime(first.lastRunAt, { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" })
    : null;

  const mps = politicians.data?.items ?? [];
  const feat = mps[0] ?? null;
  const rest = mps.slice(1, 6);

  const voteItems = votes.data?.items ?? [];

  const partyMeta = useMemo(() => {
    const list = factions.data ?? [];
    const coalitionNames = ["Reformierakond", "Eesti 200", "Sotsiaaldemokraatliku"];
    return list.slice(0, 6).map((f) => {
      const isCoalition = coalitionNames.some((n) => f.name.includes(n));
      return {
        seats: f.memberCount,
        color: partyColor(f.name),
        label: f.name.replace(/fraktsioon/i, "").trim(),
        side: (isCoalition ? "coalition" : "opposition") as "coalition" | "opposition",
        factionExternalId: f.externalId,
      };
    });
  }, [factions.data]);

  return (
    <>
      <Hero syncedAt={syncedAt} />
      <StatsStrip
        mpTotal={politicians.data?.totalElements}
        voteTotal={votes.data?.totalElements}
        billTotal={legislation.data?.totalElements}
      />
      <MarqueeStrip />

      <section className="bg-paper border-b border-rule px-5 sm:px-8 md:px-10 py-14 sm:py-20 md:py-24">
        <SectionHead
          index="I."
          kicker={t("homePage.nowVoting.kicker")}
          title={<>{t("homePage.nowVoting.titlePre")}<span className="font-serif italic font-light text-blue">{t("homePage.nowVoting.titleEm")}</span>{t("homePage.nowVoting.titlePost")}</>}
          more={{ to: "/votes", label: t("homePage.nowVoting.more") }}
        />
        {votes.isLoading && <p className="text-muted">{t("homePage.nowVoting.loading")}</p>}
        {!votes.isLoading && voteItems.length === 0 && <p className="text-muted">{t("homePage.nowVoting.empty")}</p>}
        <div className="flex flex-col gap-3 sm:gap-4">
          {voteItems.map((v, i) => (
            <VoteRowCard key={v.id} v={v} live={i === 0 && v.type === "OPEN"} />
          ))}
        </div>
      </section>

      <section className="bg-off border-b border-rule px-5 sm:px-8 md:px-10 py-14 sm:py-20 md:py-24">
        <SectionHead
          index="II."
          kicker={t("homePage.bento.kicker")}
          title={<>{t("homePage.bento.titlePre")}<span className="font-serif italic font-light text-blue">{t("homePage.bento.titleEm")}</span>{t("homePage.bento.titlePost")}</>}
          more={{ to: "/politicians", label: t("homePage.bento.more") }}
        />
        {politicians.isLoading && <p className="text-muted">{t("homePage.bento.loading")}</p>}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5 auto-rows-fr">
          <MpFeatureCard p={feat} />
          {rest.map((p, i) => (
            <MpMiniCard key={p.id} p={p} invert={i === 2} />
          ))}
        </div>
      </section>

      <SplitLegit />

      <section className="bg-paper px-5 sm:px-8 md:px-10 py-14 sm:py-20 md:py-24">
        <SectionHead
          index="IV."
          kicker={t("homePage.parties.kicker")}
          title={<>{t("homePage.parties.titlePre")}<span className="font-serif italic font-light text-blue">{t("homePage.parties.titleEm")}</span>{t("homePage.parties.titlePost")}</>}
          more={{ to: "/compare", label: t("homePage.parties.more") }}
        />
        {factions.isLoading && <p className="text-muted">{t("homePage.parties.loading")}</p>}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 sm:gap-5">
          {partyMeta.map((p) => (
            <PartyDonut key={p.label} {...p} />
          ))}
        </div>
      </section>
    </>
  );
}
