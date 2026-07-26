import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// Builds the standard set of report outputs (HTML + JSON + colored stdout) for a
// scenario's handleSummary(). Reports land in performance-tests/reports/ (gitignored)
// so every run is inspectable without polluting the repo.
export function buildSummary(scenarioName, data) {
  return {
    [`reports/${scenarioName}-summary.html`]: htmlReport(data),
    [`reports/${scenarioName}-summary.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
