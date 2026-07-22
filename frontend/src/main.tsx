import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import ErrorBoundary from "./components/ErrorBoundary";
import { ApiError } from "./api/client";
import "./styles.css";
import "./i18n";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      refetchOnWindowFocus: false,
      // Don't retry client errors (4xx) — they won't succeed on retry and hammer the
      // 60/min rate limit. Retry transient errors (5xx / network) at most twice.
      retry: (count, err) =>
        !(err instanceof ApiError && err.status >= 400 && err.status < 500) && count < 2
    }
  }
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* Outermost boundary: catches render errors outside Layout's inner boundary
        (chrome, providers). Nested boundaries are fine — the closest one wins. */}
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  </StrictMode>
);
