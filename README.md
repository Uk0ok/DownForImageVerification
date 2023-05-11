<출입국 사무소 검증을 위한 다운로드 프로그램>
// 이관 시 마운트 경로가 volume명_new 로 변경
// 검증 완료 후 ASIS 지우고 TOBE의 _new 제거


1. asysArchiveVolume 테이블에서 volume List 추출
2. volume명, volume명 _NEW으로 다운로드 PATH 생성
3. volume별 elementid 5개씩 추출해서 download && DB값 update후 다운로드
4. 다운로드 한 elementid를 txt파일에 입력
5. 볼륨아이디 업데이트
UPDATE ASYSCONTENTELEMENT SET VOLUMEID = TRIM(VOLUMEID) || '_NEW', FILEKEY = REPLACE(FILEKEY, TRIM(VOLUMEID), (TRIM(VOLUMEID) || '_NEW')) WHERE ELEMENTID IN (?, ?, ?, ?, ?);
6. 볼륨아이디 원상복구
UPDATE ASYSCONTENTELEMENT SET VOLUMEID = REPLACE(VOLUMEID, '_NEW', ''), FILEKEY = FILEKEY = REPLACE (FILEKEY, '_NEW', '') WHERE ELEMENTID IN (?, ?, ?, ?, ?)
