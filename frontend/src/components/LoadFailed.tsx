import { useLoadErrorMessage } from "../lib/useLoadErrorMessage";

/**
 * Shared inline error line for failed data fetches. When the failure is a
 * structured ApiError, the translated `apiErrors.*` message is shown instead
 * of the raw HTTP text (see lib/useLoadErrorMessage).
 */
export default function LoadFailed({
  error,
  className = "text-sm text-hot-deep",
}: {
  error?: unknown;
  className?: string;
}) {
  const message = useLoadErrorMessage(error);
  return <p className={className} role="alert">{message}</p>;
}
