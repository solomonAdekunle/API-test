$(document).ready(function() {var formatter = new CucumberHTML.DOMFormatter($('.cucumber-report'));formatter.uri("IndexableFiltersSitemap.feature");
formatter.feature({
  "line": 2,
  "name": "Indexable-Filters-Sitemap Service",
  "description": "",
  "id": "indexable-filters-sitemap-service",
  "keyword": "Feature",
  "tags": [
    {
      "line": 1,
      "name": "@Regression"
    },
    {
      "line": 1,
      "name": "@SEO-183"
    }
  ]
});
formatter.before({
  "duration": 12712815600,
  "status": "passed"
});
formatter.scenario({
  "line": 5,
  "name": "Check indexable filters sitemap service",
  "description": "",
  "id": "indexable-filters-sitemap-service;check-indexable-filters-sitemap-service",
  "type": "scenario",
  "keyword": "Scenario",
  "tags": [
    {
      "line": 4,
      "name": "@SEO-183"
    }
  ]
});
formatter.step({
  "line": 6,
  "name": "I call indexable filters sitemap service",
  "keyword": "Given "
});
formatter.step({
  "line": 7,
  "name": "I retrieve mobile and desktop sitemaps from server",
  "keyword": "When "
});
formatter.step({
  "line": 8,
  "name": "I extract and verify the output sitemaps for all expected countries",
  "keyword": "Then "
});
formatter.match({
  "location": "IndexableFiltersSitemapSteps.iCallIndexableFiltersSitemapService()"
});
formatter.result({
  "duration": 2081971800,
  "status": "passed"
});
formatter.match({
  "location": "IndexableFiltersSitemapSteps.iRetrieveSitemapTarFileFromServer()"
});
formatter.result({
  "duration": 2244352700,
  "status": "passed"
});
formatter.match({
  "location": "IndexableFiltersSitemapSteps.iExtractAndVerifyTheOutputSitemaps()"
});
formatter.result({
  "duration": 728311800,
  "status": "passed"
});
formatter.after({
  "duration": 253700,
  "status": "passed"
});
});