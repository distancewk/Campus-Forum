package com.campus.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {

    /** 总用户数 */
    private Long totalUsers;

    /** 总帖子数 */
    private Long totalPosts;

    /** 总评论数 */
    private Long totalComments;

    /** 今日新增用户 */
    private Long todayNewUsers;

    /** 今日新增帖子 */
    private Long todayNewPosts;

    /** 7日活跃用户数 */
    private Long activeUsers;

    /** 各板块帖子数统计 */
    private List<BoardStat> boardStats;
}
