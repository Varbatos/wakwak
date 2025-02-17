package com.social.bottle.service.implement;

import com.social.bottle.dto.request.BottleCommentRequestDto;
import com.social.bottle.dto.request.DeleteBottleCommentRequestDto;
import com.social.bottle.dto.response.BottleCommentResponseDto;
import com.social.bottle.dto.response.DeleteBottleCommentResponseDto;
import com.social.bottle.dto.response.GetBottleCommentResponseDto;
import com.social.bottle.entity.Bottle;
import com.social.bottle.entity.BottleComment;
import com.social.bottle.repository.BottleCommentRepository;
import com.social.bottle.repository.BottleRepository;
import com.social.bottle.service.BottleCommentService;
import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BottleCommentServiceImplement implements BottleCommentService {

    private final BottleRepository bottleRepository;
    private final BottleCommentRepository bottleCommentRepository;
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;

    @Override
    @Transactional
    public BottleCommentResponseDto addComment(String token, BottleCommentRequestDto requestDto) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [댓글 작성 실패] 인증 오류");
            return BottleCommentResponseDto.unauthorized();
        }

        if (requestDto.getContent().length() > 255) {
            log.warn("❌ [댓글 작성 실패] 댓글 길이 초과");
            return BottleCommentResponseDto.invalidContentLength();
        }

        Bottle bottle = bottleRepository.findById(requestDto.getBottleId()).orElse(null);
        if (bottle == null) {
            log.warn("❌ [댓글 작성 실패] 존재하지 않는 bottle_id={}", requestDto.getBottleId());
            return BottleCommentResponseDto.bottleNotFound();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("❌ [댓글 작성 실패] 존재하지 않는 userId={}", userId);
            return BottleCommentResponseDto.unauthorized();
        }

        BottleComment parentComment = null;
        if (requestDto.getParentId() != null) {
            parentComment = bottleCommentRepository.findById(requestDto.getParentId()).orElse(null);
            if (parentComment == null) {
                log.warn("❌ [댓글 작성 실패] 존재하지 않는 parent_id={}", requestDto.getParentId());
                return BottleCommentResponseDto.parentCommentNotFound();
            }
        }

        BottleComment newComment = BottleComment.builder()
                .bottle(bottle)
                .user(user)
                .parentId(parentComment)
                .content(requestDto.getContent())
                .createdAt(Instant.now())
                .isDeleted(0) // ✅ 삭제 여부 기본값 false
                .build();
        bottleCommentRepository.save(newComment);

        log.info("✅ [댓글 작성 성공] commentId={}, bottleId={}, userId={}", newComment.getCommentId(), requestDto.getBottleId(), userId);
        return BottleCommentResponseDto.success(BottleCommentResponseDto.Data.builder()
                .commentId(newComment.getCommentId())
                .bottleId(requestDto.getBottleId())
                .userId(userId)
                .parentId(requestDto.getParentId())
                .content(requestDto.getContent())
                .createdAt(newComment.getCreatedAt())
                .isDeleted(false)
                .build());
    }

    @Override
    @Transactional
    public GetBottleCommentResponseDto getComments(String token, Integer bottleId) {
        log.info("📩 [댓글 조회 요청] bottle_id: {}", bottleId);

        // 1. 토큰 검증
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [댓글 조회 실패] 인증 오류 - 유효하지 않은 토큰");
            return GetBottleCommentResponseDto.authRequired();
        }
        log.info("🔑 [JWT PARSE] 인증된 user_id: {}", userId);

        // 2. 필수 파라미터 및 유리병 존재 체크
        if (bottleId == null) {
            log.warn("⚠️ [유효성 검사 실패] bottle_id가 없음.");
            return GetBottleCommentResponseDto.missingBottleId();
        }
        if (!bottleRepository.existsById(bottleId)) {
            log.warn("❌ [BOTTLE NOT FOUND] 존재하지 않는 bottle_id: {}", bottleId);
            return GetBottleCommentResponseDto.bottleNotFound();
        }

        // 3. 해당 유리병의 모든 댓글 조회
        List<BottleComment> comments = bottleCommentRepository.findByBottle_BottleId(bottleId);
        if (comments.isEmpty()) {
            log.info("📭 [댓글 없음] bottle_id: {} 에 대한 댓글이 존재하지 않음.", bottleId);
            return GetBottleCommentResponseDto.noComments();
        }

        log.info("📋 [댓글 수집] 총 {}개의 댓글 조회됨.", comments.size());

        // 4. 댓글들을 트리 구조로 정리
        Map<Integer, List<BottleComment>> childrenMap = new HashMap<>();
        List<BottleComment> topLevelComments = new ArrayList<>();

        for (BottleComment comment : comments) {
            if (comment.getParentId() == null) {
                topLevelComments.add(comment);
            } else {
                int parentId = comment.getParentId().getCommentId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
            }
        }

        log.info("📂 [댓글 트리 구조 정리] 최상위 댓글 수: {}, 자식 댓글 그룹 수: {}",
                topLevelComments.size(), childrenMap.size());

        // 5. 같은 레벨 내에서는 commentId 오름차순 정렬
        topLevelComments.sort(Comparator.comparingInt(BottleComment::getCommentId));
        for (List<BottleComment> childList : childrenMap.values()) {
            childList.sort(Comparator.comparingInt(BottleComment::getCommentId));
        }

        // 6. 전위순회 방식으로 댓글을 순서대로 담기
        List<GetBottleCommentResponseDto.Data> dataList = new ArrayList<>();
        traverseComments(topLevelComments, childrenMap, dataList, 0);

        log.info("✅ [댓글 조회 성공] bottle_id: {}, 총 댓글 반환 수: {}", bottleId, dataList.size());
        return GetBottleCommentResponseDto.success(dataList);
    }

    /**
     * 재귀적으로 댓글 트리를 전위순회하여 DTO 리스트에 추가하는 헬퍼 메서드
     *
     * @param comments   현재 레벨의 댓글 리스트
     * @param childrenMap 부모 commentId로 그룹화된 자식 댓글 맵
     * @param result     최종 DTO 리스트
     * @param depth      현재 댓글의 깊이 (top-level: 0)
     */
    private void traverseComments(List<BottleComment> comments,
                                  Map<Integer, List<BottleComment>> childrenMap,
                                  List<GetBottleCommentResponseDto.Data> result,
                                  int depth) {
        for (BottleComment comment : comments) {
            log.debug("🔍 [댓글 변환] comment_id: {}, depth: {}", comment.getCommentId(), depth);

            // 댓글 엔티티를 DTO로 변환 (각 댓글의 depth 포함)
            GetBottleCommentResponseDto.Data dto = GetBottleCommentResponseDto.Data.builder()
                    .commentId(comment.getCommentId())
                    .bottleId(comment.getBottle().getBottleId())
                    .userId(comment.getUser().getUserId())
                    .nickname(comment.getUser().getNickname())
                    .parentId(comment.getParentId() != null ? comment.getParentId().getCommentId() : null)
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .isDeleted(comment.getIsDeleted() != 0)
                    .depth(depth)
                    .build();

            result.add(dto);

            // 현재 댓글의 자식 댓글이 있다면 재귀적으로 순회
            List<BottleComment> children = childrenMap.get(comment.getCommentId());
            if (children != null) {
                log.debug("📌 [자식 댓글 있음] comment_id: {} → {}개의 자식 댓글 탐색", comment.getCommentId(), children.size());
                traverseComments(children, childrenMap, result, depth + 1);
            }
        }
    }


    @Transactional
    public DeleteBottleCommentResponseDto deleteComment(String token, DeleteBottleCommentRequestDto requestDto) {
        log.info("🗑️ [DELETE COMMENT] 요청 수신 - bottle_id: {}, comment_id: {}",
                requestDto.getBottleId(), requestDto.getCommentId());

        // 1. JWT 토큰에서 user_id 추출
        Integer userId = jwtProvider.getUserIdFromToken(token);
        log.info("🔑 [JWT PARSE] 인증된 user_id: {}", userId);

        // 2. 필수 필드 검증
        if (requestDto.getBottleId() == null || requestDto.getCommentId() == null) {
            log.warn("⚠️ [VALIDATION FAILED] 필수 필드 누락 - bottle_id: {}, comment_id: {}",
                    requestDto.getBottleId(), requestDto.getCommentId());
            return DeleteBottleCommentResponseDto.missingFields();
        }

        // 3. commentId로 타겟 댓글 조회 및 soft-delete (is_deleted = 1) 처리
        Optional<BottleComment> targetOpt = bottleCommentRepository.findByCommentIdAndBottle_BottleId(
                requestDto.getCommentId(), requestDto.getBottleId());
        if (targetOpt.isEmpty()) {
            log.warn("❌ [COMMENT NOT FOUND] 존재하지 않는 댓글 - bottle_id: {}, comment_id: {}",
                    requestDto.getBottleId(), requestDto.getCommentId());
            return DeleteBottleCommentResponseDto.commentNotFound();
        }
        BottleComment target = targetOpt.get();
        Bottle bottle = target.getBottle();

        // 삭제 권한 확인 (유리병 작성자만 가능)
        if (!bottle.getUser().getUserId().equals(userId)) {
            log.warn("⛔ [ACCESS DENIED] 유저 {}가 bottle_id {}의 댓글을 삭제하려고 했지만 권한 없음.",
                    userId, requestDto.getBottleId());
            return DeleteBottleCommentResponseDto.notBottleOwner();
        }

        // 우선 타겟 댓글 soft-delete 처리
        target.setIsDeleted(1);
        bottleCommentRepository.save(target);
        log.info("🗑️ [SOFT DELETE] 댓글 {} is_deleted = 1 로 업데이트됨.", target.getCommentId());

        // 4. 해당 유리병의 모든 댓글을 조회하고, 트리 형태(부모->자식 매핑)로 구성
        List<BottleComment> allComments = bottleCommentRepository.findByBottle_BottleId(requestDto.getBottleId());
        if (allComments.isEmpty()) {
            log.info("📭 [댓글 없음] bottle_id: {} 에 대한 댓글이 존재하지 않음.", requestDto.getBottleId());
            return DeleteBottleCommentResponseDto.noComments();
        }
        Map<Integer, List<BottleComment>> childrenMap = new HashMap<>();
        for (BottleComment comment : allComments) {
            if (comment.getParentId() != null) {
                Integer parentId = comment.getParentId().getCommentId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
            }
        }

        // 5. 타겟 댓글부터 시작하여, 자손들이 모두 soft-delete 상태(is_deleted == 1)인지 확인하고,
        //    바텀업 방식으로 hard-delete할 대상을 찾음.
        List<BottleComment> deletionList = new ArrayList<>();
        if (isEligibleForDeletion(target, childrenMap)) {
            // 자손을 바텀업 순서(leaf부터 부모 순)로 수집
            collectDeletableSubtree(target, childrenMap, deletionList);
            // 마지막에 타겟 댓글을 추가
            deletionList.add(target);
            // hard-delete (한 번에 삭제)
            bottleCommentRepository.deleteAll(deletionList);
            log.info("🗑️ [HARD DELETE] 타겟 댓글 {} 및 자손 {} 개 삭제됨.",
                    target.getCommentId(), deletionList.size() - 1);

            // 6. 삭제 성공시, 부모쪽으로 올라가서 부모 댓글도 삭제 대상인지 재귀적으로 확인 및 삭제
            processParentDeletion(target.getParentId(), childrenMap);
        } else {
            log.info("📌 [DELETE SKIPPED] 타겟 댓글 {} 의 자손 중 삭제되지 않은 댓글이 존재하여 hard-delete 수행하지 않음.",
                    target.getCommentId());
        }

        return DeleteBottleCommentResponseDto.success(target.getCommentId());
    }

    /**
     * 해당 댓글 및 그 자손이 모두 soft-delete 상태(is_deleted == 1)이면 true 반환
     */
    private boolean isEligibleForDeletion(BottleComment comment, Map<Integer, List<BottleComment>> childrenMap) {
        if (comment.getIsDeleted() != 1) {
            return false;
        }
        List<BottleComment> children = childrenMap.get(comment.getCommentId());
        if (children != null) {
            for (BottleComment child : children) {
                if (!isEligibleForDeletion(child, childrenMap)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 재귀적으로 해당 댓글의 자손을 바텀업 순서(하위 댓글부터, 자신의 하위 댓글들이 모두 추가된 후 자신을 추가)로 수집
     */
    private void collectDeletableSubtree(BottleComment comment,
                                         Map<Integer, List<BottleComment>> childrenMap,
                                         List<BottleComment> deletionList) {
        List<BottleComment> children = childrenMap.get(comment.getCommentId());
        if (children != null) {
            for (BottleComment child : children) {
                // 자식의 자손부터 재귀적으로 수집
                if (isEligibleForDeletion(child, childrenMap)) {
                    collectDeletableSubtree(child, childrenMap, deletionList);
                    deletionList.add(child);
                }
            }
        }
    }

    /**
     * 재귀적으로 부모쪽으로 올라가,
     * 부모 댓글의 모든 자식(자손)이 이미 삭제되었으면 해당 부모도 hard-delete 처리
     */
    private void processParentDeletion(BottleComment parent, Map<Integer, List<BottleComment>> childrenMap) {
        if (parent == null) {
            return;
        }
        // 부모가 soft-delete 상태이고, 부모의 자손(자식)이 모두 삭제 대상이면 hard-delete 가능
        if (parent.getIsDeleted() == 1 && isEligibleForDeletion(parent, childrenMap)) {
            bottleCommentRepository.delete(parent);
            log.info("🗑️ [PARENT HARD DELETE] 부모 댓글 {} 삭제됨.", parent.getCommentId());
            // 재귀적으로 상위 부모 처리
            processParentDeletion(parent.getParentId(), childrenMap);
        }
    }

}
