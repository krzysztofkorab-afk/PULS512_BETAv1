import http from "node:http";
import { fileURLToPath } from "node:url";

const PORT = Number(process.env.PORT || 8787);
const CACHE_MS = Number(process.env.CACHE_MINUTES || 15) * 60_000;
const CATEGORIES = new Set(["POLSKA", "EUROPA", "SWIAT", "BIZNES", "TECHNOLOGIA"]);

export const SOURCES = [
  { name: "PAP", url: "https://www.pap.pl/rss.xml", category: "POLSKA", trust: 5, kind: "AGENCY" },
  { name: "Polskie Radio 24", url: "https://polskieradio24.pl/rss/35", category: "POLSKA", trust: 4, kind: "EDITORIAL" },
  { name: "RMF24", url: "https://www.rmf24.pl/fakty/feed", category: "POLSKA", trust: 4, kind: "EDITORIAL" },
  { name: "Eurostat", url: "https://ec.europa.eu/eurostat/news/euro-indicators?_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_collection=CAT_PREREL&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_pageNumber=1&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_pageSize=20&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_sort=lastUpdateDate&p_p_cacheability=cacheLevelPage&p_p_id=estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK&p_p_lifecycle=2&p_p_mode=view&p_p_resource_id=atom&p_p_state=normal", category: "BIZNES", trust: 5, kind: "OFFICIAL_DATA" },
  { name: "Europejski Bank Centralny", url: "https://www.ecb.europa.eu/rss/press.html", category: "BIZNES", trust: 5, kind: "OFFICIAL_DATA" },
  { name: "Rada UE", url: "https://www.consilium.europa.eu/en/press/press-releases/rss/", category: "EUROPA", trust: 5, kind: "OFFICIAL_STATEMENT" },
  { name: "Euronews", url: "https://www.euronews.com/rss?level=theme&name=news", category: "EUROPA", trust: 4, kind: "EDITORIAL" },
  { name: "POLITICO Europe", url: "https://www.politico.eu/feed/", category: "EUROPA", trust: 4, kind: "EDITORIAL" },
  { name: "ONZ", url: "https://news.un.org/feed/subscribe/en/news/all/rss.xml", category: "SWIAT", trust: 5, kind: "OFFICIAL_STATEMENT" },
  { name: "WHO", url: "https://www.who.int/rss-feeds/news-english.xml", category: "SWIAT", trust: 5, kind: "OFFICIAL_DATA" },
  { name: "BBC World", url: "https://feeds.bbci.co.uk/news/world/rss.xml", category: "SWIAT", trust: 5, kind: "EDITORIAL" },
  { name: "DW", url: "https://rss.dw.com/rdf/rss-en-all", category: "SWIAT", trust: 4, kind: "EDITORIAL" },
  { name: "France 24", url: "https://www.france24.com/en/rss", category: "SWIAT", trust: 4, kind: "EDITORIAL" },
  { name: "The Guardian World", url: "https://www.theguardian.com/world/rss", category: "SWIAT", trust: 3, kind: "EDITORIAL" },
  { name: "BBC Business", url: "https://feeds.bbci.co.uk/news/business/rss.xml", category: "BIZNES", trust: 4, kind: "EDITORIAL" },
  { name: "BBC Technology", url: "https://feeds.bbci.co.uk/news/technology/rss.xml", category: "TECHNOLOGIA", trust: 4, kind: "EDITORIAL" },
  { name: "NASA", url: "https://www.nasa.gov/rss/dyn/breaking_news.rss", category: "TECHNOLOGIA", trust: 5, kind: "OFFICIAL_DATA" }
];

const cache = new Map();
const requestBuckets = new Map();

function decodeXml(value = "") {
  return value
    .replace(/^\s*<!\[CDATA\[|\]\]>\s*$/g, "")
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)))
    .replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&apos;/g, "'")
    .replace(/\s+/g, " ").trim();
}

