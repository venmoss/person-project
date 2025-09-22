import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @TempDir
    private Path tempDir;

    private Path originalFile;
    private Path testFile;
    private Path resultFile;

    private static final String FULL_MATCH_TEXT = "SimHash是一种用于文本相似度计算的哈希算法，" +
            "它可以将高维的文本特征映射到低维的哈希值，适用于大规模文本查重场景。";
    private static final String HIGH_SIMILAR_TEXT = "SimHash是一种用于文本相似性计算的哈希方法，" +
            "能够把高维的文本特征转换到低维的哈希值，适合大规模文本查重应用。";
    private static final String LOW_SIMILAR_TEXT = "SimHash 算法可用于文本相似度检测，不过这里混入了部分 Java 语言的内容，" +
            "Java 是一种跨平台的编程语言，具有面向对象、分布式、安全性等特点，广泛应用于企业级应用开发。";
    private static final String VERY_LOW_SIMILAR_TEXT = "Python 是一种解释型编程语言，" +
            "语法简洁清晰，适合数据分析和人工智能开发，和 SimHash 没有直接关联。";
    private static final String MEDIUM_SIMILAR_TEXT = "SimHash 是哈希算法，可用于文本相似度计算，" +
            "Java 具有面向对象特点，适用于企业级应用开发，两者有部分领域重叠。";
    private static final String EMPTY_TEXT = "";
    private static final String SPECIAL_CHAR_TEXT = "SimHash!@#$%^&*()_+哈希123算法，test测试文本";

    @BeforeEach
    void setUp() throws IOException {
        originalFile = tempDir.resolve("D:/桌面/3123004390/test/test_orig.txt");
        testFile = tempDir.resolve("D:/桌面/3123004390/test/test_test.txt");
        resultFile = tempDir.resolve("D:/桌面/3123004390/test/test_result.txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(originalFile)) Files.delete(originalFile);
        if (Files.exists(testFile)) Files.delete(testFile);
        if (Files.exists(resultFile)) Files.delete(resultFile);
    }

    @Test
    void testSegmentText() {
        List<String> words1 = Main.segmentText(FULL_MATCH_TEXT);
        assertFalse(words1.isEmpty());
        assertTrue(words1.contains("simhash"));
        assertTrue(words1.contains("哈希"));
        assertTrue(words1.contains("算法"));

        List<String> words2 = Main.segmentText(SPECIAL_CHAR_TEXT);
        assertTrue(words2.contains("simhash"));
        assertTrue(words2.contains("哈希"));
        assertTrue(words2.contains("123"));
        assertTrue(words2.contains("算法"));
        assertTrue(words2.contains("test"));
        assertFalse(words2.contains("!"));

        List<String> words3 = Main.segmentText(EMPTY_TEXT);
        assertTrue(words3.isEmpty());

        List<String> words4 = Main.segmentText(null);
        assertTrue(words4.isEmpty());
    }

    @Test
    void testCalculateSimHash() {
        long hash1 = Main.calculateSimHash(FULL_MATCH_TEXT);
        long hash2 = Main.calculateSimHash(FULL_MATCH_TEXT);
        assertEquals(hash1, hash2);

        long hash3 = Main.calculateSimHash(HIGH_SIMILAR_TEXT);
        assertNotEquals(hash1, hash3);

        long hash4 = Main.calculateSimHash(LOW_SIMILAR_TEXT);
        assertNotEquals(hash1, hash4);

        long hash5 = Main.calculateSimHash(EMPTY_TEXT);
        long hash6 = Main.calculateSimHash(null);
        assertEquals(0, hash5);
        assertEquals(0, hash6);

        long hash7 = Main.calculateSimHash(SPECIAL_CHAR_TEXT);
        assertNotEquals(0, hash7);
    }

    @Test
    void testCalculateHammingDistance() {
        long hash1 = Main.calculateSimHash(FULL_MATCH_TEXT);
        assertEquals(0, Main.calculateHammingDistance(hash1, hash1));

        long hash2 = Main.calculateSimHash(HIGH_SIMILAR_TEXT);
        int distance1 = Main.calculateHammingDistance(hash1, hash2);
        assertTrue(distance1 > 0 && distance1 <= 64);

        long hash3 = Main.calculateSimHash(LOW_SIMILAR_TEXT);
        int distance2 = Main.calculateHammingDistance(hash1, hash3);
        assertTrue(distance2 > 0 && distance2 <= 64);

        long hash4 = Main.calculateSimHash(EMPTY_TEXT);
        int distance3 = Main.calculateHammingDistance(hash1, hash4);
        assertTrue(distance3 > 0 && distance3 <= 64);
    }

    @Test
    void testValidateCommandLineArgs() {
        String[] validArgs = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        assertTrue(Main.validateCommandLineArgs(validArgs));

        assertFalse(Main.validateCommandLineArgs(new String[0]));
        assertFalse(Main.validateCommandLineArgs(new String[]{"onlyOne"}));
        assertFalse(Main.validateCommandLineArgs(new String[]{"arg1", "arg2"}));
        assertFalse(Main.validateCommandLineArgs(new String[]{"arg1", "arg2", "arg3", "arg4"}));

        assertFalse(Main.validateCommandLineArgs(new String[]{"", testFile.toString(), resultFile.toString()}));
        assertFalse(Main.validateCommandLineArgs(new String[]{"   ", testFile.toString(), resultFile.toString()}));
        assertFalse(Main.validateCommandLineArgs(new String[]{originalFile.toString(), "", resultFile.toString()}));
        assertFalse(Main.validateCommandLineArgs(new String[]{originalFile.toString(), testFile.toString(), null}));
    }

    @Test
    void testFullFlow_FullMatch() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, FULL_MATCH_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        assertTrue(Files.exists(resultFile));
        String resultContent = readFromFile(resultFile);
        assertTrue(resultContent.contains("海明距离: 0"));
        assertTrue(resultContent.contains("文本相似度: 100.00%"));
        assertTrue(resultContent.contains("高度相似，存在严重抄袭嫌疑"));
    }

    @Test
    void testFullFlow_HighSimilarity() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, HIGH_SIMILAR_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        String resultContent = readFromFile(resultFile);
        int hammingDistance = extractHammingDistance(resultContent);
        double similarity = 1.0 - (double) hammingDistance / 64;
        assertTrue(similarity >= 0.8);
        assertTrue(resultContent.contains("高度相似，存在严重抄袭嫌疑"));
    }

    @Test
    void testFullFlow_MediumSimilarity() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, MEDIUM_SIMILAR_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        String resultContent = readFromFile(resultFile);
        int hammingDistance = extractHammingDistance(resultContent);
        double similarity = 1.0 - (double) hammingDistance / 64;
        assertTrue(similarity >= 0.5 && similarity < 0.8);
        assertTrue(resultContent.contains("中度相似，存在部分抄袭可能"));
    }

    @Test
    void testFullFlow_LowSimilarity() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, LOW_SIMILAR_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        String resultContent = readFromFile(resultFile);
        int hammingDistance = extractHammingDistance(resultContent);
        double similarity = 1.0 - (double) hammingDistance / 64;
        assertTrue(similarity >= 0.3 && similarity < 0.5);
        assertTrue(resultContent.contains("轻度相似，可能存在少量借鉴"));
    }

    @Test
    void testFullFlow_VeryLowSimilarity() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, VERY_LOW_SIMILAR_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        String resultContent = readFromFile(resultFile);
        int hammingDistance = extractHammingDistance(resultContent);
        double similarity = 1.0 - (double) hammingDistance / 64;
        assertTrue(similarity < 0.3);
        assertTrue(resultContent.contains("相似度较低，抄袭可能性小"));
    }

    @Test
    void testFullFlow_EmptyFile() throws IOException {
        writeToFile(originalFile, FULL_MATCH_TEXT);
        writeToFile(testFile, EMPTY_TEXT);

        String[] args = {originalFile.toString(), testFile.toString(), resultFile.toString()};
        Main.main(args);

        String resultContent = readFromFile(resultFile);
        assertTrue(resultContent.contains("海明距离:"));
    }

    @Test
    void testFullFlow_FileNotFound() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        String[] args = {nonExistentFile.toString(), testFile.toString(), resultFile.toString()};

        CaptureSystemOutput.captureOutput(() -> {
            Main.main(args);
        });

        String errorOutput = CaptureSystemOutput.getErrorOutput();
        assertTrue(errorOutput.contains("错误: 文件未找到 - " + nonExistentFile));
        assertFalse(Files.exists(resultFile));
    }

    @Test
    void testRunTestCases() {
        CaptureSystemOutput.captureOutput(() -> {
            Main.main(new String[]{"test"});
        });

        String output = CaptureSystemOutput.getStandardOutput();
        assertTrue(output.contains("开始运行测试用例"));
        assertTrue(output.contains("所有测试用例执行完毕"));
    }

    private void writeToFile(Path file, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private String readFromFile(Path file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private int extractHammingDistance(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("海明距离: ")) {
                String distanceStr = line.substring("海明距离: ".length()).trim();
                return Integer.parseInt(distanceStr);
            }
        }
        fail("未在结果中找到海明距离");
        return -1;
    }

    static class CaptureSystemOutput {
        private static final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        private static final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        private static final PrintStream originalOut = System.out;
        private static final PrintStream originalErr = System.err;

        static void captureOutput(Runnable runnable) {
            try {
                System.setOut(new PrintStream(outBuffer));
                System.setErr(new PrintStream(errBuffer));
                runnable.run();
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }

        static String getStandardOutput() {
            return outBuffer.toString().trim();
        }

        static String getErrorOutput() {
            return errBuffer.toString().trim();
        }
    }
}