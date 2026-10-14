# 0.2.1 — Experimental comment composer button fix

0.2.0에서 전체 화면·하단 팝업 댓글창의 GIF 버튼이 보이지 않는 기기 보고를 받아 UI 탐지·부착 방식을 수정했습니다.

- 기존 comment ID 조회 후킹에 더해 EditText 생성 지점 37곳에서 입력란을 관찰합니다.
- 메인 화면 뷰와 대화창 show 지점 61곳에서 화면 연결·포커스 변화를 확인해 XML로 생성된 입력란도 관찰합니다.
- 입력란이 화면에 연결된 후 ID·부모 ID·댓글/답글 힌트로 판별하며, 레이아웃 변경 때 위치를 갱신합니다.
- 특정 LinearLayout 구조를 요구하던 조건을 제거했습니다. 입력란 오른쪽에 GIF 버튼, 위에 선택 미리보기를 앱 내부 PopupWindow로 표시합니다.
- 작성창이 닫히면 팝업과 선택 상태를 해제하며, 검색창에서는 버튼을 숨깁니다.
- 버튼 부착 실패는 `ReVancedGIF` 태그로 기록합니다. 댓글 내용·인증 정보는 기록하지 않습니다.

빌드·본문 변환 테스트와 원본 YouTube 20.40.45 APK 패치·DEX 컴파일·서명은 통과했습니다. **실제 기기에서 버튼 표시와 GIF 게시 성공은 아직 확인하지 못했습니다.** protobuf field 32의 의미도 추론 상태이며 실험용 릴리스입니다.

Manager에서 새 번들을 가져오고, **원본 YouTube 20.40.45 APK**에 기존 공식 패치들과 **GIF comments**를 선택하여 다시 패치·설치하세요. 기존 패치 APK 위에 재패치하지 마세요. 번들을 불러오는 것만으로 설치된 앱이 갱신되지는 않습니다.

원격 소스: https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
