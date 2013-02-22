package com.aplana.timesheet.service.MailSenders;

import com.aplana.timesheet.dao.entity.VacationApproval;
import com.aplana.timesheet.properties.TSPropertyProvider;
import com.aplana.timesheet.service.SendMailService;
import org.apache.commons.lang.time.DateFormatUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author iziyangirov
 */

public class VacationApprovalAcceptanceSender extends AbstractVacationApprovalSenderWithCopyToAuthor {

    final String DATE_FORMAT = "dd.MM.yyyy";
    final String MAIL_ACCEPT_SUBJECT = "Согласование \"%s\" сотрудника %s на период с %s - %s";
    final String MAIL_ACCEPT_BODY = "%s согласовал(а) \"%s\" сотрудника %s из г. %s на период с %s - %s.";
    final String MAIL_REFUSE_SUBJECT = "Согласование \"%s\" сотрудника %s на период с %s - %s";
    final String MAIL_REFUSE_BODY = "%s не согласовал(а) \"%s\" сотрудника %s из г. %s на период с %s - %s.";

    public VacationApprovalAcceptanceSender(SendMailService sendMailService, TSPropertyProvider propertyProvider) {
        super(sendMailService, propertyProvider);
    }

    @Override
    protected List<Mail> getMainMailList(VacationApproval vacationApproval) {
        Mail mail = new Mail();

        Integer vacationId = vacationApproval.getVacation().getId();
        String matchingFIO = vacationApproval.getManager().getName();
        String vacationType = vacationApproval.getVacation().getType().getValue();
        String employeeFIO = vacationApproval.getVacation().getEmployee().getName();
        String region = vacationApproval.getVacation().getEmployee().getRegion().getName();
        String dateBegin = DateFormatUtils.format(vacationApproval.getVacation().getBeginDate(), DATE_FORMAT);
        String dateEnd = DateFormatUtils.format(vacationApproval.getVacation().getEndDate(), DATE_FORMAT);
        Boolean result = vacationApproval.getResult();

        String subject = result ? String.format(MAIL_ACCEPT_SUBJECT, vacationType, employeeFIO, dateBegin, dateEnd) :
                String.format(MAIL_REFUSE_SUBJECT, vacationType, employeeFIO, dateBegin, dateEnd);
        String text = result ? String.format(MAIL_ACCEPT_BODY, matchingFIO, vacationType, employeeFIO, region, dateBegin, dateEnd) :
                String.format(MAIL_REFUSE_BODY, matchingFIO, vacationType, employeeFIO, region, dateBegin, dateEnd);

        mail.setFromEmail(propertyProvider.getMailFromAddress());
        mail.setSubject(subject);
        mail.setPreconstructedMessageBody(text);
        mail.setToEmails( Arrays.asList(vacationApproval.getVacation().getEmployee().getEmail()) );
        mail.setCcEmails(sendMailService.getVacationApprovalEmailList(vacationId));

        return Arrays.asList(mail);
    }

}
