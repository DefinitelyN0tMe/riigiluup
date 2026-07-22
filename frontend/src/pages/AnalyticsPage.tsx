import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  fetchAttendanceMatrix, fetchBillFlow, fetchBillVelocity, fetchCoSponsorship,
  fetchDisciplineBreakers, fetchElections, fetchFactionAgreement, fetchHighlights, fetchMemberActivity,
  fetchMpSimilarity, fetchPartyFinance, fetchResponseLatency, fetchTopicTreemap, fetchVoteTiming, fetchNightVotes,
} from "../api/analytics";
import { fetchInitiativeFunnel } from "../api/initiatives";
import { ApiError } from "../api/client";
import SectionHead from "../components/SectionHead";
import InitiativeFunnel from "../components/InitiativeFunnel";
import FactionHeatmap from "../components/analytics/FactionHeatmap";
import DisciplineBreakersView from "../components/analytics/DisciplineBreakers";
import BillFlowSankey from "../components/analytics/BillFlowSankey";
import AttendanceHeatmap from "../components/analytics/AttendanceHeatmap";
import VoteTimingHeatmap from "../components/analytics/VoteTimingHeatmap";
import TopicTreemap from "../components/analytics/TopicTreemap";
import BillVelocityChart from "../components/analytics/BillVelocityChart";
import MpScatter from "../components/analytics/MpScatter";
import CoSponsorshipGraph from "../components/analytics/CoSponsorshipGraph";
import HighlightsStrip from "../components/analytics/HighlightsStrip";
import NightVotesLog from "../components/analytics/NightVotesLog";
import ActiveMembers from "../components/analytics/ActiveMembers";
import ElectionLeaders from "../components/analytics/ElectionLeaders";
import PartyFinance from "../components/analytics/PartyFinance";
import ResponseLatency from "../components/analytics/ResponseLatency";
import type { ReactNode } from "react";

function Loading() {
  const { t } = useTranslation();
  return <div className="text-muted font-mono text-sm tracking-[0.06em] py-10">{t("viz.loading")}</div>;
}
function Failed({ err }: { err: unknown }) {
  const { t } = useTranslation();
  const message = err instanceof ApiError ? t(err.i18nKey) : `${t("viz.loadFailed")} ${(err as Error).message}`;
  return <div className="text-hot-deep font-mono text-sm py-6" role="alert">{message}</div>;
}
function Section({
  index, kicker, title, children, note,
}: { index: string; kicker: string; title: ReactNode; children: ReactNode; note?: string }) {
  return (
    <section className="px-5 sm:px-8 md:px-10 py-10 sm:py-14 md:py-16 border-b border-rule">
      <SectionHead index={index} kicker={kicker} title={title} />
      {note && <p className="font-serif italic text-[16px] sm:text-[18px] text-ink-2 max-w-[64ch] mb-8 sm:mb-10">{note}</p>}
      {children}
    </section>
  );
}

