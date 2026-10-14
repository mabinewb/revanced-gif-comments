# Validation — 0.3.1

## 0.3.1 입력란 활성 상태와 아이콘 배치

사용자가 기존 이슈 해결 후 비활성 댓글 입력란에도 버튼이 나타나고 여기서 선택하면 실제 작성창으로 넘어갈 때 선택이 사라진다고 보고했다. 기존 판별은 보임·연결·댓글 ID/힌트만 확인했고 포커스를 요구하지 않았다.

0.3.1의 부착·표시는 연결·보임·enabled·focusable·hasFocus·hasWindowFocus·inputType!=0을 모두 요구한다. 180ms 뒤 조건을 재확인한다. 생성 관찰은 유지하되 비활성 입력란에는 State 생성 및 GIF 검색 실행을 허용하지 않는다. 전역 포커스·창 포커스 변경에도 표시 상태를 갱신한다. 검색 대화창 동안은 아이콘을 숨기고 닫힌 뒤 다시 표시한다.

원본 APK comment_dialog.xml에서 timestamp_button ID 0x7f0b1579와 40dp 크기를 확인했다. 동일 가로 LinearLayout에 편집기가 속하면 타임스탬프 버튼 앞에 40dp 아이콘을 삽입한다. 다른 구조에서는 기존 팝업 부착 경로를 유지하되 동일 아이콘을 쓰고 40dp 공간을 확보한다. Canvas로 24dp GIF 테두리·문자를 그리고 입력란 색상 및 원형 RippleDrawable을 사용한다.

빌드·25개 protobuf assertion·JSON 전송·기록 모델 테스트와 원본 APK 패치·8개 DEX 컴파일·정렬·서명을 통과했다. Android 기기의 실제 버튼 표시 조건·기본 행 배치·테마·다른 공식 패치 조합의 외관은 아직 확인하지 못했다.

## 0.2.1 사용자 기기 성공 보고와 0.3.0 추가 기능

사용자가 0.2.1에서 버튼 표시와 실제 GIF 댓글 게시 모두 성공했다고 보고했다. 계정·기기 전반을 검증한 결과는 아니며, 성공 요청이 JSON인지 protobuf인지는 이 보고만으로 구분할 수 없다. 이후 GIF-only 댓글은 가능하지만 GIF-only 답글의 게시 버튼은 비활성이라고 보고했다.

0.3.0은 GIF-only 입력 시 일반 텍스트 `[GIF]`를 임시로 넣고 초안 상태의 gifOnly 플래그로 구분한다. 전송 직전 일치하는 댓글·답글 본문의 임시 텍스트를 제거한다. 사용자가 직접 작성한 동일 텍스트는 gifOnly 플래그가 없으므로 유지한다. native 버튼을 무조건 활성화하지 않고 정상 텍스트 검사를 통과시키는 방식이다. UI 기기 검증은 아직 하지 못했다.

ID 조회는 Tenor 공개 페이지 `https://tenor.com/view/14083772942551242087`의 `gifs.byId.<id>.results`를 사용한다. 실제 Java 클라이언트로 이 ID 1개를 정확히 조회했고 키워드 검색 21개 결과도 확인했다. 검색과 동일한 등급·호스트·스티커 필터를 적용하며 다른 ID나 관련 GIF로 대체하지 않는다.

기록·즐겨찾기 저장 모델은 JSON 왕복, 큰 ID 및 유니코드 보존, 중복 제거·재사용 순서, 최근 50개·즐겨찾기 100개 한도, 최근 삭제와 즐겨찾기 독립성, 손상 저장 데이터 및 외부 호스트 배제 테스트를 통과했다. Android SharedPreferences 저장·탭 UI는 실제 기기에서 미검증이다.

protobuf 25개 assertion, JSON 및 protobuf GIF-only 텍스트 제거와 일반 `[GIF]` 보존, 기존 context·params 유지, gzip·초안 격리 테스트 통과. 원본 20.40.45 APK에 적용·8개 DEX 컴파일·정렬·서명 통과. 공개 릴리스에는 APK를 포함하지 않는다.

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

- 여러 기기·계정에서의 실행·게시·GIF 표시 및 0.3.0 새 기능. 0.2.1은 사용자의 성공 보고가 있다.
- 서버가 field 32를 Tenor ID로 처리하는지, 이전 클라이언트의 요청에 허용하는지.
- 계정·영상별 제한과 GIF 등급 처리, 실패 표시, malformed marker의 앱 종료 여부.
- Manager의 Android DexClassLoader에서 번들을 불러오는 실행 테스트. Manager 2.6.0의 remote JSON 및 번들 로더 소스에 맞춰 패키징했다.
- 공식 패치 전체 조합에서 적용 및 재생 동작.

0.2.1은 사용자의 실제 성공 보고가 있으나 여러 기기의 정상 작동 보장으로 해석하지 말 것. 0.3.0 새 기능은 기기 검증 전이므로 실험용 릴리스로 배포한다.

## 입력 파일 식별

20.40.45 APK SHA-256:
`b7659da492a1ebd8bd7cea2909be4ee1f58e00a2586d65a1c91b2e1e5ec6acd1`

원본 APK 및 디컴파일 결과는 저장소에 게시하지 않는다.
