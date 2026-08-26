package com.getit.domain.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelExporterTest {

  @Test
  @DisplayName("헤더와 행 데이터를 그대로 시트에 담는다")
  void writesHeaderAndRows() throws IOException {
    byte[] bytes = ExcelExporter.toXlsx(
        "지원자 목록",
        List.of("이름", "학번", "점수"),
        List.of(
            List.of("홍길동", "2021110000", 85),
            List.of("김철수", "2022110000", 90)));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheet("지원자 목록");
      assertThat(sheet).isNotNull();

      Row headerRow = sheet.getRow(0);
      assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("이름");
      assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("학번");
      assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("점수");

      Row firstDataRow = sheet.getRow(1);
      assertThat(firstDataRow.getCell(0).getStringCellValue()).isEqualTo("홍길동");
      assertThat(firstDataRow.getCell(2).getNumericCellValue()).isEqualTo(85.0);

      Row secondDataRow = sheet.getRow(2);
      assertThat(secondDataRow.getCell(0).getStringCellValue()).isEqualTo("김철수");
    }
  }

  @Test
  @DisplayName("null 값은 빈 셀로 남긴다")
  void leavesNullAsBlankCell() throws IOException {
    byte[] bytes = ExcelExporter.toXlsx("시트", List.of("값"), List.of(Arrays.asList((Object) null)));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Row row = workbook.getSheetAt(0).getRow(1);
      assertThat(row.getCell(0).getCellType()).isEqualTo(CellType.BLANK);
    }
  }

  @Test
  @DisplayName("행이 없어도 헤더만 있는 워크북을 만든다")
  void createsWorkbookWithHeaderOnly() throws IOException {
    byte[] bytes = ExcelExporter.toXlsx("빈 시트", List.of("이름"), List.of());

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getLastRowNum()).isZero();
    }
  }
}