function tagValue(block, names) {
  for (const name of names) {
    const pattern = new RegExp(`<(?:[\\w-]+:)?${name}\\b[^>]*>([\\s\\S]*?)<\\/(?:[\\w-]+:)?${name}>`, "i");
    const match = block.match(pattern);
    if (match) return decodeXml(match[1]);
  }
  return "";
}

function linkValue(block) {
  const atom = block.match(/<(?:[\w-]+:)?link\b[^>]*href=["']([^"']+)["'][^>]*\/?\s*>/i);
  return decodeXml(atom?.[1] || tagValue(block, ["link", "guid"]));
}

export function parseFeed(xml, source) {
  const blocks = [...xml.matchAll(/<item\b[^>]*>([\s\S]*?)<\/item>/gi), ...xml.matchAll(/<entry\b[^>]*>([\s\S]*?)<\/entry>/gi)]
    .map((match) => match[1]).slice(0, 20);
  return blocks.map((block) => {
    const title = tagValue(block, ["title"]);
    const description = tagValue(block, ["description", "summary", "encoded", "content"]);
    const rawDate = tagValue(block, ["pubDate", "published", "updated", "date"]);
    return {
      id: `${source.name}:${title}`,
      title: title.slice(0, 220),
      description: description.slice(0, 900),
      url: linkValue(block),
      source: source.name,
      category: source.category,
      trust: source.trust,
      kind: source.kind,
      publishedAt: Number.isFinite(Date.parse(rawDate)) ? Date.parse(rawDate) : Date.now()
    };
  }).filter((item) => item.title);
}

async function fetchSource(source) {
  const response = await fetch(source.url, {
    headers: { "user-agent": "PULS512-Backend/0.2 (+https://lab512.pl)", accept: "application/rss+xml, application/atom+xml, application/xml, text/xml" },
    signal: AbortSignal.timeout(9_000),
    redirect: "follow"
  });
  if (!response.ok) throw new Error(`${source.name}: HTTP ${response.status}`);
  return parseFeed(await response.text(), source);
}

function tokens(text) {
  const stop = new Set(["the", "and", "with", "from", "that", "this", "dla", "oraz", "jest", "się", "nie", "przez", "after", "says"]);
  return new Set(text.toLocaleLowerCase("pl-PL").replace(/[^a-ząćęłńóśźż0-9 ]/gi, " ").split(/\s+/).filter((word) => word.length > 3 && !stop.has(word)));
}

export function similarTitles(left, right) {
  const one = tokens(left); const two = tokens(right);
  if (!one.size || !two.size) return false;
  const common = [...one].filter((word) => two.has(word)).length;
  const union = new Set([...one, ...two]).size;
  return common / union >= 0.36 || common / Math.min(one.size, two.size) >= 0.60;
}

function articleScore(article) {
  const ageHours = Math.max(0, (Date.now() - article.publishedAt) / 3_600_000);
  return article.trust * 1000 - Math.min(500, ageHours);
}

export function clusterArticles(articles) {
  const queue = [...articles].sort((a, b) => articleScore(b) - articleScore(a));
  const clusters = [];
  while (queue.length) {
    const seed = queue.shift();
    const related = [seed];
    for (let index = queue.length - 1; index >= 0; index--) {
      if (similarTitles(seed.title, queue[index].title)) related.push(queue.splice(index, 1)[0]);
    }
    const sources = [...new Map(related.map((article) => [article.source, {
      name: article.source,
      url: article.url,
      primary: article.kind === "OFFICIAL_DATA" || article.kind === "OFFICIAL_STATEMENT"
    }])).values()];
    const kinds = new Set(related.map((article) => article.kind));
    const verificationStatus = kinds.has("OFFICIAL_DATA") || sources.length >= 2
      ? "CONFIRMED"
      : kinds.has("OFFICIAL_STATEMENT") ? "OFFICIAL_SOURCE" : "DEVELOPING";
    const verificationReason = verificationStatus === "CONFIRMED"
      ? kinds.has("OFFICIAL_DATA") ? "Dane pochodzą bezpośrednio z właściwej instytucji." : `Zgodność ${sources.length} niezależnych źródeł.`
      : verificationStatus === "OFFICIAL_SOURCE"
        ? "Komunikat instytucji; treść zewnętrzna nie została jeszcze niezależnie potwierdzona."
        : "Pojedyncze źródło — materiał oczekuje na dodatkowe potwierdzenie.";
    const representative = [...related].sort((a, b) => articleScore(b) - articleScore(a))[0];
    clusters.push({
      clusterId: `c_${clusters.length}_${Math.abs(hashCode(representative.title))}`,
      category: representative.category,
      publishedAt: Math.max(...related.map((item) => item.publishedAt)),
      verificationStatus,
      verificationReason,
      sources,
      articles: related
    });
  }
  return clusters;
}

