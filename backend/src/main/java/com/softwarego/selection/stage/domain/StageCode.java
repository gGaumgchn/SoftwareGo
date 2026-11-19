package com.softwarego.selection.stage.domain;

/**
 * 固定阶段编码与流程顺序。名称、编码、顺序不可由接口新增、删除或修改。
 */
public enum StageCode {

    BRIEFING("集中宣讲", 1),
    OPEN_DAY("实训室开放日", 2),
    ROUND_1("第一轮录取", 3),
    ROUND_2("第二轮录取", 4),
    ROUND_3("第三轮录取", 5),
    ADJUSTMENT("统筹调剂", 6),
    ENROLLMENT("正式入组", 7);

    private final String stageName;
    private final int sortNo;

    StageCode(String stageName, int sortNo) {
        this.stageName = stageName;
        this.sortNo = sortNo;
    }

    public String getStageName() {
        return stageName;
    }

    public int getSortNo() {
        return sortNo;
    }

    public static StageCode from(String value) {
        if (value == null) {
            return null;
        }
        for (StageCode stageCode : values()) {
            if (stageCode.name().equals(value)) {
                return stageCode;
            }
        }
        return null;
    }
}
