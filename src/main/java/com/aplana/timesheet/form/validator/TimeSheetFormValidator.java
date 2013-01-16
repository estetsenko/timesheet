package com.aplana.timesheet.form.validator;

import com.aplana.timesheet.dao.entity.Employee;
import com.aplana.timesheet.dao.entity.Project;
import com.aplana.timesheet.enums.ProjectRole;
import com.aplana.timesheet.enums.TypeOfActivity;
import com.aplana.timesheet.form.TimeSheetForm;
import com.aplana.timesheet.form.TimeSheetTableRowForm;
import com.aplana.timesheet.service.*;
import com.aplana.timesheet.util.DateTimeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aplana.timesheet.enums.ProjectRole.*;

@Service
public class TimeSheetFormValidator extends AbstractValidator {
    private static final Logger logger = LoggerFactory.getLogger(TimeSheetFormValidator.class);

    @Autowired
    private TimeSheetService timeSheetService;
    @Autowired
    private ProjectService projectService;
        @Autowired
        private ProjectRoleService projectRoleService;
        @Autowired
        private EmployeeService employeeService;
        @Autowired
        private CalendarService calendarService;
        @Autowired
        private DivisionService divisionService;
        @Autowired
        private DictionaryItemService dictionaryItemService;
        @Autowired
        private ProjectTaskService projectTaskService;

    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(TimeSheetForm.class);
    }

    public void validate(Object target, Errors errors) {
        TimeSheetForm tsForm = (TimeSheetForm) target;
        Integer selectedEmployeeId = tsForm.getEmployeeId();
        Employee employee = employeeService.find(selectedEmployeeId);
        ProjectRole emplJob = (employee != null)
                ? getByCode( employee.getJob().getCode() )
                : NOT_DEFINED;

        if (emplJob == null) {
            logger.warn("emplJob is null");
        }

        validateDivision    ( tsForm, errors );
        validateEmployee    ( selectedEmployeeId, errors );
        validateSelectedDate( tsForm, selectedEmployeeId, errors );

        boolean longInactivity = tsForm.isLongVacation() || tsForm.isLongIllness();
        logger.debug("longInactivity = {} .", longInactivity);
        boolean planNecessary = validateLongInactivity( longInactivity, selectedEmployeeId, tsForm, errors );

        // Для табличной части (по строчно).
        List<TimeSheetTableRowForm> tsTablePart = tsForm.getTimeSheetTablePart();

        if (tsTablePart != null) {
            List<TimeSheetTableRowForm> listToRemove = new ArrayList<TimeSheetTableRowForm>();
            logger.debug("TimeSheetForm table has {} lines.", tsTablePart.size());

            int notNullRowNumber = 0;

            validateDuration( tsTablePart, notNullRowNumber, errors );

            for (TimeSheetTableRowForm formRow : tsTablePart) {
                TypeOfActivity actType = TypeOfActivity.getById( formRow.getActivityTypeId() );

                // По каким-то неведомым причинам при нажатии на кнопку веб интерфейса
                // "Удалить выбранные строки" (если выбраны промежуточные строки) они удаляются с формы, но
                // в объект формы вместо них попадают null`ы. Мы эти строки удаляем из объекта формы. Если
                // удалять последние строки (с конца табличной части формы), то все работает корректно.
                // Также, если тип активности не выбран значит вся строка пустая, валидацию ее не проводим и удаляем.
                if ( actType == null ) {
                    listToRemove.add( formRow ); //Удалил проверку на валидность formRow.getActivityTypeId()
                    continue;
                }
                // Если хоть в одной строке таблицы есть тип активности
                // отгул(с отработкой или за переработки), отпуск или болезнь
                // то планы на следующий рабочий день можно не указывать.
                if ( TypeOfActivity.isNotEfficientActivity( actType ) ) {
                    planNecessary = false;
                }
                validateProjectRole       ( formRow, notNullRowNumber, errors );
                valdateCategoryOfActivity ( formRow, emplJob, notNullRowNumber, errors );
                validateDescription       ( formRow, emplJob, notNullRowNumber, errors );
                validateProject           ( formRow, actType, notNullRowNumber, errors );
                notNullRowNumber++;
            }
            tsTablePart.removeAll(listToRemove);

            if ( !longInactivity && tsTablePart.isEmpty()) {
                errors.reject("error.tsform.tablepart.required",
                        "В отчёте должны быть записи.");
            }
        } else if (!longInactivity ) {
            errors.reject("error.tsform.tablepart.required",
                    "В отчёте должны быть записи.");
        }

        validatePlan( tsForm, emplJob, planNecessary, errors );
    }

    private void validatePlan( TimeSheetForm tsForm, ProjectRole emplJob, boolean planNecessary, Errors errors ) {
        String plan = tsForm.getPlan();
        // Планы на следующий рабочий день.
        if (planNecessary && (StringUtils.isBlank( plan ))
                && emplJob != PROJECT_MANAGER
                && emplJob != HEAD_OF_CENTER
        ) {
            errors.rejectValue("plan",
                    "error.tsform.plan.required",
                    "Необходимо указать планы на следующий рабочий день.");
        }
    }

    private void validateDescription( TimeSheetTableRowForm formRow, ProjectRole emplJob, int notNullRowNumber, Errors errors ) {
        String description = formRow.getDescription();
        // Необходимо указать комментарии
        if (description != null) {
            logger.debug("Employee Job: {}", emplJob.getName());

            if ( StringUtils.isNotBlank( description ) && emplJob != PROJECT_MANAGER && emplJob != HEAD_OF_CENTER ) {
                errors.rejectValue( "timeSheetTablePart[" + notNullRowNumber + "].description",
                        "error.tsform.description.required", getErrorMessageArgs( notNullRowNumber ),
                        "Необходимо указать комментарии в строке " + ( notNullRowNumber + 1 ) + "." );
            }
        }
    }

    private Object[] getErrorMessageArgs( int notNullRowNumber ) {
        return new Object[] {"в строке №" + (notNullRowNumber + 1)};
    }

    private void valdateCategoryOfActivity( TimeSheetTableRowForm formRow, ProjectRole emplJob, int notNullRowNumber, Errors errors ) {
        Integer actCatId = formRow.getActivityCategoryId();

        // Не указана категория активности
        if ( isNotChoosed( actCatId ) && ( emplJob != HEAD_OF_CENTER ) ) {
            errors.rejectValue( "timeSheetTablePart[" + notNullRowNumber + "].activityCategoryId",
                    "error.tsform.activity.category.required", getErrorMessageArgs( notNullRowNumber ),
                    "Необходимо указать категорию активности в строке " + ( notNullRowNumber + 1 ) + "." );
        // Неверная категория активности
        } else if ( ! isActCatValid( actCatId, emplJob ) ) {
            errors.rejectValue( "timeSheetTablePart[" + notNullRowNumber + "].activityCategoryId",
                    "error.tsform.activity.category.invalid", getErrorMessageArgs( notNullRowNumber ),
                    "Неверная категория активности в строке " + ( notNullRowNumber + 1 ) + "." );
        }
    }

    private void validateProjectRole( TimeSheetTableRowForm formRow, int notNullRowNumber, Errors errors ) {
        Integer actTypeId = formRow.getActivityTypeId();
        Integer projectRoleId = formRow.getProjectRoleId();

        // Не указана проектная роль
        if ( TypeOfActivity.isEfficientActivity( actTypeId )// APLANATS-276 Роль нужно указывать только для проектных видов деятельности
                && isNotChoosed( projectRoleId )
        ) {
            errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].projectRoleId",
                    "error.tsform.projectrole.required", getErrorMessageArgs( notNullRowNumber ),
                    "Необходимо указать проектную роль в строке " + (notNullRowNumber + 1) + ".");
        // Неверная проектная роль
        } else if (!isProjectRoleValid(projectRoleId)) {
            errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].projectRoleId",
                    "error.tsform.projectrole.invalid", getErrorMessageArgs( notNullRowNumber ),
                    "Неверная проектная роль в строке " + (notNullRowNumber + 1) + ".");
        }
    }

    private void validateProject( TimeSheetTableRowForm formRow, TypeOfActivity actType, int notNullRowNumber, Errors errors ) {
        Integer projectId = formRow.getProjectId();
        String cqId = formRow.getCqId();
        if (projectId != null) {
            Project project = projectService.find(projectId);
            // Необходимо указать проектную задачу
            if (project != null && project.isCqRequired()) {
                if (cqId == null || cqId.equals("0")) {
                    errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].cqId",
                            "error.tsform.cqid.required", getErrorMessageArgs( notNullRowNumber ),
                            "Необходимо выбрать проектную задачу в строке " + (notNullRowNumber + 1) + ".");
                // Неверная проектная задача
                } else if (!isProjectTaskValid(projectId, cqId)) {
                    errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].cqId",
                            "error.tsform.cqid.invalid", getErrorMessageArgs( notNullRowNumber ),
                            "Неверная проектная задача в строке " + (notNullRowNumber + 1) + ".");
                }
            }
        }

        // Не указано название проекта
        // Не указано название пресейла
        if ( ( actType == TypeOfActivity.PROJECT  || actType == TypeOfActivity.PRESALE )
                && isNotChoosed( projectId )
                ) {
            errors.rejectValue( "timeSheetTablePart[" + notNullRowNumber + "].projectId",
                    actType == TypeOfActivity.PRESALE
                            ? "error.tsform.presale.required"
                            : "error.tsform.project.required",
                    getErrorMessageArgs( notNullRowNumber ),
                    "Необходимо указать название проекта в строке " + ( notNullRowNumber + 1 ) + "." );
            // Неверный проект\пресейл
        } else if ( ! isProjectValid( projectId ) ) {
            errors.rejectValue( "timeSheetTablePart[" + notNullRowNumber + "].projectId",
                    "error.tsform.project.presale.invalid", getErrorMessageArgs( notNullRowNumber ),
                    "Неверный проект\\пресейл в строке " + ( notNullRowNumber + 1 ) + "." );
        }
    }

    private void validateDivision( TimeSheetForm tsForm, Errors errors ) {
        // Подразделение не выбрано.
        Integer division = tsForm.getDivisionId();
        if ( isNotChoosed( division ) ) {
            errors.rejectValue( "divisionId",
                    "error.tsform.division.required",
                    "Подразделение не выбрано." );
        // Неверное подразделение.
        } else if ( ! isDivisionValid( division ) ) {
            errors.rejectValue( "divisionId",
                    "error.tsform.division.invalid",
                    "Выбрано неверное подразделение." );
        }
    }

    private void validateEmployee( Integer selectedEmployeeId, Errors errors ) {
        // Сотрудник не выбран.
        if ( isNotChoosed( selectedEmployeeId ) ) {
            errors.rejectValue( "employeeId",
                    "error.tsform.employee.required",
                    "Сотрудник не выбран." );
        // Неверный сотрудник
        } else if ( ! isEmployeeValid( selectedEmployeeId ) ) {
            errors.rejectValue( "employeeId",
                    "error.tsform.employee.invalid",
                    "Неверные данные сотрудника." );
        }
    }

    private void validateSelectedDate( TimeSheetForm tsForm, Integer selectedEmployeeId, Errors errors ) {
        String selectedDate = tsForm.getCalDate();
        // Дата не выбрана.
        if (selectedDate != null) {
            ValidationUtils.rejectIfEmptyOrWhitespace( errors,
                    "calDate",
                    "error.tsform.caldate.required",
                    "Необходимо выбрать дату." );
            // Эти проверки не проводится, если дата не выбрана.
            if ( StringUtils.isNotBlank( selectedDate ) ) {
                // Выбрана недопустимая дата (если сотрудник выбрал дату из
                // диапазона дат, которые еще не внесены в таблицу calendar.
                if ( ! isCaldateValid( tsForm.getCalDate() ) ) {
                    errors.rejectValue( "calDate",
                            "error.tsform.caldate.invalid",
                            "Выбрана недопустимая дата." );
                // Сотрудник уже отправлял отчёт за выбранную дату.
                } else if ( ! isCaldateUniqueForCurrentEmployee( selectedDate, selectedEmployeeId ) ) {
                    Object[] errorMessageArgs = { DateTimeUtil.formatDateString( selectedDate ) };
                    errors.rejectValue( "calDate",
                            "error.tsform.caldate.notuniq",
                            errorMessageArgs,
                            "Вы уже списывали занятость за " + DateTimeUtil.formatDateString( selectedDate ) );
                }
            }
        }
    }

    private boolean validateLongInactivity( boolean longInactivity, Integer selectedEmployeeId, TimeSheetForm tsForm, Errors errors ) {
        //Если выбран долгий отпуск или долгая болезнь
        if ( longInactivity ) {

            String beginDate = tsForm.getBeginLongDate();
            String endDate = tsForm.getEndLongDate();
            //Не указана дата начала
            ValidationUtils.rejectIfEmptyOrWhitespace( errors,
                    "beginLongDate",
                    "error.tsform.beginlongdate.required",
                    "Необходимо выбрать дату начала отпуска\\болезни." );
            //Не указана дата окончания
            ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                    "endLongDate",
                    "error.tsform.endlongdate.required",
                    "Необходимо выбрать дату окончания отпуска\\болезни.");
            //Дата окончания не может быть раньше даты начала
            if ( ! DateTimeUtil.isPeriodValid( beginDate, endDate ) ) {
                errors.rejectValue("beginLongDate",
                        "error.tsform.datesegment.notvalid",
                        "Дата окончания не может быть раньше даты начала.");
            }
            //Недопустимя дата начала
            if ( StringUtils.isNotBlank( beginDate ) && ! isCaldateValid( beginDate ) ) {
                errors.rejectValue( "beginLongDate",
                        "error.tsform.beginlongdate.invalid",
                        "Выбрана недопустимая дата начала отпуска\\болезни." );
            }
            //Недопустимая дата окончания
            if ( StringUtils.isNotBlank( endDate ) && ! isCaldateValid( endDate ) ) {
                errors.rejectValue( "endLongDate",
                        "error.tsform.endlongdate.invalid",
                        "Выбрана недопустимая дата окончания отпуска\\болезни." );
            }
            //Сотрудник уже отправлял отчёт за выбранную дату.
            List<String> splittedDateRange = DateTimeUtil.splitDateRangeOnDays(beginDate, endDate);
            for ( String dateInStr : splittedDateRange ) {
                if ( ! isCaldateUniqueForCurrentEmployee( dateInStr, selectedEmployeeId ) ) {
                    if ( ! ( "".equals( beginDate ) || "".equals( endDate ) ) ) {
                        Object[] errorMessageArgs = { DateTimeUtil.formatDateString( dateInStr ) };
                        errors.rejectValue( "calDate",
                                "error.tsform.caldate.notuniq",
                                errorMessageArgs,
                                "Вы уже списывали занятость за " + DateTimeUtil.formatDateString( dateInStr ) );
                    }
                }
            }
            return false;
        }
        return true;
    }

    private void validateDuration( List<TimeSheetTableRowForm> tsTablePart, int notNullRowNumber, Errors errors ) {
        double totalDuration = 0;

        for ( TimeSheetTableRowForm rowForm : tsTablePart ) {
            String durationStr = rowForm.getDuration();

            // Необходимо указать часы
            if (durationStr != null) {
                ValidationUtils.rejectIfEmptyOrWhitespace( errors,
                        "timeSheetTablePart[" + notNullRowNumber + "].duration",
                        "error.tsform.duration.required", getErrorMessageArgs( notNullRowNumber ),
                        "Необходимо указать часы в строке " + ( notNullRowNumber + 1 ) + "." );
                // Часы должны быть указаны в правильном формате (1, 1.2, 5.5 и т.п.)
                // and may be 1,2; 2,3
                if ( StringUtils.isNotBlank( durationStr )) {
                    Pattern p1 = Pattern.compile("([0-9]*)(\\.|,)[0-9]");
                    Pattern p2 = Pattern.compile("([0-9]*)");
                    Matcher m1 = p1.matcher(durationStr);
                    Matcher m2 = p2.matcher(durationStr);
                    if (!m1.matches() && !m2.matches()) {
                        errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].duration",
                                "error.tsform.duration.format", getErrorMessageArgs( notNullRowNumber ),
                                "Количество часов указано не верно в строке " + (notNullRowNumber + 1)
                                        + ". Примеры правильных значений (5, 3.5, 2.0 и т.п.).");
                    } else {
                        double duration = Double.parseDouble(durationStr.replace(",","."));
                        // Количество часов должно быть больше нуля
                        if (duration <= 0) {
                            errors.rejectValue("timeSheetTablePart[" + notNullRowNumber + "].duration",
                                    "error.tsform.duration.length", getErrorMessageArgs( notNullRowNumber ),
                                    "Количество часов должно быть больше нуля в строке " + (notNullRowNumber + 1) + ".");
                        }
                        // Считаем общее количество часов
                        totalDuration += duration;
                    }
                }
            }
        }

        logger.debug("Total duration is {}", totalDuration);

        // Сумма часов превышает 24.
        if (totalDuration > 24) {
            errors.rejectValue("totalDuration",
                    "error.tsform.total.duration.max",
                    "Сумма часов не должна превышать 24.");
        }
    }

    /*
    * Возвращает false, если сотрудник уже отправлял отчет за выбранную дату.
    */

    private boolean isCaldateUniqueForCurrentEmployee(String calDate, Integer employeeId) {
        return timeSheetService.findForDateAndEmployee( calDate, employeeId ) == null;
    }

    /*
      * Возвращает true, если введённая дата присутствует в таблице calendar и false, если нет.
      */
    private boolean isCaldateValid(String date) {
        return calendarService.find( date ) != null;
    }

    private boolean isDivisionValid(Integer division) {
        return divisionService.find( division ) != null;
    }

    private boolean isEmployeeValid(Integer employee) {
        return employeeService.find( employee ) != null;
    }

    private boolean isActCatValid(Integer actCat, ProjectRole emplJob) {
        if (actCat == null ||
                //У проектной роли "Руководитель центра" нет доступных категорий активности.
                ( emplJob == HEAD_OF_CENTER && actCat == 0 )
        ) {
            return true;
        }
        return dictionaryItemService.find(actCat) != null;
    }

    private boolean isProjectValid(Integer project) {
        return project == null || projectService.findActive( project ) != null;
    }

    private boolean isProjectRoleValid(Integer projectRole) {
        return projectRole == null || projectRoleService.findActive( projectRole ) != null;
    }

    private boolean isProjectTaskValid(Integer project, String task) {
        if (project == null && task == null) {
            return true;
        }
        return projectTaskService.find( project, task ) != null;
    }
}