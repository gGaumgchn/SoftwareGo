package com.softwarego.selection.announcement.service;

import com.softwarego.selection.announcement.domain.Announcement;
import com.softwarego.selection.announcement.dto.AnnouncementDetailResponse;
import com.softwarego.selection.announcement.dto.AnnouncementSummaryResponse;
import com.softwarego.selection.announcement.mapper.AnnouncementMapper;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.common.PageResult;
import com.softwarego.selection.user.domain.User;
import com.softwarego.selection.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final int SUMMARY_MAX_LENGTH = 80;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final AnnouncementMapper announcementMapper;
    private final UserService userService;

    public PageResult<AnnouncementSummaryResponse> list(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "分页参数不合法");
        }

        long offset = (long) (page - 1) * pageSize;
        List<AnnouncementSummaryResponse> list = announcementMapper.findPage(offset, pageSize).stream()
                .map(announcement -> new AnnouncementSummaryResponse(
                        announcement.getId(),
                        announcement.getTitle(),
                        summarize(announcement.getContent()),
                        announcement.getPublishedAt()))
                .toList();
        long total = announcementMapper.count();
        return PageResult.of(list, total, page, pageSize);
    }

    public AnnouncementDetailResponse detail(Long id) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        User publisher = userService.findById(announcement.getPublisherId());
        String publisherName = publisher == null ? null : publisher.getName();
        return new AnnouncementDetailResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getPublisherId(),
                publisherName,
                announcement.getPublishedAt());
    }

    /**
     * 仅供阶段公告发布事务调用；公告没有独立的新增接口。
     */
    public Announcement create(String title, String content, Long publisherId, LocalDateTime publishedAt) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setPublisherId(publisherId);
        announcement.setPublishedAt(publishedAt);
        announcementMapper.insert(announcement);
        return announcement;
    }

    private String summarize(String content) {
        if (content == null) {
            return "";
        }
        String normalized = WHITESPACE.matcher(content).replaceAll(" ").trim();
        if (normalized.length() <= SUMMARY_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_MAX_LENGTH) + "……";
    }
}
