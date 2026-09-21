import test from "node:test";
import assert from "node:assert/strict";
import { clusterArticles, parseFeed, similarTitles } from "../server.mjs";

const source = { name: "Test", category: "SWIAT", trust: 5, kind: "EDITORIAL" };

test("parses RSS items", () => {
  const xml = `<rss><channel><item><title>Important event in Europe</title><description>Short description.</description><link>https://example.com/a</link><pubDate>Sun, 20 Sep 2026 12:00:00 GMT</pubDate></item></channel></rss>`;
  const items = parseFeed(xml, source);
  assert.equal(items.length, 1);
  assert.equal(items[0].title, "Important event in Europe");
});

test("detects related titles", () => {
  assert.equal(similarTitles("European bank announces interest rate decision", "Interest rate decision announced by European bank"), true);
});

test("confirms a story supported by two publishers", () => {
  const base = { description: "", category: "EUROPA", trust: 5, kind: "EDITORIAL", publishedAt: Date.now() };
  const clusters = clusterArticles([
    { ...base, title: "European bank announces interest rate decision", source: "Source A", url: "https://a.example" },
    { ...base, title: "Interest rate decision announced by European bank", source: "Source B", url: "https://b.example" }
  ]);
  assert.equal(clusters[0].verificationStatus, "CONFIRMED");
  assert.equal(clusters[0].sources.length, 2);
});

test("marks one official statement without independent confirmation", () => {
  const clusters = clusterArticles([{ title: "Institution publishes a statement", description: "", category: "SWIAT", trust: 5, kind: "OFFICIAL_STATEMENT", publishedAt: Date.now(), source: "Institution", url: "https://official.example" }]);
  assert.equal(clusters[0].verificationStatus, "OFFICIAL_SOURCE");
});
