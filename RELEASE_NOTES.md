# 0.2.0 — Experimental GIF picker and transport fix

YouTube 20.40.45용 GIF 검색·선택 UI와 실제 네트워크 경계 후킹을 추가했습니다. 0.1.0에서 마커가 그대로 게시되는 기기 테스트 실패를 확인했으므로 이전 버전은 사용하지 마세요.

- 입력란 오른쪽 **GIF** 버튼, Tenor 검색 하단 패널, 애니메이션 미리보기와 제거 버튼.
- 선택 정보는 초안 상태에 저장하며, JSON 또는 protobuf의 실제 업로드 직전에 첨부.
- 확인한 전송 경로에서 수동 `[[gif:…]]` 마커 차단. 직접 입력하지 마세요.
- 번들 로딩, 본문 변환 23개 assertion, Tenor 파서·전송 경계 테스트, 실제 APK 패치·DEX 컴파일·서명 통과.
- GitHub Packages 자격 증명이나 Android SDK 없이 빌드하는 스크립트와 CI 포함.

**새 버전의 UI 동작 및 GIF 게시 성공은 기기에서 미검증입니다.** JSON은 desktop_gifs와 같은 tenorId 필드를 사용하며, protobuf field 32의 의미는 아직 추론입니다. 안정 버전이나 작동 보장 릴리스가 아닙니다. 원본 APK로 새로 패치하고 **GIF comments**를 선택하세요.

Manager 원격 소스:

```text
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
```

URL 등록이 어려우면 `.rvp`를 다운로드해 저장소에서 가져오세요. 자세한 설치·사용 방법과 한계는 README 및 docs/VALIDATION.md에 있습니다. 원본 APK나 패치된 APK는 배포하지 않습니다.
