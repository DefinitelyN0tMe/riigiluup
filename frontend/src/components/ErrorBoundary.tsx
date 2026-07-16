import { Component, type ErrorInfo, type ReactNode } from "react";
import { useTranslation } from "react-i18next";

// A stale lazy-chunk after a redeploy throws one of these — reload to fetch the new asset.
const CHUNK_ERROR = /ChunkLoadError|Loading chunk|dynamically imported module/i;

function Fallback() {
  const { t } = useTranslation();
  return (
    <div role="alert" className="max-w-[600px] mx-auto px-5 py-16 sm:py-24 text-center">
      <h1 className="font-display font-bold text-[26px] sm:text-[32px] tracking-[-0.02em] mb-3">
        {t("errors.boundaryTitle")}
      </h1>
      <p className="text-muted mb-6 max-w-[46ch] mx-auto">{t("errors.boundaryBody")}</p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="inline-flex items-center px-5 py-3 bg-ink text-white rounded-full font-semibold text-[15px]"
      >
        {t("errors.reload")}
      </button>
    </div>
  );
}

type Props = { children: ReactNode };
type State = { hasError: boolean };

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, _info: ErrorInfo) {
    // Recover automatically from a stale-chunk load error by reloading the page.
    if (error?.message && CHUNK_ERROR.test(error.message)) {
      window.location.reload();
    }
  }

  render() {
    if (this.state.hasError) return <Fallback />;
    return this.props.children;
  }
}
