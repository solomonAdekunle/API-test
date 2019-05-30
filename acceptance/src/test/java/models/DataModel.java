package models;

import helpers.Env;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

import static helpers.Env.LOCAL;

@Setter
@Getter
public class DataModel {

        private String service;
        private String categoryId;
        private String storeId;
        private String languageId;
        private String sitemapDateTime;

        public static final String POST_TO_SLACK_HOOK = "https://hooks.slack.com/services/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";

        public String getBaseURL() {

            String url = System.getProperty("serviceUrl");
            if(StringUtils.isNotBlank(url)) {
                return url;
            }

            System.out.println("Using default service urls");
            switch (Env.get()){
                case LOCAL:
                    return "http://localhost:8080";
                case "devci":
                    return "http://10.251.26.107:8088";
                case "static1":
                    return "http://brand-global-st1.ebs.ecomp.com:8087";
                case "static2":
                    return "http://brand-global-st2.ebs.ecomp.com:8087";
                case "prod":
                    return "http://brand-global.ebs.ecomp.com:8087";
                default:
                    throw new RuntimeException("Unknown environment");
            }
        }

        static public List<String> getIndexableFilterMarkets() {
            return Arrays.asList("au", "uk", "jp", "de", "f1", "hk01", "hk02", "it", "se");
        }
}
