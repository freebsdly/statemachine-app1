package com.trina.visiontask;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StringTest {

    @Test
    public void test1() {
        String str = "document/itoutsource.cz1731/qinhuajun_test/vision系统_x20.pptx";
        String fileNamePrefix = null;
        int i = str.lastIndexOf('.');
        if (i > 0) {
            fileNamePrefix = str.substring(0, i);
        } else {
            fileNamePrefix = str;
        }
        System.out.println(fileNamePrefix);
    }

    @Test
    public void test2() {
        String path = "";
        Path document = Paths.get("document", path);
        Path resolve = document.resolve("vision系统_x20.pptx");
        System.out.println(resolve);
    }

    @Test
    public void test3() {
        int currentPriority = 0;
        int nextPriority = calculatePriority(currentPriority);
        System.out.println("-------->: " + nextPriority);

        currentPriority = nextPriority;
        nextPriority = calculatePriority(currentPriority);
        System.out.println("-------->: " + nextPriority);

        currentPriority = nextPriority;
        nextPriority = calculatePriority(currentPriority);
        System.out.println("-------->: " + nextPriority);
    }

    public int calculatePriority(int currentPriority) {
        // 使用偏移量确保从0开始也能递增
        double exponent = (currentPriority + 1) / 3.0;
        int newPriority = (int) Math.round(Math.pow(2, exponent));
        // 确保结果至少比输入值大（除非达到上限）
        return Math.min(Math.max(newPriority, currentPriority + 1), 10);
    }
}
