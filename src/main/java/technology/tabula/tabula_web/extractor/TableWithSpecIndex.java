package technology.tabula.tabula_web.extractor;

import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@SuppressWarnings("serial")
public class TableWithSpecIndex extends Table {

	public int specIndex;
	public Table table;
	
	public TableWithSpecIndex(Table t, int specIndex) {
		super(new SpreadsheetExtractionAlgorithm());
		this.specIndex = specIndex;
		this.table = t;
	}
}