function hashCode(value) {
  let hash = 0;
  for (const char of value) hash = ((hash << 5) - hash + char.codePointAt(0)) | 0;
  return hash;
}

function fallbackSummary(cluster) {
  const article = [...cluster.articles].sort((a, b) => articleScore(b) - articleScore(a))[0];
  return {
    clusterId: cluster.clusterId,
    title: article.title,
    summary: article.description || "Otwórz źródła, aby przeczytać szczegóły wydarzenia.",
    whyItMatters: cluster.verificationStatus === "CONFIRMED"
      ? "Informacja przeszła automatyczną kontrolę zgodności źródeł."
      : "To ważny komunikat, ale należy go traktować jako stanowisko źródła."
  };
}

async function summarizeWithOpenAI(clusters) {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) return { aiUsed: false, summaries: clusters.map(fallbackSummary) };

  const evidence = clusters.map((cluster) => ({
    clusterId: cluster.clusterId,
    category: cluster.category,
    verificationStatus: cluster.verificationStatus,
    evidence: cluster.articles.map((article) => ({ source: article.source, title: article.title, description: article.description }))
  }));
  const schema = {
    type: "object",
    additionalProperties: false,
    properties: {
      items: {
        type: "array",
        items: {
          type: "object",
          additionalProperties: false,
          properties: {
            clusterId: { type: "string" },
            title: { type: "string" },
            summary: { type: "string" },
            whyItMatters: { type: "string" }
          },
          required: ["clusterId", "title", "summary", "whyItMatters"]
        }
      }
    },
    required: ["items"]
  };
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: { authorization: `Bearer ${apiKey}`, "content-type": "application/json" },
    body: JSON.stringify({
      model: process.env.OPENAI_MODEL || "gpt-6-astra",
      reasoning: { effort: "low" },
      instructions: [
        "Tworzysz polski briefing informacyjny PULS 512.",
        "Korzystaj wyłącznie z dostarczonego materiału evidence. Nie dopisuj faktów, nazw, liczb ani kontekstu.",
        "Jeżeli źródła opisują jedynie czyjeś stanowisko, użyj sformułowania: poinformował, oświadczył lub według źródła.",
        "Nie zmieniaj clusterId. Tytuł ma być neutralny. Summary: maksymalnie 3 krótkie zdania. WhyItMatters: jedno zdanie.",
        "Jeśli materiały są sprzeczne, napisz o rozbieżności zamiast ją rozstrzygać."
      ].join(" "),
      input: JSON.stringify(evidence),
      text: { format: { type: "json_schema", name: "puls512_briefing", strict: true, schema } }
    }),
    signal: AbortSignal.timeout(30_000)
  });
  if (!response.ok) throw new Error(`OpenAI HTTP ${response.status}: ${(await response.text()).slice(0, 300)}`);
  const result = await response.json();
  const outputText = result.output?.flatMap((item) => item.content || []).find((item) => item.type === "output_text")?.text;
  if (!outputText) throw new Error("OpenAI response did not contain output_text");
  const parsed = JSON.parse(outputText);
  return { aiUsed: true, summaries: parsed.items };
}

