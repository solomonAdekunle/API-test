package models;

public enum Endpoint {
    GET_FILTERS("/indexable-filter/storeId/{storeId}/categoryId/{categoryId}/language/{languageId}/v1", "getFilters"),//
    GET_GROUPS("/indexable-filter/locale/{locale}/pagekey/{pageKey}/v1", "getGroups"),//
    SITEMAP("/indexable-filter/createSitemap/v1", null),//
    CACHED_MARKETS("/indexable-filter/management/cached-markets", null);

    private final String path;
    private final String filePrefix;

    Endpoint(String path, String filePrefix) {
        this.path = path;
        this.filePrefix = filePrefix;
    }

    public String getPath() {
        return path;
    }

    public String getFilePrefix() {
        return filePrefix;
    }
}
