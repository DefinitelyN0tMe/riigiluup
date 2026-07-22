import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchInitiative } from "../api/initiatives";
import { ApiError } from "../api/client";
import { useLoadErrorMessage } from "../lib/useLoadErrorMessage";
import { formatDate } from "../lib/formatDate";

const NO_DATA_DEFAULT = "No data from the source";

/** Signature count against the legal threshold — same visual language as the list page's meter. */
function SignatureMeter({ count, threshold }: { count: number; threshold: number }) {
  const { t } = useTranslation();
  const reached = count >= threshold;
  const pct = Math.min(100, (count / threshold) * 100);
  return (
    <div className="flex flex-wrap items-center gap-2.5">
      <div
        className="flex items-center gap-2 shrink-0"
        role="img"
        aria-label={t("initiatives.signatureProgress", { count, threshold })}
      >
        <div className="w-24 h-1.5 rounded-full bg-rule overflow-hidden">
          <div className={`h-full ${reached ? "bg-live-deep" : "bg-blue"}`} style={{ width: `${pct}%` }} />
        </div>
        <span
          className={`font-mono text-[12px] font-bold tracking-[0.02em] whitespace-nowrap ${reached ? "text-live-deep" : "text-ink-2"}`}
        >
          {count.toLocaleString()}
          <span className="text-muted font-normal"> / {threshold.toLocaleString()}</span>
        </span>
      </div>
      <span className={`font-mono text-[10px] uppercase tracking-[0.1em] ${reached ? "text-live-deep" : "text-muted"}`}>
        {reached
          ? t("initiatives.timeline.thresholdReached", { defaultValue: "Threshold reached" })
          : t("initiatives.timeline.thresholdNotReached", { defaultValue: "Threshold not reached" })}
      </span>
    </div>
  );
}

/** One row of the vertical timeline. `done` drives the marker colour and whether the body reads as known fact vs. a gap. */
function TimelineItem({ label, done, children }: { label: string; done: boolean; children: ReactNode }) {
  return (
    <li className="relative">
      <span
        aria-hidden="true"
        className={`absolute -left-[29px] top-1 w-3 h-3 rounded-full border-2 border-paper ${done ? "bg-blue" : "bg-rule"}`}
      />
      <div className={`font-mono text-[10px] tracking-[0.14em] uppercase mb-1 ${done ? "text-blue font-bold" : "text-muted"}`}>
        {label}
      </div>
      <div className={`text-[14px] leading-snug ${done ? "text-ink" : "text-muted italic"}`}>{children}</div>
    </li>
  );
}

