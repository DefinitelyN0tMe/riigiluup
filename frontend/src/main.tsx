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
      // Don't retry client errors (4xx): they won't succeed on retry. The exception is 429 (per-IP
      // rate limit): wait as long as the server asks, then try once more, so a reader who opened
      // many tabs gets the page a moment later instead of an empty "too many requests" block.
      // Transient errors (5xx / network) are retried at most twice.
      retry: (count, err) => {
        if (err instanceof ApiError && err.status === 429) return count < 1;
        return !(err instanceof ApiError && err.status >= 400 && err.status < 500) && count < 2;
      },
      retryDelay: (count, err) =>
        err instanceof ApiError && err.status === 429
          ? Math.min((err.retryAfter ?? 10) * 1000, 60_000)
          : Math.min(1000 * 2 ** count, 30_000)
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
