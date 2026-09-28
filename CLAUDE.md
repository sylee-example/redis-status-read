## 코드 탐색·편집 원칙 (Serena MCP)

이 프로젝트는 Serena MCP가 연결되어 있다. 코드 작업 시 아래 규칙을 따른다.

### 세션 시작
- 첫 작업 전에 `list_memories`로 메모리 목록을 확인하고,
  작업과 관련된 메모리만 `read_memory`로 읽는다.
- 온보딩이 안 되어 있으면 `check_onboarding_performed` 후 온보딩을 진행한다.

### 탐색
- 소스 파일을 통째로 읽지 않는다. 먼저 `get_symbols_overview`로
  파일·디렉터리 구조를 파악한 뒤, 필요한 심볼만 `find_symbol`
  (include_body=true)로 읽는다.
- 심볼 이름을 모르거나 문자열·설정값을 찾을 때만 `search_for_pattern`을 쓴다.
- 함수·클래스·인터페이스를 수정하기 전에는 반드시
  `find_referencing_symbols`로 호출부를 확인하고, 영향 범위를 먼저 보고한다.

### 편집
- 함수·메서드 전체를 바꿀 때는 `replace_symbol_body`를 쓴다.
- 새 함수·메서드는 `insert_after_symbol` / `insert_before_symbol`로 추가한다.
- 이름 변경은 `rename_symbol`로 하여 참조까지 일괄 반영한다.
- 몇 줄 단위의 작은 수정이나 설정·문서 파일은 기본 Edit 도구를 써도 된다.

### 메모리
- 작업 중 알게 된 규칙(빌드·테스트 명령, 아키텍처 결정, 주의사항)은
  사용자 확인 후 `write_memory`로 저장한다.
- 메모리 내용이 실제 코드와 다르면 코드를 우선하고, 메모리 수정을 제안한다.

### 검증
- 편집 후에는 관련 테스트를 실행한다: `npm test` (프로젝트에 맞게 수정)
