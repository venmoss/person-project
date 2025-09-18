import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PaperCheck {
    public static void main(String[] args) {
        BufferedReader consoleReader = null;
        try {
            // 创建控制台输入流
            consoleReader = new BufferedReader(new InputStreamReader(System.in));

            // 提示用户输入文件路径
            System.out.println("请输入原文文件的绝对路径：");
            String originalPath = consoleReader.readLine().trim();

            System.out.println("请输入抄袭版论文文件的绝对路径：");
            String plagiarizedPath = consoleReader.readLine().trim();

            System.out.println("请输入结果文件的绝对路径：");
            String outputPath = consoleReader.readLine().trim();

            // 读取文件内容
            String originalText = readFile(originalPath);
            String plagiarizedText = readFile(plagiarizedPath);

            // 检查文件是否读取成功
            if (originalText.isEmpty() || plagiarizedText.isEmpty()) {
                System.out.println("警告：原文或抄袭版论文内容为空，可能导致结果不准确");
            }

            // 计算重复率（余弦相似度）
            double similarity = calculateSimilarity(originalText, plagiarizedText);

            // 写入结果到输出文件
            writeResult(outputPath, similarity);

            System.out.printf("查重完成，重复率为：%.2f%%\n", similarity * 100);
            System.out.println("结果已保存至：" + outputPath);

        } catch (IOException e) {
            System.out.println("操作异常：" + e.getMessage());
        } finally {
            // 关闭控制台输入流
            if (consoleReader != null) {
                try {
                    consoleReader.close();
                } catch (IOException e) {
                    System.out.println("关闭输入流失败：" + e.getMessage());
                }
            }
        }
    }

    // 读取文件内容，支持中文编码
    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        // 使用UTF-8编码读取文件，避免中文乱码
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 预处理：移除标点符号，保留中文字符和字母数字
                line = line.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ")
                        .replaceAll("\\s+", " "); // 多个空格合并为一个
                content.append(line).append(" ");
            }
        }
        return content.toString().trim();
    }

    // 计算两篇文本的余弦相似度（重复率）
    private static double calculateSimilarity(String text1, String text2) {
        // 中文分词处理：这里简单按字符拆分，更准确的可以使用分词库
        char[] chars1 = text1.toCharArray();
        char[] chars2 = text2.toCharArray();

        // 统计词频（对于中文按字符统计）
        Map<Character, Integer> freqMap1 = new HashMap<>();
        Map<Character, Integer> freqMap2 = new HashMap<>();

        for (char c : chars1) {
            if (c != ' ') { // 跳过空格
                freqMap1.put(c, freqMap1.getOrDefault(c, 0) + 1);
            }
        }
        for (char c : chars2) {
            if (c != ' ') { // 跳过空格
                freqMap2.put(c, freqMap2.getOrDefault(c, 0) + 1);
            }
        }

        // 获取所有唯一字符（使用HashSet确保可修改）
        Set<Character> allChars = new HashSet<>();
        allChars.addAll(freqMap1.keySet());
        allChars.addAll(freqMap2.keySet());

        // 计算向量点积、模长
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (char c : allChars) {
            int freq1 = freqMap1.getOrDefault(c, 0);
            int freq2 = freqMap2.getOrDefault(c, 0);

            dotProduct += freq1 * freq2;
            norm1 += Math.pow(freq1, 2);
            norm2 += Math.pow(freq2, 2);
        }

        // 计算余弦相似度
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // 写入重复率到输出文件
    private static void writeResult(String filePath, double similarity) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(String.format("论文重复率: %.2f%%", similarity * 100));
            writer.newLine();
            writer.write("注：值越接近100%，表示相似度越高");
        }
    }
}
