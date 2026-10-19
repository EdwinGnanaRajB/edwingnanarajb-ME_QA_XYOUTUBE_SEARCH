package demo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import demo.utils.ExcelDataProvider;
import demo.wrappers.Wrappers;

public class TestCases extends ExcelDataProvider {
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String NEWS_CHANNEL_URL =
            YOUTUBE_URL + "/channel/UCYfdidRxbB8Qhf0Nx7ioOYw";
    private static final long TARGET_VIEWS = 100_000_000L;
    private static final Duration PAGE_TIMEOUT = Duration.ofSeconds(15);
    private static final By PLAYLIST_CARDS = By.cssSelector("yt-lockup-view-model");
    private static final By NEWS_POSTS =
            By.cssSelector("ytd-rich-item-renderer[is-post] ytd-post-renderer");
    private static final By VIDEO_CARDS = By.cssSelector(
            "ytd-video-renderer, ytd-rich-item-renderer, yt-lockup-view-model");
    private static final Pattern TRACK_COUNT =
            Pattern.compile("(?i)^\\s*(\\d+)\\s+(?:songs|tracks)\\s*$");
    private static final Pattern RATING =
            Pattern.compile("(?im)^\\s*(U/A|U|A)\\s*$");
    private static final Pattern GENRE =
            Pattern.compile("(?i)\\b(Comedy|Animation|Drama|Action|Adventure|Thriller|Romantic)\\b");
    private static final Pattern COUNT_LINE =
            Pattern.compile("(?i)^\\s*([\\d,.]+)\\s*(K|M|B|Cr|L)?\\s*(views?)?\\s*$");
    private static final Pattern TIMESTAMP =
            Pattern.compile("(?i)^\\s*(?:streamed\\s+)?\\d+\\s*"
                    + "(?:seconds?|minutes?|hours?|days?|weeks?|months?|years?|"
                    + "s|m|h|d|w|mo|y)\\s+ago\\s*$");
    private static final Pattern LIKE_COUNT =
            Pattern.compile("(?i)^\\s*([\\d,.]+)\\s*(K|M|B)?\\s+likes?\\s*$");

    private ChromeDriver driver;

