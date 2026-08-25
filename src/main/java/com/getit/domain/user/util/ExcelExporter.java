package com.getit.domain.user.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * XLSX 다운로드 공용 유틸. (API 명세서 7.6, 9.x 엑셀 다운로드와 공유 예정)
 *
 * <p>호출부가 헤더 · 행 데이터만 넘기면 그대로 시트 하나짜리 워크북을 만들어준다. 스타일링은
 * 하지 않는다 — 필요해지면 그때 옵션을 추가한다.
 */
public final class ExcelExporter {

  private ExcelExporter() { }

  /**
   * @param sheetName 시트 이름
   * @param headers   첫 행에 들어갈 헤더
   * @param rows      헤더 다음 행부터 채울 데이터. 각 행의 크기는 headers 와 같아야 한다.
   */
  public static byte[] toXlsx(String sheetName, List<String> headers, List<List<Object>> rows) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName);

      Row headerRow = sheet.createRow(0);
      for (int col = 0; col < headers.size(); col++) {
        headerRow.createCell(col).setCellValue(headers.get(col));
      }

      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        Row row = sheet.createRow(rowIndex + 1);
        List<Object> values = rows.get(rowIndex);
        for (int col = 0; col < values.size(); col++) {
          setCellValue(row.createCell(col), values.get(col));
        }
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("엑셀 파일을 생성하지 못했습니다.", e);
    }
  }

  private static void setCellValue(Cell cell, Object value) {
    if (value == null) {
      cell.setBlank();
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(value.toString());
    }
  }
}
