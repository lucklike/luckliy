package com.luckyframework.httpclient.proxy.mock.utils;

import java.util.Random;

public class NameGenerator {


    // 定义常见的中文姓氏，包括复姓
    private static final String[] CHINESE_SURNAMES = {"王", "李", "张", "刘", "陈", "杨", "黄", "赵", "吴", "周", "司马", "欧阳", "诸葛", "上官"};

    // 定义常见的中文名字，包括单字名和双字名
    private static final String[] CHINESE_GIVEN_NAMES = {"伟", "芳", "娜", "敏", "静", "涛", "军", "洋", "鹏", "丽", "志强", "建国", "小明", "海峰"};

    // 定义常见的英文名（名字）
    private static final String[] ENGLISH_FIRST_NAMES = {"James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda", "David", "Elizabeth"};

    // 定义常见的英文姓氏
    private static final String[] ENGLISH_LAST_NAMES = {"Smith", "Johnson", "Brown", "Williams", "Jones", "Miller", "Davis", "Garcia", "Rodriguez", "Martinez"};

    // 创建一个随机数生成器
    private static final Random RANDOM = new Random();

    /**
     * 生成一个随机的中文姓名，支持单姓、复姓，以及单字名、双字名
     *
     * @return 组合了随机姓氏和随机名字的字符串
     */
    public static String generateChineseName() {
        // 随机选择一个姓氏
        String surname = CHINESE_SURNAMES[RANDOM.nextInt(CHINESE_SURNAMES.length)];

        // 随机选择一个名字
        String givenName = CHINESE_GIVEN_NAMES[RANDOM.nextInt(CHINESE_GIVEN_NAMES.length)];

        return surname + givenName;
    }

    /**
     * 生成一个随机的英文姓名
     *
     * @return 由名字和姓氏组成的字符串
     */
    public static String generateEnglishName() {
        // 随机选择一个名字
        String firstName = ENGLISH_FIRST_NAMES[RANDOM.nextInt(ENGLISH_FIRST_NAMES.length)];

        // 随机选择一个姓氏
        String lastName = ENGLISH_LAST_NAMES[RANDOM.nextInt(ENGLISH_LAST_NAMES.length)];

        return firstName + " " + lastName;
    }

    public static void main(String[] args) {
        // 生成并打印一个随机的中文姓名
        System.out.println("Random Chinese Name: " + generateChineseName());

        // 生成并打印一个随机的英文姓名
        System.out.println("Random English Name: " + generateEnglishName());
    }
}