export default function InitiativeDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["initiative", id],
    queryFn: () => fetchInitiative(id!),
    enabled: !!id,
  });
  const loadErrorMessage = useLoadErrorMessage(error);

  const containerCls = "max-w-[820px] mx-auto px-5 sm:px-8 py-10 sm:py-14";

  if (isLoading) {
    return (
      <div className={containerCls}>
        <p className="text-muted font-mono text-sm tracking-[0.06em]" role="status">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (error) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <div className={`${containerCls} text-center`}>
        <p className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold mb-3">
          {notFound ? "404" : t("common.failedToLoad")}
        </p>
        <h1 role={notFound ? "status" : "alert"} className="font-display font-bold text-2xl mb-6">
          {notFound ? t("common.notFound") : loadErrorMessage}
        </h1>
        <Link
          to="/initiatives"
          className="inline-flex items-center px-5 py-3 bg-ink text-white rounded-full font-semibold text-[14px]"
        >
          {t("initiatives.backAll", { defaultValue: "Back to citizen initiatives" })}
        </Link>
      </div>
    );
  }

  if (!data) {
    return (
      <div className={`${containerCls} text-center`}>
        <p role="status" className="text-muted font-mono text-sm">
          {t("common.notFound")}
        </p>
      </div>
    );
  }

  const hasThresholdData = data.signatureCount != null && data.threshold != null;

  return (
    <div className={containerCls}>
      <div className="mb-6">
        <Link to="/initiatives" className="font-mono text-[11px] tracking-[0.08em] uppercase text-blue hover:underline">
          ← {t("initiatives.backAll", { defaultValue: "Back to citizen initiatives" })}
        </Link>
      </div>

      <header className="mb-10">
        {data.phase && (
          <p className="font-mono text-[11px] tracking-[0.14em] uppercase text-muted mb-2">
            {t(`initiatives.phase.${data.phase}` as const, { defaultValue: data.phase })}
          </p>
        )}
        <h1 className="font-display font-bold text-[28px] sm:text-[38px] leading-[1.05] tracking-[-0.02em] mb-3">
          {data.title ?? `#${data.externalId}`}
        </h1>
        {/* Authors are private citizens — plain text, never a link, never aggregated. */}
        <p className="font-mono text-[12px] text-muted tracking-[0.02em]">{data.authors ?? "—"}</p>
      </header>

      <section aria-label={t("initiatives.timelineTitle", { defaultValue: "Timeline" })} className="mb-10">
        <h2 className="font-display font-bold text-[19px] sm:text-[21px] mb-5">
          {t("initiatives.timelineTitle", { defaultValue: "Timeline" })}
        </h2>
        <ol className="relative border-l-2 border-rule pl-6 space-y-6">
          <TimelineItem
            label={t("initiatives.timeline.published", { defaultValue: "Published" })}
            done={!!data.publishedAt}
          >
            {data.publishedAt ? formatDate(data.publishedAt) : t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })}
          </TimelineItem>

          <TimelineItem
            label={t("initiatives.timeline.signingStarted", { defaultValue: "Signing started" })}
            done={!!data.signingStartedAt}
          >
            {data.signingStartedAt ? (
              <>
                {formatDate(data.signingStartedAt)}
                {data.signingEndsAt && <span className="text-muted"> – {formatDate(data.signingEndsAt)}</span>}
                {data.lastSignedAt && (
                  <div className="text-muted text-[12px] mt-0.5">
                    {t("initiatives.timeline.lastSigned", { defaultValue: "Last signature recorded" })}: {formatDate(data.lastSignedAt)}
                  </div>
                )}
              </>
            ) : (
              t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })
            )}
          </TimelineItem>

          {hasThresholdData ? (
            <TimelineItem label={t("initiatives.timeline.threshold", { defaultValue: "Signature threshold" })} done>
              <SignatureMeter count={data.signatureCount as number} threshold={data.threshold as number} />
            </TimelineItem>
          ) : (
            <TimelineItem label={t("initiatives.timeline.threshold", { defaultValue: "Signature threshold" })} done={false}>
              {t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })}
            </TimelineItem>
          )}

          <TimelineItem
            label={t("initiatives.timeline.sentToParliament", { defaultValue: "Sent to parliament" })}
            done={!!data.sentToParliamentAt}
          >
            {data.sentToParliamentAt
              ? formatDate(data.sentToParliamentAt)
              : t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })}
          </TimelineItem>

          <TimelineItem
            label={t("initiatives.timeline.committees", { defaultValue: "Committees" })}
            done={data.committees.length > 0}
          >
            {data.committees.length > 0 ? (
              <ul className="flex flex-wrap gap-1.5 list-none p-0" aria-label={t("sections.committees")}>
                {data.committees.map((c) => (
                  <li
                    key={c.slug}
                    className="font-mono text-[10px] tracking-[0.06em] uppercase border border-rule rounded-full px-2.5 py-1 bg-off text-ink-2"
                  >
                    {c.groupId ? (
                      <Link to={`/politicians?committee=${c.groupId}`} className="hover:underline hover:text-blue">
                        {c.name ?? c.slug}
                      </Link>
                    ) : (
                      c.name ?? c.slug
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })
            )}
          </TimelineItem>

          <TimelineItem
            label={t("initiatives.timeline.finished", { defaultValue: "Finished in parliament" })}
            done={!!data.finishedInParliamentAt || !!data.decision}
          >
            {data.finishedInParliamentAt || data.decision ? (
              <>
                {data.finishedInParliamentAt
                  ? formatDate(data.finishedInParliamentAt)
                  : t("initiatives.timeline.noFinishDate", { defaultValue: "No finish date from the source" })}
                {data.decision && (
                  <span className="ml-2 inline-block px-2 py-0.5 rounded text-[11px] font-medium bg-off text-ink-2 border border-rule">
                    {t(`initiatives.decision.${data.decision}` as const, { defaultValue: data.decision })}
                  </span>
                )}
                {data.sentToGovernmentAt && (
                  <div className="text-muted text-[12px] mt-1">
                    {t("initiatives.timeline.sentToGovernment", { defaultValue: "Sent to the Government" })}: {formatDate(data.sentToGovernmentAt)}
                  </div>
                )}
                {data.finishedInGovernmentAt && (
                  <div className="text-muted text-[12px] mt-0.5">
                    {t("initiatives.timeline.finishedInGovernment", { defaultValue: "Finished at the Government" })}: {formatDate(data.finishedInGovernmentAt)}
                  </div>
                )}
              </>
            ) : (
              t("initiatives.timeline.noData", { defaultValue: NO_DATA_DEFAULT })
            )}
          </TimelineItem>
        </ol>
      </section>

      {data.linkedBill && (
        <section className="mb-10 bg-white border border-rule rounded-[20px] p-5 sm:p-6">
          <h2 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-2">
            {t("initiatives.timeline.linkedBill", { defaultValue: "Linked bill" })}
          </h2>
          <Link
            to={`/legislation/${data.linkedBill.id}`}
            className="font-display font-bold text-[17px] sm:text-[19px] tracking-[-0.01em] hover:underline"
          >
            {data.linkedBill.title}
          </Link>
          <p className="mt-2 font-mono text-[11px] text-muted tracking-[0.02em]">
            {t("initiatives.timeline.linkedByAt", {
              defaultValue: "Linked by {{who}} on {{date}}",
              who: data.linkedBill.linkedBy ?? "—",
              date: data.linkedBill.linkedAt ? formatDate(data.linkedBill.linkedAt) : "—",
            })}
          </p>
        </section>
      )}

      <div className="pt-6 border-t border-rule">
        <a
          href={data.sourceUrl}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={t("initiatives.openSourceAria", { title: data.title ?? data.externalId })}
          className="font-mono text-[12px] text-blue hover:underline tracking-[0.04em]"
        >
          {t("initiatives.openSource", { defaultValue: "Open source" })} ↗
        </a>
      </div>
    </div>
  );
}
