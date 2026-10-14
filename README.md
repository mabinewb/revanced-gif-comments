# ReVanced GIF Comments

YouTube **20.40.45**에서 Tenor GIF 댓글·답글을 작성하기 위한 커스텀 ReVanced 패치입니다.

**현재 릴리스는 실험용입니다.** 번들 로딩, APK 패치·빌드, 본문 변환 테스트는 통과했지만, 안드로이드 기기에서 실제 GIF 게시 성공을 확인하지 못했습니다. 최신 APK의 추가 문자열 필드 32를 기존 APK에 전달하는 구현이며, 이 필드가 서버에서 `tenorId`로 처리되는지는 기기 테스트가 필요합니다. 검증되지 않은 상태를 안정 버전으로 표시하지 않습니다.

## 다운로드

- [릴리스 파일](https://github.com/mabinewb/revanced-gif-comments/releases/tag/v0.1.0)
- 번들: `gif-comments-0.1.0.rvp`
- Manager에 추가할 원격 소스:

```text
https://raw.githubusercontent.com/mabinewb/revanced-gif-comments/main/patches-bundle.json
```

## ReVanced Manager에서 불러오기

ReVanced Manager **2.6.0**의 소스 구조를 기준으로 작성했습니다. 실제 Manager 화면에서의 조작은 아직 검증하지 못했으며, 언어와 버전에 따라 명칭이 다를 수 있습니다.

1. Manager의 패치 번들 목록을 열고 **Add patches / 패치 추가**를 누릅니다.
2. **Enter URL / URL 입력**을 선택하고 위 `patches-bundle.json` 주소를 입력합니다. GitHub 저장소 페이지 주소나 `.rvp` 직접 다운로드 주소를 이 칸에 넣지 않습니다.
3. 원격 등록이 되지 않으면 릴리스의 `.rvp` 파일을 휴대폰에 다운로드하고 **Select from storage / 저장소에서 선택**으로 추가합니다.
4. 패치할 앱으로 원본 **YouTube 20.40.45 APK**를 선택합니다.
5. 사용할 공식 패치들과 함께 **GIF comments (Tenor ID)**를 직접 선택합니다. 실험용이므로 기본 선택되어 있지 않습니다.
6. 패치·설치합니다. 기존 ReVanced 앱을 업데이트한다면 같은 Manager 서명 키를 사용해야 합니다. 비루팅 설치에서는 평소 사용하는 GmsCore support 등의 공식 패치도 필요합니다.

이 번들은 GIF 패치 하나만 포함합니다. 광고 차단·재생 관련 공식 패치를 대체하지 않습니다. 공식 번들과 함께 적용한 전체 조합은 아직 테스트하지 않았습니다.

## 댓글 작성

이 버전은 GIF 검색 버튼 대신 **Tenor 숫자 ID를 입력하는 방식**입니다.

1. Tenor에서 GIF 페이지를 엽니다. 예를 들어 주소가 `https://tenor.com/view/example-gif-123456789`라면 마지막 숫자 `123456789`가 ID입니다. 이 주소는 형식 설명용 예시이며 실제 GIF를 보장하지 않습니다.
2. 새 댓글 또는 답글 작성란에 다음 형태로 입력합니다.

```text
[[gif:123456789]]
```

텍스트와 함께 작성하려면:

```text
이 장면 좋네요 [[gif:123456789]]
```

3. 유튜브의 원래 게시 버튼을 누릅니다. 패치는 마커를 제거하고 앱의 기존 요청에 attachment 문자열 필드를 추가합니다.
4. 다른 계정이나 원본 유튜브 앱에서도 GIF가 보이는지 확인합니다. **게시 성공 메시지만으로 GIF 첨부 성공이 확인되지는 않습니다.** 서버가 필드를 무시하면 일반 텍스트만 게시되거나 GIF-only 댓글이 거절될 수 있습니다.

마커는 소문자 `gif`와 숫자 ID로 정확하게 입력하고, 하나만 사용합니다. 잘못된 예약 마커는 요청 생성 단계에서 오류를 발생시켜 전송을 중단합니다. 앱 버전에 따라 오류 표시 또는 앱 종료가 발생할 가능성을 아직 검증하지 못했습니다.

**기존 댓글 편집·커뮤니티 글에서는 사용하지 마세요.** 해당 요청은 이번 패치 대상이 아니므로 마커가 그대로 게시될 수 있습니다. 임의 GIF 파일 업로드, Giphy, Tenor 검색·즐겨찾기 UI는 지원하지 않습니다. 원본 desktop_gifs 프로젝트는 Tenor G 등급만 허용된다고 설명합니다.

## 실패 시 확인

- 목록에 패치가 없다면 번들을 새로 고침하거나 파일 가져오기를 사용합니다. 구형 Manager 1.x는 지원 대상으로 검증하지 않았습니다.
- 호환되지 않는 앱 버전 메시지가 나오면 20.40.45를 사용합니다. 호환성 검사를 강제로 무시하지 않습니다.
- 일반 댓글은 되지만 GIF가 없다면 attachment field 32의 서버 처리 여부를 확인해야 합니다. 계정·영상별 허용 여부도 정적 분석으로 알 수 없습니다.
- 문제를 보고할 때 앱 버전, Manager 버전, 선택한 패치와 표시된 오류를 알려주세요. 계정 쿠키·토큰·댓글 요청 원문은 공유하지 마세요.

## 빌드

Python 3.10+와 Java 21만 필요합니다. Android SDK나 GitHub Packages 토큰은 필요하지 않습니다.

```text
python tools/build.py
```

빌드 도구를 공식 배포처에서 다운로드하고 `tools/dependencies.json`의 SHA-256으로 확인합니다. 결과는 `build/gif-comments-0.1.0.rvp`와 `build/SHA256SUMS.txt`입니다. 번들에는 JVM 클래스와 Android DEX, APK에 주입할 확장 DEX가 함께 들어 있습니다.

## 검증 범위와 설계

[검증 기록](docs/VALIDATION.md)을 참고하세요. 사용자 계정 정보나 네트워크 인증을 직접 읽지 않으며, 별도 서버나 API 키를 사용하지 않습니다. 유튜브 APK, 패치된 APK, 로그인 정보와 서명 개인 키는 이 저장소에 포함하지 않습니다.

아이디어와 웹 구현 참고: [rayytor/desktop_gifs](https://github.com/rayytor/desktop_gifs). 본 저장소의 요청 변환 코드는 Android protobuf 경로에 맞춰 새로 작성했습니다.

라이선스: GPL-3.0. ReVanced 공식 프로젝트와 별도로 유지하는 커스텀 패치입니다.
