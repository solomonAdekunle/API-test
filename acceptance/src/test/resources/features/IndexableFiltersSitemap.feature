@Regression @SEO-183
Feature: Indexable-Filters-Sitemap Service

  @SEO-183
  Scenario: Check indexable filters sitemap service
    Given I call indexable filters sitemap service
    When I retrieve mobile and desktop sitemaps from server
    Then I extract and verify the output sitemaps for all expected countries