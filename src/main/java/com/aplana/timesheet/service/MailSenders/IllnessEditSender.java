package com.aplana.timesheet.service.MailSenders;

import com.aplana.timesheet.constants.PadegConstants;
import com.aplana.timesheet.dao.entity.Illness;
import com.aplana.timesheet.properties.TSPropertyProvider;
import com.aplana.timesheet.service.EmployeeService;
import com.aplana.timesheet.service.ProjectService;
import com.aplana.timesheet.service.SendMailService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import padeg.lib.Padeg;

public class IllnessEditSender extends AbstractIllnessSender{
    public IllnessEditSender(SendMailService sendMailService, TSPropertyProvider propertyProvider, ProjectService projectService, EmployeeService employeeService) {
        super(sendMailService, propertyProvider, projectService, employeeService);
    }

    @Override
    protected String getSubject(Illness illness) {
        return propertyProvider.getIllnessMailMarker() +
                String.format(" Отредактирован больничный %s", Padeg.getFIOPadegFS(illness.getEmployee().getName(), true, PadegConstants.Roditelnyy));
    }

    @Override
    protected String getBody(Illness illness) {
        String employeeNameStr = Padeg.getFIOPadegFS(illness.getEmployee().getName(), true, PadegConstants.Roditelnyy);
        String regionNameStr = illness.getEmployee().getRegion().getName();
        String beginDateStr = DateFormatUtils.format(illness.getBeginDate(), DATE_FORMAT);
        String endDateStr = DateFormatUtils.format(illness.getEndDate(), DATE_FORMAT);
        String editionDate = DateFormatUtils.format(illness.getEditionDate(), DATE_FORMAT);
        String authorVacation = Padeg.getFIOPadegFS(illness.getAuthor().getName(), true, PadegConstants.Tvoritelnyy);
        String commentStr = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(illness.getComment())) {
            commentStr = String.format("Комментарий: %s. ", illness.getComment());
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Информируем Вас о редактировании больничного "));
        stringBuilder.append(String.format("сотрудника %s ", employeeNameStr));
        stringBuilder.append(String.format("из г. %s ", regionNameStr));
        stringBuilder.append(String.format("на период с %s по %s. ", beginDateStr, endDateStr));
        stringBuilder.append(String.format("%s", commentStr));
        stringBuilder.append(String.format("Запись о больничном отредактирована %s. ", authorVacation));
        stringBuilder.append(String.format("Дата редактирования %s.", editionDate));

        return stringBuilder.toString();
    }
}
