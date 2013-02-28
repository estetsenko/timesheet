package com.aplana.timesheet.reports;

import com.aplana.timesheet.enums.OvertimeCategory;
import net.sf.jasperreports.engine.JRDataSource;

public class Report01 extends BaseReport {

    public static final String jrName = "report01";

    public static final String jrNameFile = "Отчет №1. Переработки, работа в выходные и праздничные дни";
	
	private OvertimeCategory category;

    @Override
    public String getJRName() {
        return jrName;
    }

    @Override
    public String getJRNameFile() {
        return jrNameFile;
    }

	public OvertimeCategory getCategory() {
		return category;
	}

    @Override
    public JRDataSource prepareDataSource() {
        return reportDAO.getReportData(this);
    }

	public void setCategory(OvertimeCategory category) {
		this.category = category;
	}
}
