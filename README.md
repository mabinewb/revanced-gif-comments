# YouTube GIF Comments — ReVanced & Morphe

한 공개 저장소에서 ReVanced와 Morphe용 GIF 댓글 패치를 함께 관리합니다. 검색·ID 조회·최근 기록·즐겨찾기·입력 UI 및 본문 변환 코드는 공유하고, 패치 API와 대상 YouTube 버전에 맞춰 두 번들을 별도로 빌드합니다.

| 사용 도구 | 대상 원본 YouTube APK | 번들 |
| --- | --- | --- |
| ReVanced Manager | **20.40.45** | `gif-comments-0.4.0.rvp` |
| Morphe Manager | **21.20.405** | `morphe-gif-comments-0.4.0.mpp` |

Morphe 대상은 [공식 stable 패치 1.47.0의 첫 비실험 대상](https://github.com/MorpheApp/morphe-patches/blob/v1.47.0/patches/src/main/kotlin/app/morphe/patches/youtube/shared/Constants.kt)이며 Android 9 이상인 universal nodpi 일반 APK를 분석했습니다. 21.40.161 등 공식 실험 대상과는 구분합니다.

[0.4.0 릴리스 다운로드](https://github.com/mabinewb/revanced-gif-comments/releases/tag/v0.4.0)

**검증:** ReVanced 이전 버전은 사용자의 실제 GIF 게시 성공 보고가 있습니다. 0.4.0의 양쪽 빌드·테스트·원본 APK 패치·서명을 확인했고, Morphe 공식 기본 선택 패치와 GIF 패치의 총 97개 조합에서도 실패 없이 패치·리빌드·서명했습니다. **Morphe 21.20.405에서 실제 버튼 표시·GIF 게시 성공은 아직 기기에서 확인하지 못했습니다.**

## ReVanced Manager

1. 패치 번들 목록에서 URL로 아래 ReVanced 소스를 추가합니다. 기존에 이 주소를 등록했다면 새로 고침합니다.
2. URL 등록이 어려우면 `.rvp` 파일을 다운로드하고 저장소에서 가져옵니다.
3. **원본 YouTube 20.40.45 APK**를 선택하고 평소 사용하는 공식 패치들과 **GIF comments**를 직접 선택합니다.
4. 패치·설치합니다. 기존 앱을 업데이트하려면 동일한 서명 키를 사용합니다.

```text
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
```

## Morphe Manager

Morphe Manager 1.35.0의 소스 로더를 기준으로 작성했습니다. 실제 Manager UI 조작은 미검증이며 언어·버전에 따라 메뉴 명칭이 다를 수 있습니다.

1. 고급 모드에서 패치 소스 추가의 URL 입력으로 **아래 Morphe 전용 JSON 주소**를 등록합니다. 이 저장소의 기본 `patches-bundle.json`은 ReVanced용이므로 저장소 페이지 주소만 입력하지 마세요.
2. URL 등록이 어려우면 릴리스의 **`morphe-gif-comments-0.4.0.mpp`**를 내려받아 파일 소스로 가져옵니다.
3. 공식 Morphe 소스를 함께 유지하고 **원본 YouTube 21.20.405 APK**를 선택합니다. `.apkm` 번들이 아닌 일반 APK를 기준으로 검증했습니다.
4. 사용할 공식 패치들과 이 소스의 **GIF comments**를 선택합니다. 기본 선택은 꺼져 있습니다. 비루팅 설치에서는 공식 **GmsCore support** 등 평소 사용하는 패치를 포함합니다.
5. 패치·설치합니다. 기존 Morphe 앱과 같은 서명 키·패키지로 데이터를 유지해 업데이트하면 기록·즐겨찾기도 유지할 수 있습니다.

```text
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/morphe-patches-bundle.json
```

두 형식은 상호 교환용 파일이 아닙니다. 각 Manager에는 맞는 파일 또는 소스 주소를 사용하세요. 이미 패치한 APK 위에 재패치하지 말고 원본 APK로 시작합니다. 이 번들은 GIF 패치 하나만 포함하며 공식 광고 차단·재생·로그인 패치를 대체하지 않습니다.

## 사용 방법

1. 댓글·답글 입력란을 눌러 실제 작성창을 활성화합니다. 포커스를 받기 전에는 GIF 버튼을 표시하지 않습니다.
2. **GIF 아이콘**을 누릅니다. 기본 댓글창에서는 현재 시간 버튼 옆에 배치하며 다른 구조에서는 입력란 오른쪽에 표시합니다.
3. 검색어 또는 Tenor 숫자 ID를 입력합니다. 예: `14083772942551242087`. 숫자만 입력하면 해당 ID를 조회합니다.
4. 결과를 눌러 첨부합니다. **최근 사용**에서 이전 선택을 다시 쓰거나 **☆ 저장 / ★ 해제**로 즐겨찾기를 관리합니다.
5. 선택 미리보기에서 제거하거나 댓글 텍스트를 함께 입력합니다. GIF만 선택하면 임시 `[GIF]` 표시로 댓글·답글 게시 조건을 충족하고 전송 직전에 제거합니다. 직접 작성한 동일 텍스트는 보존합니다.
6. 유튜브의 원래 게시 버튼을 누릅니다.

`[[gif:…]]` 수동 마커는 지원하지 않습니다. GIF 선택 상태와 일치하는 초안의 JSON `tenorId` 또는 protobuf field 32를 변환합니다. field 32의 공식 스키마는 확인하지 못했으며 Morphe의 실제 게시 성공은 미검증입니다. 기존 댓글 편집·커뮤니티 글·임의 GIF 업로드는 지원하지 않습니다.

## 기록 저장

최근 50개는 **선택 시점**의 기록이며 게시 성공 목록이 아닙니다. 중복을 제거하고 재사용한 GIF를 맨 앞에 표시합니다. 즐겨찾기는 별도 최대 100개로 최근 기록 삭제 시에도 유지됩니다.

앱 내부에 ID·제목·미리보기 URL만 저장합니다. 댓글 텍스트·계정 정보는 저장하지 않으며 서버 동기화하지 않습니다. 앱 데이터 삭제·앱 제거 시 기록도 지워집니다. 서로 다른 패키지로 설치한 ReVanced와 Morphe 앱 사이의 기록은 자동 공유하지 않습니다. GIF 이미지는 오프라인 저장하지 않으므로 미리보기에는 인터넷이 필요합니다.

## 빌드

Python 3.10+와 Java 21을 사용합니다. 도구는 공식 배포처에서 내려받아 고정 SHA-256으로 확인하며 Android API 28 플랫폼을 자동 준비합니다.

```text
python tools/build.py
python tools/build.py --morphe
```

결과는 각각 `.rvp`와 `.mpp`이며 `GIF_BUILD_DIR`로 출력 디렉터리를 지정할 수 있습니다. GitHub Actions도 두 플랫폼을 별도 작업으로 빌드합니다. APK·로그인 정보·서명 개인 키는 저장소나 릴리스에 포함하지 않습니다.

[검증 기록](docs/VALIDATION.md) · [이전 ReVanced 개발 기록](docs/REVANCED_HISTORY.md)

GPL-3.0. ReVanced·Morphe 공식 프로젝트와 별도로 유지하는 커스텀 패치입니다. 아이디어 참고: [rayytor/desktop_gifs](https://github.com/rayytor/desktop_gifs).
