# Validation — 0.4.0

## 대상과 도구

- ReVanced: YouTube 20.40.45 / CLI 6.0.0. 이전 사용자 GIF 게시 성공 보고 있음.
- Morphe: 공식 patches stable 1.47.0 commit `e546439`, 기본 비실험 대상 YouTube 21.20.405 / desktop CLI 1.18.1 / Manager stable 1.35.0 (patcher 1.15.1).
- [공식 대상 목록](https://github.com/MorpheApp/morphe-patches/blob/v1.47.0/patches/src/main/kotlin/app/morphe/patches/youtube/shared/Constants.kt)의 첫 nonexperimental 버전은 21.20.405이다. 더 최신 experimental 버전으로 대체하지 않았다.
- 분석 APK: 21.20.405, versionCode 1561173354, universal nodpi minSdk 28, 185947985 bytes. SHA-256: `4b3ca9de0eb6857d43fa8766a724a2eba6405dfdbcdfb20eef6cb8950e805e2c`.

## 포팅 근거

Morphe 패치 API의 execute / mutableClassDefBy / classDefForEach와 Compatibility / AppTarget을 사용한다. JVM 클래스와 Android DEX, 공통 확장 DEX를 .mpp에 포함한다. Manager의 JSON 소스 필드 download_url / created_at / signature_download_url / description / version에 맞춰 별도 Morphe 소스를 제공한다. 저장소 기본 JSON은 기존 ReVanced 주소로 유지한다.

21.20.405의 Cronet 업로드 경계는 `aajp.Q(adaf, Map, byte[], ...)`이다. JADX에서 URL `adaf.w()`와 UploadDataProviders.create(byte[])를 확인했다. Morphe 패치는 실제 메서드 참조에서 URL accessor를 유일하게 찾아 GifTransport의 reflection accessor를 변경한다. 패치한 확장을 재디컴파일해 getMethod("w")를 확인했다. ReVanced 20.40.45의 v()는 유지한다.

Morphe는 legacy comment ID 조회에 의존하지 않고 EditText 생성 37곳, MainActivity decor, Dialog.show 61곳을 관찰한다. 공통 런타임에서는 send_button / timestamp_button을 이름으로 찾아 버전별 정수 ID 차이를 처리한다. 댓글 입력란 포커스·창 포커스·편집 가능 조건 및 기존 아이콘·검색·기록·즐겨찾기·GIF-only 전송은 공유한다.

## 통과한 정적 검증

- 두 플랫폼 Kotlin 패치, Java 확장 컴파일 및 D8 생성, 해당 CLI에서 번들 로딩과 버전 정보 확인.
- protobuf 25 assertion, JSON·gzip·context/params 보존·초안 격리·임시 GIF-only 텍스트 제거 및 일반 텍스트 보존.
- Tenor ID·호스트·등급·스티커 필터, 기록 JSON 왕복·큰 ID 문자열·Unicode·중복 제거·한도·즐겨찾기 독립성.
- ReVanced 20.40.45에 적용·8개 DEX 컴파일·정렬·서명.
- Morphe 21.20.405에 GIF 패치 단독 적용·9개 DEX 컴파일·정렬·서명.
- Morphe 공식 1.47.0 기본 선택 패치와 GIF 패치 조합: appliedPatches 97, failedPatches 0. PATCHING / REBUILDING / SIGNING 모두 success. GmsCore support 포함. APK는 scratch에만 보관하며 배포하지 않는다.

## 미검증

Morphe Manager Android에서의 실제 소스 등록·UI, YouTube 21.20.405 기기의 버튼 표시·로그인·재생·GIF 댓글/답글 게시·서버 표시, 여러 계정·기기·실험 버전 조합은 확인하지 못했다. ReVanced의 이전 기기 성공 보고를 Morphe 성공으로 간주하지 않는다. protobuf field 32 의미는 공식 스키마 없이 추론 상태다. 릴리스는 experimental prerelease로 배포한다.
