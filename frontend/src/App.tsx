import { Route, Routes, useLocation } from "react-router-dom";
import { Suspense, lazy, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import PoliticiansPage from "./pages/PoliticiansPage";
import VotesPage from "./pages/VotesPage";
import LegislationPage from "./pages/LegislationPage";
import ComparePage from "./pages/ComparePage";

const PoliticianProfilePage = lazy(() => import("./pages/PoliticianProfilePage"));
const VoteDetailPage = lazy(() => import("./pages/VoteDetailPage"));
const LegislationDetailPage = lazy(() => import("./pages/LegislationDetailPage"));
const AdminPage = lazy(() => import("./pages/AdminPage"));
const AnalyticsPage = lazy(() => import("./pages/AnalyticsPage"));
const SpeechesPage = lazy(() => import("./pages/SpeechesPage"));
const AboutPage = lazy(() => import("./pages/static/AboutPage"));
const MethodologyIndexPage = lazy(() => import("./pages/static/MethodologyIndexPage"));
const MethodologyParticipationPage = lazy(() => import("./pages/static/MethodologyParticipationPage"));
const MethodologyAlignmentPage = lazy(() => import("./pages/static/MethodologyAlignmentPage"));
const MethodologyAgreementPage = lazy(() => import("./pages/static/MethodologyAgreementPage"));
const MethodologyAnalyticsPage = lazy(() => import("./pages/static/MethodologyAnalyticsPage"));
const SourcesPage = lazy(() => import("./pages/static/SourcesPage"));
const PrivacyPage = lazy(() => import("./pages/static/PrivacyPage"));
const TermsPage = lazy(() => import("./pages/static/TermsPage"));
const DataStatusPage = lazy(() => import("./pages/static/DataStatusPage"));
const CorrectionsPage = lazy(() => import("./pages/static/CorrectionsPage"));
const NotFoundPage = lazy(() => import("./pages/static/NotFoundPage"));

// Path prefix → i18n key for the per-route document title (longest prefix wins via order).
const TITLE_KEYS: Array<[string, string]> = [
  ["/politicians", "nav.mps"],
  ["/votes", "nav.votes"],
  ["/legislation", "nav.bills"],
  ["/analytics", "nav.analytics"],
  ["/compare", "nav.compare"],
  ["/methodology", "footer.methodology"],
  ["/about", "footer.about"],
  ["/sources", "footer.sources"],
  ["/data-status", "footer.dataFreshness"],
  ["/privacy", "footer.privacy"],
  ["/terms", "footer.terms"],
  ["/corrections", "footer.corrections"],
  ["/admin", "admin.title"],
];

export default function App() {
  const { t, i18n } = useTranslation();
  const location = useLocation();
  const firstNav = useRef(true);

  useEffect(() => {
    document.documentElement.lang = i18n.resolvedLanguage ?? "en";
  }, [i18n.resolvedLanguage]);

  // Per-route, per-locale document title. index.html <title> stays as the fallback.
  useEffect(() => {
    const brand = t("nav.brand");
    const p = location.pathname;
    if (p === "/") {
      document.title = brand;
      return;
    }
    const match = TITLE_KEYS.find(([prefix]) => p === prefix || p.startsWith(`${prefix}/`));
    const routeTitle = match ? t(match[1]) : t("notFound.kicker");
    document.title = `${routeTitle} — ${brand}`;
  }, [location.pathname, i18n.resolvedLanguage, t]);

  // Move focus to the main region on navigation so keyboard/AT users land in fresh content.
  useEffect(() => {
    if (firstNav.current) {
      firstNav.current = false;
      return;
    }
    document.getElementById("main")?.focus({ preventScroll: true });
  }, [location.pathname]);

  const fallback = (
    <div className="p-8 text-muted font-mono text-sm tracking-[0.06em]" role="status" aria-live="polite">
      {t("common.loading")}
    </div>
  );

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/politicians" element={<Suspense fallback={fallback}><PoliticiansPage /></Suspense>} />
        <Route path="/politicians/:slug" element={<Suspense fallback={fallback}><PoliticianProfilePage /></Suspense>} />
        <Route path="/votes" element={<Suspense fallback={fallback}><VotesPage /></Suspense>} />
        <Route path="/votes/:id" element={<Suspense fallback={fallback}><VoteDetailPage /></Suspense>} />
        <Route path="/compare" element={<Suspense fallback={fallback}><ComparePage /></Suspense>} />
        <Route path="/legislation" element={<Suspense fallback={fallback}><LegislationPage /></Suspense>} />
        <Route path="/legislation/:id" element={<Suspense fallback={fallback}><LegislationDetailPage /></Suspense>} />
        <Route path="/about" element={<Suspense fallback={fallback}><AboutPage /></Suspense>} />
        <Route path="/methodology" element={<Suspense fallback={fallback}><MethodologyIndexPage /></Suspense>} />
        <Route path="/methodology/participation" element={<Suspense fallback={fallback}><MethodologyParticipationPage /></Suspense>} />
        <Route path="/methodology/alignment" element={<Suspense fallback={fallback}><MethodologyAlignmentPage /></Suspense>} />
        <Route path="/methodology/agreement" element={<Suspense fallback={fallback}><MethodologyAgreementPage /></Suspense>} />
        <Route path="/methodology/analytics" element={<Suspense fallback={fallback}><MethodologyAnalyticsPage /></Suspense>} />
        <Route path="/sources" element={<Suspense fallback={fallback}><SourcesPage /></Suspense>} />
        <Route path="/privacy" element={<Suspense fallback={fallback}><PrivacyPage /></Suspense>} />
        <Route path="/terms" element={<Suspense fallback={fallback}><TermsPage /></Suspense>} />
        <Route path="/data-status" element={<Suspense fallback={fallback}><DataStatusPage /></Suspense>} />
        <Route path="/corrections" element={<Suspense fallback={fallback}><CorrectionsPage /></Suspense>} />
        <Route path="/analytics" element={<Suspense fallback={fallback}><AnalyticsPage /></Suspense>} />
        <Route path="/speeches" element={<Suspense fallback={fallback}><SpeechesPage /></Suspense>} />
        <Route path="/admin" element={<Suspense fallback={fallback}><AdminPage /></Suspense>} />
        <Route path="*" element={<Suspense fallback={fallback}><NotFoundPage /></Suspense>} />
      </Route>
    </Routes>
  );
}
