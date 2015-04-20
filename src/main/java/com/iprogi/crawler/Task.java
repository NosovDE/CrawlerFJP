package com.iprogi.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

/**
 * @author: Dmitriy E. Nosov <br>
 * @date: 18.04.15 22:35 <br>
 * @description: <br>
 */
public class Task extends RecursiveAction {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    /**
     * Link of current site
     */
    private String link;
    /**
     * Statistics of the site
     */
    private Domain domain;

    private Set<String> globalSet;

    public Task(String link, Domain domain, Set<String> globalSet) {
        this.link = link;
        this.domain = domain;
        this.globalSet = globalSet;
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {

        if (domain.getCounts().get() < Crawler.MAX_LINK_AMOUNT) {
            final List<Task> actions = new ArrayList<>();
            try {
                /*
                URL uriLink = new URL(link);
                Parser parser = new Parser(uriLink.openConnection());
                NodeList list = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));

                if (!domain.getInnerLinkSet().contains(link)) {
                    domain.getInnerLinkSet().add(link);
                    domain.getCounts().getAndIncrement();
                }

                for (int i = 0; i < list.size(); i++) {
                    LinkTag extracted = (LinkTag) list.elementAt(i);

                    if (!extracted.extractLink().isEmpty()
                            && extracted.isHTTPLink()
                            && !extracted.extractLink().contains(".jpg")
                            && !extracted.extractLink().contains(".pdf")
                            && !extracted.extractLink().contains(".png")
                            && !extracted.extractLink().contains(".gif")
                            && !extracted.extractLink().contains(".dmg")
                            && !extracted.extractLink().contains(".exe")
                            && !domain.getInnerLinkSet().contains(extracted.extractLink())) {

                        actions.add(new Task(extracted.extractLink(), domain));
                    }
                }
                */

                final String response = Jsoup.connect(new URL(Crawler.DEFAULT_PROTOCOL, domain.getUrl(), "").toString())
                        .userAgent("Mozilla")
                        .timeout(15000)
                        .followRedirects(true) //to follow redirects
                        .execute()
                        .url()
                        .toExternalForm();

                final Document doc = Jsoup.connect(response).userAgent("Mozilla").timeout(15000).get();

                if (domain.getCounts().get() < Crawler.MAX_LINK_AMOUNT) {
                    domain.getCounts().getAndIncrement();

                    for (Element findElement : doc.select("a[href]")) {
                        final String findLink = findElement.attr("abs:href");

                        if (isInnerLink(findLink)) {
                            actions.add(new Task(findLink, domain, globalSet));
                            domain.addInnerLink(findLink);
                        }

                        if (!findLink.isEmpty() && !globalSet.contains(findLink)) {
                            globalSet.add(findLink);
                        }
                    }

                    TimeUnit.SECONDS.sleep(Crawler.TIMEOUT_REQUEST_PAGE_SECONDS);

                    if (!actions.isEmpty()) {
                        //invoke recursively
                        invokeAll(actions);
                    }
                }
                //  logger.info("After timeout [{}]", link);
            } catch (Exception e) {
                // logger.info("ERROR! Skip parse page [{}] [{}]", link, e.getMessage());
            }
        } else {
            // logger.info("Skip {}", domain);
        }
    }

    /**
     * Detect is link is inner
     */
    private boolean isInnerLink(final String link) {
        return !link.isEmpty()
                && !link.contains(".jpg")
                && !link.contains(".jpeg")
                && !link.contains(".png")
                && !link.contains(".gif")
                && !link.contains(".exe")
                && !link.contains("#")
                && !link.contains("mailto:")
                && link.contains(domain.getUrl())
                && !domain.isExist(link);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Task{");
        sb.append("link='").append(link).append('\'');
        sb.append(", domain=").append(domain);
        sb.append('}');
        return sb.toString();
    }
}