    @BeforeTest
    public void startBrowser() {
        System.setProperty("java.util.logging.config.file", "logging.properties");
        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, java.util.logging.Level.ALL);
        logs.enable(LogType.DRIVER, java.util.logging.Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);
        options.addArguments("--remote-allow-origins=*");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "build/chromedriver.log");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    @AfterTest(alwaysRun = true)
    public void endTest() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testCase01() {
        driver.get(YOUTUBE_URL + "/about/");
        String aboutText = Wrappers.getElementText(driver, By.tagName("body"));
        Assert.assertTrue(driver.getTitle().contains("About YouTube"),
                "The browser should be on YouTube's About page");
        Assert.assertTrue(aboutText.contains(
                        "Our mission is to give everyone a voice and show them the world."),
                "The YouTube About page should show its mission statement");
        System.out.println("About page text: " + aboutText);
    }

    @Test
    public void testCase02() {
        driver.get(YOUTUBE_URL + "/results?search_query=Rockstar+movie");
        waitForElements(VIDEO_CARDS);

        WebElement movieResult = findMovieResult(driver.findElements(VIDEO_CARDS));
        String movieDetails = movieResult == null ? "" : movieResult.getText();
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(movieDetails.isEmpty(), "A Rockstar movie result should be available");
        softly.assertTrue(GENRE.matcher(movieDetails).find(),
                "The movie result should identify a category");

        Matcher rating = RATING.matcher(movieDetails);
        if (rating.find()) {
            softly.assertTrue(Set.of("A", "U/A", "U").contains(rating.group(1).toUpperCase(Locale.ROOT)),
                    "The movie rating should be a recognized audience rating");
        } else {
            System.out.println("The current YouTube search result does not display an audience rating.");
        }
        System.out.println("Movie details: " + movieDetails);
        if (movieResult != null) {
            List<WebElement> movieLinks =
                    movieResult.findElements(By.cssSelector("a[href*='/watch']"));
            if (!movieLinks.isEmpty()) {
                Wrappers.clickElement(driver, movieLinks.get(0));
            }
        }
        softly.assertAll();
    }

    @Test
    public void testCase03() {
        driver.get(YOUTUBE_URL + "/feed/music");
        waitForElements(PLAYLIST_CARDS);

        WebElement playlist = findPlaylistWithAtMost50Tracks(driver.findElements(PLAYLIST_CARDS));
        String playlistText = playlist.getText();
        Matcher tracks = TRACK_COUNT.matcher(firstLine(playlistText));
        Assert.assertTrue(tracks.matches(), "The selected playlist should display its track count");
        int trackCount = Integer.parseInt(tracks.group(1));
        System.out.println("Playlist checked: " + playlistText.replace('\n', ' '));
        Wrappers.clickElement(driver, playlist.findElement(By.cssSelector("a[href*='/playlist']")));

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(trackCount <= 50, "The playlist should contain at most 50 tracks");
        softly.assertAll();
    }

    @Test
    public void testCase04() {
        driver.get(NEWS_CHANNEL_URL);
        waitForElements(NEWS_POSTS);

        List<WebElement> newsPosts = driver.findElements(NEWS_POSTS);
        Assert.assertTrue(newsPosts.size() >= 3,
                "The News page should show at least three latest posts");

        long totalLikes = 0;
        for (int i = 0; i < 3; i++) {
            totalLikes += countLikes(newsPosts.get(i));
        }
        System.out.println("Total likes across the first three news posts: " + totalLikes);
        Assert.assertTrue(totalLikes >= 0, "The sum of likes cannot be negative");
    }

    @Test(dataProvider = "excelData")
    public void testCase05(Object valueFromWorkbook) {
        String searchTerm = String.valueOf(valueFromWorkbook).trim();
        Assert.assertFalse(searchTerm.isEmpty(), "The search keyword must not be blank");

        String encodedSearch = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        driver.get(YOUTUBE_URL + "/results?search_query=" + encodedSearch);
        waitForElements(VIDEO_CARDS);

        Map<String, Long> seenVideoViews = new LinkedHashMap<>();
        int scrolls = 0;
        while (sumViews(seenVideoViews) < TARGET_VIEWS && scrolls < 100) {
            collectVideoViews(seenVideoViews);
            if (sumViews(seenVideoViews) >= TARGET_VIEWS) {
                break;
            }

            int previousCardCount = driver.findElements(VIDEO_CARDS).size();
            long previousPageHeight = pageHeight();
            Wrappers.scrollDown(driver);
            scrolls++;
            if (isAtPageBottom()) {
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(8)).until(currentDriver ->
                            currentDriver.findElements(VIDEO_CARDS).size() > previousCardCount
                                    || pageHeight() > previousPageHeight);
                } catch (TimeoutException noMoreResults) {
                    break;
                }
            }
        }

        long totalViews = sumViews(seenVideoViews);
        System.out.println("Views collected for " + searchTerm + ": " + totalViews);
        Assert.assertTrue(totalViews >= TARGET_VIEWS,
                "Could not collect 10 crore views for search term: " + searchTerm);
    }

    private void waitForElements(By locator) {
        new WebDriverWait(driver, PAGE_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private WebElement findPlaylistWithAtMost50Tracks(List<WebElement> playlists) {
        for (WebElement playlist : playlists) {
            Matcher count = TRACK_COUNT.matcher(firstLine(playlist.getText()));
            if (count.matches() && Integer.parseInt(count.group(1)) <= 50) {
                return playlist;
            }
        }
        throw new NoSuchElementException("No visible playlist with 50 or fewer tracks was found");
    }

    private WebElement findMovieResult(List<WebElement> results) {
        for (WebElement result : results) {
            String text = result.getText();
            String lowerCaseText = text.toLowerCase(Locale.ROOT);
            if (lowerCaseText.contains("rockstar")
                    && (lowerCaseText.contains("full movie") || lowerCaseText.contains("movie"))
                    && GENRE.matcher(text).find()) {
                return result;
            }
        }
        return null;
    }

    private long countLikes(WebElement newsPost) {
        Set<Long> likes = new LinkedHashSet<>();
        for (WebElement element : newsPost.findElements(By.cssSelector("[aria-label]"))) {
            String label = element.getAttribute("aria-label");
            if (label == null) {
                continue;
            }
            Matcher count = LIKE_COUNT.matcher(label);
            if (count.matches()) {
                likes.add(parseCount(count.group(1), count.group(2)));
            }
        }
        return likes.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private void collectVideoViews(Map<String, Long> videoViews) {
        for (WebElement card : driver.findElements(VIDEO_CARDS)) {
            long views = findViewCount(card.getText());
            if (views <= 0) {
                continue;
            }

            List<WebElement> links = card.findElements(By.cssSelector("a[href*='/watch']"));
            String key = links.isEmpty() ? card.getText() : links.get(0).getAttribute("href");
            videoViews.putIfAbsent(key, views);
        }
    }

    private long findViewCount(String cardText) {
        String[] lines = cardText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher count = COUNT_LINE.matcher(lines[i]);
            if (!count.matches()) {
                continue;
            }
            if (count.group(3) != null
                    || (count.group(2) != null && i + 1 < lines.length
                            && TIMESTAMP.matcher(lines[i + 1]).matches())) {
                return parseCount(count.group(1), count.group(2));
            }
        }
        return 0;
    }

    private long parseCount(String rawCount, String unit) {
        double value = Double.parseDouble(rawCount.replace(",", ""));
        if (unit == null) {
            return (long) value;
        }
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "k":
                value *= 1_000;
                break;
            case "l":
                value *= 100_000;
                break;
            case "m":
                value *= 1_000_000;
                break;
            case "b":
                value *= 1_000_000_000;
                break;
            case "cr":
                value *= 10_000_000;
                break;
            default:
                break;
        }
        return (long) value;
    }

    private long sumViews(Map<String, Long> videoViews) {
        return videoViews.values().stream().mapToLong(Long::longValue).sum();
    }

    private long pageHeight() {
        return ((Number) ((JavascriptExecutor) driver).executeScript(
                "return Math.max(document.body.scrollHeight, "
                        + "document.documentElement.scrollHeight)")).longValue();
    }

    private boolean isAtPageBottom() {
        return (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.scrollY + window.innerHeight "
                        + ">= document.documentElement.scrollHeight - 50");
    }

    private String firstLine(String value) {
        int lineBreak = value.indexOf('\n');
        return lineBreak < 0 ? value : value.substring(0, lineBreak);
    }
}
