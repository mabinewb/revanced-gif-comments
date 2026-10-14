# Validation — 0.1.0

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
