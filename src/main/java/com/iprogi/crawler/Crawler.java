package com.iprogi.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author: Dmitriy E. Nosov <br>
 * @date: 18.04.15 18:11 <br>
 * @description: <br>
 */
public class Crawler {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
    /**
     * Default file name with domains list
     */
    private static final String DEFAULT_FILENAME = "100DomainsForCrawling.txt";
    /**
     * Default protocol for crawling
     */
    public static final String DEFAULT_PROTOCOL = "http";
    /**
     * Default amounts of threads for ForkJoinPool
     */
    private static final int DEFAULT_MAX_THREADS = 128;
    /**
     * Max amount of links on the one site
     */
    public static final int MAX_LINK_AMOUNT = 100;
    /**
     * Timeout within request page
     */
    public static final int TIMEOUT_REQUEST_PAGE_SECONDS = 1;

    private final ForkJoinPool pool;

    private final Map<String, Domain> domainsMap = new ConcurrentHashMap<>();

    private final Set<String> linksSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final boolean allowRobots;

    /**
     * Used for statistics
     */
    private static final long t0 = System.currentTimeMillis();


    public Crawler(final boolean allowRobots, final int maxThreads) {
        this.allowRobots = allowRobots;
        this.pool = new ForkJoinPool(maxThreads);

        logger.info("");
        logger.info("Start Crawler v.0.1 with [{}] threads", maxThreads);
        logger.info("-------------------------");
    }

    /**
     * Start crawling of web pages
     */
    public void start() {
        logger.info("Start crawling...");
        for (final Map.Entry<String, Domain> entry : domainsMap.entrySet()) {
            pool.submit(new Task(entry.getKey(), entry.getValue(), linksSet));
        }

        pool.awaitQuiescence(15, TimeUnit.MINUTES);
        final long min = System.currentTimeMillis() - t0;
        logger.info("Crawling is complete! Elapsed time [{}] minute(s) ([{}] second(s))", min / (1000 * 60), min / 1000);
        logger.info("");
        logger.info("Total:");
        logger.info("");
        logger.info("");

        for (Map.Entry<String, Domain> entry : domainsMap.entrySet()) {
            logger.info("[{}] the number of visited internal links [{}]", entry.getKey(), entry.getValue().getCounts());
            logger.info("");
            for (String link : entry.getValue().getInnerLinkSet()) {
                logger.info("   Link [{}]", link);
            }
            logger.info("");
            logger.info("");
            logger.info("");
        }

        try {
            final StringBuilder result = new StringBuilder();
            for (String link : linksSet) {
                result.append(link).append("\r\n");
            }
            final Path path = Paths.get("./result.txt");
            Files.write(path, result.toString().getBytes(StandardCharsets.UTF_8));
            logger.info("Write result to file [{}], elapsed time [{}] minute(s) ([{}] second(s))",
                    path.toAbsolutePath().toString(), min / (1000 * 60), min / 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load domains url(s) from default file 100DomainsForCrawling.txt
     */
    public void loadDefaultConfig() throws URISyntaxException {
        final File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        int lineNum = 1;
        try {
            final String filename = jarFile.toPath().getParent() + File.separator + DEFAULT_FILENAME;
            logger.info("Get default domains from file [{}]", filename);

            for (String domain : Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8)) {
                try {
//                    domain = domain.startsWith("www.") ? domain : "www." + domain;
                    final URL url = new URL(DEFAULT_PROTOCOL, domain, "");
                    logger.info(" Getting url {}.[{}]", lineNum, url.toString());

                    domainsMap.putIfAbsent(domain, Domain.getInstance(domain, allowRobots ? allowRobotstxt(domain) : new HashSet<>()));
                    lineNum++;
                } catch (MalformedURLException e) {
                    logger.error("Error load URL [{}] from file, line [{}]", domain, lineNum);
                }
            }
        } catch (Exception e) {
            logger.error("Exception " + e, e);
        } finally {
            logger.info("Loaded [{}] urls...", domainsMap.size());
        }
    }

    /**
     * Get disallow links from robots.txt
     */
    private Set<String> allowRobotstxt(final String domain) throws MalformedURLException {
        final Set<String> disallowLinkSet = new HashSet<>();
        final URL robotstxt = new URL(DEFAULT_PROTOCOL, domain, "/robots.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(robotstxt.openStream()))) {
            String l;
            while ((l = reader.readLine()) != null) {
                if (l.indexOf("Disallow:") == 0) {
                    String disallow = l.trim().substring("Disallow:".length());
                    int commentIndex = disallow.indexOf("#");
                    if (commentIndex != -1) {
                        disallow = disallow.substring(0, commentIndex);
                    }
                    disallow = disallow.trim();
                    logger.info("  Disallow [{}]", disallow);
                    disallowLinkSet.add(disallow);
                }
            }
        } catch (IOException e) {
            logger.info("  robots.txt is not loaded! Case [{}]", e.getMessage());
        }
        return disallowLinkSet;
    }

    public static void main(String[] args) throws URISyntaxException {
        final int maxThreads = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MAX_THREADS;
        final String allowRobots = args.length > 1 && args[1] != null ? args[0] : null;

        final Crawler cr = new Crawler(allowRobots != null && !"".equals(allowRobots), maxThreads);
        cr.loadDefaultConfig();
        cr.start();
    }
}
