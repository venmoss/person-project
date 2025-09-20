import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // 分词正则表达式，匹配中文字符、字母和数字
    private static final Pattern WORD_PATTERN = Pattern.compile("[\u4e00-\u9fa5a-zA-Z0-9]+");

    // SimHash的位数，通常使用64位
    private static final int HASH_BITS = 64;

    public static void main(String[] args) {
        // 如果传入"test"作为参数，运行测试函数
        if (args.length == 1 && "test".equalsIgnoreCase(args[0])) {
            runTestCases();
            return;
        }

        // 验证命令行参数
        if (!validateCommandLineArgs(args)) {
            return;
        }

        String originalFilePath = args[0];
        String testFilePath = args[1];
        String resultFilePath = args[2];

        // 记录开始时间
        long startTime = System.currentTimeMillis();
        Date startDate = new Date(startTime);

        try {
            // 验证文件是否存在
            validateFileExists(originalFilePath);
            validateFileExists(testFilePath);

            // 读取文件内容（自动检测编码）
            String originalText = readFileWithEncodingDetection(originalFilePath);
            String testText = readFileWithEncodingDetection(testFilePath);

            // 计算SimHash值
            long originalHash = calculateSimHash(originalText);
            long testHash = calculateSimHash(testText);

            // 计算海明距离
            int hammingDistance = calculateHammingDistance(originalHash, testHash);

            // 计算相似度 (1 - 海明距离 / 哈希位数)
            double similarity = 1.0 - (double) hammingDistance / HASH_BITS;

            // 记录结束时间
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            // 写入结果（追加模式，保留历史结果）
            writeResult(resultFilePath, hammingDistance, similarity,
                    startDate, new Date(endTime), elapsedTime, originalFilePath, testFilePath);

            System.out.println("查重完成，结果已追加至: " + resultFilePath);
            System.out.println("相似度: " + String.format("%.2f%%", similarity * 100));
            System.out.println("耗时: " + elapsedTime + " 毫秒");

        } catch (FileNotFoundException e) {
            System.err.println("错误: 文件未找到 - " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("错误: 不支持的文件编码 - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("处理文件时发生错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("发生意外错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证命令行参数是否规范
     */
    private static boolean validateCommandLineArgs(String[] args) {
        if (args.length != 3) {
            System.err.println("命令行参数不规范！");
            System.err.println("正确使用方法: java SimHashPlagiarismChecker <原文文件路径> <待检测文件路径> <结果文件路径>");
            System.err.println("运行测试: java SimHashPlagiarismChecker test");
            return false;
        }

        // 检查文件路径是否为空
        if (args[0] == null || args[0].trim().isEmpty()) {
            System.err.println("错误: 原文文件路径不能为空");
            return false;
        }

        if (args[1] == null || args[1].trim().isEmpty()) {
            System.err.println("错误: 待检测文件路径不能为空");
            return false;
        }

        if (args[2] == null || args[2].trim().isEmpty()) {
            System.err.println("错误: 结果文件路径不能为空");
            return false;
        }

        return true;
    }

    /**
     * 验证文件是否存在
     */
    private static void validateFileExists(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        }
        if (!file.isFile()) {
            throw new FileNotFoundException(filePath + " 不是一个有效的文件");
        }
    }

    /**
     * 读取文件内容并尝试检测编码
     */
    private static String readFileWithEncodingDetection(String filePath) throws IOException {
        // 尝试常见编码读取文件
        Charset[] charsetsToTry = {
                StandardCharsets.UTF_8,
                Charset.forName("GBK"),
                Charset.forName("GB2312"),
                Charset.forName("ISO-8859-1"),
                Charset.forName("UTF-16")
        };

        for (Charset charset : charsetsToTry) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                return new String(bytes, charset);
            } catch (Exception e) {
                // 尝试下一种编码
                continue;
            }
        }

        // 如果所有编码尝试都失败，抛出异常
        throw new UnsupportedEncodingException("无法识别文件 " + filePath + " 的编码格式，请尝试使用UTF-8编码保存文件");
    }

    /**
     * 计算文本的SimHash值
     */
    private static long calculateSimHash(String text) {
        // 处理空文本情况
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // 1. 分词
        List<String> words = segmentText(text);

        // 处理没有分词结果的情况
        if (words.isEmpty()) {
            return 0;
        }

        // 2. 计算每个词的权重（这里简单使用词频作为权重）
        Map<String, Integer> wordFrequency = calculateWordFrequency(words);

        // 3. 初始化向量
        int[] vector = new int[HASH_BITS];

        // 4. 计算每个词的哈希值并更新向量
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            String word = entry.getKey();
            int weight = entry.getValue();

            // 计算词的哈希值
            long wordHash = calculateWordHash(word);

            // 更新向量
            for (int i = 0; i < HASH_BITS; i++) {
                // 右移并与1按位与，获取当前位的值
                long bit = (wordHash >> (HASH_BITS - 1 - i)) & 1;
                vector[i] += (bit == 1) ? weight : -weight;
            }
        }

        // 5. 生成SimHash值
        long simHash = 0;
        for (int i = 0; i < HASH_BITS; i++) {
            if (vector[i] > 0) {
                simHash |= (1L << (HASH_BITS - 1 - i));
            }
        }

        return simHash;
    }

    /**
     * 对文本进行分词
     */
    private static List<String> segmentText(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return words;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String word = matcher.group().toLowerCase(); // 转为小写，忽略大小写差异
            words.add(word);
        }

        return words;
    }

    /**
     * 计算词频
     */
    private static Map<String, Integer> calculateWordFrequency(List<String> words) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String word : words) {
            frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
        }

        return frequencyMap;
    }

    /**
     * 计算单个词的哈希值
     */
    private static long calculateWordHash(String word) {
        if (word == null || word.isEmpty()) {
            return 0;
        }

        // 使用DJB2哈希算法
        long hash = 5381;
        for (int i = 0; i < word.length(); i++) {
            hash = ((hash << 5) + hash) + word.charAt(i);
        }

        // 确保返回64位哈希值
        return hash & 0xFFFFFFFFFFFFFFFFL;
    }

    /**
     * 计算两个哈希值之间的海明距离
     */
    private static int calculateHammingDistance(long hash1, long hash2) {
        // 异或运算，相同位为0，不同位为1
        long xor = hash1 ^ hash2;

        // 计算异或结果中1的个数，即海明距离
        int distance = 0;
        while (xor != 0) {
            distance++;
            xor &= xor - 1; // 清除最低位的1
        }

        return distance;
    }

    /**
     * 将查重结果写入文件（追加模式，保留历史结果）
     */
    private static void writeResult(String resultFilePath, int hammingDistance,
                                    double similarity, Date startTime, Date endTime, long elapsedTime,
                                    String originalFilePath, String testFilePath)
            throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean fileExists = new File(resultFilePath).exists();

        // 确保结果文件所在目录存在
        File resultFile = new File(resultFilePath);
        if (!resultFile.getParentFile().exists()) {
            resultFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(resultFilePath, true), StandardCharsets.UTF_8))) {

            // 如果文件已存在，添加分隔线区分不同批次的查重结果
            if (fileExists) {
                writer.write("\n========================================\n");
                writer.write("========== 新的查重记录开始 ==========\n");
                writer.write("========================================\n\n");
            }

            writer.write("====== 文本查重结果 ======\n");
            writer.write("检测开始时间: " + sdf.format(startTime) + "\n");
            writer.write("检测完成时间: " + sdf.format(endTime) + "\n");
            writer.write("查重总耗时: " + elapsedTime + " 毫秒\n\n");

            // 增加参与比对的文件信息，方便区分不同的查重记录
            writer.write("参与比对的文件:\n");
            writer.write("  原文文件: " + originalFilePath + "\n");
            writer.write("  待检测文件: " + testFilePath + "\n\n");

            writer.write("海明距离: " + hammingDistance + "\n");
            writer.write("文本相似度: " + String.format("%.2f%%", similarity * 100) + "\n\n");

            // 根据相似度给出判断
            if (similarity >= 0.8) {
                writer.write("判断结果: 高度相似，存在严重抄袭嫌疑\n");
            } else if (similarity >= 0.5) {
                writer.write("判断结果: 中度相似，存在部分抄袭可能\n");
            } else if (similarity >= 0.3) {
                writer.write("判断结果: 轻度相似，可能存在少量借鉴\n");
            } else {
                writer.write("判断结果: 相似度较低，抄袭可能性小\n");
            }
        }
    }

    /**
     * 运行测试用例
     */
    public static void runTestCases() {
        System.out.println("开始运行测试用例...\n");

        // 测试用例数组，每个元素是一个包含3个字符串的数组：[原文路径, 待检测路径, 结果路径]
        String[][] testCases = {
                // 正常测试用例
                {"testfiles/original_utf8.txt", "testfiles/copy_utf8.txt", "testresults/result1.txt"},
                {"testfiles/original_utf8.txt", "testfiles/modified_utf8.txt", "testresults/result1.txt"},
                {"testfiles/short_text1.txt", "testfiles/short_text2.txt", "testresults/result1.txt"},
                {"testfiles/long_text1.txt", "testfiles/long_text2.txt", "testresults/result1.txt"},
                {"testfiles/empty.txt", "testfiles/empty.txt", "testresults/result1.txt"},
                {"testfiles/empty.txt", "testfiles/original_utf8.txt", "testresults/result1.txt"},

                // 异常测试用例 - 文件不存在
                {"testfiles/not_exist1.txt", "testfiles/original_utf8.txt", "testresults/result3.txt"},

                // 异常测试用例 - 命令行参数
                {"only_one_arg"},
                {"two", "args"},
                {"five", "args", "are", "too", "many"}
        };

        // 执行每个测试用例
        for (int i = 0; i < testCases.length; i++) {
            System.out.println("测试用例 " + (i + 1) + "/" + testCases.length + ":");
            try {
                // 调用主逻辑处理测试用例
                main(testCases[i]);
            } catch (Exception e) {
                System.err.println("测试用例 " + (i + 1) + " 发生异常: " + e.getMessage());
            }
            System.out.println("----------------------------------------");
        }

        System.out.println("所有测试用例执行完毕");
    }
}
