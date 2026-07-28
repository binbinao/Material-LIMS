package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.SecurityUtils;
import com.lims.dao.mapper.TestResultMapper;
import com.lims.model.entity.TestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Issue #79: Structured test result service with specification judgment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestResultService {

    private final TestResultMapper testResultMapper;

    @Transactional(rollbackFor = Exception.class)
    public TestResult create(TestResult result) {
        result.setEnteredBy(SecurityUtils.getCurrentUserId());
        result.setEnteredAt(LocalDateTime.now());
        result.setStatus("ENTERED");
        result.setJudgment(autoJudge(result));
        testResultMapper.insert(result);
        log.info("Test result created: taskId={}, judgment={}", result.getAnalysisTaskId(), result.getJudgment());
        return result;
    }

    public List<TestResult> getByTaskId(String taskId) {
        return testResultMapper.selectList(
                new LambdaQueryWrapper<TestResult>()
                        .eq(TestResult::getAnalysisTaskId, taskId)
                        .orderByDesc(TestResult::getEnteredAt));
    }

    public List<TestResult> getByRequestId(String requestId) {
        return testResultMapper.selectList(
                new LambdaQueryWrapper<TestResult>()
                        .eq(TestResult::getRequestId, requestId)
                        .orderByAsc(TestResult::getItemId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(String resultId, String reviewDecision) {
        TestResult result = testResultMapper.selectById(resultId);
        if (result == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        if (!"ENTERED".equals(result.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Only ENTERED results can be reviewed");
        }
        result.setReviewedBy(SecurityUtils.getCurrentUserId());
        result.setReviewedAt(LocalDateTime.now());
        result.setStatus("REVIEWED".equals(reviewDecision) ? "REVIEWED" : "REJECTED");
        testResultMapper.updateById(result);
        log.info("Test result reviewed: resultId={}, decision={}", resultId, reviewDecision);
    }

    /**
     * Auto-judge PASS/FAIL/CONDITIONAL based on spec limits and entered value.
     */
    private String autoJudge(TestResult result) {
        if (result.getEnteredValue() == null) return "PENDING";
        BigDecimal value = result.getEnteredValue();
        BigDecimal lower = result.getSpecLower();
        BigDecimal upper = result.getSpecUpper();

        if (lower != null && upper != null) {
            if (value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0) return "PASS";
            return "FAIL";
        }
        if (lower != null && value.compareTo(lower) >= 0) return "PASS";
        if (upper != null && value.compareTo(upper) <= 0) return "PASS";
        if (lower != null || upper != null) return "FAIL";
        return "PENDING";
    }
}
