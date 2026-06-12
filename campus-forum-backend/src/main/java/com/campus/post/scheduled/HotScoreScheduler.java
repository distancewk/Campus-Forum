package com.campus.post.scheduled;

import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HotScoreScheduler {

    private final PostMapper postMapper;

    /**
     * 每 10 分钟执行一次，重新计算近 7 天帖子的热度分。
     * 热度公式：(like_count*3 + comment_count*2 + fav_count*1) / POWER(hours+2, 1.5)
     */
    @Scheduled(fixedRate = 600000)
    public void recalculateHotScore() {
        log.info("开始计算帖子热度分...");
        int updated = postMapper.recalculateHotScore();
        log.info("热度分计算完成，更新 {} 条帖子", updated);
    }
}
