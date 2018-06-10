package com.fusionjack.adhell3.utils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockUrlPatternsMatch {

    private static final String WILDCARD_PATTERN = "(?im)^(([*])([A-Z0-9-_.]+))$|^(([A-Z0-9-_.]+)([*]))$|^(([*])([A-Z0-9-_.]+)([*])$)";
    private static final Pattern wildcard_r = Pattern.compile(WILDCARD_PATTERN);

    private static final String DOMAIN_PATTERN = "(?im)(?=^.{4,253}$)(^((?!-)[a-z0-9-]{1,63}(?<!-)\\.)+[a-z]{2,63}$)";
    private static final Pattern domain_r = Pattern.compile(DOMAIN_PATTERN);

    // Define pattern for filter files: ||something.com^ or ||something.com^$third-party
    private static final String FILTER_PATTERN = "(?im)((?<=^\\|\\|)([A-Z0-9-_.]+)(?=\\^([$]third-party)?$))";
    private static final Pattern filter_r = Pattern.compile(FILTER_PATTERN);

    private BlockUrlPatternsMatch() {
    }

    private static boolean wildcardValid (String domain) {
        return wildcard_r.matcher(domain).matches();
    }

    private static boolean domainValid (String domain){
        return domain_r.matcher(domain).matches();
    }

    private static String validHostFileDomains(String hostFileStr) {

        final Matcher filterPatterMatch = filter_r.matcher(hostFileStr);
        final Matcher domainPatterMatch = domain_r.matcher(hostFileStr);
        final Matcher wildcardPatternMatch = wildcard_r.matcher(hostFileStr);

        // Create a new string builder to hold our valid domains
        StringBuilder validDomainsStrBuilder = new StringBuilder();

        // If the input file is in filter file format
        if(filterPatterMatch.find())
        {
            // Reset the find()
            filterPatterMatch.reset();
            // While there are matches, add each to the StringBuilder
            while(filterPatterMatch.find())
            {
                String filterListDomain = filterPatterMatch.group();
                validDomainsStrBuilder.append(filterListDomain);
                validDomainsStrBuilder.append("\n");
            }
        }
        // Otherwise, process as a standard host file
        else
        {
            // If we find valid hosts
            if(domainPatterMatch.find())
            {
                // Reset the find()
                domainPatterMatch.reset();
                // While there are matches, add each to the StringBuilder
                while(domainPatterMatch.find())
                {
                    String Domain = domainPatterMatch.group();
                    validDomainsStrBuilder.append(Domain);
                    validDomainsStrBuilder.append("\n");
                }
            }

            // If we find valid wildcards
            if(wildcardPatternMatch.find()) {
                // Reset the find()
                wildcardPatternMatch.reset();
                // While there are matches, add each to the StringBuilder
                while (wildcardPatternMatch.find())
                {
                    String Wildcard = wildcardPatternMatch.group();
                    validDomainsStrBuilder.append(Wildcard);
                    validDomainsStrBuilder.append("\n");
                }
            }
        }

        String validDomainsStr = validDomainsStrBuilder.toString();

        return validDomainsStr;
    }

    public static boolean isUrlValid(String url) {
        if (url.contains("*")) {
            return BlockUrlPatternsMatch.wildcardValid(url);
        }
        return BlockUrlPatternsMatch.domainValid(url);
    }

    public static String getValidHostFileDomains(String hostFileStr) {
        return BlockUrlPatternsMatch.validHostFileDomains(hostFileStr);
    }

    public static String getValidatedUrl(String url) {
        return (url.contains("*") ? "" : "*") + url;
    }

}