export default function AnalyticsPage() {
  const { t } = useTranslation();
  const heat = useQuery({ queryKey: ["ana:agreement"], queryFn: fetchFactionAgreement });
  const disc = useQuery({ queryKey: ["ana:discipline"], queryFn: () => fetchDisciplineBreakers(24) });
  const flow = useQuery({ queryKey: ["ana:billflow"], queryFn: fetchBillFlow });
  const att = useQuery({ queryKey: ["ana:attendance"], queryFn: () => fetchAttendanceMatrix(40) });
  const timing = useQuery({ queryKey: ["ana:timing"], queryFn: fetchVoteTiming });
  const topics = useQuery({ queryKey: ["ana:topics"], queryFn: () => fetchTopicTreemap(24) });
  const vel = useQuery({ queryKey: ["ana:velocity"], queryFn: fetchBillVelocity });
  const scat = useQuery({ queryKey: ["ana:similarity"], queryFn: fetchMpSimilarity });
  const cosp = useQuery({ queryKey: ["ana:cospons"], queryFn: () => fetchCoSponsorship(2) });
  const high = useQuery({ queryKey: ["ana:highlights"], queryFn: fetchHighlights });
  const night = useQuery({ queryKey: ["ana:night"], queryFn: () => fetchNightVotes(20) });
  const activity = useQuery({ queryKey: ["ana:activity"], queryFn: fetchMemberActivity });
  const elections = useQuery({ queryKey: ["ana:elections"], queryFn: fetchElections });
  const finance = useQuery({ queryKey: ["ana:finance"], queryFn: fetchPartyFinance });
  const latency = useQuery({ queryKey: ["ana:latency"], queryFn: fetchResponseLatency });
  const initiatives = useQuery({ queryKey: ["ana:initiatives"], queryFn: fetchInitiativeFunnel });

  const tocItems: [string, string][] = [
    ["I.", t("analytics.hero.toc1")],
    ["II.", t("analytics.hero.toc2")],
    ["III.", t("analytics.hero.toc3")],
    ["IV.", t("analytics.hero.toc4")],
    ["V.", t("analytics.hero.toc5")],
    ["VI.", t("analytics.hero.toc6")],
    ["VII.", t("analytics.hero.toc7")],
    ["VIII.", t("analytics.hero.toc8")],
    ["IX.", t("analytics.hero.toc9")],
    ["X.", t("analytics.hero.toc10")],
    ["XI.", t("analytics.hero.toc11")],
    ["XII.", t("analytics.hero.toc12")],
    ["XIII.", t("analytics.hero.toc13")],
    ["XIV.", t("analytics.hero.toc14")],
    ["XV.", t("analytics.hero.toc15")],
  ];

  return (
    <>
      <section className="bg-ink text-white relative overflow-hidden px-5 sm:px-8 md:px-10 pt-14 sm:pt-20 pb-12 sm:pb-16">
        <div className="grain" />
        <div className="relative max-w-[1400px] mx-auto">
          <div className="font-mono text-[11px] tracking-[0.22em] uppercase text-blue-glow font-bold mb-6 flex items-center gap-3">
            <span className="w-10 h-px bg-blue-glow" />
            {t("analytics.hero.kicker")}
          </div>
          <h1 className="font-display font-bold h-display-xl leading-[0.9] max-w-[16ch]">
            {t("analytics.hero.titlePre")}<span className="font-serif italic font-light text-stroke-white">{t("analytics.hero.titleEm")}</span>{t("analytics.hero.titlePost")}
          </h1>
          <p className="font-serif italic text-[18px] sm:text-[22px] text-white/85 max-w-[52ch] mt-6">
            {t("analytics.hero.lede")}
          </p>
          <div className="mt-8 grid grid-cols-2 sm:grid-cols-4 gap-4 font-mono text-[10px] tracking-[0.18em] uppercase text-white/70">
            {tocItems.map(([n, label]) => (
              <div key={n}><span className="text-blue-glow">{n}</span> {label}</div>
            ))}
          </div>
        </div>
      </section>

      <section className="px-5 sm:px-8 md:px-10 py-12 sm:py-14 bg-off border-b border-rule">
        {high.isLoading ? <Loading /> : high.error ? <Failed err={high.error} /> :
          high.data && <HighlightsStrip data={high.data} />}
      </section>

      <Section
        index="I." kicker={t("analytics.sec.agreement.kicker")}
        title={<>{t("analytics.sec.agreement.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.agreement.titleEm")}</span>{t("analytics.sec.agreement.titlePost")}</>}
        note={t("analytics.sec.agreement.note")}
      >
        {heat.isLoading ? <Loading /> : heat.error ? <Failed err={heat.error} /> :
          heat.data && <FactionHeatmap data={heat.data} />}
      </Section>

      <Section
        index="II." kicker={t("analytics.sec.discipline.kicker")}
        title={<>{t("analytics.sec.discipline.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.discipline.titleEm")}</span>{t("analytics.sec.discipline.titlePost")}</>}
        note={t("analytics.sec.discipline.note")}
      >
        {disc.isLoading ? <Loading /> : disc.error ? <Failed err={disc.error} /> :
          disc.data && <DisciplineBreakersView data={disc.data} />}
      </Section>

      <Section
        index="III." kicker={t("analytics.sec.flow.kicker")}
        title={<>{t("analytics.sec.flow.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.flow.titleEm")}</span>{t("analytics.sec.flow.titlePost")}</>}
        note={t("analytics.sec.flow.note")}
      >
        {flow.isLoading ? <Loading /> : flow.error ? <Failed err={flow.error} /> :
          flow.data && <BillFlowSankey data={flow.data} />}
      </Section>

      <Section
        index="IV." kicker={t("analytics.sec.attendance.kicker")}
        title={<>{t("analytics.sec.attendance.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.attendance.titleEm")}</span>{t("analytics.sec.attendance.titlePost")}</>}
        note={t("analytics.sec.attendance.note")}
      >
        {att.isLoading ? <Loading /> : att.error ? <Failed err={att.error} /> :
          att.data && <AttendanceHeatmap data={att.data} />}
      </Section>

      <Section
        index="V." kicker={t("analytics.sec.timing.kicker")}
        title={<>{t("analytics.sec.timing.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.timing.titleEm")}</span>{t("analytics.sec.timing.titlePost")}</>}
        note={t("analytics.sec.timing.note")}
      >
        {timing.isLoading ? <Loading /> : timing.error ? <Failed err={timing.error} /> :
          timing.data && <VoteTimingHeatmap data={timing.data} />}
      </Section>

      <Section
        index="VI." kicker={t("analytics.sec.topics.kicker")}
        title={<>{t("analytics.sec.topics.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.topics.titleEm")}</span>{t("analytics.sec.topics.titlePost")}</>}
        note={t("analytics.sec.topics.note")}
      >
        {topics.isLoading ? <Loading /> : topics.error ? <Failed err={topics.error} /> :
          topics.data && <TopicTreemap data={topics.data} />}
      </Section>

      <Section
        index="VII." kicker={t("analytics.sec.velocity.kicker")}
        title={<>{t("analytics.sec.velocity.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.velocity.titleEm")}</span>{t("analytics.sec.velocity.titlePost")}</>}
        note={t("analytics.sec.velocity.note")}
      >
        {vel.isLoading ? <Loading /> : vel.error ? <Failed err={vel.error} /> :
          vel.data && <BillVelocityChart data={vel.data} />}
      </Section>

      <Section
        index="VIII." kicker={t("analytics.sec.scatter.kicker")}
        title={<>{t("analytics.sec.scatter.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.scatter.titleEm")}</span>{t("analytics.sec.scatter.titlePost")}</>}
        note={t("analytics.sec.scatter.note")}
      >
        {scat.isLoading ? <Loading /> : scat.error ? <Failed err={scat.error} /> :
          scat.data && <MpScatter data={scat.data} />}
      </Section>

      <Section
        index="IX." kicker={t("analytics.sec.cospons.kicker")}
        title={<>{t("analytics.sec.cospons.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.cospons.titleEm")}</span>{t("analytics.sec.cospons.titlePost")}</>}
        note={t("analytics.sec.cospons.note")}
      >
        {cosp.isLoading ? <Loading /> : cosp.error ? <Failed err={cosp.error} /> :
          cosp.data && <CoSponsorshipGraph data={cosp.data} />}
      </Section>

      <Section
        index="X." kicker={t("analytics.sec.night.kicker")}
        title={<>{t("analytics.sec.night.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.night.titleEm")}</span>{t("analytics.sec.night.titlePost")}</>}
        note={t("analytics.sec.night.note")}
      >
        {night.isLoading ? <Loading /> : night.error ? <Failed err={night.error} /> :
          night.data && <NightVotesLog data={night.data} />}
      </Section>

      <Section
        index="XI." kicker={t("analytics.sec.activity.kicker")}
        title={<>{t("analytics.sec.activity.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.activity.titleEm")}</span>{t("analytics.sec.activity.titlePost")}</>}
        note={t("analytics.sec.activity.note")}
      >
        {activity.isLoading ? <Loading /> : activity.error ? <Failed err={activity.error} /> :
          activity.data && <ActiveMembers data={activity.data} />}
      </Section>

      <Section
        index="XII." kicker={t("analytics.sec.elections.kicker")}
        title={<>{t("analytics.sec.elections.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.elections.titleEm")}</span>{t("analytics.sec.elections.titlePost")}</>}
        note={t("analytics.sec.elections.note")}
      >
        {elections.isLoading ? <Loading /> : elections.error ? <Failed err={elections.error} /> :
          elections.data && <ElectionLeaders data={elections.data} />}
      </Section>

      <Section
        index="XIII." kicker={t("analytics.sec.finance.kicker")}
        title={<>{t("analytics.sec.finance.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.finance.titleEm")}</span>{t("analytics.sec.finance.titlePost")}</>}
        note={t("analytics.sec.finance.note")}
      >
        {finance.isLoading ? <Loading /> : finance.error ? <Failed err={finance.error} /> :
          finance.data && <PartyFinance data={finance.data} />}
      </Section>

      <Section
        index="XIV." kicker={t("analytics.sec.latency.kicker")}
        title={<>{t("analytics.sec.latency.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.latency.titleEm")}</span>{t("analytics.sec.latency.titlePost")}</>}
        note={t("analytics.sec.latency.note")}
      >
        {latency.isLoading ? <Loading /> : latency.error ? <Failed err={latency.error} /> :
          latency.data && <ResponseLatency data={latency.data} />}
      </Section>

      <Section
        index="XV." kicker={t("analytics.sec.initiatives.kicker")}
        title={<>{t("analytics.sec.initiatives.titlePre")}<span className="font-serif italic font-light text-blue">{t("analytics.sec.initiatives.titleEm")}</span>{t("analytics.sec.initiatives.titlePost")}</>}
        note={t("analytics.sec.initiatives.note")}
      >
        {initiatives.isLoading ? <Loading /> : initiatives.error ? <Failed err={initiatives.error} /> :
          initiatives.data && <InitiativeFunnel data={initiatives.data} />}
      </Section>
    </>
  );
}
