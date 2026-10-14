# 0.1.0 — Experimental

YouTube 20.40.45용 커스텀 GIF 댓글 패치의 첫 실험용 릴리스입니다.

- `GIF comments (Tenor ID)` 패치를 포함하는 Manager용 `.rvp` 번들.
- 새 댓글·답글의 `[[gif:NUMERIC_TENOR_ID]]`를 제거하고 protobuf field 32에 ID를 전달.
- 번들 로딩, 본문 변환 테스트 20개 assertion, 실제 APK 패치·DEX 컴파일·서명 통과.
- GitHub Packages 자격 증명이나 Android SDK 없이 빌드하는 스크립트와 CI 포함.

**실제 Android 기기의 GIF 게시 성공은 미검증입니다.** Field 32가 Tenor ID로 처리된다는 판단은 최신 APK의 요청 구조에서 추론한 것이며 서버 수용 여부를 확인하지 못했습니다. 안정 버전이나 작동 보장 릴리스가 아닙니다. GIF 검색 버튼 없이 숫자 ID를 입력하는 방식입니다.

Manager 원격 소스:

```text
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
```

URL 등록이 어려우면 `.rvp`를 다운로드해 저장소에서 가져오세요. 자세한 설치·사용 방법과 한계는 README 및 docs/VALIDATION.md에 있습니다. 원본 APK나 패치된 APK는 배포하지 않습니다.
