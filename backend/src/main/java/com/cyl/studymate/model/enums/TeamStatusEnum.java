package com.cyl.studymate.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 队伍状态枚举
 */
public enum TeamStatusEnum {

    PUBLIC(0, "公开"),

    PRIVATE(1, "私有"),

    SECRET(2, "加密");

    /**
     * 状态值
     */
    private final int value;

    /**
     * 状态描述
     */
    private final String text;

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    /**
     * 获取所有状态值
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values())
                .map(TeamStatusEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 根据 value 获取对应枚举
     *
     * @param value
     * @return
     */
    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }

        for (TeamStatusEnum teamStatusEnum : TeamStatusEnum.values()) {
            if (Objects.equals(teamStatusEnum.getValue(), value)) {
                return teamStatusEnum;
            }
        }

        return null;
    }
}