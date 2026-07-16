import { useEffect, useRef, useState } from "react";

export default function SearchInput({
  value,
  onChange,
  placeholder = "Search…",
  ariaLabel,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  ariaLabel?: string;
}) {
  const [local, setLocal] = useState(value);

  // Keep the latest onChange in a ref so the debounce effect doesn't re-fire
  // when the parent re-renders and passes a new inline callback. Without this,
  // parent re-renders (e.g. from setPage) triggered a fresh setTimeout that
  // ended up calling onChange("") 250 ms later — silently resetting page state.
  const onChangeRef = useRef(onChange);
  useEffect(() => { onChangeRef.current = onChange; }, [onChange]);

  // Sync internal state when the external value prop changes (e.g. URL reset / back nav).
  useEffect(() => { setLocal(value); }, [value]);

  // Skip the mount pass so onChange only fires on genuine user input — otherwise the
  // debounce fired with the initial value on every visit, deleting the `page` URL param
  // and pushing a junk history entry ~250ms after load.
  const firstRun = useRef(true);
  useEffect(() => {
    if (firstRun.current) {
      firstRun.current = false;
      return;
    }
    const t = setTimeout(() => onChangeRef.current(local), 250);
    return () => clearTimeout(t);
  }, [local]);

  return (
    <input
      type="search"
      value={local}
      onChange={(e) => setLocal(e.target.value)}
      placeholder={placeholder}
      aria-label={ariaLabel ?? placeholder}
      className="w-full max-w-md bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-blue"
    />
  );
}
