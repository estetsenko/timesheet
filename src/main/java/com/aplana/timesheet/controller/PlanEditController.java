package com.aplana.timesheet.controller;

import argo.jdom.*;
import argo.saj.InvalidSyntaxException;
import com.aplana.timesheet.constants.TimeSheetConstants;
import com.aplana.timesheet.dao.EmployeeDAO;
import com.aplana.timesheet.dao.entity.*;
import com.aplana.timesheet.enums.*;
import com.aplana.timesheet.exception.service.NotDataForYearInCalendarException;
import com.aplana.timesheet.form.PlanEditForm;
import com.aplana.timesheet.form.validator.PlanEditFormValidator;
import com.aplana.timesheet.service.*;
import com.aplana.timesheet.util.DateTimeUtil;
import com.aplana.timesheet.util.EnumsUtils;
import com.aplana.timesheet.util.JsonUtil;
import com.google.common.collect.Maps;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;
import java.util.Calendar;

import static argo.jdom.JsonNodeBuilders.aStringBuilder;
import static argo.jdom.JsonNodeBuilders.anObjectBuilder;
import static argo.jdom.JsonNodeFactories.*;
import static com.aplana.timesheet.util.JsonUtil.aNumberBuilder;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
@Controller
public class PlanEditController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanEditController.class);

    public static final String _PLAN = "_plan";
    public static final String _FACT = "_fact";

    public static final String SUMMARY = "summary";
    public static final String CENTER_PROJECTS = "center_projects";
    public static final String CENTER_PRESALES = "center_presales";
    public static final String OTHER_PROJECTS_AND_PRESALES = "other_projects_and_presales";
    public static final String NON_PROJECT = "non_project";
    public static final String ILLNESS = "illness";
    public static final String VACATION = "vacation";
    public static final String EMPLOYEE = "employee";
    public static final String EMPLOYEE_ID = "employee_id";
    public static final String PERCENT_OF_CHARGE = "percent_of_charge";

    public static final String SUMMARY_PROJECTS = "summary_projects";
    public static final String SUMMARY_PRESALES = "summary_presales";
    public static final String SUMMARY_INVESTMENT = "summary_investment";
    public static final String SUMMARY_COMMERCIAL = "summary_commercial";
    public static final String SUMMARY_PROJECTS_PLAN = SUMMARY_PROJECTS + _PLAN;
    public static final String SUMMARY_PROJECTS_FACT = SUMMARY_PROJECTS + _FACT;
    public static final String SUMMARY_PRESALES_PLAN = SUMMARY_PRESALES + _PLAN;
    public static final String SUMMARY_PRESALES_FACT = SUMMARY_PRESALES + _FACT;
    public static final String SUMMARY_INVESTMENT_PLAN = SUMMARY_INVESTMENT + _PLAN;
    public static final String SUMMARY_INVESTMENT_FACT = SUMMARY_INVESTMENT + _FACT;
    public static final String SUMMARY_COMMERCIAL_PLAN = SUMMARY_COMMERCIAL + _PLAN;
    public static final String SUMMARY_COMMERCIAL_FACT = SUMMARY_COMMERCIAL + _FACT;

    public static final String PROJECT_ID = "id";
    public static final String PROJECT_NAME = "name";
    public static final String PROJECTS_PLANS = "projects_plans";
    public static final String SUMMARY_PLAN = SUMMARY + _PLAN;

    public static final String SUMMARY_FACT = SUMMARY + _FACT;
    public static final String CENTER_PROJECTS_PLAN = CENTER_PROJECTS + _PLAN;
    public static final String CENTER_PROJECTS_FACT = CENTER_PROJECTS + _FACT;
    public static final String CENTER_PRESALES_PLAN = CENTER_PRESALES + _PLAN;
    public static final String CENTER_PRESALES_FACT = CENTER_PRESALES + _FACT;
    public static final String OTHER_PROJECTS_AND_PRESALES_PLAN = OTHER_PROJECTS_AND_PRESALES + _PLAN;
    public static final String OTHER_PROJECTS_AND_PRESALES_FACT = OTHER_PROJECTS_AND_PRESALES + _FACT;
    public static final String NON_PROJECT_PLAN = NON_PROJECT + _PLAN;
    public static final String NON_PROJECT_FACT = NON_PROJECT + _FACT;
    public static final String ILLNESS_PLAN = ILLNESS + _PLAN;
    public static final String ILLNESS_FACT = ILLNESS + _FACT;
    public static final String VACATION_PLAN = VACATION + _PLAN;
    public static final String VACATION_FACT = VACATION + _FACT;
    public static final String PERCENT_OF_CHARGE_PLAN = PERCENT_OF_CHARGE + _PLAN;
    public static final String PERCENT_OF_CHARGE_FACT = PERCENT_OF_CHARGE + _FACT;

    public static final String JSON_DATA_YEAR = "year";
    public static final String JSON_DATA_MONTH = "month";
    public static final String JSON_DATA_ITEMS = "items";

    public static final String PLAN_SAVE_URL = "/planSave";
    public static final String PLAN_EDIT_URL = "/planEdit";
    public static final String EXPORT_TABLE_EXCEL = "/exportTableExcel";
    private static final String COOKIE_DIVISION_ID = "cookie_division_id";

    private static final String COOKIE_REGIONS = "cookie_regions";
    private static final String COOKIE_PROJECT_ROLES = "cookie_project_roles";

    private static final String COOKIE_SHOW_PLANS = "cookie_show_plans";
    private static final String COOKIE_SHOW_FACTS = "cookie_show_facts";
    private static final String COOKIE_SHOW_PROJECTS = "cookie_show_projects";
    private static final String COOKIE_SHOW_PRESALES = "cookie_show_presales";
    private static final String COOKIE_SHOW_SUMMARY_PROJECTS_PRESALES = "cookie_show_summary_projects_presales";
    private static final String COOKIE_SHOW_SUMMARY_FUNDING = "cookie_show_summary_funding";
    private static final String COOKIE_MONTH = "cookie_month";
    private static final String COOKIE_MANAGER = "cookie_manager";
    public  static final int    COOKIE_MAX_AGE = 999999999;

    private static final String SEPARATOR = "~";

    public static final JsonStringNode PROJECTS_PLANS_FIELD = string(PROJECTS_PLANS);
    public static final JsonStringNode NON_PROJECT_PLAN_FIELD = string(NON_PROJECT_PLAN);
    public static final JsonStringNode ILLNESS_PLAN_FIELD = string(ILLNESS_PLAN);
    public static final JsonStringNode VACATION_PLAN_FIELD = string(VACATION_PLAN);
    public static final JsonStringNode OTHER_PROJECTS_AND_PRESALES_PLAN_FIELD =
            string(OTHER_PROJECTS_AND_PRESALES_PLAN);

    public static final Map<JsonStringNode, TSEnum> PLAN_TYPE_MAP = new HashMap<JsonStringNode, TSEnum>();

    static {
        PLAN_TYPE_MAP.put(NON_PROJECT_PLAN_FIELD, EmployeePlanType.NON_PROJECT);
        PLAN_TYPE_MAP.put(ILLNESS_PLAN_FIELD, EmployeePlanType.ILLNESS);
        PLAN_TYPE_MAP.put(VACATION_PLAN_FIELD, EmployeePlanType.VACATION);
        PLAN_TYPE_MAP.put(OTHER_PROJECTS_AND_PRESALES_PLAN_FIELD, EmployeePlanType.WORK_FOR_OTHER_DIVISIONS);
    }

    ////////////////////////////////////////////////////////////////

    private static double nilIfNull(Double value) {
        return (value == null) ? 0 : value;
    }

    private static boolean isPresale(Project project) {
        return (EnumsUtils.getEnumById(project.getState().getId(), TypesOfActivityEnum.class) == TypesOfActivityEnum.PRESALE);
    }

    private static boolean isProject(Project project) {
        return (EnumsUtils.getEnumById(project.getState().getId(), TypesOfActivityEnum.class) == TypesOfActivityEnum.PROJECT);
    }

    private static boolean isCommercialProject(Project project) {
        return (EnumsUtils.getEnumById(project.getFundingType().getId(), ProjectFundingTypeEnum.class) == ProjectFundingTypeEnum.COMMERCIAL_PROJECT);
    }

    private static boolean isInvestmentProject(Project project) {
        return (EnumsUtils.getEnumById(project.getFundingType().getId(), ProjectFundingTypeEnum.class) == ProjectFundingTypeEnum.INVESTMENT_PROJECT);
    }

    private static <T> T defaultValue(T value, T defaultValue) {
        return (value == null) ? defaultValue : value;
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Integer> tryParseIntegerListFromString(String string) {
        try {
            final List<Integer> list = new ArrayList<Integer>();

            for (String s : StringUtils.split(string, SEPARATOR)) {
                list.add(Integer.valueOf(s));
            }

            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean tryParseBoolean(String value) {
        try {
            return Boolean.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static void addCookie(HttpServletResponse response, String name, Object value) {
        final String valueStr = String.valueOf(value);

        final Cookie cookieToDelete = new Cookie(name, valueStr);
        final Cookie cookie = new Cookie(name, valueStr);

        cookieToDelete.setMaxAge(0);
        cookie.setMaxAge(COOKIE_MAX_AGE);

        response.addCookie(cookieToDelete);
        response.addCookie(cookie);
    }

    @Autowired
    private PlanEditFormValidator planEditFormValidator;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRoleService projectRoleService;

    @Autowired
    private RegionService regionregionService;

    @Autowired
    private DivisionService divisionService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private EmployeeProjectPlanService employeeProjectPlanService;

    @Autowired
    private EmployeePlanService employeePlanService;

    @Autowired
    private PlanEditService planEditService;

    @Autowired
    private VacationService vacationService;

    @Autowired
    private PlanEditExcelReportService planEditExcelReportService;

    @Autowired
    private IllnessService illnessService;

    @RequestMapping(PLAN_EDIT_URL)
    public ModelAndView showForm(
            @ModelAttribute(PlanEditForm.FORM) PlanEditForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        initForm(form, request);

        saveCookie(form, response);

        return createModelAndView(form, bindingResult);
    }

    /**
     *
     * @param form
     * @return true, if form was init not from cookies
     */
    private void initForm(PlanEditForm form, HttpServletRequest request) {
        initDefaultForm(form);

        final Cookie[] cookies = request.getCookies();

        String name, value;

        for (Cookie cookie : cookies) {
            name = cookie.getName();
            value = cookie.getValue();
            if (COOKIE_DIVISION_ID.equals(name)) {
                form.setDivisionId(defaultValue(tryParseInt(value), form.getDivisionId()));
            } else if (COOKIE_REGIONS.equals(name)) {
                form.setRegions(defaultValue(tryParseIntegerListFromString(value), form.getRegions()));
            } else if (COOKIE_PROJECT_ROLES.equals(name)) {
                form.setProjectRoles(defaultValue(tryParseIntegerListFromString(value), form.getProjectRoles()));
            } else if (COOKIE_SHOW_PLANS.equals(name)) {
                form.setShowPlans(defaultValue(tryParseBoolean(value), form.getShowPlans()));
            } else if (COOKIE_SHOW_FACTS.equals(name)) {
                form.setShowFacts(defaultValue(tryParseBoolean(value), form.getShowFacts()));
            } else if (COOKIE_SHOW_PROJECTS.equals(name)) {
                form.setShowProjects(defaultValue(tryParseBoolean(value), form.getShowProjects()));
            } else if (COOKIE_SHOW_PRESALES.equals(name)) {
                form.setShowPresales(defaultValue(tryParseBoolean(value), form.getShowPresales()));
            } else if (COOKIE_MONTH.equals(name)) {
                form.setMonth(defaultValue(tryParseInt(value), form.getMonth()));
            } else if (COOKIE_MANAGER.equals(name)) {
                form.setManager(defaultValue(tryParseInt(value), form.getManager()));
            } else if (COOKIE_SHOW_SUMMARY_PROJECTS_PRESALES.equals(name)) {
                form.setShowSumProjectsPresales(defaultValue(tryParseBoolean(value), form.getShowSumProjectsPresales()));
            } else if (COOKIE_SHOW_SUMMARY_FUNDING.equals(name)) {
                form.setShowSumFundingType(defaultValue(tryParseBoolean(value), form.getShowSumFundingType()));
            }
        }
    }

    private void initDefaultForm(PlanEditForm form) {
        final Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);

        form.setDivisionId(securityService.getSecurityPrincipal().getEmployee().getDivision().getId());
        form.setYear(year);
        form.setMonth(calendar.get(Calendar.MONTH) + 1);
        form.setRegions(Arrays.asList(PlanEditForm.ALL_VALUE));
        form.setProjectRoles(Arrays.asList(PlanEditForm.ALL_VALUE));
        form.setShowPlans(Boolean.TRUE);
        form.setShowFacts(Boolean.TRUE);
        form.setShowProjects(Boolean.TRUE);
        form.setShowPresales(Boolean.TRUE);
        form.setShowSumProjectsPresales(Boolean.TRUE);
        form.setShowSumFundingType(Boolean.FALSE);
    }

    private List<Region> getRegionList() {
        return regionregionService.getRegions();
    }

    private List<Employee> getManagerList() {
        return employeeService.getManagerListForAllEmployee();
    }

    private String getManagerListJson() {
        return employeeService.getManagerListJson();
    }

    private List<ProjectRole> getProjectRoleList() {
        return projectRoleService.getProjectRoles();
    }

    private List<Division> getDivisionList() {
        return divisionService.getDivisions();
    }

    private List<com.aplana.timesheet.dao.entity.Calendar> getYearList() {
        return DateTimeUtil.getYearsList(calendarService);
    }

    private String getMonthMapAsJson(List<com.aplana.timesheet.dao.entity.Calendar> yearList) {
        return DateTimeUtil.getMonthListJson(yearList, calendarService);
    }

    @RequestMapping(value = PLAN_EDIT_URL, method = RequestMethod.POST)
    public ModelAndView showTable(
            @ModelAttribute(PlanEditForm.FORM) PlanEditForm form,
            BindingResult bindingResult,
            HttpServletResponse response
    ) {
        final ModelAndView modelAndView = createModelAndView(form, bindingResult);

        if (!bindingResult.hasErrors()) {
            saveCookie(form, response);
        }

        return modelAndView;
    }

    @RequestMapping(value = EXPORT_TABLE_EXCEL+"/{year}/{month}", method = RequestMethod.POST)
    public ModelAndView exportTableExcel(
            @ModelAttribute("year") Integer year,
            @ModelAttribute("month") Integer month,
            @ModelAttribute(PlanEditForm.FORM) PlanEditForm form,
            BindingResult bindingResult, HttpServletRequest request,
            HttpServletResponse response
    ) {

        final Date date = DateTimeUtil.createDate(year, month);

        String dataAsJson = getDataAsJson(form, date);
        List<Project> projectList = getProjects(form, date);

        com.aplana.timesheet.dao.entity.Calendar calDate = calendarService.find(new Timestamp(date.getTime()));

        String reportName = "Планирование занятости за "+calDate.getMonthTxt()+" "+year.toString()+" года";

        // TODO Спорный способ передавать сюда JSON, чтобы потом его снова разбирать. Переделать
        planEditExcelReportService.createAndExportReport(reportName, dataAsJson, projectList, response, request);

        return null;
    }

    private void saveCookie(PlanEditForm form, HttpServletResponse response) {
        addCookie(response, COOKIE_DIVISION_ID, form.getDivisionId());
        addCookie(response, COOKIE_SHOW_PLANS, form.getShowPlans());
        addCookie(response, COOKIE_SHOW_FACTS, form.getShowFacts());
        addCookie(response, COOKIE_SHOW_PROJECTS, form.getShowProjects());
        addCookie(response, COOKIE_SHOW_PRESALES, form.getShowPresales());
        addCookie(response, COOKIE_REGIONS, StringUtils.join(form.getRegions(), SEPARATOR));
        addCookie(response, COOKIE_PROJECT_ROLES, StringUtils.join(form.getProjectRoles(), SEPARATOR));
        addCookie(response, COOKIE_MONTH, form.getMonth());
        addCookie(response, COOKIE_MANAGER, form.getManager());
        addCookie(response, COOKIE_SHOW_SUMMARY_PROJECTS_PRESALES, form.getShowSumProjectsPresales());
        addCookie(response, COOKIE_SHOW_SUMMARY_FUNDING, form.getShowSumFundingType());
    }

    private ModelAndView createModelAndView(PlanEditForm form, BindingResult bindingResult) {
        final ModelAndView modelAndView = new ModelAndView("planEdit");

        modelAndView.addObject("regionList", getRegionList());
        modelAndView.addObject("managerList", getManagerList());
        modelAndView.addObject("managerMapJson", getManagerListJson());
        modelAndView.addObject("projectRoleList", getProjectRoleList());
        modelAndView.addObject("divisionList", getDivisionList());

        final List<com.aplana.timesheet.dao.entity.Calendar> yearList = getYearList();

        modelAndView.addObject("yearList", yearList);
        modelAndView.addObject("monthMapJson", getMonthMapAsJson(yearList));

        planEditFormValidator.validate(form, bindingResult);

        Boolean editable = Boolean.FALSE;

        if (!bindingResult.hasErrors() && (form.getShowPlans() || form.getShowFacts())) {
            fillTableData(modelAndView, form);
            modelAndView.addObject("monthList", calendarService.getMonthList(form.getYear()));
            editable = isEditable(form);
        }

        modelAndView.addObject("editable", editable);

        return modelAndView;
    }

    private Boolean isEditable(PlanEditForm form) {
        final Calendar calendar = DateTimeUtil.getCalendar(form.getYear(), form.getMonth());

        return (DateUtils.truncatedCompareTo(new Date(), calendar.getTime(), Calendar.MONTH) <= 0);
    }

    private void fillTableData(ModelAndView modelAndView, PlanEditForm form) {
        final Date date = DateTimeUtil.createDate(form.getYear(), form.getMonth());

        modelAndView.addObject("jsonDataToShow", getDataAsJson(form, date));
        modelAndView.addObject("projectListJson", getProjectListAsJson(getProjects(form, date)));
    }

    private List<Project> getProjects(PlanEditForm form, Date date) {
        final List<Integer> projectStates = getProjectStates(form);

        return projectStates.isEmpty()
                ? new ArrayList<Project>()
                : projectService.getProjectsByStatesForDateAndDivisionId(
                        projectStates,
                        date,
                        form.getDivisionId()
                );
    }

    private String getProjectListAsJson(List<Project> projects) {
        final List<JsonNode> nodes = new ArrayList<JsonNode>();

        for (Project project : projects) {
            nodes.add(
                    object(
                            field(PROJECT_ID, number(project.getId())),
                            field(PROJECT_NAME, string(project.getName()))
                    )
            );
        }

        return JsonUtil.format(array(nodes));
    }

    private List<Integer> getProjectStates(PlanEditForm form) {
        final List<Integer> states = new ArrayList<Integer>();

        if (form.getShowProjects()) {
            states.add(TypesOfActivityEnum.PROJECT.getId());
        }

        if (form.getShowPresales()) {
            states.add(TypesOfActivityEnum.PRESALE.getId());
        }

        return states;
    }

    private String getDataAsJson(PlanEditForm form, Date date) {
        final List<Employee> employees;
        final Integer managerId = form.getManager();
        LOGGER.debug("manager = {}",managerId);
        if (managerId == null || managerId < 1) {
            employees = employeeService.getDivisionEmployees(
                    form.getDivisionId(),
                    date,
                    getRegionIds(form),
                    getProjectRoleIds(form)
            );
        } else {
            employees = employeeService.getDivisionEmployeesByManager(
                    form.getDivisionId(),
                    date,
                    getRegionIds(form),
                    getProjectRoleIds(form),
                    managerId
            );
            Employee manager = employeeService.find(managerId);
            if (!employees.contains(manager)) {
                employees.add(manager);
                Collections.sort(employees);
            }
        }
        final ArrayList<JsonNode> nodes = new ArrayList<JsonNode>();

        final Integer year = form.getYear();
        final Integer month = form.getMonth();
        final boolean showPlans = form.getShowPlans();
        final boolean showFacts = form.getShowFacts();

        JsonObjectNodeBuilder builder;
        int workDaysCount;

        for (Employee employee : employees) {
            builder = anObjectBuilder().
                    withField(EMPLOYEE_ID, aNumberBuilder(employee.getId())).
                    withField(EMPLOYEE, aStringBuilder(employee.getName()));

            workDaysCount = calendarService.getEmployeeRegionWorkDaysCount(employee, year, month);


            final double summaryPlan = TimeSheetConstants.WORK_DAY_DURATION * workDaysCount * employee.getJobRate();

            if (showPlans) {
                appendToBuilder(builder, getPlans(employee, year, month, summaryPlan, form));
            }

            if (showFacts) {
                appendToBuilder(builder, getFacts(employee, year, month, summaryPlan, form));
            }

            nodes.add(builder.build());
        }

        return JsonUtil.format(array(nodes));
    }

    private void appendToBuilder(JsonObjectNodeBuilder builder, Map<String, JsonNodeBuilder> map) {
        for (Map.Entry<String, JsonNodeBuilder> entry : map.entrySet()) {
            builder.withField(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, JsonNodeBuilder> getPlans(Employee employee, Integer year, Integer month, Double summaryPlan, PlanEditForm form) {
        final Division division = employee.getDivision();
        final Map<String, JsonNodeBuilder> map = Maps.newHashMap();

        Double centerProjectsPlan = null;
        Double centerPresalesPlan = null;
        double otherProjectsPlan = 0;
        double sumOfPlanCharge = 0;
        Double sumProjectsPlan = null;
        Double sumPresalesPlan = null;
        Double sumInvestPlan = null;
        Double sumCommercePlan = null;

        for (EmployeeProjectPlan employeeProjectPlan : employeeProjectPlanService.find(employee, year, month)) {
            final Project project = employeeProjectPlan.getProject();
            final double duration = nilIfNull(employeeProjectPlan.getValue());

            if (project != null) {
                if (division.equals(project.getDivision())) {
                    if (isPresale(project)) {
                        centerPresalesPlan = nilIfNull(centerPresalesPlan) + duration;
                    } else {
                        centerProjectsPlan = nilIfNull(centerProjectsPlan) + duration;
                    }
                } else {
                    otherProjectsPlan += duration;
                }

                /* расчёт итого по проектам/пресейлам */
                if (isProject(project)) {
                    sumProjectsPlan = nilIfNull(sumProjectsPlan) + duration;
                } else {
                    sumPresalesPlan = nilIfNull(sumPresalesPlan) + duration;
                }

                /* расчёт итого по инвест/комерц проектам */
                if (isProject(project) && isCommercialProject(project)) {
                    sumCommercePlan = nilIfNull(sumCommercePlan) + duration;
                }
                if ( (isProject(project) && isInvestmentProject(project)) || (isPresale(project)) ) {
                    sumInvestPlan = nilIfNull(sumInvestPlan) + duration;
                }

                appendNumberField(map, String.format("%d" + _PLAN, project.getId()), employeeProjectPlan.getValue());
            }
        }

        appendNumberField(map, OTHER_PROJECTS_AND_PRESALES_PLAN, otherProjectsPlan);
        Double summaryWorkHours = getEmployeeProjectDurationPlan(employee, year, month);
        Double nonProjectDuration = getEmployeeNonProjectDuration(employee, year, month);

        sumOfPlanCharge += nilIfNull(centerProjectsPlan) + nilIfNull(centerPresalesPlan) + nilIfNull(otherProjectsPlan);

        Double vacationPlan = 0.0;
        try {
            vacationPlan = vacationService.getVacationsWorkdaysCount(employee, year, month);
        } catch (NotDataForYearInCalendarException e) {
            LOGGER.error("Error in com.aplana.timesheet.controller.PlanEditController.getPlans : " + e.getMessage());
        }
        vacationPlan*=TimeSheetConstants.WORK_DAY_DURATION;

        appendNumberField(map, CENTER_PROJECTS_PLAN, centerProjectsPlan);
        appendNumberField(map, CENTER_PRESALES_PLAN, centerPresalesPlan);

        if (form.getShowSumProjectsPresales()) {
            appendNumberField(map, SUMMARY_PROJECTS_PLAN, sumProjectsPlan);
            appendNumberField(map, SUMMARY_PRESALES_PLAN, sumPresalesPlan);
        }

        Double value;

        for (EmployeePlan employeePlan : employeePlanService.find(employee, year, month)) {
            value = employeePlan.getValue();

            sumOfPlanCharge += nilIfNull(value);

            /* непроектная активность */
            if ( employeePlan.getType().getId().equals(TypesOfActivityEnum.NON_PROJECT.getId()) ) {
                sumInvestPlan = nilIfNull(sumInvestPlan) + value;
            }

            appendNumberField(map, getFieldNameForEmployeePlan(employeePlan), value);
        }
        /* т.к. план по болезни входит в EmployeePlan перепишем его значением фактического */
        appendNumberField(
                map,
                ILLNESS_PLAN,
                TimeSheetConstants.WORK_DAY_DURATION * illnessService.getIllnessWorkdaysCount(employee, year, month)
        );

        if (form.getShowSumFundingType()) {
            appendNumberField(map, SUMMARY_INVESTMENT_PLAN, sumInvestPlan);
            appendNumberField(map, SUMMARY_COMMERCIAL_PLAN, sumCommercePlan);
        }

        appendStringField(map, SUMMARY_PLAN, formatSummaryPlan(summaryWorkHours + nonProjectDuration, summaryPlan));

        appendStringField(
                map,
                PERCENT_OF_CHARGE_PLAN,
                formatPercentOfCharge((summaryPlan != 0) ? sumOfPlanCharge / summaryPlan : 0D)
        );

        appendNumberField(map, VACATION_PLAN, vacationPlan);

        return map;
    }

    private Map<String, JsonNodeBuilder> getFacts(Employee employee, Integer year, Integer month, double summaryPlan, PlanEditForm form) {
        final Division division = employee.getDivision();
        final Map<Integer, Double> projectsFactMap = Maps.newHashMap();

        double summaryFact = 0;
        double centerProjectsFact = 0;
        double centerPresalesFact = 0;
        double otherProjectsFact = 0;
        double nonProjectFact = 0;
        Double sumProjectsFact = null;
        Double sumPresalesFact = null;
        Double sumInvestFact = null;
        Double sumCommerceFact = null;

        Integer projectId;

        for (TimeSheet timeSheet : timeSheetService.getTimeSheetsForEmployee(employee, year, month)) {
            for (TimeSheetDetail timeSheetDetail : timeSheet.getTimeSheetDetails()) {
                final double duration = nilIfNull(timeSheetDetail.getDuration());

                summaryFact += duration;

                final TypesOfActivityEnum actType =
                        EnumsUtils.getEnumById(timeSheetDetail.getActType().getId(), TypesOfActivityEnum.class);

                if (actType == TypesOfActivityEnum.NON_PROJECT) {
                    nonProjectFact += duration;
                }

                final Project project = timeSheetDetail.getProject();

                if (project != null) {
                    projectId = project.getId();

                    if (division.equals(project.getDivision())) {
                        if (isPresale(project)) {
                            centerPresalesFact += duration;
                        } else {
                            centerProjectsFact += duration;
                        }
                    } else {
                        otherProjectsFact += duration;
                    }

                    /* расчёт итого по проектам/пресейлам */
                    if (isProject(project)) {
                        sumProjectsFact = nilIfNull(sumProjectsFact) + duration;
                    } else {
                        sumPresalesFact = nilIfNull(sumPresalesFact) + duration;
                    }

                    /* расчёт итого по инвест/комерц проектам */
                    if (isProject(project) && isCommercialProject(project)) {
                        sumCommerceFact = nilIfNull(sumCommerceFact) + duration;
                    }
                    if ( (isProject(project) && isInvestmentProject(project)) || (isPresale(project)) ) {
                        sumInvestFact = nilIfNull(sumInvestFact) + duration;
                    }


                    projectsFactMap.put(projectId, nilIfNull(projectsFactMap.get(projectId)) + duration);
                }
            }
        }

        summaryFact += TimeSheetConstants.WORK_DAY_DURATION * vacationService.getVacationsWorkdaysCount(
                employee, year, month,
                VacationStatusEnum.APPROVED);

        final Map<String, JsonNodeBuilder> map = Maps.newHashMap();

        for (Map.Entry<Integer, Double> entry : projectsFactMap.entrySet()) {
            appendNumberField(map, String.format("%d" + _FACT, entry.getKey()), entry.getValue());
        }

        appendNumberField(map, SUMMARY_FACT, summaryFact);

        appendStringField(
                map,
                PERCENT_OF_CHARGE_FACT,
                formatPercentOfCharge((summaryPlan != 0) ? summaryFact / summaryPlan : 0D)
        );

        appendNumberField(map, CENTER_PROJECTS_FACT, centerProjectsFact);

        appendNumberField(map, CENTER_PRESALES_FACT, centerPresalesFact);

        appendNumberField(map, OTHER_PROJECTS_AND_PRESALES_FACT, otherProjectsFact);

        appendNumberField(map, NON_PROJECT_FACT, nonProjectFact);

        if (form.getShowSumProjectsPresales()) {
            appendNumberField(map, SUMMARY_PROJECTS_FACT, sumProjectsFact);
            appendNumberField(map, SUMMARY_PRESALES_FACT, sumPresalesFact);
        }

        if (form.getShowSumFundingType()) {
            appendNumberField(map, SUMMARY_INVESTMENT_FACT, nilIfNull(sumInvestFact) + nilIfNull(nonProjectFact));
            appendNumberField(map, SUMMARY_COMMERCIAL_FACT, sumCommerceFact);
        }

        appendNumberField(
                map,
                ILLNESS_FACT,
                TimeSheetConstants.WORK_DAY_DURATION * illnessService.getIllnessWorkdaysCount(employee, year, month)
        );

        appendNumberField(
                map,
                VACATION_FACT,
                TimeSheetConstants.WORK_DAY_DURATION * vacationService.getVacationsWorkdaysCount(
                        employee, year, month,
                        VacationStatusEnum.APPROVED
                )
        );

        return map;
    }

    private String formatPercentOfCharge(double normalizedValueOfCharge) {
        return String.format("%d" + "%%", Math.round(normalizedValueOfCharge * 100));
    }

    private String formatSummaryPlan(double sumPlan, double monthPlan) {
        return String.format("%d/%d", Math.round(sumPlan), Math.round(monthPlan));
    }

    private String getFieldNameForEmployeePlan(EmployeePlan employeePlan) {
        for (Map.Entry<JsonStringNode, TSEnum> entry : PLAN_TYPE_MAP.entrySet()) {
            if (entry.getValue() == EnumsUtils.getEnumById(employeePlan.getType(), EmployeePlanType.class)) {
                return entry.getKey().getText();
            }
        }

        throw new IllegalArgumentException();
    }

    private void appendNumberField(Map<String, JsonNodeBuilder> map, String fieldName, Double value) {
        if (value != null) {
            map.put(fieldName, aNumberBuilder(Math.round(value)));
        }
    }

    private void appendStringField(Map<String, JsonNodeBuilder> map, String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(fieldName, aStringBuilder(value));
        }
    }

    private List<Integer> getRegionIds(PlanEditForm form) {
        final List<Integer> regions = form.getRegions();

        if (regions.contains(PlanEditForm.ALL_VALUE)) {
            return Arrays.asList(EmployeeDAO.ALL_REGIONS);
        }

        return regions;
    }

    private List<Integer> getProjectRoleIds(PlanEditForm form) {
        final List<Integer> projectRoles = form.getProjectRoles();

        if (projectRoles.contains(PlanEditForm.ALL_VALUE)) {
            return Arrays.asList(EmployeeDAO.ALL_PROJECT_ROLES);
        }

        return projectRoles;
    }

    @RequestMapping(value = PLAN_SAVE_URL, method = RequestMethod.POST)
    public String save(
            @ModelAttribute(PlanEditForm.FORM) PlanEditForm form,
            HttpServletResponse response
    ) {
        try {
            final JsonRootNode rootNode = JsonUtil.parse(form.getJsonData());
            final Integer year = JsonUtil.getDecNumberValue(rootNode, JSON_DATA_YEAR);
            final Integer month = JsonUtil.getDecNumberValue(rootNode, JSON_DATA_MONTH);

            planEditService.savePlans(rootNode, year, month);
        } catch (InvalidSyntaxException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        saveCookie(form, response);
        return "redirect:" + PLAN_EDIT_URL;
    }

    public Double getEmployeeProjectDurationPlan(Employee employee, Integer year, Integer month) {
        Double duration = 0.0;
        for (EmployeeProjectPlan employeeProjectPlan : employeeProjectPlanService.find(employee, year, month)) {
            duration += nilIfNull(employeeProjectPlan.getValue());
        }
        return duration;
    }

    public Double getEmployeeNonProjectDuration(Employee employee, Integer year, Integer month) {
        Double duration = 0.0;
        for (EmployeePlan employeePlan : employeePlanService.find(employee, year, month)) {
            if (employeePlan.getType()!= null && employeePlan.getType().getId().equals(EmployeePlanType.NON_PROJECT.getId())) {
                duration += nilIfNull(employeePlan.getValue());
            }
        }
        return duration;
    }

}
