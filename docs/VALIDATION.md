# Validation — 0.2.1

## 0.2.0 댓글창 버튼 미노출 보고와 0.2.1 수정

사용자가 전체 화면 및 하단 팝업 댓글창 모두에서 GIF 버튼이 나타나지 않는다고 보고했다. 기존 네 곳의 comment ID 조회 후킹 또는 LinearLayout 조건으로 실제 작성창을 처리하지 못했을 가능성이 있으며, 기기 로그 없이 원인을 확정하지는 않았다.

0.2.1은 원본 APK에서 EditText 및 그 하위 클래스의 생성 지점 37곳에 관찰 함수를 추가했다. 하위 클래스의 super 호출에는 바로 설치하지 않고 생성자의 return 직전에 관찰을 연결한다. 연결·레이아웃이 끝난 뒤 ID·부모 ID·한국어/영어 댓글·답글 힌트로 판별한다. 특정 부모 구조를 요구하지 않는 PopupWindow를 사용하며 화면 연결 해제 시 해제한다. Tenor 검색 입력란은 확장 자체 생성 코드이며 이 관찰 대상에서 제외된다.

프레임워크가 XML에서 직접 생성하는 기본 EditText도 관찰하기 위해 MainActivity.onCreate 종료 후 decor 뷰를 확인한다. 앱의 Dialog.show 호출 61곳 뒤에도 대화창 decor 뷰를 확인한다. 초기 스캔과 이후 포커스·레이아웃 변경을 관찰한다. 생성 후킹으로 잡지 못한 입력란을 보완하는 목적이며, 이 경로가 사용자 작성창을 확실히 포함하는지는 기기에서 확인해야 한다.

0.2.1 번들 컴파일·본문 변환 테스트, 원본 APK 패치(기존 UI 4곳 + 생성 37곳 + MainActivity 1곳 + Dialog 61곳 + Cronet 1곳), 8개 DEX 컴파일·정렬·서명 통과. 이 결과는 앱 실행 및 버튼 실제 표시를 입증하지 않는다. 실제 기기 버튼 표시와 게시 성공은 아직 확인하지 못했다.

## 0.1.0 기기 테스트 실패와 수정

사용자가 `[[gif:14083772942551242087]]`가 그대로 댓글로 게시되었다고 보고했다. 이는 0.1.0의 request-builder 후킹이 실제 사용된 전송 경로를 처리하지 않았거나 해당 본문 형식을 처리하지 못했다는 근거다. 기존 컴파일·정적 분석 성공을 실제 작동 성공으로 해석할 수 없다는 점이 확인되었다. 0.1.0 릴리스에는 사용 중단 안내를 붙였다.

0.2.0에는 원본 APK의 네 곳에서 comment ID `0x7f0b040f`를 조회한 직후 UI 부착 함수를 호출하도록 패치했다. 실제 네트워크 경계 `qon.bG(abcb, Map, byte[], ...)`도 후킹한다. 이 메서드는 `abcb.v()`로 Cronet URL을 얻고 이후 `UploadDataProviders.create(bArr)`로 본문을 전달하므로, URL·본문을 실제 업로드 직전에 처리할 수 있다.

GIF ID는 입력 텍스트에 넣지 않는다. 선택 상태와 해당 초안 텍스트를 연결하고 JSON commentText 또는 protobuf field 3이 일치할 때만 변환한다. JSON에서는 `tenorId`를 추가하며, protobuf에서는 여전히 추론한 field 32를 사용한다. 지원하지 않는 경로는 변환하지 않는다. `[[gif:…]]`가 발견되는 확인된 댓글 경로는 차단한다.

추가 검증: Tenor 검색 페이지 실제 응답에서 22개 결과를 읽었다. 본문 변환 23개 assertion, 큰 숫자 ID 문자열 보존, 등급·스티커·외부 호스트 필터, JSON 본문·context 보존, gzip 재압축, Content-Length 갱신, 다른 초안 불변, 수동 마커 차단 테스트를 통과했다. UI 및 실제 게시 성공의 기기 검증은 아직 하지 못했다. 이전의 marker 기반 구현 설명은 0.1.0 기록이며 현재 사용자 흐름은 README를 따른다.

분석일: 2026-10-14. 대상 앱: YouTube 20.40.45, versionCode 1557278144.

## 통과한 검증

- Java 확장 및 Kotlin 패치 컴파일, D8 Android DEX 생성.
- 실 protobuf 라이브러리를 사용한 20개 assertion: 일반 댓글 불변, 한국어·이모지 보존, opaque params 보존, 64-bit varint·fixed32·fixed64 보존, GIF-only 본문, 잘못된/중복 마커 차단, 기존 attachment 충돌 차단, reflection clear/merge와 unknown-field 보존.
- ReVanced CLI 6.0.0으로 RVP 로딩. 패치 이름·20.40.45 호환 정보 정상 표시.
- 원본 20.40.45 APK에 적용 성공, 8개 DEX 컴파일·APK 정렬·서명 성공.
- 패치 후 댓글·답글 request builder를 디컴파일하여 helper 호출 삽입 확인.
- 대상 APK의 raw DEX method ID에서 buildPartial(), toByteArray(), clear(), mergeFrom(byte[], int, int)의 실제 메서드 이름 및 시그니처 확인.

## 근거와 미확정 부분

20.40.45의 일반 댓글은 afoj → axud, 답글은 afoi → axua의 protobuf builder를 사용한다. 최신 비교 APK 21.40.161에서는 albw → bfzh, albv → bfze로 바뀐다. 두 메시지에 32번 length-delimited 문자열 필드가 추가되었고, 작성 검사에서 텍스트가 없어도 이 추가 문자열을 허용한다. 일반 댓글의 값은 endpoint bdtz.n에서 albw.K로, 답글은 bduc.k에서 albv.f로 전달된다.

field 32가 GIF/Tenor ID라는 판단은 위 변경과 desktop_gifs의 웹 `tenorId` 동작을 근거로 한 **추론**이다. 필드 이름을 가진 공식 스키마 또는 실제 서버 응답으로 확인한 것은 아니다. 이 때문에 릴리스는 experimental prerelease이다.

일반 텍스트 필드는 메시지의 field 3이며, 32번 필드는 20.40.45의 알려진 필드 목록에 없다. 따라서 helper는 원본 protobuf를 직렬화해 field 3의 마커를 제거하고 field 32를 추가한 다음 동일 builder로 다시 파싱한다. 앱 protobuf의 unknown fields 보존을 이용하며, 나머지 요청 필드와 이후 앱의 context 생성 흐름을 유지한다.

## 아직 검증하지 못한 항목

- 실제 안드로이드 기기의 설치·실행·게시 성공·GIF 표시.
- 서버가 field 32를 Tenor ID로 처리하는지, 이전 클라이언트의 요청에 허용하는지.
- 계정·영상별 제한과 GIF 등급 처리, 실패 표시, malformed marker의 앱 종료 여부.
- Manager의 Android DexClassLoader에서 번들을 불러오는 실행 테스트. Manager 2.6.0의 remote JSON 및 번들 로더 소스에 맞춰 패키징했다.
- 공식 패치 전체 조합에서 적용 및 재생 동작.

완성 또는 정상 작동 확인으로 해석하지 말 것. 실제 GIF 게시를 확인하기 전에는 stable 태그를 발행하지 않는다.

## 입력 파일 식별

20.40.45 APK SHA-256:
`b7659da492a1ebd8bd7cea2909be4ee1f58e00a2586d65a1c91b2e1e5ec6acd1`

원본 APK 및 디컴파일 결과는 저장소에 게시하지 않는다.
