import typography from "@tailwindcss/typography";

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        blue: {
          DEFAULT: "#0072CE",
          deep: "#003E7E",
          glow: "#6BB4F0",
          ink: "#0A1F3D",
        },
        estonia: "#0072CE",
        ink: {
          DEFAULT: "#0A0A0A",
          2: "#1C1C1C",
        },
        off: "#F4F4F1",
        paper: "#FCFCFA",
        rule: "#E3E3DE",
        muted: "#7A7A72",
        live: "#4DFF9C",
        hot: "#FF4B3E",
        amber: "#FFB020",
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', "system-ui", "sans-serif"],
        sans: ['"Bricolage Grotesque"', "system-ui", "sans-serif"],
        serif: ['Fraunces', 'Georgia', 'serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      letterSpacing: {
        tightest: "-0.055em",
        wider2: "0.18em",
        wider3: "0.24em",
      },
      screens: {
        xs: "420px",
      },
      keyframes: {
        pulse: {
          "0%,100%": { opacity: "1", transform: "scale(1)" },
          "50%":     { opacity: "0.35", transform: "scale(0.75)" },
        },
        scroll: {
          from: { transform: "translateX(0)" },
          to:   { transform: "translateX(-50%)" },
        },
      },
      animation: {
        "pulse-dot": "pulse 1.4s ease-in-out infinite",
        "scroll-x": "scroll 60s linear infinite",
        "scroll-x-fast": "scroll 40s linear infinite",
      },
    },
  },
  plugins: [typography],
};
