#!/usr/bin/env node
// Render the paper's vector figure from the canonical interactive explainer.
// Run from the futon2 repository root after r18_badges_to_js.bb --check.
import { createHash } from "node:crypto";
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import playwright from "../web/war-machine/node_modules/playwright/index.js";

const { chromium } = playwright;

const root = process.cwd();
const htmlPath = resolve(root, "holes/aif-wiring-explainer.html");
const manifestPath = resolve(root, "data/r18-badges.edn");
const svgPath = resolve(root, "../p4ng/aif-wiring-brain.svg");
const pdfPath = resolve(root, "../p4ng/aif-wiring-brain.pdf");
const manifestHash = createHash("sha256")
  .update(readFileSync(manifestPath))
  .digest("hex");

const browser = await chromium.launch({
  executablePath: process.env.CHROME_BIN || "/usr/bin/google-chrome",
  headless: true,
  args: ["--no-sandbox"],
});

try {
  const page = await browser.newPage({ viewport: { width: 1220, height: 900 } });
  await page.goto(pathToFileURL(htmlPath).href, { waitUntil: "load" });
  await page.waitForFunction(() => document.querySelectorAll("#svg .node").length > 0);
  const svg = await page.locator("#svg").evaluate((source, hash) => {
    const clone = source.cloneNode(true);
    clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    clone.setAttribute("width", "820");
    clone.setAttribute("height", "620");
    const properties = [
      "fill", "stroke", "stroke-width", "stroke-dasharray", "opacity",
      "font-family", "font-size", "font-style", "font-weight",
      "text-anchor", "marker-end", "filter",
    ];
    const originals = [source, ...source.querySelectorAll("*")];
    const clones = [clone, ...clone.querySelectorAll("*")];
    originals.forEach((element, index) => {
      const computed = getComputedStyle(element);
      const declarations = properties
        .map((property) => `${property}:${computed.getPropertyValue(property)}`)
        .join(";");
      clones[index].setAttribute("style", declarations);
      if (element.matches(".node rect")) {
        clones[index].setAttribute("rx", "8");
        clones[index].setAttribute("ry", "8");
      }
    });
    const metadata = document.createElementNS("http://www.w3.org/2000/svg", "metadata");
    metadata.textContent = `Generated from aif-wiring-explainer.html; r18-badges.edn sha256=${hash}`;
    clone.prepend(metadata);
    return `<?xml version="1.0" encoding="UTF-8"?>\n${clone.outerHTML}`;
  }, manifestHash);
  writeFileSync(svgPath, svg);
  await page.evaluate(() => {
    const figure = document.querySelector("#svg");
    document.body.replaceChildren(figure);
    document.documentElement.style.margin = "0";
    document.body.style.margin = "0";
    figure.style.width = "820px";
    figure.style.height = "620px";
  });
  await page.pdf({
    path: pdfPath,
    width: "820px",
    height: "620px",
    margin: { top: 0, right: 0, bottom: 0, left: 0 },
    printBackground: true,
  });
} finally {
  await browser.close();
}

console.log(`rendered ${svgPath}`);
console.log(`rendered ${pdfPath}`);
