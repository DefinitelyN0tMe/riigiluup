import { Link, Route, Routes } from "react-router-dom";
import { Suspense, lazy, useEffect } from "react";
import { useTranslation } from "react-i18next";
import LocaleSwitcher from "./components/LocaleSwitcher";
import HomePage from "./pages/HomePage";
import PoliticiansPage from "./pages/PoliticiansPage";
import VotesPage from "./pages/VotesPage";
import LegislationPage from "./pages/LegislationPage";
import ComparePage from "./pages/ComparePage";

const PoliticianProfilePage = lazy(() => import("./pages/PoliticianProfilePage"));
const VoteDetailPage = lazy(() => import("./pages/VoteDetailPage"));
const LegislationDetailPage = lazy(() => import("./pages/LegislationDetailPage"));
const AdminPage = lazy(() => import("./pages/AdminPage"));
const AboutPage = lazy(() => import("./pages/static/AboutPage"));
const MethodologyIndexPage = lazy(() => import("./pages/static/MethodologyIndexPage"));
const MethodologyParticipationPage = lazy(() => import("./pages/static/MethodologyParticipationPage"));
const MethodologyAlignmentPage = lazy(() => import("./pages/static/MethodologyAlignmentPage"));
const MethodologyAgreementPage = lazy(() => import("./pages/static/MethodologyAgreementPage"));
const SourcesPage = lazy(() => import("./pages/static/SourcesPage"));
const PrivacyPage = lazy(() => import("./pages/static/PrivacyPage"));
const TermsPage = lazy(() => import("./pages/static/TermsPage"));
const DataStatusPage = lazy(() => import("./pages/static/DataStatusPage"));
const CorrectionsPage = lazy(() => import("./pages/static/CorrectionsPage"));

export default function App() {
  const { t, i18n } = useTranslation();
  useEffect(() => {
    document.documentElement.lang = i18n.resolvedLanguage ?? "en";
  }, [i18n.resolvedLanguage]);
  const fallback = (
    <div className="p-8 text-slate-500" role="status" aria-live="polite">
      {t("common.loading")}
    </div>
  );
  return (
    <div className="min-h-screen flex flex-col">
      <a href="#main" className="skip-link">Skip to content</a>
      <header className="border-b border-slate-200 px-6 py-4 flex items-center gap-6">
        <Link to="/" className="text-lg font-semibold text-ink">{t("nav.brand")}</Link>
        <nav className="text-sm text-slate-600 flex gap-4 flex-1">
          <Link to="/politicians" className="hover:text-estonia">{t("nav.mps")}</Link>
          <Link to="/votes" className="hover:text-estonia">{t("nav.votes")}</Link>
          <Link to="/compare" className="hover:text-estonia">{t("nav.compare")}</Link>
          <Link to="/legislation" className="hover:text-estonia">{t("nav.bills")}</Link>
        </nav>
        <LocaleSwitcher />
      </header>
      <main id="main" className="flex-1 max-w-6xl mx-auto w-full px-6 py-8">
        <Suspense fallback={fallback}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/politicians" element={<PoliticiansPage />} />
            <Route path="/politicians/:slug" element={<PoliticianProfilePage />} />
            <Route path="/votes" element={<VotesPage />} />
            <Route path="/votes/:id" element={<VoteDetailPage />} />
            <Route path="/compare" element={<ComparePage />} />
            <Route path="/legislation" element={<LegislationPage />} />
            <Route path="/legislation/:id" element={<LegislationDetailPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/methodology" element={<MethodologyIndexPage />} />
            <Route path="/methodology/participation" element={<MethodologyParticipationPage />} />
            <Route path="/methodology/alignment" element={<MethodologyAlignmentPage />} />
            <Route path="/methodology/agreement" element={<MethodologyAgreementPage />} />
            <Route path="/sources" element={<SourcesPage />} />
            <Route path="/privacy" element={<PrivacyPage />} />
            <Route path="/terms" element={<TermsPage />} />
            <Route path="/data-status" element={<DataStatusPage />} />
            <Route path="/corrections" element={<CorrectionsPage />} />
            <Route path="/admin" element={<AdminPage />} />
          </Routes>
        </Suspense>
      </main>
      <footer className="border-t border-slate-200 px-6 py-4 text-xs text-slate-500 flex flex-wrap gap-x-4 gap-y-1">
        <span>{t("footer.dataCredit")}</span>
        <Link to="/about" className="hover:text-estonia">{t("footer.about")}</Link>
        <Link to="/methodology" className="hover:text-estonia">{t("footer.methodology")}</Link>
        <Link to="/sources" className="hover:text-estonia">{t("footer.sources")}</Link>
        <Link to="/data-status" className="hover:text-estonia">{t("footer.dataFreshness")}</Link>
        <Link to="/privacy" className="hover:text-estonia">{t("footer.privacy")}</Link>
        <Link to="/terms" className="hover:text-estonia">{t("footer.terms")}</Link>
        <Link to="/corrections" className="hover:text-estonia">{t("footer.corrections")}</Link>
      </footer>
    </div>
  );
}
