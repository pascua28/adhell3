package com.fusionjack.adhell3.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockUrlPatternsMatch {

    private static final String WILDCARD_PATTERN = "(?im)^(([*])([A-Z0-9-_.]+))$|^(([A-Z0-9-_.]+)([*]))$|^(([*])([A-Z0-9-_.]+)([*])$)";
    private static final Pattern wildcard_r = Pattern.compile(WILDCARD_PATTERN);

    private static final String DOMAIN_PATTERN = "(?im)(?=^.{4,253}$)(^((?!-)[a-z0-9-]{1,63}(?<!-)\\.)+[a-z]{2,63}$)";
    private static final Pattern domain_r = Pattern.compile(DOMAIN_PATTERN);

    // Define pattern for filter files: ||something.com^ or ||something.com^$third-party
    private static final String FILTER_PATTERN = "(?im)(?=.{4,253}\\^)((?<=^[|]{2})(((?!-)[a-z0-9-]{1,63}(?<!-)\\.)+[a-z]{2,63})(?=\\^([$]third-party)?$))";
    private static final Pattern filter_r = Pattern.compile(FILTER_PATTERN);

    // Define patter for Adhell specific filters
    private static final String AH_FILTER_PATTERN = "(?im)(?=.{4,253}\\^)^(?:@dhell(\\|\\|))((?:(?!-)[a-z0-9-]{1,63}(?<!-)\\.)+[a-z]{2,63})(?=\\^([$]third-party)?$)";
    private static final Pattern ah_filter_r = Pattern.compile(AH_FILTER_PATTERN);

    // Knox URL - Must contain a letter in prefix / domain
    private static final String KNOX_VALID_PATTERN = "(?i)(^(?=.*[a-z]).*$)";
    private static final Pattern knox_valid_r = Pattern.compile(KNOX_VALID_PATTERN);

    private BlockUrlPatternsMatch() {
    }

    private static boolean wildcardValid (String domain) {
        return wildcard_r.matcher(domain).matches();
    }

    private static boolean domainValid (String domain){
        return domain_r.matcher(domain).matches();
    }

    private static String validHostFileDomains(String hostFileStr) {

        final Matcher filterPatternMatch = filter_r.matcher(hostFileStr);
        final Matcher domainPatternMatch = domain_r.matcher(hostFileStr);
        final Matcher wildcardPatternMatch = wildcard_r.matcher(hostFileStr);
        final Matcher ah_filterPatternMatch = ah_filter_r.matcher(hostFileStr);

        // Create a new string builder to hold our valid domains
        StringBuilder validDomainsStrBuilder = new StringBuilder();

        // If the input file is in filter file format
        if (filterPatternMatch.find()) {
            // Reset the find()
            filterPatternMatch.reset();
            // While there are matches, add each to the StringBuilder
            while (filterPatternMatch.find()) {
                String filterListDomain = filterPatternMatch.group();
                validDomainsStrBuilder.append(filterListDomain);
                validDomainsStrBuilder.append("\n");
            }
        }
        // Otherwise, process as a standard host file
        else {
            // While there are matches, add each to the StringBuilder
            while (domainPatternMatch.find()) {
                String domain = domainPatternMatch.group();
                validDomainsStrBuilder.append(domain);
                validDomainsStrBuilder.append("\n");
            }


            while(ah_filterPatternMatch.find()){
                // Capture the whole syntax
                String filterSyntax = ah_filterPatternMatch.group();
                // Capture the delimiter (filter option)
                String filterDelimiter = ah_filterPatternMatch.group(1);
                // Send for processing
                List<String>processedFilters = BlockUrlUtils.getProcessedAdhellFilters(filterSyntax, filterDelimiter);
                // Add each domain to the list
                if(!processedFilters.isEmpty()) {
                    for (String domain : processedFilters) {
                        validDomainsStrBuilder.append(domain);
                        validDomainsStrBuilder.append("\n");
                    }
                }
            }

            // While there are matches, add each to the StringBuilder
            while (wildcardPatternMatch.find()) {
                String wildcard = wildcardPatternMatch.group();
                validDomainsStrBuilder.append(wildcard);
                validDomainsStrBuilder.append("\n");
            }
        }
        return validDomainsStrBuilder.toString();
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

    public static String getValidKnoxUrl(String url) {
        // Knox seems invalidate a domain if the prefix does not contain any letters.
        // We will programmatically prefix domains such as 123.test.com, but not t123.test.com

        // If we have a wildcard, skip and pattern compiling / matching
        if (url.contains("*")) {
            return url;
        }

        // Otherwise...

        // Grab the prefix
        String prefix = url.split("\\Q.\\E")[0];
        // Regex: must contain a letter (excl wildcards)
        final Matcher prefix_valid = knox_valid_r.matcher(prefix);

        // If we don't have any letters in the prefix
        // Add a wildcard prefix as a safety net
        return (prefix_valid.find() ? "" : "*") + url;

    }

}