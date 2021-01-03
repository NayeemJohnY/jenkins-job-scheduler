package utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtils {

	private String filepath;
	private static Logger log = LogManager.getLogger();
	private Workbook workbook;

	public ExcelUtils(String filepath) {
		this.filepath = filepath;

		try (FileInputStream fileInput = new FileInputStream(filepath)) {
			workbook = new XSSFWorkbook(fileInput);
		} catch (IOException e) {
			log.error("Exception occurred while reading filename : {}", filepath, e);
		}
	}

	/**
	 * Method to get the total rows in sheet
	 * 
	 * @param sheetName - String
	 * @return number Of Rows
	 */
	public int getRowCount(String sheetName) {
		Sheet sheet = workbook.getSheet(sheetName);
		return sheet != null ? sheet.getLastRowNum() + 1 : 0;

	}

	/**
	 * Method to get the total columns defined in the row
	 * 
	 * @param sheetName - String
	 * @param rownum    - int
	 * @return number Of Columns
	 */
	public int getColumnCount(String sheetName, int rownum) {
		Row row = getRow(sheetName, rownum);
		return row.getLastCellNum();

	}

	/**
	 * Method to get the sheet by the sheetName
	 * 
	 * @param sheetName - String
	 * @return sheet - Sheet
	 * @throws IllegalStateException - If the sheet is not exists in the  workbook
	 */
	private Sheet getSheet(String sheetName) {
		Sheet sheet = workbook.getSheet(sheetName);
		if (sheet != null) {
			return sheet;
		} else {
			throw new IllegalStateException("Sheet with name - " + sheetName + " is not found in the Workbook");
		}
	}

	/**
	 * Method to get the Row specified by rownum
	 * 
	 * @param sheetName - String
	 * @param rownum    - int
	 * @return Row
	 */
	public Row getRow(String sheetName, int rownum) {
		Sheet sheet = getSheet(sheetName);
		return sheet.getRow(rownum);
	}

	/**
	 * Method to get the index of the column specified by column Name
	 * 
	 * @param sheetName  - String
	 * @param columnName - String
	 * @return columnNum - index of column
	 * @throws IllegalStateException - If the column header is not defined in the sheet
	 */
	private int getColumnIndexForCoulumnName(String sheetName, String columnName) {
		int columnNum = -1;
		Row row = getRow(sheetName, 0);
		if (row == null) {
			throw new IllegalStateException("Row Header is not defined in the sheet: " + sheetName);
		}
		for (int i = 0; i < row.getLastCellNum(); i++) {
			if (row.getCell(i).getStringCellValue().trim().equals(columnName)) {
				columnNum = i;
				break;
			}
		}
		if (columnNum != -1) {
			return columnNum;
		} else {
			throw new IllegalStateException(
					"Column header - " + columnName + " is not defined in the sheet: " + sheetName);
		}
	}

	/**
	 * Method to get the value of the cell specified by row number and column Name
	 * 
	 * @param sheetName  - String
	 * @param columnName - String
	 * @param rownum     - int
	 * @return cellValue - String
	 * @throws IllegalStateException - If the row for the rownum is not defined in the sheet
	 */
	public String getCellValue(String sheetName, String columnName, int rownum) {
		String cellValue = "";
		int columnIndex = getColumnIndexForCoulumnName(sheetName, columnName);
		Row row = getRow(sheetName, rownum);
		if (row == null) {
			throw new IllegalStateException(
					"Row Number -" + (rownum + 1) + " is not defined in the sheet: " + sheetName);
		}
		Cell cell = row.getCell(columnIndex);
		if (cell != null) {
			if (cell.getCellTypeEnum() == CellType.NUMERIC) {
				cellValue = String.valueOf((int) cell.getNumericCellValue());
			} else {
				cellValue = cell.getStringCellValue();
			}
		} else {
			log.debug("Column value for coulmn name - {} is not defined in the row number - {} in the sheet - {}",
					columnName, (rownum + 1), sheetName);
		}

		return cellValue;
	}

	/**
	 * Method to get the value of the cell specified by row number and column number
	 * 
	 * @param sheetName - String
	 * @param columnnum - int
	 * @param rownum    - int
	 * @return cellValue - String
	 * @throws IllegalStateException - If the row for the rownum is not defined in
	 *                               the sheet
	 */
	public String getCellValue(String sheetName, int columnnum, int rownum) {
		String cellValue = "";
		Row row = getRow(sheetName, rownum);
		if (row == null) {
			throw new IllegalStateException(
					"Row Number -" + (rownum + 1) + " is not defined in the sheet: " + sheetName);
		}
		Cell cell = row.getCell(columnnum);
		if (cell != null) {
			if (cell.getCellTypeEnum() == CellType.NUMERIC) {
				cellValue = String.valueOf((int) cell.getNumericCellValue());
			} else {
				cellValue = cell.getStringCellValue();
			}
		} else {
			log.debug("Column value for coulmn number - {} is not defined in the row number - {} in the sheet - {}",
					(columnnum + 1), (rownum + 1), sheetName);
		}

		return cellValue;
	}

	/**
	 * Method to set the value in the cell specified by row number and column number
	 * 
	 * @param sheetName - String
	 * @param columnnum - int
	 * @param rownum    - int
	 * @param value     - String
	 */
	public void setCellValue(String sheetName, int columnNum, int rownum, String value) {
		Row row = getRow(sheetName, rownum);
		if (row == null) {
			row = getSheet(sheetName).createRow(rownum);
		}
		Cell cell = row.createCell(columnNum, CellType.STRING);
		cell.setCellValue(value);
	}

	/**
	 * Method to write the values in the workbook
	 */
	public void writeToWorkbook() {
		try (FileOutputStream outputStream = new FileOutputStream(filepath)) {
			workbook.write(outputStream);
			workbook.close();
		} catch (IOException e) {
			log.error("Exception occurred while updating the Excel!", e);
		}
	}

	/**
	 * Method to set the value in the cell specified by row number and column Name
	 * 
	 * @param sheetName  - String
	 * @param columnName - String
	 * @param rownum     - int
	 * @param value      - String
	 */
	public void setCellValue(String sheetName, String columnName, int rownum, String value) {
		int columnIndex = getColumnIndexForCoulumnName(sheetName, columnName);
		setCellValue(sheetName, columnIndex, rownum, value);
	}

	/**
	 * Method to create a sheet in the workbook.
	 * Remove the sheet if it is already
	 * exists.
	 * 
	 * @param sheetName
	 */
	public void createSheet(String sheetName) {
		int index = workbook.getSheetIndex(sheetName);
		if (index != -1) {
			workbook.removeSheetAt(index);
		}
		workbook.createSheet(sheetName);
	}
}
