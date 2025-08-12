package com.zj.xjm.service.novel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OptimizedNovelCrawler {

    // 配置参数
    private static final String NOVEL_HOME_PAGE = "https://owlook.com.cn/chapter?url=http://www.xbiquzw.net/15_15271/&novels_name=吞噬星空2起源大陆";
    private static final String OUTPUT_FILE = "吞噬星空2起源大陆.txt";
    private static final String NOVEL_NAME = "《吞噬星空2起源大陆》";
    private static final int DELAY_MS = 2500; // 请求延迟(毫秒)
    private static final int TIMEOUT_MS = 15000; // 请求超时时间
    private static final int FLUSH_FREQUENCY = 10; // 每多少章刷新一次缓冲区
    private static final int MAX_RETRY = 3; // 最大重试次数

    private static final String NOVEL_PRE_URL = "http://www.xbiquzw.net/15_15271/";

    // 用户代理轮换池
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
    };

    public static void main(String[] args) {
        try {
            System.out.println("开始爬取《吞噬星空2起源大陆》...");
            crawlNovel();
            System.out.println("小说爬取完成，保存至: " + OUTPUT_FILE);
        } catch (Exception e) {
            System.err.println("爬取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void crawlNovel() throws Exception {
        // 1. 获取目录页
        Document indexPage = getDocumentWithRetry(NOVEL_HOME_PAGE, MAX_RETRY);

        // 2. 提取所有章节链接
        List<Chapter> chapters = extractChapterLinks(indexPage);
        System.out.println("共找到 " + chapters.size() + " 个章节");

        // 3. 检查文件是否存在并读取最后记录的位置
        int startIndex = 0;
        File outputFile = new File(OUTPUT_FILE);
        boolean fileExists = outputFile.exists();

        if (fileExists) {
            // 读取文件最后一行获取上次的进度
            String lastLine = Files.readAllLines(outputFile.toPath()).stream()
                    .reduce((first, second) -> second)
                    .orElse("0");

            try {
                startIndex = Integer.parseInt(lastLine.trim()) + 1;
                System.out.println("检测到上次进度，将从第 " + (startIndex + 1) + " 章开始");
            } catch (NumberFormatException e) {
                System.out.println("无法解析进度记录，将从第一章开始");
                startIndex = 0;
            }
        }

        // 4. 创建或追加文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, fileExists))) {
            // 如果文件不存在，写入小说标题
            if (!fileExists) {
                writer.write(NOVEL_NAME);
                writer.newLine();
                writer.newLine();
            }

            // 5. 从记录的进度开始遍历章节
            for (int i = startIndex; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                System.out.printf("正在处理 (%d/%d): %s%n", i + 1, chapters.size(), chapter.title);

                // 获取章节内容（带重试机制）
                String content = getChapterContentWithRetry(chapter.url, MAX_RETRY);

                // 写入章节信息
                writeChapter(writer, i + 1, chapter.title, content);

                // 写入当前进度（i的值）
                writer.write(String.valueOf(i));
                // 定期刷新缓冲区
                if ((i + 1) % FLUSH_FREQUENCY == 0) {
                    writer.flush();
                    logMemoryUsage(); // 记录内存使用情况
                }

                // 随机延迟防止被封
                TimeUnit.MILLISECONDS.sleep(DELAY_MS + (int) (Math.random() * 1000));
            }
        }
    }

    // 修改writeChapter方法，确保不写入进度标记行
    private static void writeChapter2(BufferedWriter writer, int chapterNum, String title, String content) throws IOException {
        writer.write("第" + chapterNum + "章 " + title);
        writer.newLine();
        writer.write(content);
        writer.newLine();
        writer.newLine();
    }

    // 带重试的获取文档方法
    private static Document getDocumentWithRetry(String url, int maxRetry) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < maxRetry; i++) {
            try {
                String userAgent = USER_AGENTS[i % USER_AGENTS.length];
                return Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(TIMEOUT_MS)
                        .get();
            } catch (IOException e) {
                lastException = e;
                System.err.printf("请求失败(尝试 %d/%d)，%d秒后重试...%n",
                        i + 1, maxRetry, (i + 1) * 2);
                try {
                    TimeUnit.SECONDS.sleep((i + 1) * 2);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("请求被中断", ie);
                }
            }
        }
        throw new IOException("达到最大重试次数", lastException);
    }

    // 带重试的获取章节内容方法
    private static String getChapterContentWithRetry(String url, int maxRetry) {
        for (int i = 0; i < maxRetry; i++) {
            try {
                Document doc = getDocumentWithRetry(url, 1);
                Element content = doc.selectFirst("div#content, .chapter-content, .content");
                return content != null ? cleanContent(content.html()) : "【内容获取失败】";
            } catch (Exception e) {
                System.err.printf("获取章节内容失败(尝试 %d/%d): %s%n",
                        i + 1, maxRetry, e.getMessage());
                if (i == maxRetry - 1) {
                    return "【内容获取失败，已达最大重试次数】";
                }
            }
        }
        return "【内容获取失败】";
    }

    // 清理章节内容
    private static String cleanContent(String html) {
        return html.replaceAll("<br>", "\n")
                .replaceAll("&nbsp;", " ")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+\n", "\n")
                .replaceAll("\n\\s+", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    // 写入章节内容（带分段写入）
    private static void writeChapter(BufferedWriter writer, int index, String title, String content) throws IOException {
        try {
            writer.newLine();
            writer.newLine();
            // 写入章节标题
            writer.write(String.format("第%d章 %s%n%n", index, title));

            // 分段写入内容（防止超大章节内存问题）
            int chunkSize = 50000; // 每5万字一段
            for (int i = 0; i < content.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, content.length());
                writer.write(content.substring(i, end));
            }

            writer.newLine();
            writer.write("==============================");
            writer.newLine();
        } catch (IOException e) {
            System.err.println("写入失败，尝试刷新缓冲区后重试...");
            writer.flush();
            // 简单重试一次
            writer.write(String.format("第%d章 %s%n%n", index, title));
            writer.write(content);
            writer.newLine();
            writer.write("==============================");
            writer.newLine();
            writer.newLine();
        }
    }

    // 提取章节链接
    private static List<Chapter> extractChapterLinks(Document indexPage) {
        Elements links = indexPage.select("a[href]");
        return initElementList(links);
    }

    private static List<Chapter> initElementList(Elements links) {
        List<Chapter> chapters = new ArrayList<>();
        for (int i1 = 0; i1 < links.size(); i1++) {
            Element link = links.get(i1);
            String href = link.attr("href");
            String title = link.text().trim();
            if (!href.isEmpty() && !title.isEmpty() && href.contains("html")&& href.length()<15) {
                chapters.add(new Chapter(title, NOVEL_PRE_URL + href));
            }
        }
//        chapters.clear();
        return chapters;
    }

    // 记录内存使用情况
    private static void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        System.out.printf("[内存使用] %dMB/%dMB%n", usedMB, maxMB);
    }

    // 章节信息类
    static class Chapter {
        String title;
        String url;

        Chapter(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    // 测试1
}