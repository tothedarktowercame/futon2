import { defineConfig } from "@playwright/test";

// baseURL resolution order:
//   1. WM_BASE_URL env var (explicit override)
//   2. http://localhost:3110 — standalone server (clojure -M:run inside web/war-machine)
//
// Rolled-up mode: start shadow-cljs dev-http (:8710) proxying to futon3c (:7070)
// and run with `WM_BASE_URL=http://localhost:8710 npm run test:e2e`.
const baseURL = process.env.WM_BASE_URL || "http://localhost:3110";

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  use: {
    baseURL,
    headless: true,
    screenshot: "only-on-failure",
  },
});
