/**
 * 홈 화면 활동 사진 마퀴. (이슈 #146)
 *
 * <p>사진 자체는 file 도메인이 보관한다. 여기서는 순서와 노출 여부만 들고 있고,
 * 파일 연결·해제는 {@code FileConnectionService} 를 거친다.
 *
 * <p>담당: R
 * <p>패키지 = 소유권. 자기 패키지 밖의 파일은 수정하지 않고 소유자에게 요청한다.
 */
package com.getit.domain.setting.photo;
