# 0.4.0 — One repository, ReVanced and Morphe bundles

같은 공개 저장소에서 양쪽을 지원하도록 공통 확장과 플랫폼별 패치를 빌드합니다.

| 도구 | YouTube APK | 번들 |
| --- | --- | --- |
| ReVanced Manager | 20.40.45 | gif-comments-0.4.0.rvp |
| Morphe Manager | 21.20.405 | morphe-gif-comments-0.4.0.mpp |

Morphe는 공식 stable patches 1.47.0의 기본 비실험 버전을 기준으로 했습니다. 전송 URL accessor를 실제 APK에서 식별하고, UI 리소스는 이름으로 조회합니다. GIF 검색·숫자 ID·최근 기록·즐겨찾기·GIF-only 답글·활성 입력란 아이콘을 유지합니다.

Morphe용 URL:
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/morphe-patches-bundle.json

기존 ReVanced용 URL:
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json

각 Manager에서 맞는 URL 또는 번들을 가져오고 원본 APK에 공식 패치들과 **GIF comments**를 선택해 패치하세요. 저장소 페이지 URL만 Morphe에 넣으면 기본 ReVanced JSON을 찾을 수 있으므로 위 Morphe 전용 주소를 사용합니다.

양쪽 빌드·테스트·APK 패치·컴파일·서명 통과. Morphe 공식 기본 패치와 GIF 패치의 총 97개 조합도 실패 없이 패치·리빌드·서명했습니다. **Morphe에서 실제 GIF 게시와 새 UI는 기기 미검증입니다.** APK·서명 개인 키는 배포하지 않습니다.
