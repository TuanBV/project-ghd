package com.example.mcprice.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * Kiem tra URL truoc khi crawl de giam rui ro SSRF: chi cho domain da khai bao, chan
 * localhost/private IP, khong cho scheme khac http/https.
 */
public final class SafeUrlValidator {

    private SafeUrlValidator() {
    }

    public static boolean isSafeToCrawl(String url, List<String> allowedDomains) {
        if (url == null || url.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        if (!isAllowedDomain(host, allowedDomains)) {
            return false;
        }
        return !isPrivateOrLoopback(host);
    }

    private static boolean isAllowedDomain(String host, List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return false;
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        return allowedDomains.stream().anyMatch(domain -> {
            String lowerDomain = domain.toLowerCase(Locale.ROOT);
            return lowerHost.equals(lowerDomain) || lowerHost.endsWith("." + lowerDomain);
        });
    }

    private static boolean isPrivateOrLoopback(String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost")) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()
                    || address.isMulticastAddress();
        } catch (UnknownHostException e) {
            return true;
        }
    }
}