async function buildBriefing(categories, limit, verifiedOnly) {
  const selected = SOURCES.filter((source) => categories.has(source.category));
  const settled = await Promise.allSettled(selected.map(fetchSource));
  const articles = settled.flatMap((result) => result.status === "fulfilled" ? result.value : [])
    .filter((article) => Date.now() - article.publishedAt < 48 * 3_600_000);
  let clusters = clusterArticles(articles)
    .filter((cluster) => !verifiedOnly || cluster.verificationStatus === "CONFIRMED")
    .sort((a, b) => b.publishedAt - a.publishedAt)
    .slice(0, limit);

  let summaryResult;
  try {
    summaryResult = await summarizeWithOpenAI(clusters);
  } catch (error) {
    console.error("AI summary fallback:", error.message);
    summaryResult = { aiUsed: false, summaries: clusters.map(fallbackSummary) };
  }
  const summaries = new Map(summaryResult.summaries.map((item) => [item.clusterId, item]));
  const items = clusters.map((cluster) => {
    const summary = summaries.get(cluster.clusterId) || fallbackSummary(cluster);
    return {
      title: summary.title,
      summary: summary.summary,
      whyItMatters: summary.whyItMatters,
      category: cluster.category,
      publishedAt: cluster.publishedAt,
      verificationStatus: cluster.verificationStatus,
      verificationReason: cluster.verificationReason,
      sources: cluster.sources
    };
  });
  return {
    generatedAt: Date.now(),
    aiUsed: summaryResult.aiUsed,
    verifiedOnly,
    sourceHealth: { requested: selected.length, successful: settled.filter((item) => item.status === "fulfilled").length },
    policy: "official-data-or-two-independent-sources",
    items
  };
}

function rateAllowed(ip) {
  const minute = Math.floor(Date.now() / 60_000);
  const key = `${ip}:${minute}`;
  const count = (requestBuckets.get(key) || 0) + 1;
  requestBuckets.set(key, count);
  if (requestBuckets.size > 2_000) requestBuckets.clear();
  return count <= 60;
}

function json(res, status, body) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": process.env.ALLOWED_ORIGIN || "*"
  });
  res.end(JSON.stringify(body));
}

export async function handleRequest(req, res) {
  const requestUrl = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  if (!rateAllowed(req.socket.remoteAddress || "unknown")) return json(res, 429, { error: "rate_limit" });
  if (requestUrl.pathname === "/health") return json(res, 200, { ok: true, version: "0.2.0", sources: SOURCES.length });
  if (requestUrl.pathname === "/sources") return json(res, 200, { sources: SOURCES.map(({ name, category, kind }) => ({ name, category, kind })) });
  if (requestUrl.pathname !== "/briefing") return json(res, 404, { error: "not_found" });

  const categories = new Set((requestUrl.searchParams.get("categories") || [...CATEGORIES].join(",")).split(",").filter((value) => CATEGORIES.has(value)));
  const limit = Math.max(1, Math.min(10, Number(requestUrl.searchParams.get("limit") || 6)));
  const verifiedOnly = requestUrl.searchParams.get("verifiedOnly") !== "false";
  const cacheKey = `${[...categories].sort().join(",")}:${limit}:${verifiedOnly}`;
  const cached = cache.get(cacheKey);
  if (cached && Date.now() - cached.createdAt < CACHE_MS) return json(res, 200, { ...cached.value, cached: true });

  try {
    const value = await buildBriefing(categories, limit, verifiedOnly);
    cache.set(cacheKey, { createdAt: Date.now(), value });
    return json(res, 200, { ...value, cached: false });
  } catch (error) {
    console.error(error);
    return json(res, 503, { error: "briefing_unavailable", message: "Nie udało się przygotować briefingu." });
  }
}

const isMain = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMain) {
  http.createServer((req, res) => void handleRequest(req, res)).listen(PORT, "0.0.0.0", () => {
    console.log(`PULS 512 backend listening on :${PORT}`);
  });
}
