@Regression @SEO-34 @SEO-35
Feature: Indexable-Filters Service

  @SEO-34_Regression_UK @SEO-34_Regression_JP
  Scenario Outline: Check that indexable-filters service returns correct response
    Given I am on <locale> site
    When I access IFS GET_FILTERS endpoint WITH authentication and <categoryID>
    Then I verify <responseCode> is correct
    And I verify cache-control has max-age 86400
    And I verify response schema is as expected

    Examples:
    | locale | categoryID | responseCode |
    | uk     | PSF_436057 | 200          |
    | jp     | PSF_412533 | 200          |

  @SEO-35_Regression_UK
  Scenario Outline: Check that Seo text returns empty when all seo text contain a javascript content type
    Given I am on <locale> site
    When I access IFS GET_FILTERS endpoint WITH authentication and <categoryID>
    Then I verify <responseCode> is correct
    And I verify the groups.seoText field is empty

    Examples:
    | locale | categoryID | responseCode |
    | uk     | PSF_437408 | 200          |

  @SEO-34_Regression_UK @SEO-34_Regression_JP
  Scenario Outline: Check category with multiple dimensions are returned correctly for <locale> sites
    Given I am on <locale> site
    When I access IFS GET_FILTERS endpoint WITH authentication and <categoryID>
    Then I verify response schema is as expected
    And I verify the dimensions is in ascending order

    Examples:
    | locale | categoryID |
    | uk     | PSF_432164 |
    | jp     | PSF_430779 |

  @SEO-34_Regression_UK @SEO-470_Regression
  Scenario Outline: Check IFS endpoints are authenticated
    Given I am on <locale> site
    When I access IFS <endpoint> endpoint WITHOUT authentication and <categoryID>
    Then I verify <responseCode> is correct

    Examples:
      | locale | categoryID | responseCode | endpoint    |
      | uk     | PSF_436057 | 401          | GET_FILTERS |
      | uk     | PSF_436057 | 401          | GET_GROUPS  |

  @SEO-470_Regression @gc
  Scenario Outline: Check that indexable-filters service returns correct response for getGroups
    Given I am on <locale> site
    When I access IFS GET_GROUPS endpoint WITH authentication and <pageKey>
    Then I verify <responseCode> is correct
    And I verify cache-control has max-age 86400
    And I verify response schema is as expected

    Examples:
    | locale | pageKey     | responseCode |
    | uk     | PSSS_432064 | 200          |
    | hk02   | PSS_435715  | 200          |
    | jp     | PSF_430779  | 200          |
    | uk     | PSF_432164  | 200          |
    | uk     | PSF_438811  | 200          |

  @SEO-470_Regression
  Scenario Outline: Check that indexable-filters service returns correct response for unknown keys
    Given I am on <locale> site
    When I access IFS <endpoint> endpoint WITH authentication and <pageKey>
    Then I verify <responseCode> is correct
    And I verify cache-control has max-age 86400
    And I verify response schema is as expected

    Examples:
      | locale | pageKey | responseCode | endpoint    |
      | uk     | UNKNOWN | 200          | GET_FILTERS |
      | uk     | UNKNOWN | 200          | GET_GROUPS  |