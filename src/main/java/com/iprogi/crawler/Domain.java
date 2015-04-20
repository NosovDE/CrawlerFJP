package com.iprogi.crawler;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: Dmitriy E. Nosov <br>
 * @date: 18.04.15 18:58 <br>
 * @description: <br>
 */
public class Domain {

    private final AtomicInteger counts;
    private final String url;
    private final Set<String> excludeLink;
    private final Set<String> innerLinkSet;

    private Domain(final AtomicInteger counts,final String url, final Set<String> excludeLink, final Set<String> innerLinkSet) {
        this.counts = counts;
        this.url = url;
        this.excludeLink = excludeLink;
        this.innerLinkSet = innerLinkSet;
    }

    public static Domain getInstance(final String url, final Set<String> disallowLinks) {
        return new Domain(new AtomicInteger(0), url, disallowLinks, new HashSet<>());
    }

    public void addInnerLink(final String link) {
        innerLinkSet.add(link);
    }

    public boolean isExist(final String link) {
        return innerLinkSet.contains(link);
    }

    public AtomicInteger getCounts() {
        return counts;
    }

    public Set<String> getExcludeLink() {
        return excludeLink;
    }

    public Set<String> getInnerLinkSet() {
        return innerLinkSet;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "Domain{" +
                "counts=" + counts +
                ", url='" + url + '\'' +
                ", excludeLink=" + excludeLink +
                ", innerLinkSet=" + innerLinkSet +
                '}';
    }
}
