package com.getit.domain.file.repository;

import com.getit.domain.file.entity.FileStatus;

/**
 * 연결 가능 여부를 판단하는 데 필요한 값만 담은 조회 결과.
 *
 * <p>엔티티가 아니라 이 형태로 읽는 이유가 있다. 엔티티로 읽으면 영속성 컨텍스트에 올라가고,
 * 뒤이은 {@code FOR UPDATE} 조회는 DB 를 다시 읽어도 <b>1차 캐시에 있던 인스턴스를</b>
 * 그대로 돌려준다. 그러면 락을 잡고 하는 재확인이 낡은 상태를 보게 되어 의미가 없어진다.
 * 프로젝션은 컨텍스트에 올라가지 않으므로 그 뒤의 엔티티 조회가 진짜 첫 조회가 된다.
 *
 * @param size 클라이언트가 신고한 크기
 */
public record FileConnectionView(Long id, String storedKey, Long size, FileStatus status) {
}
