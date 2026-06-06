package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.KnowledgeDocMapper;
import com.lims.model.entity.KnowledgeDoc;
import com.lims.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Knowledge Hub 文档服务。
 *
 * <p>支持 MANUAL（PDF/DOCX 等）和 VIDEO 两种分类，统一走 FileStorageService 上传，
 * MinIO 不可用时自动降级本地。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KnowledgeDocService {

    public static final String CATEGORY_MANUAL = "MANUAL";
    public static final String CATEGORY_VIDEO = "VIDEO";

    private final KnowledgeDocMapper mapper;
    private final FileStorageService fileStorageService;

    public Page<KnowledgeDoc> list(int page, int size, String category, String keyword) {
        LambdaQueryWrapper<KnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(KnowledgeDoc::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeDoc::getTitle, keyword);
        }
        wrapper.orderByDesc(KnowledgeDoc::getUpdatedAt);
        // page is 1-based (MyBatis-Plus convention)
        long current = page <= 0 ? 1 : page;
        return mapper.selectPage(new Page<>(current, size), wrapper);
    }

    public KnowledgeDoc getById(String id) {
        return mapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDoc upload(MultipartFile file, String title, String category, String description) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
        if (!CATEGORY_MANUAL.equals(category) && !CATEGORY_VIDEO.equals(category)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile("knowledge_", "_" + file.getOriginalFilename());
            file.transferTo(tmp.toFile());
            String url = fileStorageService.upload(tmp, "knowledge/" + category.toLowerCase());

            KnowledgeDoc doc = new KnowledgeDoc();
            doc.setTitle(title != null ? title : file.getOriginalFilename());
            doc.setCategory(category);
            doc.setFileUrl(url);
            doc.setFileSize(file.getSize());
            doc.setDescription(description);
            mapper.insert(doc);
            log.info("Uploaded knowledge doc {} ({} bytes) -> {}", doc.getTitle(), doc.getFileSize(), url);
            return doc;
        } catch (IOException e) {
            log.error("Knowledge upload failed", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDoc updateMeta(String id, String title, String description) {
        KnowledgeDoc existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (title != null) existing.setTitle(title);
        if (description != null) existing.setDescription(description);
        mapper.updateById(existing);
        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        mapper.deleteById(id);
    }
}
