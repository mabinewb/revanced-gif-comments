# 0.3.0 — Tenor ID lookup, recents, favorites and GIF-only replies

사용자가 0.2.1의 버튼 표시와 실제 GIF 댓글 게시 성공을 확인했습니다. 이번 버전에는 다음을 추가했습니다.

- 검색창에 숫자만 입력하면 Tenor GIF ID로 조회합니다. `14083772942551242087`처럼 큰 ID도 문자열로 보존합니다.
- **최근 사용** 탭: 선택한 GIF 50개를 중복 없이 기록하며 바로 재사용합니다. 기록 지우기를 지원합니다.
- **즐겨찾기** 탭: 카드의 ☆ 저장 / ★ 해제로 관리합니다. 최대 100개이며 최근 기록과 별도로 저장합니다.
- 기록은 기기 앱 내부에 ID·제목·미리보기 URL만 저장합니다. 과거 버전의 기록을 소급해서 가져오지는 않습니다.
- **GIF-only 답글 보완:** 보이지 않는 문자 대신 임시 `[GIF]` 표시로 네이티브 게시 조건을 충족합니다. GIF-only 초안의 전송에서는 표시를 제거하고 GIF만 첨부합니다. 일반 텍스트는 유지합니다.

빌드, protobuf 25개 assertion, JSON/GIF-only 답글 변환, 기록 직렬화·중복 제거·한도·즐겨찾기 독립성, Tenor 실제 검색·숫자 ID 조회, 원본 APK 패치·DEX 컴파일·서명을 확인했습니다. **새 UI 기능 및 GIF-only 답글 활성화는 기기에서 아직 미검증입니다.**

Manager에서 0.3.0 번들을 가져온 뒤 **원본 YouTube 20.40.45 APK**에 기존 공식 패치들과 **GIF comments**를 선택해 다시 패치·설치하세요. 같은 서명 키로 업데이트하고 앱 데이터를 유지하면 이후 저장한 기록이 유지됩니다.

원격 소스: https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
