package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.SecurityUtils;
import com.lims.dao.mapper.SampleMapper;
import com.lims.model.entity.Sample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Issue #80: Sample lifecycle service — barcode, receive, split, store, dispose.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SampleService {

    private final SampleMapper sampleMapper;

    @Transactional(rollbackFor = Exception.class)
    public Sample receive(Sample sample) {
        sample.setBarcode(generateBarcode());
        sample.setSampleStatus("RECEIVED");
        sample.setReceivedAt(LocalDateTime.now());
        sample.setCustodianId(SecurityUtils.getCurrentUserId());
        sampleMapper.insert(sample);
        log.info("Sample received: barcode={}, requestId={}", sample.getBarcode(), sample.getRequestId());
        return sample;
    }

    public Sample getById(String id) {
        Sample sample = sampleMapper.selectById(id);
        if (sample == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        return sample;
    }

    public List<Sample> getByRequestId(String requestId) {
        return sampleMapper.selectList(
                new LambdaQueryWrapper<Sample>()
                        .eq(Sample::getRequestId, requestId)
                        .orderByAsc(Sample::getReceivedAt));
    }

    @Transactional(rollbackFor = Exception.class)
    public Sample split(String parentSampleId, Sample child) {
        Sample parent = getById(parentSampleId);
        if (!"RECEIVED".equals(parent.getSampleStatus()) && !"STORED".equals(parent.getSampleStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Parent sample must be RECEIVED or STORED to split");
        }
        child.setParentSampleId(parentSampleId);
        child.setRequestId(parent.getRequestId());
        child.setBarcode(generateBarcode());
        child.setSampleStatus("RECEIVED");
        child.setReceivedAt(LocalDateTime.now());
        child.setCustodianId(SecurityUtils.getCurrentUserId());
        sampleMapper.insert(child);
        parent.setSampleStatus("SPLIT");
        sampleMapper.updateById(parent);
        log.info("Sample split: parent={}, child={}", parentSampleId, child.getId());
        return child;
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispose(String sampleId, String disposalMethod) {
        Sample sample = getById(sampleId);
        sample.setSampleStatus("DISPOSED");
        sample.setDisposedAt(LocalDateTime.now());
        sample.setDisposalMethod(disposalMethod);
        sampleMapper.updateById(sample);
        log.info("Sample disposed: id={}, method={}", sampleId, disposalMethod);
    }

    private String generateBarcode() {
        return "SMP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (int) (Math.random() * 10000);
    }
}
